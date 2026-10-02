# openstack4j 후속 포크 — B. Placement API 전체 지원 설계

- 작성일: 2026-10-02
- 상태: 사용자 검토 대기
- 대상 버전: 4.1.0 (기능 추가, 하위 호환)
- 선행: A. 기반 및 최신화 (4.0.0 릴리스 완료)

## 0. 상위 목표와 로드맵

포크의 최종 목표는 **OpenStack 최신 릴리스가 제공하는 모든 서비스 API 를 openstack4j 가 지원하는 것**이다. 범위가 매우 크므로 서비스 단위 하위 프로젝트로 나누고, 각각 spec → plan → 구현 → 마이너 릴리스를 반복한다.

- 이 문서(B)는 그 첫 번째로 **Placement** 를 다룬다. Placement 는 API 표면이 비교적 작고 microversion 설계가 다른 서비스(Nova, Cinder, Manila, Ironic 등)와 같은 구조라서, 여기서 만든 틀(협상, 최소 지원 버전, 기능별 버전 검사, 오류 매핑, 실환경 통합 테스트)을 이후 서비스에 재사용한다.
- 다음 하위 프로젝트를 고르기 전에 **서비스별 지원 현황 분석(gap analysis)** 을 한다: 최신 릴리스의 각 서비스 API reference 와 현재 openstack4j 구현을 비교해 누락 API 수, 사용 빈도, 의존 관계로 우선순위를 정한다. 이 분석은 B 완료 후 별도 작업으로 진행한다.
- 공통 원칙: 기존 코드는 유지하고 새 기능은 추가로만 넣는다(하위 호환, 마이너 릴리스). microversion 이 있는 서비스는 서버와 협상하고 서비스별 최소 지원 버전을 둔다.

## 1. 목적과 범위

openstack4j 를 쓰는 불특정 다수의 개발자를 위해 **OpenStack Placement API 전체**를 타입 안전한 Java API 로 제공한다. 4.0.0 의 Placement 지원은 resource provider 조회, inventories 조회, usages 조회 4개뿐이다.

### 범위

| 영역 | 지원 |
|---|---|
| Resource providers | 생성·조회·수정·삭제, 목록 필터 |
| Inventories | 전체 조회·교체·삭제, resource class 별 생성·조회·수정·삭제 |
| Resource classes | 목록, 생성, 조회, 생성 또는 확인(PUT), 삭제 |
| Traits | 목록(필터), 존재 확인, 생성, 삭제, provider 별 조회·교체·삭제 |
| Aggregates | provider 별 조회·교체 |
| Allocations | consumer 별 조회·설정·삭제, 일괄 설정(POST /allocations), provider 별 조회 |
| Allocation candidates | granular request group 을 포함한 전체 쿼리 |
| Usages | provider 별, 프로젝트·사용자 별, 편의 기능 `capacity()` |
| Reshaper | POST /reshaper |
| Versions | 서버 지원 범위, 협상된 microversion |

### 범위 밖

- Placement 외 서비스
- 1.28 미만 서버에서 새 API 사용
- 비동기 API

### 제약

- **기존 코드는 그대로 둔다.** `PlacementService.resourceProviders()` 와 `org.openstack4j.{api,model,openstack}.placement.ext` 패키지는 수정하지 않고 `@Deprecated` 도 붙이지 않는다. 기존 메서드는 지금처럼 microversion 헤더 없이(서버 기본값 1.0) 동작한다.
- 새 기능은 새 메서드·새 클래스로만 추가한다 (SemVer 마이너 → 4.1.0).
- 회사 코드는 가져오지 않는다. OpenStack 공식 Placement API reference 를 기준으로 구현한다.

## 2. microversion

### 2.1 협상

- 라이브러리 최고 버전 상수: `PlacementMicroVersions.LATEST = 1.39`.
- 새 API 최소 지원 버전 상수: `PlacementMicroVersions.MINIMUM = 1.28` (Rocky, 2018). 이 아래로는 allocation 요청 형식과 consumer generation 처리가 달라서 지원하지 않는다.
- 세션에서 새 Placement API 를 처음 호출할 때 Placement 루트 `GET /` (인증 불필요) 로 서버의 `min_version`, `max_version` 을 조회하고, **endpoint URL 별로 세션(`OSClientSession`)에 캐시**한다.
- 사용 버전 = `min(LATEST, 서버 max)`. 이 값이 `MINIMUM` 미만이면 새 API 호출은 `PlacementMicroVersionException` 을 던진다.
- 모든 새 API 요청에 `OpenStack-API-Version: placement <버전>` 헤더를 붙인다.

### 2.2 수동 지정

