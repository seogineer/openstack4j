# openstack4j 후속 포크 — G′. Glance(image v2) 누락 API 설계

- 날짜: 2026-10-07
- 대상 버전: 4.6.0
- 상태: 사용자 지시("나한테 물어보지 말고 계속 진행해", "네가 권장하는대로 따를게")에 따라 권장안으로 결정하고 이 문서에 기록한다.

## 1. 배경과 목적

openstack4j 의 image v2 는 Glance api-ref(2026.2) 79 메서드 중 19 개만 구현한다(이미지 CRUD·멤버·태그·업로드/다운로드·deactivate/reactivate·tasks, 그리고 v1 시절의 `cached_images`). 빠진 60 개는 interoperable image import, 다중 store, 이미지 캐시 API, metadata definitions(metadefs), JSON schema, 버전 조회다.

### 로드맵 조정
원래 순서는 G = Manila 였다. 개발용 OpenStack(epoxy)에 Manila 가 없어 실환경 검증(gap 분석의 기준 ④)을 할 수 없으므로, 같은 환경에 있는 H 묶음(Glance, Octavia, Heat)을 먼저 하고 Glance 를 G′ 로 진행한다. Manila 와 Neutron stadium(F2)은 검증 환경이 생기거나 H 를 마친 뒤로 미룬다.

### 제약
- 기존 코드는 바꾸지 않고 추가만 한다(`@Deprecated` 없음). 기존 요청 본문은 그대로다.
- Glance 는 microversion 헤더가 없다. 기능은 서버 API 버전(`GET /versions` 의 CURRENT)과 설정(예: 캐시 middleware, 다중 store)에 따라 있거나 없다. 없으면 서버가 404 를 준다.
- 세 connector 에서 같은 결과.
- 회사 코드(openstackit-java)는 복사하지 않는다.

### 참고 (개발용 Glance)
`GET /versions`: CURRENT v2.17, SUPPORTED v2.0~v2.15(v2.14 없음 — 캐시 middleware 가 꺼져 있을 가능성). 캐시 API 는 실환경에서 404 일 수 있다.

## 2. 결정 사항

| 항목 | 결정 | 이유 |
|---|---|---|
| 범위 | 59 메서드. `GET /` 는 `GET /versions` 와 같은 응답이라 `versions()` 하나로 다룬다 | 중복 |
| 배치 | `os.imagesV2()` 의 새 하위 accessor: `metadefs()`, `schemas()`, `cache()`, `info()`; 이미지 하위 경로(import, stage, locations, tasks, store 삭제)와 `versions()` 는 `ImageService` 에 메서드 추가 | 기존 `tasks()` 하위 accessor 와 같은 모양 |
| 버전 확인 | `imagesV2().versions()` 가 버전 목록을 주고, `ImageVersions.supports("2.6")` 같은 helper 로 확인. 메서드가 자동으로 확인하지는 않는다 | F 의 extension 확인과 같은 이유(호출 두 배) |
| 404 | 목록·생성·수정·동작은 404 를 예외로(F 의 `propagate404` 와 같은 방식), 단건 `get` 은 null, 삭제는 실패 `ActionResponse` | F 최종 리뷰의 교훈 |
| 옵션 | `NeutronAttributes` 와 같은 모양의 `ImageAttributes`(null 생략, `attribute(k, null)` 명시) | 같은 이유 |
| schema | `Map<String, Object>`(JSON schema 원문) | 구조가 서버마다 다르고 소비자는 보통 그대로 넘긴다 |
| metadef property·object 의 schema 부분 | 공통 필드(`name`, `title`, `description`, `type`)는 getter, 나머지(enum, minimum, items …)는 `Map<String, Object> getSchema()` | JSON schema 를 모두 타입으로 만들면 크고 깨지기 쉽다 |

## 3. 새 메서드

### 3.1 `ImageService` 에 추가 (`os.imagesV2()`)

| 메서드 | 엔드포인트 | 최소 버전 |
|---|---|---|
| `ImageVersions versions()` | `GET /versions` | — |
| `ActionResponse importImage(String imageId, ImageImportOptions options)` | `POST /v2/images/{id}/import` | 2.6 |
| `ActionResponse stage(String imageId, Payload<?> payload)` | `PUT /v2/images/{id}/stage`(octet-stream) | 2.6 |
| `List<? extends ImageLocation> listLocations(String imageId)` | `GET /v2/images/{id}/locations` | 2.17 |
| `ActionResponse addLocation(String imageId, String url, Map<String, Object> validationData)` | `POST /v2/images/{id}/locations` | 2.17 |
| `List<? extends Task> listTasks(String imageId)` | `GET /v2/images/{id}/tasks` | 2.12 |
| `ActionResponse deleteFromStore(String storeId, String imageId)` | `DELETE /v2/stores/{store}/{image}` | 2.10 |

