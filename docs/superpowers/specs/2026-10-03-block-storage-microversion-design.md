# openstack4j 후속 포크 — D. Cinder(block storage) v3 microversion 프레임워크와 누락 API 설계

- 작성일: 2026-10-03
- 상태: 확정(사용자 지시 "질문 없이 권장안으로 진행"에 따라 작성자가 결정하고 근거를 기록)
- 대상 버전: 4.3.0 (기능 추가, 하위 호환)
- 선행: A. 기반(4.0.0), B. Placement(4.1.0), C. Nova microversion(4.2.0), 서비스별 지원 현황 분석(`docs/superpowers/analysis/2026-10-02-api-coverage-gap-analysis.md`)

## 1. 배경과 목적

openstack4j 의 block storage 모듈은 Cinder microversion 헤더를 보내지 않아 모든 요청이 **3.0**(= v2 와 기능 동일) 으로 동작한다. 3.0 이후 추가된 응답 필드(group_id, provider_id, shared_targets, volume_type_id, consumes_quota, encryption_key_id 등)와 엔드포인트(attachments, messages, clusters, groups, group types/snapshots, default types, resource filters, volume summary, workers cleanup, 새 transfers 등)를 쓸 수 없다. microversion 과 무관하게 3.0 부터 있던 API(qos-specs, type extra specs/access, metadata CRUD, hosts, capabilities, manageable volumes/snapshots, backup export/import, services actions, quota classes 등)도 대부분 없다. 경로 단위 커버리지는 22% 다.

이 하위 프로젝트는 ① C 에서 만든 microversion 프레임워크를 **서비스 공용**으로 일반화해 block storage 에 opt-in 으로 적용하고, ② 3.0~3.71 의 응답 변화를 기존 모델이 모두 읽게 하며, ③ Cinder v3 api-ref 의 누락 엔드포인트와 액션을 채운다.

### 제약

- **기존 코드는 유지하고 추가만 한다.** 기존 메서드의 시그니처·반환 타입·기본 동작(microversion 을 켜지 않았을 때)은 바꾸지 않는다. `@Deprecated` 도 붙이지 않는다. 기존 block storage 테스트(core-test `api/storage/*`)는 수정 없이 통과해야 한다.
- 레거시 경로(`/os-volume-transfer`)를 제거하지 않는다. Cinder 가 deprecated 로 표시한 consistency groups(`/consistencygroups`, `/cgsnapshots`) 는 새로 추가하지 않는다.
- 회사 코드는 참고·복사하지 않는다. Cinder api-ref(`api-ref/source/v3/*.inc`), `cinder/api/openstack/rest_api_version_history.rst`, 요청 스키마(`cinder/api/schemas/*.py`)를 기준으로 구현한다.
- C 의 compute 코드는 **내부 구현만** 공용 유틸을 쓰도록 바꾼다. `BaseComputeServices` 의 protected API(`capped`, `requireMicroVersion`, `effectiveMicroVersion`, `isMicroVersionAtLeast`, `classCeiling`)와 `ComputeMicroVersionService`/`ComputeVersion` 의 공개 API 는 그대로 둔다. 기존 compute microversion 테스트가 그 증거다.

### 참고 수치

- Cinder 최대 microversion: Hibiscus(2026.2) **3.71** (2024.1 이후 변화 없음, `cinder/api/openstack/api_version_request.py` 의 `_MAX_API_VERSION = "3.71"`). 개발용 OpenStack(epoxy)도 **3.71**. 최소 3.0.
- 헤더: `OpenStack-API-Version: volume <v>` 하나. Nova 와 달리 레거시 전용 헤더가 없다.
- 개발용 catalog: `volumev3` → `http://…:8776/v3/<project>`, `block-storage` → `http://…:8776/v3`. 라이브러리는 catalog 순서상 첫 번째로 일치하는 `volumev3`(이름 패턴 `volume[v\d.]*`)을 고른다(`ServiceType.forName("block-storage")` 는 UNKNOWN). 3.67 부터 경로의 project id 는 선택이지만, 라이브러리는 catalog 가 주는 URL 을 그대로 쓴다.
- 현재 block storage: 서비스 8(volumes, snapshots, backups, transfer, quotaSets, services, zones, schedulerStatsPools) + `getLimits()`. 경로 21개 구현, 73개 누락.

