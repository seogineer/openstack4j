# openstack4j 후속 포크 — C. Nova(compute) microversion 프레임워크와 누락 API 설계

- 작성일: 2026-10-02
- 상태: 사용자 검토 대기
- 대상 버전: 4.2.0 (기능 추가, 하위 호환)
- 선행: A. 기반(4.0.0), B. Placement(4.1.0), 서비스별 지원 현황 분석(`docs/superpowers/analysis/2026-10-02-api-coverage-gap-analysis.md`)

## 1. 배경과 목적

openstack4j 의 compute 모듈은 Nova microversion 헤더를 보내지 않아 모든 요청이 **2.1** 로 동작한다. 2.1 이후 추가된 응답 필드(tags, description, locked_reason, server_groups, pinned_availability_zone 등)와 엔드포인트(topology, server migrations, remote consoles, shares 등)를 쓸 수 없다.

이 하위 프로젝트는 ① compute 에 microversion 협상·고정 기능을 **opt-in** 으로 추가하고, ② 2.1~2.104(Hibiscus Nova 최대) 사이의 응답 형식 변화를 기존 모델이 모두 읽게 하며, ③ 누락 엔드포인트와 액션 옵션을 채운다.

### 제약

- **기존 코드는 유지하고 추가만 한다.** 기존 메서드의 시그니처·반환 타입·기본 동작(microversion 을 켜지 않았을 때)은 바꾸지 않는다. `@Deprecated` 도 붙이지 않는다.
- 레거시·410 API 를 제거하지 않는다. 다만 새로 추가하지도 않는다.
- 회사 코드는 참고·복사하지 않는다. Nova api-ref 와 `nova/api/openstack/rest_api_version_history.rst` 를 기준으로 구현한다.

### 참고 수치

- Nova 최대 microversion: Hibiscus(2026.2) **2.104**, Gazpacho 2.103, Epoxy(개발용 OpenStack) **2.100**. 최소 2.1.
- 현재 compute: 서비스 17 + 확장 8, microversion 헤더는 `force-down` 에 2.11 하드코딩 1곳.

## 2. 결정 사항

| 항목 | 결정 | 이유 |
|---|---|---|
| 구조 | 기존 `os.compute()` 서비스를 microversion 인식형으로 확장, 기본은 꺼짐(opt-in) | 단일 API 유지, 하위 호환. `compute.v2` 분리는 최대 모듈을 두 벌로 만든다 |
| 기본 동작 | 4.x 내내 opt-in. 자동 켜기는 5.0 에서 검토 | 2.36/2.43/2.47/2.53 등에서 기존 사용자 코드가 깨질 수 있다 |
| 기존 API 보호 | 메서드별 **상한(ceiling)**: 요청 버전 = min(적용 버전, 상한) | 사용자가 2.100 을 켜도 프록시 API 등이 계속 동작 |
| 새 기능 | 메서드·옵션별 **하한(floor)** 검사, 부족하면 요청 전에 예외 | Placement 와 같은 방식 |
| 라이브러리 최고 버전 | **2.104** | Hibiscus Nova 최대 |
| 공용화 | 협상·캐시·상한/하한·예외를 `openstack.internal` 공용 유틸로 만들고 compute 가 사용 | 이후 Cinder·Manila 재사용. Placement 내부 이전은 범위 밖 |
| 버전 | **4.2.0** | 기존 동작 불변, 기능 추가 |

## 3. microversion 프레임워크

### 3.1 공용 유틸 (`org.openstack4j.openstack.internal.microversion`)

- `MicroVersionNegotiator`: 서비스별 루트 조회 함수(`Supplier<Range>`)를 받아 세션(`OSClientSession` identity) + endpoint 별로 서버 범위를 캐시한다. `WeakHashMap`, 동기화. Placement 의 `PlacementSessionState` 와 같은 구조를 일반화한 것이다.
- `MicroVersionState`: `serverMin`, `serverMax`, `pinned`(선택), `enabled`.
- `MicroVersionPolicy`: 라이브러리 최고/최소 버전을 갖고 `effective(state, ceiling)` = `min(pinned ?: min(LATEST, serverMax), ceiling)` 를 계산한다.
- `MicroVersionException extends OS4JException` (`org.openstack4j.api.exceptions`). 기존 `PlacementMicroVersionException` 은 이 클래스를 상속하도록 바꾼다(부모 변경만, 기존 catch 코드 영향 없음).

### 3.2 compute 적용

