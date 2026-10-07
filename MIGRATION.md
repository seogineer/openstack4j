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
- 새 서비스의 목록·생성·수정·동작 메서드는 404(extension 꺼짐, 상위 자원 없음)를 예외로 던집니다. 단건 `get(id)` 은 `null`, 삭제는 실패한 `ActionResponse` 입니다. `os.networking().extensions().isEnabled(alias)` 로 확인할 수 있습니다.

# 4.5 → 4.6

런타임 동작을 바꾸는 변경은 없습니다. 기존 image v2 메서드의 요청은 4.5 와 같습니다.

- `ImageService` 에 추상 메서드(`versions`, `info`, `cache`, `schemas`, `metadefs`, `importImage`, `stage`, `listLocations`, `addLocation`, `listTasks`, `deleteFromStore`)가 추가되었습니다. 이 인터페이스를 직접 구현한 가짜 구현은 새 메서드를 구현해야 합니다.
- 새 메서드는 서버 API 버전이 낮거나 기능이 꺼져 있으면 404 를 예외로 받습니다. `os.imagesV2().versions().supports("2.x")` 로 먼저 확인할 수 있습니다.