## 2. 결정 사항

| 항목 | 결정 | 이유 |
|---|---|---|
| 구조 | 기존 `os.blockStorage()` 를 microversion 인식형으로 확장, 기본은 꺼짐(opt-in) | C 와 같은 모델. 사용자가 두 서비스를 같은 방식으로 다룬다 |
| 공용화 | C 의 compute 전용 코드(`ComputeMicroVersions`, `BaseComputeServices` 의 헤더·상한·하한 로직, `ComputeMicroVersionServiceImpl`)를 **`org.openstack4j.openstack.internal.microversion.MicroVersionSupport`** 로 일반화하고 compute 와 block storage 가 함께 쓴다 | 세 번째 서비스(Manila)까지 같은 코드를 복사하면 유지가 어렵다. compute 테스트가 리팩터링을 보호한다 |
| 공개 인터페이스 공용화 | `org.openstack4j.common.MicroVersionService<V extends MicroVersionInfo>`(negotiate/use/clear/get)와 `org.openstack4j.model.common.MicroVersionInfo` 를 추가하고, 기존 `ComputeMicroVersionService extends MicroVersionService<ComputeVersion>`, `ComputeVersion extends MicroVersionInfo` 로 바꾼다 | 추가만 하는 변경(상위 타입 도입)이라 소스·바이너리 호환. block storage 는 `BlockStorageMicroVersionService extends MicroVersionService<BlockStorageVersion>` |
| 기본 동작 | 4.x 내내 opt-in | 3.53(생성 스키마 엄격 검증)·3.66(스냅샷 force) 에서 기존 호출이 깨질 수 있다 |
| 기존 API 보호 | 메서드별 **상한(ceiling)** — 2곳뿐(3.3절) | Cinder v3 는 거의 추가만 했다 |
| 새 기능 | 메서드·옵션별 **하한(floor)** 검사 | C 와 동일 |
| 라이브러리 최고 버전 | **3.71** | Hibiscus Cinder 최대, 개발용과 같아 전부 실환경 검증 가능 |
| 새 transfers 경로 | `transfers()`(기존, `/os-volume-transfer`) 는 유지. 새 `volumeTransfers()` 가 `/volume-transfers`(3.55+) | 두 경로는 응답이 같지만 새 경로만 `no_snapshots`·페이지네이션을 지원한다 |
| 테스트 catalog | 기존 테스트 token 의 `volumev2` 항목을 바꾸지 않는다. D 테스트는 `AbstractTest` 의 새 hook 으로 token JSON 의 `8776/v2/` 를 `8776/v3/`, `volumev2` 를 `volumev3` 로 바꿔 쓴다 | 기존 테스트의 `/v[12]/` 경로 검사가 그대로 통과해야 한다 |
| 버전 | **4.3.0** | 기존 동작 불변, 기능 추가 |

## 3. microversion 프레임워크

### 3.1 공용 유틸 (`org.openstack4j.openstack.internal.microversion`)

기존(C): `MicroVersionState`, `MicroVersionStore`, `MicroVersions`, `MicroVersionException`.

추가:

