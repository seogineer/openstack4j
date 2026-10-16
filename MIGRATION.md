# 3.x → 4.0 이전 가이드

4.0 은 원본 openstack4j 3.x 의 후속 포크의 첫 릴리스입니다. Java 패키지(`org.openstack4j`)와 API 는 그대로이고, 주로 빌드 좌표와 실행 환경이 바뀌었습니다.

## 1. 요구 사항

- **JDK 17 이상** (3.x 는 Java 8 타깃)

## 2. Maven 좌표

groupId 가 하나로 합쳐졌습니다. artifactId 는 같습니다.

| 3.x | 4.0 |
|---|---|
| `com.github.openstack4j.core:openstack4j` | `io.github.seogineer:openstack4j` |
| `com.github.openstack4j.core:openstack4j-core` | `io.github.seogineer:openstack4j-core` |
| `com.github.openstack4j.core.connectors:openstack4j-httpclient` | `io.github.seogineer:openstack4j-httpclient` |
| `com.github.openstack4j.core.connectors:openstack4j-okhttp` | `io.github.seogineer:openstack4j-okhttp` |
| `com.github.openstack4j.core.connectors:openstack4j-http-connector` | `io.github.seogineer:openstack4j-http-connector` |

```xml
<dependency>
    <groupId>io.github.seogineer</groupId>
    <artifactId>openstack4j</artifactId>
    <version>4.0.0</version>
</dependency>
```

3.x 와 4.0 은 같은 패키지를 쓰므로 **한 클래스패스에 함께 두지 마세요.**

`openstack4j`(distribution) 아티팩트는 이제 core 와 httpclient connector 를 **의존성으로** 끌어옵니다. 3.x 처럼 core 클래스를 jar 안에 합쳐 넣지 않습니다. 모든 의존성을 한 jar 에 담은 `withdeps` classifier jar 는 그대로 제공합니다.

## 3. connector

| connector | 4.0 |
|---|---|
| jersey2 | **제거** (javax 기반, Spring Boot 3 과 충돌) |
| resteasy | **제거** (같은 이유) |
| httpclient | Apache HttpClient **5** 로 전환. `openstack4j`(distribution)의 기본 connector |
| okhttp | OkHttp **4.12** 로 전환. kotlin-stdlib 가 전이 의존성으로 추가됨 |
| http-connector | JDK `java.net.http.HttpClient` 로 재구현. JDK 17+ 에서 PATCH 가 동작함 |

jersey2·resteasy 를 쓰던 경우 `openstack4j-httpclient` 로 바꾸세요. 코드 변경은 필요 없습니다(connector 는 ServiceLoader 로 선택됩니다).

### httpclient connector 를 직접 설정하던 경우

`HttpClientConfigInterceptor` 와 `HttpResponseImpl.unwrap()` 이 HttpClient 5 타입을 씁니다.

```java
// 3.x
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
// 4.0
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
```

`RequestConfig.Builder.setSocketTimeout(...)` 은 `setResponseTimeout(Timeout.ofMilliseconds(...))` 로 바뀌었습니다. 연결 풀 설정은 `onClientCreate` 가 호출될 때 이미 `HttpClientBuilder` 에 들어가 있으며, 새 connection manager 를 지정하면 덮어씁니다.

### http-connector 의 동작 차이

- `Config.withHostnameVerifier(...)` 는 무시되고 WARN 로그가 남습니다(JDK HttpClient 에 해당 기능이 없음).
- `Config.withSSLVerificationDisabled()` 는 인증서 검증만 끕니다. 호스트명 검증까지 끄려면 JVM 옵션 `-Djdk.internal.httpclient.disableHostnameVerification=true` 를 쓰세요.
- I/O 오류(예: 서버가 이미 닫은 keep-alive 연결)로 실패한 요청은 한 번 다시 보냅니다. 3.x 의 `HttpURLConnection` 이 POST 를 한 번 재시도하던 것과 같습니다. 타임아웃은 재시도하지 않습니다.
- 응답 헤더 이름은 `X-Container-Meta-Year` 처럼 단어 첫 글자를 대문자로 정규화해서 돌려줍니다(JDK 가 소문자로 넘기기 때문).

## 4. 의존성

| 의존성 | 3.12 | 4.0 |
|---|---|---|
| Jackson | 2.14 | 2.22 |
| Guava | 29 | 33 |
| SnakeYAML | 1.33 | 2.7 |
| SLF4J API | 1.7 | 2.0 |
| json-patch | `com.github.fge` 1.9 | `com.github.java-json-tools` 1.13 (패키지 동일) |
| jsr305 | compile | provided (전이되지 않음) |

Spring Boot 3.5 처럼 Jackson 을 더 낮은 2.x 버전으로 고정하는 환경에서도 동작하는 것을 CI 에서 확인합니다.

## 5. 그 밖의 제거

- OSGi Karaf `features.xml` 부가 아티팩트(2014년 이후 갱신되지 않았음). 번들 manifest 는 그대로 있습니다.

# 4.1 → 4.2

런타임 동작을 바꾸는 변경은 없습니다. compute microversion 은 선택 사항(`os.compute().microVersions().negotiate()`)이고, 켜지 않으면 요청은 4.1 과 같습니다.

