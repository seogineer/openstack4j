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