- **`MicroVersionSupport`** — 서비스 하나의 microversion 정책과 세션 상태 접근을 묶는다. 생성자 인자: `ServiceType`, 상태 키 접두사(`"compute"`, `"volume"`), 최소·최고 버전, 헤더 이름 목록(헤더 이름 → 값 형식; compute 는 `OpenStack-API-Version: compute <v>` + `X-OpenStack-Nova-API-Version: <v>`, volume 은 `OpenStack-API-Version: volume <v>`), 루트 조회(`Supplier<VersionRange>`, 서버 min/max 를 돌려주며 microversion 미지원 서버면 `null`).
  - `MicroVersionState currentState()` — `MicroVersionStore.hasAny()` 빠른 경로 포함(C 리뷰 수정 유지).
  - `MicroVersion effective(MicroVersion classCeiling, MicroVersion ceiling)` — `min(pinned ?: min(LATEST, serverMax), classCeiling, ceiling)`, 꺼져 있으면 `null`.
  - `<R> Invocation<R> apply(Invocation<R> inv, MicroVersion v)` — 헤더 설정.
  - `void require(String feature, MicroVersion floor, MicroVersion effective, String enableHint)` — 예외 메시지 생성.
  - `MicroVersionInfo negotiate()`, `use(String)`, `clear()`, `get()` — 세션 상태 생성·고정·해제·조회. 상태 생성 시 한 번만 루트를 조회한다.
- **`BaseMicroVersionService`**(선택적 편의 클래스)는 만들지 않는다. 각 서비스의 `Base*Services` 가 `MicroVersionSupport` 인스턴스(static final)를 두고 `decorate`/`capped`/`requireMicroVersion` 을 위임한다. compute 는 기존 protected 메서드 본문만 위임으로 바꾼다.

### 3.2 block storage 적용

**`BlockStorageService.microVersions()`** → `BlockStorageMicroVersionService extends MicroVersionService<BlockStorageVersion>`
- `negotiate()`: `GET <block storage endpoint 루트>/` 에서 `versions[]` 중 `id` 가 `v3` 로 시작하는 항목의 `min_version`·`version` 을 읽어 적용 버전 = min(3.71, 서버 max). 루트 = endpoint 에서 `/v3(/.*)?$` 를 제거한 URL(예: `http://host:8776/v3/<project>` → `http://host:8776`, `https://cloud/volume/v3` → `https://cloud/volume`). 조회 요청은 `BaseOpenStackService` 를 직접 상속한 `BlockStorageVersionDiscovery` 가 보내므로 헤더가 붙지 않는다. v3 항목이 없거나 `version` 이 비어 있으면 `MicroVersionException`.
- `use(String)`: 범위 `[max(3.0, 서버 min), min(3.71, 서버 max)]` 검사 후 고정.
- `clear()`, `get()`: C 와 같다. `BlockStorageVersion extends MicroVersionInfo` (`getServerMinVersion/getServerMaxVersion/getMicroVersion/isPinned/isEnabled`).
- 상태 키: `"volume|" + session.getEndpoint(BLOCK_STORAGE)`.

**`BaseBlockStorageServices`**
- 켜져 있으면 모든 요청에 `OpenStack-API-Version: volume <v>` 를 붙인다. 꺼져 있으면 아무 헤더도 붙이지 않고 상태 조회도 하지 않는다(`hasAny()` 빠른 경로).
- `protected <R> Invocation<R> capped(Invocation<R> inv, MicroVersion ceiling)`, `protected void requireMicroVersion(String feature, MicroVersion floor)`, `protected MicroVersion effectiveMicroVersion(MicroVersion ceiling)`, `protected boolean isMicroVersionAtLeast(MicroVersion)` — compute 와 같은 이름·의미. 꺼져 있을 때 `requireMicroVersion` 의 안내 문구는 `os.blockStorage().microVersions().negotiate()`.
- `BlockStorageMicroVersions`: `MINIMUM`(3.0), `LATEST`(3.71), `V(int minor)`, `rootUrl(String)`, `SUPPORT`(공용 인스턴스).

### 3.3 메서드별 상한 (기존 API 보호)

| 상한 | 대상 | 근거 |
|---|---|---|
| 3.52 | `volumes().create(volume)` 에 `bootable` 이 설정된 경우 | 3.53 부터 생성 본문이 `additionalProperties: false` 이고 `bootable` 은 생성 스키마에 없다(그 전에는 무시됨) |
| 3.65 | `snapshots().create(snapshot)` 에 `force == false` 가 설정된 경우 | 3.66 부터 `force` 가 invalid 이며 true 만 조용히 무시된다 |