- `PlacementMicroVersionException` 의 부모가 새 `org.openstack4j.api.exceptions.MicroVersionException`(여전히 `OS4JException`)으로 바뀌었습니다. 기존 `catch` 는 그대로 동작합니다.
- `Server`, `Flavor`, `Keypair`, `ServerGroup` 같은 모델 인터페이스에 `default` getter 가 추가되었습니다. 라이브러리 밖에서 이 인터페이스를 구현한 클래스도 그대로 컴파일됩니다. 다만 `ServerCreateBuilder`, `BlockDeviceMappingBuilder` 와 서비스 인터페이스(`ComputeService`, `ServerService`, `KeypairService`, `ServicesService`, `HypervisorService`, `MigrationService`, `HostAggregateService`, `QuotaSetService`, `ServerGroupService`)에는 추상 메서드가 추가되었습니다. 테스트용 가짜 구현처럼 이 인터페이스를 직접 구현했다면 새 메서드를 구현해야 합니다.
- `servers().list(...)`, `migrations().list(...)` 등에 타입 있는 옵션 오버로드가 생겨 `list(null)` 처럼 `null` 리터럴을 넘기면 컴파일 오류(모호함)가 납니다. `list()` 를 쓰거나 `(Map<String, String>) null` 처럼 캐스트하세요. 바이너리 호환은 유지됩니다.
- `negotiate()` 후에는 Nova 2.47+ 가 flavor 를 내장해 서버의 `getFlavorId()` 가 `null` 입니다. `getFlavorSummary()` 를 쓰세요.
- `negotiate()` 후에는 2.88 에서 사라진 하이퍼바이저 통계 필드가 `0` 으로 읽힙니다. Placement inventory 를 쓰세요.

# 4.2 → 4.3

런타임 동작을 바꾸는 변경은 없습니다. block storage microversion 은 선택 사항(`os.blockStorage().microVersions().negotiate()`)이고, 켜지 않으면 요청은 4.2 와 같습니다.

- `ComputeMicroVersionService` 는 이제 공용 `org.openstack4j.common.MicroVersionService<ComputeVersion>` 을, `ComputeVersion` 은 `org.openstack4j.model.common.MicroVersionInfo` 를 상속합니다. 메서드는 그대로라 기존 코드는 변경 없이 컴파일됩니다.
- 모델 인터페이스(`Volume`, `VolumeSnapshot`, `VolumeBackup`, `VolumeType`, `VolumeTransfer`, `BlockQuotaSet`, block storage `Service`)에 `default` getter 가 추가되었습니다. 라이브러리 밖의 구현체도 그대로 컴파일됩니다. 다만 빌더 인터페이스(`VolumeBuilder`, `VolumeTypeBuilder`, `VolumeBackupCreateBuilder`, `BlockQuotaSetBuilder`)와 서비스 인터페이스(`BlockStorageService`, `BlockVolumeService`, `BlockVolumeSnapshotService`, `BlockVolumeBackupService`, `BlockQuotaSetService`, `BlockStorageServiceService`, `SchedulerStatsGetPoolService`)에는 추상 메서드가 추가되었습니다. 테스트용 가짜 구현처럼 이 인터페이스를 직접 구현했다면 새 메서드를 구현해야 합니다.
- `volumes().list(...)`, `snapshots().listDetail(...)`, `backups().list(...)` 등에 타입 있는 옵션 오버로드가 생겨 `list(null)` 처럼 `null` 리터럴을 넘기면 컴파일 오류(모호함)가 납니다. `list()` 를 쓰거나 `(Map<String, String>) null` 로 캐스트하세요. 바이너리 호환은 유지됩니다.
- `BlockQuotaSet` 의 `volumes`/`snapshots`/`gigabytes` 는 설정하지 않으면 요청 본문에서 빠집니다(이전에는 0 으로 전송되어 다른 quota 를 0 으로 덮어썼습니다). getter 는 여전히 `int` 이며 미설정 값은 0 으로 읽힙니다.
- `negotiate()` 후 Cinder 3.53+ 는 생성 본문의 알 수 없는 필드를 거부합니다. 라이브러리는 `bootable` 을 설정한 생성 요청을 3.52 로 보내 이를 피합니다.
- `volumes().create()` 에 `multiattach` 를 설정하면 Cinder 는 버전과 무관하게 400 을 돌려줍니다(멀티어태치는 volume type 으로 지정). 이는 4.2 와 같습니다.

# 4.3 → 4.4

런타임 동작을 바꾸는 변경은 없습니다. 기존 password·token 인증의 요청 본문, 기존 identity 메서드의 요청은 4.3 과 같습니다.

- `IOSClientBuilder.V3`, `IdentityService`, `UserService`, `ProjectService`, `RoleService`, `DomainService`, `TokenService` 에 추상 메서드가 추가되었습니다. 이 인터페이스를 직접 구현한 테스트용 가짜 구현은 새 메서드를 구현해야 합니다. 모델 인터페이스(`Token`, `User`, `Project`, `Domain`, `Role`)에는 `default` getter 만 추가되어 그대로 컴파일됩니다.
- 새 응답 필드(user `options`/`password_expires_at`/`federated`, project `is_domain`, domain `tags`, role `description`)는 읽기 전용이라 생성·수정 요청 본문에 들어가지 않습니다. 기존 project/domain/role `options` 는 그대로 `Map<String, String>` 입니다(불리언 값은 `"true"` 문자열).
- application credential 인증에 scope 를 함께 주면 요청 전에 `IllegalStateException` 이 납니다. credential 이 이미 project 에 묶여 있기 때문입니다.
- TOTP(`passcode`) 세션은 토큰 만료 후 같은 passcode 로 재인증할 수 없습니다. 만료 전에 새 passcode 로 다시 인증하세요.
- trust 또는 system scope 로 받은 토큰도 이제 세션 컨텍스트를 만들 수 있습니다(4.3 까지는 trust scope 토큰 인증에서 `NullPointerException`).
- Keystone 이 410 을 돌려주는 OS-SIMPLE-CERT·OS-PKI, 그리고 브라우저 리다이렉트용 websso 두 경로는 지원하지 않습니다.

# 4.4 → 4.5