- `os.placement().useMicroVersion("1.30")` — 세션에 저장한다. 형식이 잘못되었거나 `[max(MINIMUM, 서버 min), min(LATEST, 서버 max)]` 범위를 벗어나면 즉시 `PlacementMicroVersionException`.
- `os.placement().useMicroVersion(null)` — 자동 협상으로 되돌린다.

### 2.3 기능별 최소 버전

1.28 보다 높은 버전이 필요한 기능은 메서드나 옵션 단위로 검사하고, 부족하면 **요청을 보내기 전에** `PlacementMicroVersionException` 을 던진다. 메시지 예: `"allocation candidates 'same_subtree' requires placement microversion 1.36, but the negotiated version is 1.31 (server max 1.31)"`.

| 버전 | 기능 |
|---|---|
| 1.29 | allocation candidates 에서 nested provider 반영 (요청 형식 변화 없음) |
| 1.30 | reshaper |
| 1.31 | allocation candidates `in_tree` |
| 1.32 | forbidden aggregate (`member_of=!...`) — provider 목록·candidates |
| 1.33 | granular request group suffix 를 숫자 외 문자열로 사용 |
| 1.34 | allocation candidates 응답의 `mappings` |
| 1.35 | allocation candidates `root_required` |
| 1.36 | allocation candidates `same_subtree` |
| 1.37 | provider 의 parent 변경·해제 (update) |
| 1.38 | consumer type (allocation 설정, usages 조회) |
| 1.39 | trait `in:` 문법 (여러 trait 중 하나) — provider 목록·candidates |

### 2.4 응답 모델

모델은 1.39 까지의 필드를 모두 갖는다. 낮은 버전 서버가 주지 않는 필드는 `null` 이다 (예: 1.38 미만의 `consumerType`, 1.34 미만의 `mappings`).

## 3. 동시성과 오류

### 3.1 generation

- provider 에 딸린 쓰기(inventory, trait, aggregate, provider 수정)는 `resource_provider_generation` 을, allocation 쓰기는 `consumer_generation` 을 **메서드 인자로 명시적으로 받는다**.
- 쓰기 결과로 새 generation 이 담긴 상태를 돌려준다 (예: `inventories().replace(...)` → `ResourceProviderInventories`).
- 새 consumer 의 첫 allocation 은 `consumerGeneration = null` 로 보낸다 (Placement 규칙).

### 3.2 오류 매핑

Placement 오류 응답 `{"errors":[{"status":..,"code":"placement....","title":..,"detail":..,"request_id":..}]}` 를 다음으로 변환한다.

- `PlacementException extends ClientResponseException` — HTTP 상태, `getErrorCode()` (예: `placement.inventory.inuse`), `getDetail()`, `getRequestId()`.
- `PlacementConcurrentUpdateException extends PlacementException` — 오류 코드 `placement.concurrent_update` (generation 충돌).
- `PlacementMicroVersionException extends OS4JException` — 협상·수동 지정·기능별 버전 검사 실패. 서버 요청 없이 발생한다.

### 3.3 openstack4j 관례

- `get(...)` 은 404 면 `null`.
- `delete(...)` 는 `ActionResponse`.
- 존재 확인(`traits().exists(name)`)은 `boolean`.
- 그 밖의 쓰기 실패는 예외 (3.2).

### 3.4 재시도 도우미

```java
public final class Placement {
    public static <T> T retryOnConcurrentUpdate(int maxAttempts, Supplier<T> action);
}
```
`PlacementConcurrentUpdateException` 이면 `maxAttempts` 까지 `action` 을 다시 실행하고, 마지막 실패는 그대로 던진다. 다른 예외는 즉시 던진다. `maxAttempts < 1` 이면 `IllegalArgumentException`.

## 4. 구조

### 4.1 패키지

| 종류 | 패키지 |
|---|---|
| 서비스 인터페이스 | `org.openstack4j.api.placement.v1` |
| 모델 인터페이스, 옵션·쿼리 빌더, 상수 | `org.openstack4j.model.placement.v1` |
| 서비스 구현 | `org.openstack4j.openstack.placement.v1.internal` |
| Jackson 모델 | `org.openstack4j.openstack.placement.v1.domain` |
| 예외 | `org.openstack4j.api.placement.v1.exceptions` |

`PlacementService` 에 accessor 만 추가한다: `providers()`, `inventories()`, `resourceClasses()`, `traits()`, `aggregates()`, `allocations()`, `allocationCandidates()`, `usages()`, `reshaper()`, `versions()`, `useMicroVersion(String)`. 새 서비스 구현은 공통 기반 클래스 `BasePlacementV1Service` (microversion 헤더, 기능별 버전 검사, 오류 매핑)를 상속한다.

### 4.2 서비스별 메서드