그 밖의 기존 메서드는 3.71 까지 요청·응답 형식이 유지된다(스키마 확인: volume update 는 3.53 부터 필드 1개 이상 필요 — 기존 `update(name, description)` 는 둘 다 `null` 이면 어차피 무의미하므로 상한을 두지 않는다; backup create 의 `incremental/force` 기본값 false 전송은 모든 버전에서 유효; `multiattach` 생성 파라미터는 버전과 무관하게 Cinder 가 400 으로 거부하므로 Javadoc 에만 적는다).

구현 중 추가로 발견되는 제거·검증 지점은 같은 방식으로 상한을 붙이고 이 표를 갱신한다.

## 4. 모델

### 4.1 원칙
- 기존 모델 클래스와 getter 유지. **필드 추가**만 한다. 낮은 버전이 주지 않는 필드는 `null`. 인터페이스에는 `default` getter 로 추가한다(외부 구현체 호환).
- 요청 모델(빌더)에도 추가만 한다. 새 옵션은 하한을 검사한다.

### 4.2 추가 필드 (모두 nullable)

- **Volume**: `user_id`, `updated_at`, `replication_status`, `consistencygroup_id`, `migration_status`, `os-vol-mig-status-attr:name_id`, `links`, `group_id`(3.13), `provider_id`(3.21, admin), `shared_targets`(3.48; 3.69 부터 tristate — `Boolean`), `service_uuid`(3.48), `cluster_name`(3.61, admin), `volume_type_id`(3.63), `encryption_key_id`(3.64), `consumes_quota`(3.65). 기존 `getMigrateStatus()` 는 유지.
- **VolumeSnapshot**: `updated_at`, `os-extended-snapshot-attributes:project_id`, `os-extended-snapshot-attributes:progress`, `group_snapshot_id`(3.14), `user_id`(3.41), `consumes_quota`(3.65).
- **VolumeBackup**: `updated_at`, `data_timestamp`, `links`, `os-backup-project-attr:project_id`(3.18), `metadata`(3.43), `user_id`(3.56), `encryption_key_id`(3.64).
- **VolumeType**: `description`, `is_public`, `os-volume-type-access:is_public`, `qos_specs_id`.
- **VolumeTransfer**: `source_project_id`, `destination_project_id`, `accepted`(3.57), `no_snapshots`(3.55).
- **Service**(ext): `cluster`(3.7), `replication_status`, `active_backend_id`, `frozen`(3.26), `backend_state`(3.49).
- **BlockQuotaSet**: `backups`, `backup_gigabytes`, `per_volume_gigabytes`, `groups`. 타입별 키(`volumes_<type>`, `snapshots_<type>`, `gigabytes_<type>`)는 기존 `getVolumeTypesQuotas()` 가 받는다(Cinder 는 이 세 종류에만 타입별 quota 가 있다). 빌더에도 같은 네 필드를 추가한다.
- **BlockLimits**: 변경 없음. api-ref 의 absolute limits 10개가 모두 이미 있다.

### 4.3 요청 모델(빌더) 확장과 하한