런타임 동작을 바꾸는 변경은 없습니다. 기존 networking 메서드의 요청은 4.4 와 같습니다.

- `NetworkingService`, `RouterService`, `AgentService`, `PortService`, `NetQuotaService`, `NetFloatingIPService`, `PortForwardingService` 에 추상 메서드가 추가되었습니다. 이 인터페이스를 직접 구현한 테스트용 가짜 구현은 새 메서드를 구현해야 합니다.
- `Router` 와 `NetQosPolicy` 에는 `default` getter(`getExternalGateways()`, `getId()`)만 추가되어 그대로 컴파일됩니다. `NeutronRouter` 의 `external_gateways` 는 응답 전용이라 router 생성·수정 본문은 바뀌지 않습니다.
- 새 옵션 클래스(`*Options`)는 설정하지 않은 필드를 보내지 않습니다. 값을 지우려면 `attribute("field", null)` 을 쓰세요.
- 404(extension 꺼짐, 상위 자원 없음): 모델이나 목록을 돌려주는 메서드(목록, 생성, 수정, prefix·주소 추가처럼 결과를 돌려주는 동작)는 404 를 예외로 던집니다. `ActionResponse` 를 돌려주는 메서드(삭제, import·stage·캐시 동작, agent 스케줄링, flavor 의 profile 연결)는 실패한 `ActionResponse`(코드 404)를 돌려주고, 단건 `get(id)` 은 없는 자원에 `null` 을 돌려줍니다. `os.networking().extensions().isEnabled(alias)` 로 확인할 수 있습니다.

# 4.5 → 4.6

런타임 동작을 바꾸는 변경은 없습니다. 기존 image v2 메서드의 요청은 4.5 와 같습니다.

- `ImageService` 에 추상 메서드(`versions`, `info`, `cache`, `schemas`, `metadefs`, `importImage`, `stage`, `listLocations`, `addLocation`, `listTasks`, `deleteFromStore`)가 추가되었습니다. 이 인터페이스를 직접 구현한 가짜 구현은 새 메서드를 구현해야 합니다.
- 서버 API 버전이 낮거나 기능이 꺼져 있으면(404): 모델이나 목록을 돌려주는 메서드(목록, 생성, 수정, prefix·주소 추가처럼 결과를 돌려주는 동작)는 404 를 예외로 던집니다. `ActionResponse` 를 돌려주는 메서드(삭제, import·stage·캐시 동작, agent 스케줄링, flavor 의 profile 연결)는 실패한 `ActionResponse`(코드 404)를 돌려주고, 단건 `get(id)` 은 없는 자원에 `null` 을 돌려줍니다. `os.imagesV2().versions().supports("2.x")` 로 먼저 확인할 수 있습니다.
- `OctaviaService`, `ListenerV2Service`, `LoadBalancerV2Service`, `LbPoolV2Service` 에도 추상 메서드가 추가되었습니다(Octavia 확장). 직접 구현한 가짜 구현은 새 메서드가 필요합니다.
- `HeatService`, `StackService`, `EventsService`, `SoftwareConfigService` 에도 추상 메서드가 추가되었습니다(Heat 확장).
- `OSClient` 에 추상 메서드 `baremetal()` 이, `ServiceType` 에 `BAREMETAL` 이 추가되었습니다(Ironic). `OSClient` 를 직접 구현한 가짜 구현은 새 메서드가 필요하고, `ServiceType.values()` 를 순회하는 코드에는 값이 하나 늘어납니다.
- `DNSService`, `ZoneService`, `RecordsetService` 에 추상 메서드가 추가되었습니다(Designate 확장). 직접 구현한 가짜 구현은 새 메서드가 필요합니다.
- `ShareService`(Manila)에 추상 메서드(`microVersions()`, `messages()`, `administration()`, `sharesExt()` … `qosTypes()`)가 추가되었습니다. 기존 Manila 메서드의 요청은 그대로 2.6 입니다.
- `NetworkingService` 에 stadium 프로젝트의 추상 accessor(`vpnServices()` … `bgpvpnAssociations()`)가 추가되었습니다.
- `BarbicanService` 에 추상 accessor(`acls()`, `secretsExt()`, `containersExt()`, `orders()`, `quotas()`, `secretStores()`)가 추가되었습니다.
- `TroveService` 에 추상 accessor(`instancesExt()`, `backups()`, `backupStrategies()`, `configurations()`, `datastoresExt()`, `troveAdmin()`)가 추가되었습니다.
- `ObjectStorageService`(`info()`, `listEndpoints`), `TelemetryService`(`alarmsExt()`), `MagnumService`(`extensions()`), `WorkflowService`(`extensions()`)에 추상 메서드가 추가되었습니다.
- `OSClient` 에 추상 accessor `instanceHa()`, `reservation()`, `rating()`, `accelerator()`, `messaging()`, `registration()`, `optimization()`, `containerApp()`, `ServiceType` 에 `INSTANCE_HA`, `RESERVATION`, `RATING`, `ACCELERATOR`, `MESSAGING`, `REGISTRATION`, `OPTIMIZATION`, `CONTAINER_APP` 이 추가되었습니다(Masakari, Blazar, CloudKitty, Cyborg, Zaqar, Adjutant, Watcher, Zun). `OSClient` 를 직접 구현한 가짜 구현은 새 메서드가 필요하고, `ServiceType.values()` 를 순회하는 코드에는 값이 늘어납니다.
- Keystone v3 catalog 해석: 항목의 이름(name)이 알려진 서비스를 가리키면 이름이 type 보다 우선합니다(`ServiceType.forCatalogEntry`). Zun 은 type `container`(기존 `MAGNUM` 의 type 과 같음)를 이름 `zun` 으로 등록하기 때문에, 전에는 Zun 이 있는 클라우드에서 `os.magnum()` 이 Zun endpoint 로 갈 수 있었습니다. 이름과 type 이 서로 다른 알려진 서비스를 가리키는 항목만 영향을 받습니다.
- `TackerService` 에 추상 메서드 `vnfPackages()`, `vnfLcm()`, `vnfLcmV1()`, `vnfFaults()`, `vnfPerformance()`, `VimService` 에 `update(String, Map)` 이 추가되었습니다. 이 인터페이스를 직접 구현한 코드는 새 메서드가 필요합니다.
- 오류 메시지: 응답 본문의 최상위 `message`(Designate) 와 `error_message` 안의 `faultstring`(Ironic, Magnum) 을 `ActionResponse.getFault()`·예외 메시지로 씁니다. 이전에는 `Status: 404, Reason: Not Found` 같은 상태 줄이나 JSON 원문이었습니다.
- httpclient connector: 요청에 `Accept` 헤더를 주면 기본값 `application/json` 을 대체합니다(이전에는 `Accept` 가 두 개 전송되었습니다). http-connector 는 원래 이렇게 동작했습니다.

