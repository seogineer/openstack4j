OpenStack4j Connectors
======================

`io.github.seogineer:openstack4j` 는 Apache HttpClient 5 connector 를 기본으로 씁니다. 다른 connector 를 쓰려면 `openstack4j` 대신 `openstack4j-core` 를 의존하고, 아래 connector 중 **하나만** 추가하세요. connector 는 ServiceLoader 로 선택되므로 코드 변경은 필요 없습니다.

```xml
<dependency>
    <groupId>io.github.seogineer</groupId>
    <artifactId>openstack4j-core</artifactId>
    <version>4.0.0</version>
</dependency>
```

**Apache HttpClient 5**
```xml
<dependency>
    <groupId>io.github.seogineer</groupId>
    <artifactId>openstack4j-httpclient</artifactId>
    <version>4.0.0</version>
</dependency>
```

**OkHttp 4**
```xml
<dependency>
    <groupId>io.github.seogineer</groupId>
    <artifactId>openstack4j-okhttp</artifactId>
    <version>4.0.0</version>
</dependency>
```

**JDK HttpClient (외부 의존성 없음)**
```xml
<dependency>
    <groupId>io.github.seogineer</groupId>
    <artifactId>openstack4j-http-connector</artifactId>
    <version>4.0.0</version>
</dependency>
```

http-connector 의 TLS 제약:
- `Config.withHostnameVerifier(...)` 는 무시되고 WARN 로그가 남습니다(JDK HttpClient 에 해당 기능이 없음).
- `Config.withSSLVerificationDisabled()` 는 인증서 검증만 끕니다. 호스트명 검증까지 끄려면 JVM 옵션 `-Djdk.internal.httpclient.disableHostnameVerification=true` 를 쓰세요.

jersey2·resteasy connector 는 4.0 에서 제거되었습니다([MIGRATION.md](../MIGRATION.md)).