- `VolumeBuilder`: `groupId(String)`(3.13), `backupId(String)`(3.47), `imageId(String)`, `consistencyGroupId(String)`, `sourceVolumeId(String)`, `schedulerHints(Map)`(`OS-SCH-HNT:scheduler_hints`, 하한 없음), `volumeTypeId` 는 없음(생성은 이름/ID 를 `volume_type` 에 넣는다).
- `VolumeSnapshotBuilder`: 변경 없음(`force` 는 상한으로 처리).
- `VolumeBackupCreateBuilder`: `metadata(Map)`(3.43), `availabilityZone(String)`(3.51).
- 목록 옵션(C 의 `ServerListOptions` 와 같은 fluent 클래스, `toQueryParams()`·`getRequiredMicroVersion()`):
  - `VolumeListOptions`: `name`, `status`, `bootable`, `allTenants`, `limit`, `marker`, `offset`, `sortKey`/`sortDir`/`sort`, `metadata`, `glanceMetadata`(3.4), `groupId`(3.10), `withCount`(3.45), `createdAt`/`updatedAt` 비교 필터(3.60), `consumesQuota`(3.65). `name~` 류 like 필터는 3.34 — `nameLike(String)` 로 제공.
  - `SnapshotListOptions`: `name`, `status`, `volumeId`, `allTenants`, `limit`, `marker`, `offset`, `sortKey`/`sortDir`, `metadata`(3.22), `withCount`(3.45), `consumesQuota`(3.65), `nameLike`(3.34).
  - `BackupListOptions`: `name`, `status`, `volumeId`, `allTenants`, `limit`, `marker`, `offset`, `sortKey`/`sortDir`(name 정렬 3.37), `withCount`(3.45), `nameLike`(3.34).
  - `MessageListOptions`(3.3): `limit`/`marker`/`offset`/`sort`(3.5), `resourceType`, `resourceUuid`, `eventId`, `requestId`, `messageLevel`.
  - `ClusterListOptions`(3.7): `name`, `binary`, `isUp`, `disabled`, `numHosts`, `numDownHosts`, `replicationStatus`/`frozen`/`activeBackendId`(3.26).
  - `AttachmentListOptions`(3.27): `allTenants`, `instanceId`, `volumeId`, `status`, `limit`, `marker`, `offset`, `sort`.
  - `GroupListOptions`, `GroupSnapshotListOptions`(3.29 필터·정렬·페이지네이션), `TransferListOptions`(3.59 `limit`/`marker`/`offset`/`sort`, `allTenants`).
  - `PoolListOptions`(3.28 임의 capability 필터, `volumeType`(3.35)).
  - `VolumeTypeListOptions`: `isPublic`, `extraSpecs`(3.52), `sort`, `limit`, `marker`, `offset`.
  - `ManageableListOptions`: `host` 또는 `cluster`(3.17), `limit`, `marker`, `offset`, `sort`.

## 5. 누락 엔드포인트와 액션

### 5.1 기존 서비스에 메서드 추가