# 4.6 → 4.7

- Keystone v2 catalog 해석: 서비스 이름으로 찾지 못해 type 으로 찾을 때, 그 항목이 이름으로 다른 알려진 서비스를 가리키면 쓰지 않습니다(v3 의 `ServiceType.forCatalogEntry` 규칙과 같음). Zun 만 있는 v2 클라우드에서 `os.magnum()` 은 더 이상 Zun endpoint 로 가지 않습니다.
- `ServiceType` 별칭은 이름·type 일치 판정(`forName`, `forCatalogEntry`, v3 catalog)에만 쓰입니다. `getType()` 키로 집계 catalog 를 찾는 v2 type fallback 과 `getServiceVersion()` 에는 적용되지 않습니다.
- Zaqar `ping()` 은 5xx 에서 예외 대신 `false` 를 돌려줍니다. connector 가 따라가지 않은 3xx 최종 응답도 이제 `false`(이전 `true`)입니다.
- `executeWithResponse()` 경로의 요청도 `User-Agent` 를 `OpenStack4j / OpenStack Client` 로 덮어씁니다(세션 헤더에 `User-Agent` 를 넣은 경우 포함).
- 식별자 인코딩: 위 서비스들이 경로에 넣는 id·이름은 영문자·숫자·`-._~:@` 외의 문자를 UTF-8 percent 인코딩합니다(공백 → `%20`, `+` → `%2B`, `%` → `%25`, `큐` → `%ED%81%90`). 이미 `%XX` 로 인코딩된 부분은 그대로 두므로, 직접 인코딩해 넘기던 코드도 두 번 인코딩되지 않습니다. 대신 리터럴 `%` 뒤에 16진수 두 자리가 오는 이름(`a%20b`)은 디코딩된 이름으로 해석됩니다.
- 빈 문자열(공백만 있는 값 포함), `/`·`?`·`#` 를 포함한 값, `.`·`..`(`%2E` 표기 포함), `%2F` 를 포함한 값은 `IllegalArgumentException` 입니다. Heat·Octavia·Glance·Neutron 확장과 Keystone endpoint filter 는 이전에 null 만 거부했습니다.
- `BaseDesignateExtService.validId` 는 인코딩된 segment 를 돌려줍니다.

# 4.7 → 4.8

- `BaseBaremetalServices.requireMicroVersion`(internal 패키지, protected)을 삭제했습니다. 쓰는 곳이 없었습니다.
- Neutron FWaaS v2 `insertRule`·`removeRule` 이 돌려주는 정책 객체의 `getAttributes()` 에는 값이 null 인 속성이 더 이상 들어 있지 않습니다(공용 `ObjectMapperSingleton` 의 NON_NULL 직렬화 규칙). `create`·`get` 은 이전과 같습니다.
- Manila v2 URL: `/v1/<project>/<extra>` 모양 endpoint 도 `/v2/<project>/<extra>` 로 바뀝니다(이전에는 v1 endpoint 를 그대로 써서 새 API 가 v1 으로 갔습니다).
- `ImageVersions.supports()`·`getCurrent()` 는 숫자 아닌 version 조각에서 예외 대신 앞자리 숫자(없으면 0)로 비교합니다. 숫자가 없는 입력(빈 문자열 등)은 0 으로 비교되므로 `supports()` 가 `true` 를 돌려줍니다.
- `MistralExtService.listTaskExecutions(String, Map)`, `ReservableHostService.listProperties(boolean, boolean)`, `AodhAlarmExtService.history(String, List, Map)` 추상 메서드가 추가되었습니다. 이 인터페이스를 직접 구현한 코드는 새 메서드가 필요합니다.

# 4.9 → 4.10

컴파일에 영향을 주는 변경(추상 메서드 추가)과 런타임 동작이 바뀌는 변경은 아래와 같습니다. enum 값 추가는 CHANGELOG 4.10.0 을 보세요.