`rp` = resource provider UUID, `rc` = resource class 이름, `gen` = generation.

**ResourceProviderService** — `providers()`
- `List<? extends ResourceProvider> list()`, `list(ResourceProviderListOptions)`
- `ResourceProvider get(String rp)`
- `ResourceProvider create(ResourceProviderCreate)` — name, uuid(선택), parentProviderUuid(선택)
- `ResourceProvider update(String rp, ResourceProviderUpdate)` — name, parentProviderUuid(변경·해제는 1.37), generation 은 Placement 가 요구하지 않으므로 받지 않는다
- `ActionResponse delete(String rp)`
- `ResourceProviderListOptions`: `name`, `uuid`, `memberOf(String...)` (여러 번 호출 = AND, `in:` 문자열과 `!` 접두사 허용, `!` 는 1.32), `resources(rc, amount)`, `inTree(rp)`, `required(String...)` (`!` 접두사 = forbidden, `in:` 은 1.39)

**InventoryService** — `inventories()`
- `ResourceProviderInventories list(String rp)` — generation + `Map<String, Inventory>`
- `Inventory get(String rp, String rc)`
- `ResourceProviderInventories replace(String rp, long gen, Map<String, Inventory>)` — PUT 전체 교체
- `Inventory create(String rp, long gen, String rc, Inventory)` — POST
- `Inventory update(String rp, long gen, String rc, Inventory)` — PUT 단일
- `ActionResponse delete(String rp, String rc)`, `ActionResponse deleteAll(String rp)`
- `Inventory`: total, reserved, minUnit, maxUnit, stepSize, allocationRatio (+ 응답의 resourceProviderGeneration)

**ResourceClassService** — `resourceClasses()`
- `List<String> list()`, `boolean exists(String rc)`
- `void create(String rc)` — `CUSTOM_` 접두사 필수 (클라이언트에서 검사)
- `void ensure(String rc)` — PUT (멱등)
- `ActionResponse delete(String rc)`
- 표준 이름 상수: `ResourceClasses.VCPU`, `MEMORY_MB`, `DISK_GB`, `PCI_DEVICE`, `SRIOV_NET_VF`, `VGPU`, `PCPU` 등

**TraitService** — `traits()`
- `List<String> list()`, `list(TraitListOptions)` — `name` (`startswith:`, `in:`), `associated`
- `boolean exists(String trait)`, `void create(String trait)` (`CUSTOM_` 필수), `ActionResponse delete(String trait)`
- `ResourceProviderTraits listForProvider(String rp)` — generation + 목록
- `ResourceProviderTraits replaceForProvider(String rp, long gen, Collection<String>)`
- `ActionResponse deleteForProvider(String rp)`

**AggregateService** — `aggregates()`
- `ResourceProviderAggregates listForProvider(String rp)` — generation + UUID 목록
- `ResourceProviderAggregates replaceForProvider(String rp, long gen, Collection<String>)`

**AllocationService** — `allocations()`
- `ConsumerAllocations get(String consumer)` — provider 별 `resources`, projectId, userId, consumerGeneration, consumerType(1.38)
- `void set(String consumer, AllocationRequest)` — PUT; projectId, userId, consumerGeneration(null 허용), consumerType, `Map<rp, Map<rc, amount>>`. Placement 는 1.38 이상에서 consumer_type 을 필수로 요구하므로, 협상 버전이 1.38 이상인데 consumerType 이 비어 있으면 요청 전에 `IllegalArgumentException` 을 던진다(기본값을 임의로 채우지 않는다). 1.38 미만에서 consumerType 을 지정하면 `PlacementMicroVersionException`.
- `void setMany(Map<String, AllocationRequest>)` — POST /allocations
- `ActionResponse delete(String consumer)`
- `ResourceProviderAllocations listForProvider(String rp)` — generation + consumer 별 resources

**AllocationCandidateService** — `allocationCandidates()`
- `AllocationCandidates list(AllocationCandidatesQuery)` — `allocationRequests` (allocations, mappings(1.34)) + `providerSummaries` (resources capacity/used, traits, parent/root)
- `AllocationCandidatesQuery.builder()`: 비접미사 그룹 `resources`, `required`, `memberOf`, `inTree`; `group(suffix, g -> ...)` 로 granular group (`resources<suffix>`, `required<suffix>`, `member_of<suffix>`, `in_tree<suffix>`); `groupPolicy(NONE|ISOLATE)` (그룹이 2개 이상이면 필수 — 빌더가 검사); `limit`; `rootRequired` (1.35); `sameSubtree` (1.36). 숫자가 아닌 suffix 는 1.33, `in:` trait 은 1.39.