| 서비스 | 메서드(하한) | 엔드포인트 |
|---|---|---|
| `volumes()` | `list(VolumeListOptions)`, `summary()`/`summary(VolumeListOptions)`(3.12; 3.36 metadata) | `GET /volumes/detail`, `GET /volumes/summary` |
| | `get(id)` 기존 유지; `update(id, VolumeUpdate)`(name, description, metadata) | `PUT /volumes/{id}` |
| | `metadata(id)`, `setMetadata(id, Map)`(POST 추가), `replaceMetadata(id, Map)`(PUT), `metadataItem(id, key)`, `updateMetadataItem(id, key, value)`, `deleteMetadataItem(id, key)` | `/volumes/{id}/metadata[/{key}]` |
| | `imageMetadata(id)`, `setImageMetadata(id, Map)`, `unsetImageMetadata(id, key)` | action `os-show_image_metadata`/`os-set_image_metadata`/`os-unset_image_metadata` |
| | `revertToSnapshot(id, snapshotId)`(3.40), `reimage(id, imageId, reimageReserved)`(3.68), `completeExtend(id, errorFlag)`(3.71, `os-extend_volume_completion`), `retype(id, newType, migrationPolicy)`, `migrate(id, VolumeMigrateRequest)`(host 또는 cluster 3.16, `force_host_copy`, `lock_volume`), `completeMigration(id, newVolumeId, error)`, `unmanage(id)`, `reserve(id)`, `unreserve(id)`, `beginDetaching(id)`, `rollDetaching(id)`, `initializeConnection(id, connector)`(`Map` 반환), `terminateConnection(id, connector)`, `setStatus(id, status, attachStatus, migrationStatus)`(`os-reset_status` 확장), `uploadToImage(id, UploadImageData)` 기존 유지 + `UploadImageData` 에 `protected`/`visibility`(3.1) | `POST /volumes/{id}/action` |
| | `extend(id, size)` 기존 유지(3.42 부터 in-use 허용은 서버 측) | |
| `snapshots()` | `list(SnapshotListOptions)`, `listDetail`(기존 `list()` 는 `/snapshots` 요약) → `listDetail(SnapshotListOptions)` | `GET /snapshots/detail` |
| | `metadata(id)`, `setMetadata`, `replaceMetadata`, `metadataItem`, `updateMetadataItem`, `deleteMetadataItem` | `/snapshots/{id}/metadata[/{key}]` (`GET /snapshot/{id}/metadata/{key}` 의 단수 경로는 api-ref 오타; 실제 Cinder 라우트는 `/snapshots/...`) |
| | `resetStatus(id, status)`, `forceDelete(id)`, `updateStatus(id, status, progress)`(`os-update_snapshot_status`), `unmanage(id)` | `POST /snapshots/{id}/action` |
| `backups()` | `list(BackupListOptions)`, `update(id, name, description)`(3.9), `exportRecord(id)`, `importRecord(service, url)`, `forceDelete(id)`, `resetStatus(id, status)` | `/backups/...`, `/backups/{id}/export_record`, `/backups/import_record`, `/backups/{id}/action` |
| `transfers()`(기존) | 변경 없음 | `/os-volume-transfer` |
| `quotaSets()` | 기존 메서드 유지(Cinder 는 user 별 quota 가 없으므로 그 방향의 추가는 없다); `quotaClass(className)`, `updateQuotaClass(className, BlockQuotaSet)` | `GET/PUT /os-quota-class-sets/{class}` |
| `services()` | `enable(host, binary)`, `disable(host, binary)`, `disableWithReason(host, binary, reason)`, `freeze(host)`, `thaw(host)`, `failoverHost(host, backendId)`, `failover(host 또는 cluster, backendId)`(3.26), `setLog(ServiceLogRequest)`(3.32), `getLog(server, binary, prefix)`(3.32) | `PUT /os-services/...` |
| `schedulerStatsPools()` | `pools(PoolListOptions)`, `poolsDetail(PoolListOptions)` | `GET /scheduler-stats/get_pools` |
| `volumeTypes` (기존 `volumes().listVolumeTypes()` 등 유지) | 새 accessor `volumeTypes()` → `BlockVolumeTypeService`: `list(VolumeTypeListOptions)`, `get(id)`, `getDefault()`, `create(VolumeType)`, `update(id, name, description, isPublic)`, `delete(id)`, `extraSpecs(id)`, `setExtraSpecs(id, Map)`, `extraSpec(id, key)`, `updateExtraSpec(id, key, value)`, `deleteExtraSpec(id, key)`, `addProjectAccess(id, projectId)`, `removeProjectAccess(id, projectId)`, `listProjectAccess(id)`, `encryption(id)`, `encryptionSpec(id, key)`, `createEncryption(id, spec)`, `updateEncryption(id, encryptionId, spec)`, `deleteEncryption(id, encryptionId)` | `/types[/default|/{id}[/extra_specs[/{key}]|/action|/os-volume-type-access|/encryption[/{key}]]]` |
| `getLimits()` | `getLimits(projectId)`(3.39, admin) | `GET /limits?project_id=` |

### 5.2 새 서비스 (`BlockStorageService` accessor 추가)