- `DriverService`(`listVendorPassthruMethods`, `vendorPassthru` 2개), `NodeService`(`vendorPassthru(String, HttpMethod, String, Map)`) 에 추상 메서드가 추가되었습니다. 이 인터페이스를 직접 구현한 코드는 새 메서드가 필요합니다.
- `TroveAdminService` 에 추상 메서드(`listDatastoreVersionFlavors`, `addDatastoreVersionFlavors`, `removeDatastoreVersionFlavor`, `listDatastoreVersionVolumeTypes`, `addDatastoreVersionVolumeTypes`, `removeDatastoreVersionVolumeType`, `updateAllParameters`, `deleteAllParameters`)가 추가되었습니다.
- `AodhAlarmExtService`(`capabilities`, `metrics`), `ShareExtService`(`getInstanceExportLocation`), `SecretExtService`(`migrateToSecretStore`), `DesignateQuotaService`(`get()`), `HypervisorService`(`listSummary` 2개)에 추상 메서드가 추가되었습니다. `SecretStore` 에는 `default` getter `getSecretStoreId()` 만 추가되어 그대로 컴파일됩니다.
- Aodh `deleteQuotas(projectId)` 는 `DELETE /v2/quotas/{project_id}` 로 보냅니다(이전에는 `DELETE /v2/quotas?project_id=…`). project id 는 이제 path segment 로 percent 인코딩되며, `/`·`?`·`#`·`%2F` 를 포함하거나, 비어 있거나, `.`·`..` 인 값은 이전처럼 `IllegalArgumentException` 입니다. 요청 URL 을 직접 검사하던 테스트·프록시 규칙은 새 경로에 맞춰야 합니다.
- Nova `Migration.Status`: `queued`·`running`·`completed`·`failed`·`cancelled`·`pre-migrating`·`post-migrating` 등이 이제 각자의 상수로 읽힙니다(이전에는 모두 `MIGRATING`). `getStatus() == MIGRATING` 으로 진행 중 여부를 판단하던 코드는 새 상수를 함께 확인해야 합니다. 알 수 없는 값은 이전처럼 `MIGRATING` 입니다.
- Manila `Share.Status`·`Share.TaskState`·`Share.Protocol`·`ShareInstance.Status`·`ShareSnapshot.Status`: 알 수 없는 값이 오면 이전에는 응답 전체의 역직렬화가 실패했지만 이제 `UNRECOGNIZED` 로 읽힙니다.
- `RoleService`(identity v3)에 추상 메서드 `listRoleAssignments(String, boolean)` 가 추가되었습니다. compute `Service`, identity v3 `RoleAssignment` 에는 `default` 메서드만 추가되었습니다.
- `ServerCreateBuilder`(`diskConfig`), `ServerService`(`resize(String, String, DiskConfig)`)에 추상 메서드가 추가되었습니다.
- `LbPoolV2Builder`(`tlsCiphers`, `tlsVersions`, `caTlsContainerRef`)와 `MemberV2Builder`(`name`)(Octavia)에 추상 메서드가 추가되었습니다. 이 빌더를 직접 구현한 코드는 새 메서드가 필요합니다. `Network`, `Port`, `Router`, `Agent`, Octavia `LbPoolV2`, `ListenerV2`, `MemberV2`, `LoadBalancerV2Stats` 에는 `default` getter 만 추가되어 그대로 컴파일됩니다.
- `ShareService`(`securityServicesExt()`), `ShareExtService`(`list`, `listDetails`, `get`), `SnapshotExtService`(`grantAccess`, `revokeAccess`, `listAccess`)(Manila)에 추상 메서드가 추가되었습니다. Manila `SecurityService` 에는 `default` getter(`getOu()`)만 추가되어 그대로 컴파일됩니다.
- Cinder `VolumeBackendPool.Capabilities` 의 용량 필드(`getTotalCapacityGb`, `getFreeCapacityGb`, `getProvisionedcapacitygb`, `getAllocatedcapacitygb`, `getTotalvolumes`, `getReservedPercentage`)는 `"infinite"`·`"unknown"` 같은 숫자가 아닌 문자열과 범위를 넘는 문자열에서 예외 대신 `null` 을, `"1230.75"`·`"1e3"` 같은 소수·지수 문자열에서 반올림한 값을 돌려줍니다(이전에는 모두 예외). JSON 숫자와 정수 문자열, 빈 문자열의 처리는 같습니다.
- `TemplateService`(`validate`), `TemplateBuilder`(`environment`, `environmentFiles`, `files`, `parameters`), `ResourceTypeService`(`listWithDescription`)(Heat)에 추상 메서드가 추가되었습니다. `Template` 에는 `default` getter 만 추가되어 그대로 컴파일됩니다.
- `NetFloatingIPService`(`associateToPort(String, String, String)`), image v2 `ImageService`(`download(String)`)에 추상 메서드가 추가되었습니다.
- Heat `TemplateService.validateTemplate*`: 유효한 템플릿에 이제 `isValid()==true` 를 돌려줍니다(이전에는 응답 파싱 실패로 항상 `false`). 이 결과를 근거로 분기하던 코드는 동작이 바뀝니다.

# 4.10 → 4.11

컴파일에 영향을 주는 변경(추상 메서드 추가)과 요청·응답 동작이 바뀌는 변경은 아래와 같습니다.