**`ComputeService.microVersions()`** → `ComputeMicroVersionService`
- `ComputeVersion negotiate()`: `GET <compute endpoint 루트>/` (`/v2.1/`)에서 `min_version`·`version` 을 받아 적용 버전 = min(2.104, 서버 max). 세션에 켜진 상태로 저장.
- `ComputeVersion use(String version)`: 범위(`[max(2.1, 서버 min), min(2.104, 서버 max)]`) 검사 후 고정하고 켠다.
- `void clear()`: 끈다(헤더 없음, 현재 동작).
- `ComputeVersion get()`: `getServerMinVersion()`, `getServerMaxVersion()`, `getMicroVersion()`(꺼져 있으면 `null`), `isPinned()`, `isEnabled()`.

**`BaseComputeServices`**
- 켜져 있으면 모든 요청에 `OpenStack-API-Version: compute <v>` 와 `X-OpenStack-Nova-API-Version: <v>` 두 헤더를 붙인다. 꺼져 있으면 아무 헤더도 붙이지 않는다(기존 `force-down` 의 2.11 하드코딩은 그대로 둔다).
- `protected <R> Invocation<R> capped(Invocation<R> inv, MicroVersion ceiling)`: 켜져 있고 적용 버전이 상한보다 높으면 헤더를 상한 값으로 바꾼다.
- `protected void requireMicroVersion(String feature, MicroVersion floor)`: 꺼져 있으면 "`os.compute().microVersions().negotiate()` 로 microversion 을 켜야 한다"는 `MicroVersionException`, 켜져 있지만 부족하면 "X 는 compute microversion F 이상이 필요하지만 적용 버전은 V(서버 max M)" 예외. 서버 요청 전에 던진다.

### 3.3 메서드별 상한 (기존 API 보호)

| 상한 | 대상 | 근거 |
|---|---|---|
| 2.35 | `floatingIps()`, `securityGroups()`, `images()`, `floatingIPDNS()`, `servers().addFloatingIp/removeFloatingIp`, os-networks·os-tenant-networks·os-fixed-ips 류 | 2.36 프록시 API 404 |
| 2.38 | `images().getMetaData/setMetaData/…` | 2.39 image-metadata 프록시 404 |
| 2.42 | `host()` 전체 | 2.43 os-hosts 404 |
| 2.5 | `servers().getVNCConsole/getRDPConsole/getSerialConsole/getSpiceConsole`(action 기반) | 2.6 remote-consoles 로 대체 |
| 2.52 | `hypervisors().search/servers/uptime` 류 | 2.53 제거(쿼리 파라미터로 대체) |
| 2.55 | `servers().create()` 에 personality 가 있을 때 | 2.57 제거 |
| 2.87 | `hypervisors().statistics()` 및 detail 의 통계 필드 의존 메서드 | 2.88 제거 |
| 2.91 | `keypairs().create(name)`(공개키 없이 서버 생성) | 2.92 제거 |
| 2.43 | `servers().getVirtualInterfaces` 류(존재 시) | 2.44 제거 |

구현 중 Nova 이력에서 추가로 발견되는 제거 지점은 같은 방식으로 상한을 붙이고 spec 의 이 표를 갱신한다.

## 4. 모델

### 4.1 원칙
- 기존 모델 클래스와 getter 유지. **필드 추가**와 **두 형식 모두 읽기**만 한다. 낮은 버전이 주지 않는 필드는 `null`.
- 형식이 바뀌는 곳은 새 getter 로 노출한다.

### 4.2 형식 변화 처리

| 버전 | 변화 | 처리 |
|---|---|---|
| 2.47 | 서버 `flavor` 가 `{id, links}` → `{original_name, vcpus, ram, disk, ephemeral, swap, extra_specs}` | 커스텀 역직렬화로 두 형식을 `NovaFlavor` 에 채운다. `getFlavorId()` 는 id 없으면 `null`, `getFlavor()` 의 지연 조회는 id 가 있을 때만. 새 `getFlavorSummary()`(내장 값 그대로) |
| 2.98 | 서버 `image.properties` 추가 | 필드 추가 |
| 2.48 | diagnostics 표준 구조 | 기존 `ServerDiagnostics`(Map) 유지, 새 `ServerDiagnosticsStandard` + `servers().diagnosticsStandard(id)`(하한 2.48) |
| 2.45 | createImage/createBackup 응답이 `Location` 헤더 → 본문 `{"image_id"}` | 둘 다 읽어 이미지 id 반환 |
| 2.89 | 볼륨 attachment `id` 제거, `attachment_id`·`bdm_uuid` 추가 | 필드 추가, `getId()` null 허용 |
| 2.88 | 하이퍼바이저 통계 필드 제거 | null 허용 |
| 2.75 | 응답 정리(update/rebuild 전체 서버 반환 등) | 기존 모델 수용, 테스트로 확인 |
| 2.69 | 셀 장애 시 최소 필드 | null 허용 |