| accessor | 하한 | 엔드포인트 |
|---|---|---|
| `attachments()` | 3.27 (`complete` 3.44, `mode` 3.54) | `GET /attachments[/detail|/{id}]`, `POST /attachments`, `PUT /attachments/{id}`, `DELETE /attachments/{id}`, `POST /attachments/{id}/action` (`os-complete`) |
| `messages()` | 3.3 (페이지네이션 3.5) | `GET /messages[/{id}]`, `DELETE /messages/{id}` |
| `clusters()` | 3.7 (3.26 필드·필터) | `GET /clusters[/detail|/{name}?binary=]`, `PUT /clusters/enable`, `PUT /clusters/disable` |
| `groups()` | 3.13 (`volumes` 필드 3.25, `project_id` 3.58) | `GET /groups[/detail|/{id}]`, `POST /groups`, `PUT /groups/{id}`, `POST /groups/action`(create-from-src 3.14), `POST /groups/{id}/action`(delete, reset_status 3.20, enable/disable/failover replication·list_replication_targets 3.38) |
| `groupTypes()` | 3.11 | `GET /group_types[/default|/{id}]`, `POST`, `PUT /group_types/{id}`, `DELETE`, `GET/POST /group_types/{id}/group_specs`, `GET/PUT/DELETE /group_types/{id}/group_specs/{key}` |
| `groupSnapshots()` | 3.14 (reset_status 3.19, 필터·페이지네이션 3.29) | `GET /group_snapshots[/detail|/{id}]`, `POST`, `DELETE`, `POST /group_snapshots/{id}/action` |
| `defaultTypes()` | 3.62 | `GET /default-types[/{project}]`, `PUT /default-types/{project}`, `DELETE /default-types/{project}` |
| `resourceFilters()` | 3.33 | `GET /resource_filters[?resource=]` |
| `workers()` | 3.24 | `POST /workers/cleanup` |
| `volumeTransfers()` | 3.55 (3.57 필드, 3.59 페이지네이션, 3.70 암호화 볼륨) | `/volume-transfers[/detail|/{id}|/{id}/accept]` |
| `manageableVolumes()` | 3.8 (cluster 3.17) | `GET /manageable_volumes[/detail]`, `POST /manageable_volumes` (manage), `volumes().unmanage(id)` 와 짝 |
| `manageableSnapshots()` | 3.8 | `GET /manageable_snapshots[/detail]`, `POST /manageable_snapshots` |
| `qosSpecs()` | 없음 | `GET /qos-specs[/{id}]`, `POST`, `PUT /qos-specs/{id}`, `DELETE /qos-specs/{id}?force=`, `PUT /qos-specs/{id}/delete_keys`, `GET /qos-specs/{id}/associations`, `GET /qos-specs/{id}/associate?vol_type_id=`, `GET /qos-specs/{id}/disassociate?vol_type_id=`, `GET /qos-specs/{id}/disassociate_all` |
| `hosts()` | 없음 (admin) | `GET /os-hosts`, `GET /os-hosts/{host}` |
| `capabilities()` | 없음 (admin) | `GET /capabilities/{host}` |
| `extensions()` | 없음 | `GET /extensions` |

### 5.3 범위 밖
- deprecated consistency groups(`/consistencygroups`, `/cgsnapshots`)와 Cinder v2 전용 경로.
- `/os-volume-manage`, `/os-snapshot-manage` 레거시 경로(3.8 의 `manageable_*` 로 대체) — 새로 추가하지 않는다.
- Cinder 외 서비스, Placement 내부의 공용 유틸 이전.

## 6. 테스트