- `ServerCreateBuilder`(`minCount`, `maxCount`)에 추상 메서드가 추가되었습니다. `ServerUpdateOptions`·`ServerListOptions` 는 클래스라 메서드 추가만 있습니다.
- `ServerCreate.getMin()`·`getMax()` 의 값은 이제 `min_count`·`max_count` 로 전송됩니다(이전에는 Nova 가 거부하는 `min`·`max` 키였고, 설정할 빌더가 없었습니다).
- Neutron `qos_policy_id`(동작 변경): 4.10.0 은 이 필드를 응답 전용으로 두어 요청에 보낼 수 없었습니다. 이제 `NetworkBuilder`·`NetworkUpdateBuilder`·`PortBuilder` 에 추상 메서드 `qosPolicyId(String)` 가 추가되었고, 이 메서드로 설정한 값(null 이면 해제)만 생성·수정 본문에 들어갑니다. 응답에서 읽은 객체를 그대로 `update` 에 넘기면 이전처럼 `qos_policy_id` 를 보내지 않습니다(Neutron 은 키가 있으면 같은 값이어도 기존 정책을 떼고 다시 붙이며, 기존 정책에 대한 권한이 없으면 실패합니다). `NetworkUpdate` 에는 `default` getter 만 추가되었습니다.
- `SubnetBuilder`(`description`)에 추상 메서드가 추가되었습니다. `Subnet` 에는 `default` getter `getDescription()` 만 추가되었습니다. 응답에서 읽은 description 은 다시 보내지 않습니다. `subnets().update` 가 `gateway_ip` 를 항상 보내는 동작은 그대로입니다.
- Octavia `LbPoolV2UpdateBuilder`(`tlsCiphers`, `tlsVersions`, `caTlsContainerRef`)에 추상 메서드가, `LbPoolV2Update` 에 `default` getter 가 추가되었습니다.
- Octavia·neutron-lbaas `ListenerV2.getLoadBalancers()` 는 이제 응답의 `loadbalancers` 를 돌려줍니다(이전에는 항상 `null`). null 로 분기하던 코드는 결과가 달라집니다.
- `RoleService`(identity v3)에 추상 메서드 `listRoleAssignments(RoleAssignmentListOptions)` 가 추가되었습니다. `listRoleAssignments(null)` 처럼 `null` 리터럴을 넘기던 코드는 모호해져 컴파일되지 않습니다 — `(String) null` 로 캐스트하세요. 바이너리 호환은 유지됩니다.
- Cinder `Volume` 에는 `default` getter `getImageMetadata()` 만 추가되어 그대로 컴파일됩니다.
- Neutron `Agent.Type` 에 상수가 늘어 `default` 없는 switch 표현식은 컴파일되지 않을 수 있고, 이전에 `UNRECOGNIZED` 로 읽히던 `agent_type` 은 새 상수로 읽힙니다.

# 4.11 → 4.12

컴파일에 영향을 주는 변경(추상 메서드 추가)과 요청·응답 동작이 바뀌는 변경은 아래와 같습니다.

- `ActionResponse` 를 돌려주는 메서드 중 내부적으로 `ToActionResponseFunction` 을 쓰는 것(compute 서버 액션·`detachVolume`·`attachVolumeAsync`, Octavia·neutron-lbaas, Manila legacy, Tacker, Heat `update`·`patchUpdate`, Swift container `delete` 등)은 410~499·501~599 응답을 이제 실패(`isSuccess()==false`, `getCode()`=응답 status)로 돌려줍니다(동작 변경). 이전에는 409·500 과 400~408 만 실패였고 나머지는 code 200 성공이었습니다. 본문이 비었거나 JSON 이 아닌 오류는 이전의 `ClientResponseException` 대신 같은 status 의 실패 응답이 되며, JSON 이 왔지만 매핑되지 않는 409·500 은 기존 fallback(code 409)을 유지합니다.
- `ServerService`(`setMetadata`, `listVolumeAttachments`, `getVolumeAttachment`), `InterfaceService`(`attachToNetwork`), `ServerGroupService`(`list(ServerGroupListOptions)`)에 추상 메서드가 추가되었습니다. `ServerGroupListOptions` 는 새 클래스입니다. `updateMetadata` 의 동작(PUT, 주지 않은 키 삭제)은 그대로이고 javadoc 만 바로잡았습니다.
- Manila `SnapshotExtService`(`list`, `listDetails`, `get`), `ShareExtService`(`listShareInstances`, `getShareInstance`)에 추상 메서드가 추가되었습니다. `ShareSnapshotInfo` 에는 `default` getter `getUserId()` 만 추가되었고, 응답의 `user_id` 는 이제 `getAttributes()` 대신 `getUserId()` 로 읽힙니다.
- Manila `ShareService` 에 추상 메서드 `shareNetworksExt()`, `shareTypesExt()` 가 추가되었습니다(새 interface `ShareNetworkExtService`, `ShareTypeExtService`).
- `SubnetService`(`list(Map)`), `AgentService`(`list(Map)`), `MetadefService`(`getNamespace(String, String)`), `MagnumService`(`showClustertemplate`), `ServiceEndpointService`(`list(ServiceListOptions)`, `listEndpoints(EndpointListOptions)`), `CredentialService`(`list(CredentialListOptions)`)에 추상 메서드가 추가되었습니다. `ServiceListOptions`·`EndpointListOptions`·`CredentialListOptions` 는 새 클래스입니다.
- Neutron `NetQosPolicyUpdate`: 구현체와 `Builders.netQosPolicyUpdate()` 가 추가되었습니다(공개 시그니처 변경 없음). 이전에는 이 interface 의 구현체가 없어 `netQosPolicy().update(...)` 를 호출할 수 없었습니다.
- 응답 필드 getter 가 `default` 메서드로 추가되어 그대로 컴파일됩니다: Neutron `Router`·`ExternalGateway`·`NetFloatingIP`. 이 인터페이스를 직접 구현한 클래스는 새 getter 가 `null` 을 돌려줍니다. 응답에서 읽은 객체를 생성·수정 요청에 넘길 때 본문은 이전과 같습니다.
- 응답 필드 getter 가 `default` 메서드로 추가되어 그대로 컴파일됩니다: Neutron `Network`·`Port`·`Agent`·`NetQosPolicy`·`NetworkIPAvailability`·`SubnetIPAvailability`. 이 인터페이스를 직접 구현한 클래스는 새 getter 가 `null` 을 돌려줍니다. 응답에서 읽은 객체를 생성·수정 요청에 넘길 때 본문은 이전과 같습니다.
- 응답 필드 getter 가 `default` 메서드로 추가되어 그대로 컴파일됩니다: Octavia `LoadBalancerV2`·`ListenerV2`·`LbPoolV2`·`MemberV2`·`HealthMonitorV2`. 이 인터페이스를 직접 구현한 클래스는 새 getter 가 `null` 을 돌려줍니다. 응답에서 읽은 객체를 생성 요청에 넘길 때 본문은 이전과 같습니다.
- 응답 필드 getter 가 `default` 메서드로 추가되어 그대로 컴파일됩니다: compute `QuotaSet`, Heat `Resource`·`Stack`, `SwiftContainer`. 이 인터페이스를 직접 구현한 클래스는 새 getter 가 `null` 을 돌려줍니다.
- Heat `resources().show()`(동작 변경): 이제 값이 채워진 `Resource` 를 돌려줍니다(이전에는 모든 필드가 `null`). null 여부로 분기하던 코드는 결과가 달라지며, `resource` 루트 없이 응답하는 mock 은 역직렬화 예외가 납니다.