### 4.3 추가 필드 (모두 nullable)
- 서버: `OS-EXT-SRV-ATTR:*` 확장(hostname, reservation_id, launch_index, kernel_id, ramdisk_id, root_device_name, user_data)(2.3), `locked`(2.9), `host_status`(2.16), `description`(2.19), `tags`(2.26), `trusted_image_certificates`(2.63), `server_groups`(2.71), `locked_reason`(2.73), `pinned_availability_zone`(2.96), `scheduler_hints`(2.100)
- 플레이버: `description`(2.55), `extra_specs`(2.61)
- 키페어: `type`(2.2), `user_id`(2.10)
- 집계: `uuid`(2.41)
- 인스턴스 액션: `updated_at`(2.58), `host`·`hostId`(2.62), 이벤트 `details`(2.84)
- 마이그레이션: `migration_type`·`uuid`(2.23/2.59), `user_id`·`project_id`(2.80)
- 서비스: `forced_down`(2.11)
- 서버 그룹: `policy`·`rules`(2.64)

### 4.4 요청 모델(빌더) 확장과 하한
- 서버 생성: `tags`(2.52), `trustedImageCertificates`(2.63), `host`/`hypervisorHostname`(2.74), `hostname`(2.90), `autoAllocateNetwork()`/`noNetwork()`(2.37, `networks: "auto"|"none"`), BDM `tag`(2.42)·`volumeType`(2.67)·`deleteOnTermination`, 네트워크 `tag`(2.42). 옵션을 썼는데 적용 버전이 모자라면 요청 전에 `MicroVersionException`.
- 서버 목록 필터: `changes-before`(2.66), `locked`(2.73), 2.83 의 추가 필터(`availability_zone`, `config_drive`, `key_name`, `created_at`, `launched_at`, `terminated_at`, `power_state`, `task_state`, `vm_state`, `progress`, `user_id`).
- 2.101~2.104(BDM 경유 볼륨 연결, 플레이버 이름 필터, `pinned_availability_zone` 수정)는 옵션으로 포함하되 실환경 검증은 하지 않는다(개발용 서버가 2.100).

## 5. 누락 엔드포인트와 액션

### 5.1 기존 서비스에 메서드 추가

| 서비스 | 메서드(하한) | 엔드포인트 |
|---|---|---|
| `servers()` | `topology(id)`(2.78) | `GET /servers/{id}/topology` |
| | `ips(id)`, `ips(id, network)` | `GET /servers/{id}/ips[/{network}]` |
| | `remoteConsole(id, protocol, type)`(2.6; mks 2.8; spice-direct 2.99) | `POST /servers/{id}/remote-consoles` |
| | `migrations(id)`, `migration(id, mid)`, `forceCompleteMigration(id, mid)`(2.22), `abortMigration(id, mid)`(2.24) | `/servers/{id}/migrations[/{mid}[/action]]` |
| | `shares(id)`, `share(id, sid)`, `attachShare(id, req)`, `detachShare(id, sid)`(2.97) | `/servers/{id}/shares[/{sid}]` |
| | `diagnosticsStandard(id)`(2.48) | `GET /servers/{id}/diagnostics` |
| `hypervisors()` | `list(HypervisorListOptions)`: `hypervisor_hostname_pattern`·`with_servers`(2.53), `limit`·`marker`(2.33) | `GET /os-hypervisors[/detail]` |
| `services()` | `update(id, ServiceUpdate)`(2.53: status, disabled_reason, forced_down), `delete(id)`(2.53), `disableWithReason(host, binary, reason)` | `PUT/DELETE /os-services/{id}`, `PUT /os-services/disable-log-reason` |
| `hostAggregates()` | `cacheImages(id, imageIds)`(2.81) | `POST /os-aggregates/{id}/images` |
| `quotaSets()` | `defaults(tenantId)` | `GET /os-quota-sets/{id}/defaults` |
| `migrations()`(ext) | `list(MigrationListOptions)`: 기존 필터 + `changes-since`·`limit`·`marker`(2.59), `user_id`·`project_id`(2.80) | `GET /os-migrations` |
| `keypairs()` | `list(KeypairListOptions)`: `user_id`(2.10), `limit`·`marker`(2.35); `create(name, publicKey, type)`(2.2) | `/os-keypairs` |
| `serverGroups()` | `create(name, policy, rules)`(2.64) | `/os-server-groups` |

### 5.2 새 서비스 (`ComputeService` accessor 추가)

| accessor | 엔드포인트 |
|---|---|
| `serverExternalEvents()` | `POST /os-server-external-events` — network-changed, network-vif-plugged/unplugged/deleted, volume-extended(2.51), power-update(2.76), accelerator-request-bound(2.82) |
| `assistedVolumeSnapshots()` | `POST /os-assisted-volume-snapshots`, `DELETE /os-assisted-volume-snapshots/{id}` |
| `consoleAuthTokens()` | `GET /os-console-auth-tokens/{token}`(2.31) |
| `instanceUsageAuditLogs()` | `GET /os-instance_usage_audit_log[/{before}]` |