### 6.1 단위 테스트 (core-test)
- 기존 block storage 테스트 8 클래스는 **변경 없이** 통과해야 한다. 기존 compute microversion 테스트(`api/compute/microversion/*`)도 변경 없이 통과해야 한다(공용화 리팩터링의 증거).
- `AbstractTest` 에 `protected String adjustTokenJson(String json)`(기본: 그대로) hook 을 추가하고 `osv3()` 가 host 치환 뒤 호출한다. 새 테스트의 기반 클래스 `AbstractBlockStorageMicroVersionTest` 가 `8776/v2/` → `8776/v3/`, `"volumev2"` → `"volumev3"` 로 바꿔 `/v3/<project>/...` 경로를 쓴다.
- 새 테스트는 `core-test/src/main/java/org/openstack4j/api/storage/microversion/` 에 둔다. 항목: 협상(서버 max 3.71/3.40/v3 없음)·고정·범위 검사·헤더 1종·꺼진 상태에서 헤더·루트 조회 없음, 상한 2곳, 하한(꺼짐·부족 시 요청 없이 예외), 3.71 응답 모델(volume/snapshot/backup/type/transfer/service), 새 엔드포인트별 요청·응답, 목록 옵션의 쿼리 인코딩과 하한, 액션 본문.
- fixture 는 개발용 epoxy(Cinder 3.71)의 실제 응답으로 만든다(`scratchpad/cinder/*.json` 캡처본: volume/snapshot/attachment/types/services/limits/pools/hosts/resource_filters/group_types/extensions/az/volume-summary 등). 개발용에 없는 자원(clusters, groups, messages, qos, default types 설정값)은 api-ref 예시로 보강한다.

### 6.2 실환경 통합 테스트 (`BlockStorageLiveTests`, 환경 변수 있을 때만)
협상 결과 3.71 확인 → 타입 목록·기본 타입·resource filters·pools·services·limits(읽기) → 임시 1 GB 볼륨 생성(`consumes_quota`, `volume_type_id` 확인) → metadata 설정·조회 → 스냅샷 생성(`user_id`) → 스냅샷 metadata → attachment 생성(instance 없이 `reserved`)·조회·삭제 → volume summary → 삭제. 기존 자원은 읽기만 한다. 임시 자원은 `finally` 에서 삭제한다.

## 7. 작업 순서

| # | 작업 |
|---|---|
| 1 | 공용 `MicroVersionSupport`·`MicroVersionService`·`MicroVersionInfo`, compute 가 이를 쓰도록 내부 리팩터링(기존 compute 테스트 불변) |
| 2 | block storage 적용: `microVersions()`, 헤더, `capped`/`requireMicroVersion`, 상한 2곳, 테스트 token hook |
| 3 | 응답 모델 필드(volume/snapshot/backup/type/transfer/service/quota) |
| 4 | 목록 옵션 클래스와 `list(options)`/`summary`, 빌더 확장(volume create 3.13/3.47, backup create 3.43/3.51, upload image 3.1) |
| 5 | volume 액션 보강(revert, reimage, extend completion, retype, migrate, metadata/image metadata, reserve/detaching/connection) |
| 6 | snapshots·backups 보강(metadata, actions, update, export/import) |
| 7 | attachments(3.27), messages(3.3) |
| 8 | volume types 서비스(extra specs, access, encryption, default), default-types(3.62), qos-specs |
| 9 | groups·group types·group snapshots(3.11~3.14, 3.38) |
| 10 | clusters, services actions, workers cleanup, hosts, capabilities, resource filters, extensions, limits(project), quota classes, pools 옵션 |
| 11 | volume-transfers(3.55), manageable volumes/snapshots(3.8) |
| 12 | 실환경 통합 테스트, README block storage 절, MIGRATION/CHANGELOG |
| 13 | 전체 리뷰 → 4.3.0 릴리스 → 4.4.0-SNAPSHOT |

## 8. 완료 기준

1. microversion 을 켜지 않은 기존 block storage 테스트와 기존 compute microversion 테스트가 전부 변경 없이 통과한다.
2. 3.71 을 켠 상태에서 3.3 의 두 레거시 호출이 상한 덕분에 계속 동작한다(단위 테스트로 헤더 값 확인).
3. 4·5장의 모든 필드·메서드가 구현되고 단위 테스트가 세 connector 에서 통과한다.
4. 실환경 테스트가 개발용 OpenStack(epoxy, Cinder 3.71)에서 통과하고 임시 자원이 남지 않는다.
5. `io.github.seogineer:openstack4j:4.3.0` 이 Maven Central 에 배포된다.