# 4.12 → 4.13

포크가 추가한 서비스 9개의 접근자·패키지·진입 인터페이스를 프로젝트 이름으로 바꿨습니다(호환되지 않는 변경, 이전 이름은 별칭 없이 삭제). 원본 openstack4j 에서 온 접근자(`compute()`, `networking()`, `dns()`, `workflow()`, `heat()` 등)는 그대로입니다.

| 이전 접근자 | 새 접근자 | 패키지(api·openstack·model 공통) | 진입 인터페이스 / 구현 | `Apis` 게터 |
|---|---|---|---|---|
| `baremetal()` | `ironic()` | `baremetal` → `ironic` | `BaremetalService` → `IronicService` / `BaremetalServiceImpl` → `IronicServiceImpl` | `getBaremetalService` → `getIronicService` |
| `instanceHa()` | `masakari()` | `instanceha` → `masakari` | `InstanceHaService` → `MasakariService` / `InstanceHaServiceImpl` → `MasakariServiceImpl` | `getInstanceHaService` → `getMasakariService` |
| `reservation()` | `blazar()` | `reservation` → `blazar` | `ReservationService` → `BlazarService` / `ReservationServiceImpl` → `BlazarServiceImpl` | `getReservationService` → `getBlazarService` |
| `accelerator()` | `cyborg()` | `accelerator` → `cyborg` | `AcceleratorService` → `CyborgService` / `AcceleratorServiceImpl` → `CyborgServiceImpl` | `getAcceleratorService` → `getCyborgService` |
| `rating()` | `cloudkitty()` | `rating` → `cloudkitty` | `RatingService` → `CloudKittyService` / `RatingServiceImpl` → `CloudKittyServiceImpl` | `getRatingService` → `getCloudKittyService` |
| `messaging()` | `zaqar()` | `messaging` → `zaqar` | `MessagingService` → `ZaqarService` / `MessagingServiceImpl` → `ZaqarServiceImpl` | `getMessagingService` → `getZaqarService` |
| `registration()` | `adjutant()` | `registration` → `adjutant` | `RegistrationService` → `AdjutantService` / `RegistrationServiceImpl` → `AdjutantServiceImpl` | `getRegistrationService` → `getAdjutantService` |
| `optimization()` | `watcher()` | `optimization` → `watcher` | `OptimizationService` → `WatcherService` / `OptimizationServiceImpl` → `WatcherServiceImpl` | `getOptimizationService` → `getWatcherService` |
| `containerApp()` | `zun()` | `containerapp` → `zun` | `ContainerAppService` → `ZunService` / `ContainerAppServiceImpl` → `ZunServiceImpl` | `getContainerAppService` → `getZunService` |

- Ironic 내부 클래스 이름도 바뀌었습니다: `BaremetalMicroVersionService` → `IronicMicroVersionService`, `BaremetalMicroVersionServiceImpl` → `IronicMicroVersionServiceImpl`, `BaremetalMicroVersions` → `IronicMicroVersions`, `BaseBaremetalServices` → `BaseIronicServices`, `BaremetalVersionDiscovery` → `IronicVersionDiscovery`. `BaremetalVersion`·`BaremetalPatch`·`BaremetalAttributes` 는 이름은 그대로이고 패키지만 `model.ironic`(`BaremetalAttributes` 는 `model.ironic.options`) 으로 옮겨졌습니다. 하위 서비스(`NodeService`, `SegmentService`, `LeaseService` 등)와 `Ironic*`·`Masakari*`·`Blazar*` 도메인 클래스 이름은 그대로입니다.
- 마이크로버전을 켜지 않았을 때 Ironic 오류 메시지가 안내하는 호출이 `os.ironic().microVersions().negotiate()` 로 바뀌었습니다.
- 그대로인 것: `ServiceType` 상수(`BAREMETAL`, `INSTANCE_HA`, `RESERVATION`, `ACCELERATOR`, `RATING`, `MESSAGING`, `REGISTRATION`, `OPTIMIZATION`, `CONTAINER_APP`), 카탈로그 매칭, `OpenStack-API-Version` 헤더 값(`baremetal`, `instance-ha`, `accelerator` 등)과 Ironic 의 `X-OpenStack-Ironic-API-Version` 헤더. 원본도 type 이름을 쓰는 상수가 많고 enum 이름이 카탈로그 매칭에 쓰이므로 바꾸지 않았습니다.
- 옮기는 방법(필요에 따라 항목을 골라 쓰세요; 일반 단어이므로 패키지·클래스는 전체 이름 기준으로 바꾸세요):