`ImageImportOptions`: `glanceDirect()`, `webDownload(String uri)`, `copyImage(List<String> stores)`, `glanceDownload(String regionName, String imageId)`(+`serviceInterface`); 공통 `stores(List)`, `allStores(boolean)`, `allStoresMustSucceed(boolean)`. 본문 `{"method": {"name": ..., "uri": ...}, "stores": [...], ...}`.

### 3.2 `info()`
`importMethods()`(`GET /v2/info/import` → `List<String>`), `stores()`(`GET /v2/info/stores`), `storesDetail()`(`GET /v2/info/stores/detail`, 관리자, 2.15), `usage()`(`GET /v2/info/usage` → quota 사용량 `Map<String, ImageUsage>`, 2.9).

### 3.3 `cache()` (2.14)
`list()`(`GET /v2/cache` → cached/queued), `queue(imageId)`(`PUT /v2/cache/{id}`), `delete(imageId)`, `clear()`(`DELETE /v2/cache`, 헤더 `x-image-cache-clear-target` 선택), `clean()`(`POST /v2/cache/clean`), `prune()`(`POST /v2/cache/prune`).

### 3.4 `schemas()`
`image()`, `images()`, `member()`, `members()`, `task()`, `tasks()`, metadef 10종(`namespace`, `namespaces`, `object`, `objects`, `property`, `properties`, `resourceType`, `resourceTypes`, `tag`, `tags`) — 모두 `Map<String, Object>`.

### 3.5 `metadefs()`
- namespaces: `listNamespaces()`, `listNamespaces(Map)`, `getNamespace(name)`, `createNamespace(MetadefNamespaceOptions)`, `updateNamespace(name, MetadefNamespaceOptions)`, `deleteNamespace(name)`
- resource types: `listResourceTypes()`, `listResourceTypeAssociations(namespace)`, `associateResourceType(namespace, name, prefix, propertiesTarget)`, `dissociateResourceType(namespace, name)`
- objects: `listObjects(ns)`, `getObject(ns, name)`, `createObject(ns, MetadefObjectOptions)`, `updateObject(ns, name, MetadefObjectOptions)`, `deleteObject(ns, name)`
- properties: `listProperties(ns)`(`{"properties": {name: schema}}` → `Map<String, MetadefProperty>`), `getProperty(ns, name)`, `createProperty(ns, MetadefPropertyOptions)`, `updateProperty(ns, name, MetadefPropertyOptions)`, `deleteProperty(ns, name)`
- tags: `listTags(ns)`, `getTag(ns, name)`, `createTag(ns, name)`(`POST …/tags/{name}`), `createTags(ns, List<String>, boolean append)`(`POST …/tags`, 헤더 `X-Openstack-Append`), `updateTag(ns, name, newName)`, `deleteTag(ns, name)`, `deleteAllTags(ns)`

## 4. 테스트

### 4.1 단위 테스트
- 기존 image 테스트는 변경 없이 통과.
- 새 테스트는 `core-test/src/main/java/org/openstack4j/api/image/v2/ext/`, 기반 `AbstractImageExtTest`(Service.IMAGE).
- fixture 는 api-ref 예시(`scratchpad/glance/api-ref/source/v2/samples/`)로 만든다(개발용 토큰이 만료되어 실응답 수집은 실환경 테스트 단계에서 한다).
- 404 전달·null·`ActionResponse` 규칙, 옵션의 null 생략, stage 의 octet-stream 본문, tags append 헤더를 세 connector 에서 확인한다.

### 4.2 실환경 통합 테스트 (`ImageExtensionsLiveTests`, `OS_AUTH_URL`·`OS_TOKEN`/password)
versions(CURRENT 확인), info import methods·stores·usage, 임시 이미지에 stage → import(glance-direct) → active 대기 → locations 조회 → 삭제, image tasks 조회, schemas(image, metadef namespace), metadef 임시 namespace 에 object·property·tag·resource type 연결 → 삭제, cache(404 면 skip). 임시 자원은 `finally` 에서 지우고 삭제 성공을 확인한다.

## 5. 작업 순서

| # | 작업 |
|---|---|
| 1 | 테스트 기반, `BaseImageExtService`(404 전달), `ImageAttributes`, `versions()`, `info()` |
| 2 | image import·stage·locations·image tasks·store 삭제 |
| 3 | cache API |
| 4 | schemas |
| 5 | metadef namespaces, resource types |
| 6 | metadef objects, properties |
| 7 | metadef tags |
| 8 | 실환경 통합 테스트, README image 절, MIGRATION/CHANGELOG |
| 9 | 전체 리뷰 → 4.6.0 릴리스 → 4.7.0-SNAPSHOT |

## 6. 완료 기준

1. 기존 image 테스트가 변경 없이 통과한다.
2. 3장의 59 메서드가 구현되고 단위 테스트가 세 connector 에서 통과한다.
3. 실환경 테스트가 개발용 Glance 에서 통과하고 임시 자원이 남지 않는다(토큰이 필요하다).
4. `io.github.seogineer:openstack4j:4.6.0` 이 Maven Central 에 배포된다.