### 5.3 서버 액션 보강 (`POST /servers/{id}/action`)
lock with reason(2.73), unshelve to AZ(2.77)/host(2.91), migrate with host(2.56), live-migrate `block_migration: "auto"`(2.25)·`force` 제거(2.68), evacuate 변화(2.14 onSharedStorage 제거, 2.29 force, 2.68 force 제거, 2.95), rebuild 의 key_name(2.54)·user_data(2.57)·trusted certs(2.63)·hostname(2.90), rescue BFV(2.87), createImage/createBackup 본문 응답(2.45). 각 옵션은 하한을 검사한다.

### 5.4 범위 밖
- 410 API(os-cells, os-certificates, os-cloudpipe, os-fixed-ips, os-floating-ips-bulk, os-agents, os-fping, os-security-group-default-rules)와 2.6/2.44 에서 제거된 `servers/{id}/consoles`, `os-virtual-interfaces` 는 추가하지 않는다. 기존 레거시 메서드는 상한을 붙여 유지한다.
- Placement 내부의 공용 유틸 이전.
- compute 외 서비스.

## 6. 테스트

### 6.1 단위 테스트 (core-test)
- 기존 compute 테스트 12 클래스는 **변경 없이** 통과해야 한다(꺼진 기본 경로 불변의 증거).
- 새 테스트는 `core-test/src/main/java/org/openstack4j/api/compute/microversion/` 에 둔다. 항목: 협상(서버 max 2.100/2.50/2.0 에서 각각), 고정·범위 검사, 헤더 2종, 꺼진 상태에서 헤더 없음, 메서드별 상한(2.100 을 켜도 프록시 요청 헤더가 2.35), 하한(꺼짐·부족 시 요청 없이 예외), 2.47 flavor 두 형식, 2.45 image_id 본문, 2.48 diagnostics, 새 엔드포인트별 요청·응답, 서버 생성 빌더 옵션 직렬화와 하한, 액션 본문.
- fixture 는 개발용 epoxy(Nova 2.100)의 실제 응답으로 만들고, 2.101~2.104 필드는 api-ref 예시로 보강한다.

### 6.2 실환경 통합 테스트 (`ComputeLiveTests`, 환경 변수 있을 때만)
협상 결과 2.100 확인 → 서버 목록·상세(2.47 flavor 내장, tags) → 키페어 생성(공개키 없이, 2.91 상한 확인)·삭제 → 임시 서버 생성(가장 작은 flavor, 사용 가능한 이미지) → tags·lock with reason·topology·ips·remote-console·migrations 목록 → 삭제. 서버 생성이 불가능하면 그 부분만 `SkipException`. 기존 서버는 읽기만 한다.

## 7. 작업 순서

| # | 작업 |
|---|---|
| 1 | 공용 microversion 유틸과 `MicroVersionException`(Placement 예외 부모 변경) |
| 2 | compute 적용: `microVersions()`, 헤더 2종, `capped`/`requireMicroVersion`, 기존 레거시 메서드 상한 |
| 3 | 서버 응답 모델 확장, 2.47 flavor 이중 형식, 2.98 image properties |
| 4 | 서버 생성·목록 옵션 확장 + personality 상한 |
| 5 | 서버 액션 보강 |
| 6 | topology, ips, remote-consoles, diagnostics 표준 |
| 7 | 서버 마이그레이션, os-migrations 필터 |
| 8 | 서버 shares(2.97) |
| 9 | hypervisors·services·aggregates·quota-sets·keypairs·server-groups 보강 |
| 10 | 새 서비스 4개 |
| 11 | 플레이버·키페어·집계·인스턴스 액션·마이그레이션·서비스·서버 그룹 모델 필드 추가 |
| 12 | 실환경 통합 테스트, README compute 절, MIGRATION/CHANGELOG |
| 13 | 전체 리뷰 → 4.2.0 릴리스(tag push 전 사용자 확인) → 4.3.0-SNAPSHOT |

## 8. 완료 기준

1. microversion 을 켜지 않은 기존 compute 테스트가 전부 변경 없이 통과한다.
2. 2.100 을 켠 상태에서 3.3 의 레거시 메서드가 상한 덕분에 계속 동작한다(단위 테스트로 헤더 값 확인).
3. 4·5장의 모든 필드·메서드가 구현되고 단위 테스트가 세 connector 에서 통과한다.
4. 실환경 테스트가 개발용 OpenStack(epoxy)에서 통과하고 임시 자원이 남지 않는다.
5. `io.github.seogineer:openstack4j:4.2.0` 이 Maven Central 에 배포된다.