```sh
sed -i -E \
  -e 's/\.baremetal\(\)/.ironic()/g'        -e 's/org\.openstack4j\.(api|openstack|model)\.baremetal\b/org.openstack4j.\1.ironic/g' \
  -e 's/\.instanceHa\(\)/.masakari()/g'     -e 's/org\.openstack4j\.(api|openstack|model)\.instanceha\b/org.openstack4j.\1.masakari/g' \
  -e 's/\.reservation\(\)/.blazar()/g'      -e 's/org\.openstack4j\.(api|openstack|model)\.reservation\b/org.openstack4j.\1.blazar/g' \
  -e 's/\.accelerator\(\)/.cyborg()/g'      -e 's/org\.openstack4j\.(api|openstack|model)\.accelerator\b/org.openstack4j.\1.cyborg/g' \
  -e 's/\.rating\(\)/.cloudkitty()/g'       -e 's/org\.openstack4j\.(api|openstack|model)\.rating\b/org.openstack4j.\1.cloudkitty/g' \
  -e 's/\.messaging\(\)/.zaqar()/g'         -e 's/org\.openstack4j\.(api|openstack|model)\.messaging\b/org.openstack4j.\1.zaqar/g' \
  -e 's/\.registration\(\)/.adjutant()/g'   -e 's/org\.openstack4j\.(api|openstack|model)\.registration\b/org.openstack4j.\1.adjutant/g' \
  -e 's/\.optimization\(\)/.watcher()/g'    -e 's/org\.openstack4j\.(api|openstack|model)\.optimization\b/org.openstack4j.\1.watcher/g' \
  -e 's/\.containerApp\(\)/.zun()/g'        -e 's/org\.openstack4j\.(api|openstack|model)\.containerapp\b/org.openstack4j.\1.zun/g' \
  $(git grep -l 'openstack4j')
```

  진입 인터페이스·`Apis` 게터 이름은 위 표대로 직접 바꾸세요. 다른 객체의 같은 이름 메서드(예: 다른 서비스의 `reservation()`)까지 바꾸지 않도록 변경 후 diff 를 확인하세요.

# 4.13 → 4.14

컴파일에 영향을 주는 변경(추상 메서드 추가)과 요청·응답 동작이 바뀌는 변경은 아래와 같습니다.

- httpclient connector 의 재시도 규칙이 바뀌었습니다. 429·503 응답을 다시 보내는 대상이 GET·HEAD·PUT·DELETE·OPTIONS 로 한정되고, POST·PATCH 는 첫 응답(429·503)을 그대로 돌려줍니다. 이 재시도 전략은 `HttpClientConfigInterceptor.onClientCreate` 가 불리기 전에 설정되므로, 이전 동작이나 다른 규칙이 필요하면 인터셉터에서 `cb.setRetryStrategy(...)` 또는 `cb.disableAutomaticRetries()` 로 바꾸면 됩니다.
- httpclient connector 는 응답 본문을 끝까지 읽기 전에 닫으면 남은 본문을 끝까지 읽고 연결을 풀에 돌려줍니다(이전과 같음; 큰 다운로드도 나머지를 다 받습니다). 바로 끊고 싶으면 `HttpResponse` 를 직접 쥔 경우 `getInputStream()` 또는 `((HttpResponseImpl) response).unwrap().getEntity().getContent()` 가 돌려주는 스트림이 `org.apache.hc.core5.http.io.EofSensorInputStream` 이면(`instanceof` 로 확인; `Content-Encoding: gzip` 같은 압축 응답은 압축 해제 스트림이라 해당하지 않습니다) 그 `abort()`(4.x `abortConnection()` 의 5.x 이름)를 호출하세요. `imagesV2().download(...)` 가 돌려주는 스트림은 한 번 감싼 것이라 이 방법을 쓸 수 없으므로, 중간에 끊어야 하면 okhttp connector 를 쓰세요.
- Neutron QoS `netQosPolicy().update(...)`·`netQosPolicyBandwidthLimitRule().update(...)` 를 호출한 뒤에도 넘긴 객체의 `getId()` 가 그대로입니다(이전에는 `null`). bandwidth limit rule `create` 에 id 를 설정한 객체를 넘기면 이제 id 를 보내지 않습니다.
- Cinder `resetState(volumeId, Status)` 에 `ERROR_*` 상태를 넘기면 이제 성공합니다(이전에는 `error-extending` 처럼 보내 항상 400). `setStatus(volumeId, "error_extending", …)` 로 우회하던 코드는 그대로 동작합니다. `Volume.Status.value()` 는 이전과 같습니다.
- `ServerCreateBuilder` 에 추상 메서드 `accessIPv4(String)`·`accessIPv6(String)` 가 추가되었습니다. 이 인터페이스를 직접 구현한 코드는 새 메서드를 구현해야 합니다.

# 4.14 → 4.15

요청·응답 동작이 바뀌는 변경은 아래와 같습니다.

- Neutron `port().update(port)` 를 호출한 뒤에도 넘긴 객체의 `getId()`·`getNetworkId()`·`getState()`·`getTenantId()`·`getMacAddress()`·`getVifType()`·`getVifDetails()` 가 그대로입니다(이전에는 `null`). 수정 본문에서 `created_at`·`updated_at` 이 빠졌고 그 밖의 키는 이전과 같습니다(`mac_address` 는 이전처럼 보내지 않음) [live 확인 대기: created_at/updated_at 이 400 원인이었는지에 따라 문구 확정]. `port.toBuilder()` 는 이전처럼 같은 객체를 돌려주므로 builder 로 바꾼 값은 원본 객체에도 반영됩니다.
- Cinder quota set 을 `toBuilder()` 한 뒤에도 원본의 `getId()` 가 그대로입니다(이전에는 `null`). `quotaSets().get(...)`·`quotaClass(...)` 로 받은 객체를 그대로 `updateForTenant`·`updateQuotaClass` 에 넘기면 이전에는 `id` 가 본문에 들어갔지만 이제 보내지 않습니다(Cinder 는 이 키를 무시하므로 결과는 같음).