**UsageService** — `usages()`
- `ResourceProviderUsages forProvider(String rp)` — generation + `Map<rc, Long>`
- `ProjectUsages forProject(String projectId, String userId, String consumerType)` — userId·consumerType 은 null 허용, consumerType 은 1.38; 1.38 이상 응답(consumer type 별 그룹)도 같은 모델로 표현
- `Map<String, ResourceCapacity> capacity(String rp)` — inventories + usages 로 계산: `capacity = floor((total - reserved) × allocationRatio)`, `used`, `free = capacity - used`. 두 요청 사이의 변경은 반영되지 않을 수 있다고 Javadoc 에 명시

**ReshaperService** — `reshaper()`
- `void reshape(ReshapeRequest)` — inventories(provider 별 generation 포함) + allocations; 1.30

**VersionService** — `versions()`
- `PlacementVersion get()` — 서버 min/max, 사용 중인 버전, 수동 지정 여부

### 4.3 모델

- 응답 모델: 읽기 전용 인터페이스(`model.placement.v1`) + Jackson 구현(`openstack.placement.v1.domain`). 기존 openstack4j 패턴과 같다.
- 요청 모델: 인터페이스의 static `builder()` (예: `Inventory.builder().total(64).reserved(2).allocationRatio(4.0f).build()`).
- resource class·trait 은 `String` 으로 다룬다.

## 5. 테스트

### 5.1 단위 테스트 (core-test)

- 위치: `core-test/src/main/java/org/openstack4j/api/placement/v1/*Tests.java`, fixture: `core-test/src/main/resources/placement/v1/*.json`
- core-test 는 connector 세 개 각각에서 실행되므로 httpclient·okhttp·http-connector 모두에서 검증된다.
- 서비스마다: 요청 메서드·경로·쿼리 문자열, `OpenStack-API-Version` 헤더, 요청 JSON, 응답 파싱, 404 → null, 오류 매핑(409 concurrent_update, inuse 등), 기능별 버전 검사.
- fixture 는 개발용 OpenStack(epoxy, placement 1.39)의 실제 응답을 바탕으로 만들고 호스트명 등은 일반화한다.
- 협상 테스트: 서버 max 1.30 → 1.30 사용; 서버 max 1.27 → 새 API 호출 시 예외; 범위 밖 수동 지정 거부; 1.36 옵션을 1.31 서버에 쓰면 요청 없이 예외(MockWebServer 요청 수로 확인); 루트 조회는 세션당 endpoint 별 1회.

### 5.2 실환경 통합 테스트 (선택 실행)

- 위치: `core-test/src/main/java/org/openstack4j/api/placement/v1/PlacementLiveTests.java`, TestNG group `placement-live`.
- `OS_AUTH_URL`, `OS_USERNAME`, `OS_PASSWORD`, `OS_PROJECT_NAME` (+ 도메인) 이 없으면 `SkipException` 으로 건너뛴다. CI 에는 이 값이 없으므로 돌지 않는다.
- 시험용 provider `os4j-it-<uuid>` 와 `CUSTOM_OS4J_IT_<uuid>` resource class·trait 을 만들어 inventory, trait, aggregate, allocation(임시 consumer UUID), candidates, usages, capacity, 수정·삭제를 한 바퀴 돌리고 `finally` 에서 모두 지운다. 기존 compute 노드 provider 는 읽기만 한다.
- 릴리스 전에 개발용 OpenStack 에서 수동으로 실행한다.

## 6. 작업 순서

각 작업은 PR 하나, CI 통과 후 머지.

| # | 작업 |
|---|---|
| 1 | 기반: `PlacementMicroVersions`, 협상·캐시·수동 지정, `versions()`, 오류 매핑과 예외 3종, `Placement.retryOnConcurrentUpdate`, `BasePlacementV1Service`, `PlacementService` accessor 골격 |
| 2 | resource providers |
| 3 | inventories |
| 4 | resource classes |
| 5 | traits |
| 6 | aggregates |
| 7 | usages + capacity |
| 8 | allocations |
| 9 | allocation candidates |
| 10 | reshaper |
| 11 | 실환경 통합 테스트, README Placement 절, CHANGELOG |
| 12 | 전체 리뷰 → 4.1.0 릴리스 (tag push 전 사용자 확인) |

## 7. 완료 기준

1. 4.2 의 모든 메서드가 구현되고 단위 테스트가 세 connector 에서 통과한다.
2. 협상 테스트(5.1)가 통과한다.
3. 실환경 통합 테스트가 개발용 OpenStack(epoxy)에서 통과하고, 실행 후 시험용 자원이 남지 않는다.
4. 기존 `resourceProviders()` 관련 동작과 테스트가 바뀌지 않는다.
5. `io.github.seogineer:openstack4j:4.1.0` 이 Maven Central 에 배포된다.
