# Keystone identity v3 확장과 인증 방식 (E) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `os.identity()` 에 Keystone api-ref 의 누락 메서드 152개(410·브라우저 전용 제외)와 application credential·TOTP·system·trust 인증을 추가해 4.4.0 으로 배포한다.

**Architecture:** 기존 identity v3 서비스(`BaseIdentityServices` 상속, `EnforceVersionToURL("/v3")`)에 하위 리소스 메서드를 추가하고, 확장(OS-*)·limits·application credentials 는 새 서비스로 만든다. 인증은 `KeystoneAuth`(`AuthIdentity`/`AuthScope`) 모델과 `IOSClientBuilder.V3` 만 확장한다 — `OSAuthenticator.reAuthenticate()` 가 토큰의 `KeystoneAuth` 를 재사용하므로 재인증도 같은 방식으로 된다. 요청 본문에 맵·명시적 null 이 필요하면 `org.openstack4j.openstack.internal.microversion.JsonBody` 를 쓴다(D 에서 만든 공용 클래스).

**Tech Stack:** Java 17, Jackson 2.x, TestNG 7, OkHttp MockWebServer 4.12, Maven Wrapper.

**Spec:** `docs/superpowers/specs/2026-10-03-identity-extensions-design.md`

## Global Constraints

- 기존 메서드의 시그니처·반환 타입·동작은 바꾸지 않는다. `@Deprecated` 를 새로 붙이지 않는다. 기존 identity 테스트(core-test `api/identity/v3/*`)는 **수정 없이** 통과해야 한다.
- 추가하지 않는 것: OS-SIMPLE-CERT(`/OS-SIMPLE-CERT/ca|certificates`), OS-PKI(`/auth/tokens/OS-PKI/revoked`) — Keystone 이 410; websso 2개(브라우저 리다이렉트 전용).
- 모델 인터페이스에 추가하는 getter 는 `default`(기본 `null`). 서비스·빌더 인터페이스에는 추상 메서드를 추가한다(MIGRATION 에 기록).
- 회사 코드(`~/IdeaProjects/openstackit-java`)는 참고·복사하지 않는다.
- 각 Task = 브랜치 + PR, CI 통과 후 squash 머지. tag push(배포)는 사용자 지시("질문 없이 진행")에 따라 main CI 통과 확인 후 바로 한다.
- 커밋 끝 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` 과 `Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt`, PR 본문 끝 `🤖 Generated with [Claude Code](https://claude.com/claude-code)` 와 세션 링크.
- 빌드 `./mvnw`. core-test 클래스 실행: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='<Class>' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test`. 버전을 올린 직후에는 `-pl .,connectors,core,core-test` 로 부모 POM 도 설치한다.
- 전체 빌드는 `grep -q 'BUILD SUCCESS'` 로 성공을 확인한 뒤에만 커밋한다.
- PR 도우미: `.superpowers/pr.sh "<본문>"` (push → PR → CI 대기 → squash 머지, git-ignored).
- 새 테스트는 `org.openstack4j.api.identity.v3.ext` 패키지(core-test)에 둔다. 공통 기반 `AbstractIdentityExtTest`(Task 1) 를 상속한다. fixture 는 `core-test/src/main/resources/identity/v3/ext/`.
- 쿼리·경로 검사는 URL-디코딩한 경로로 비교한다(connector 마다 인코딩이 다르다). 테스트는 enqueue 한 응답 수만큼 `takeRequest()` 한다(누수 방지).
- 테스트 이름이 `Service` 를 쓰면 `AbstractTest.Service` 와 충돌하므로 모델 타입은 정규화된 이름으로 쓴다.

## Review Focus

1. **기존 인증의 요청 본문·재인증이 그대로여야 한다** — password/token/tokenless 의 `POST /auth/tokens` 본문, 토큰 만료 후 재인증 경로. → Task 1 `existingPasswordAuthBodyUnchanged` 와 기존 `KeystoneAuthenticationTests` 전부.
2. **application credential 로 받은 project-scoped 토큰** — `authenticateV3` 의 TokenAuth 분기(`auth.getScope().getProject()`)가 scope 없는 application credential 에서 NPE 를 내면 안 되고, 재인증 정보는 원래 application credential 이어야 한다. → Task 1 `applicationCredentialTokenKeepsCredentialsForReauth`.
3. **application credential 과 scope 를 같이 지정** — Keystone 이 400 을 주므로 요청 전에 `IllegalStateException`. → Task 1 `applicationCredentialWithScopeIsRejected`.
4. **OAuth1 서명** — 독립 구현(Python HMAC-SHA1)으로 계산한 기대값과 같아야 한다. → Task 12 `signatureMatchesIndependentVector`.
5. **XML·form 응답** — SAML2 metadata/assertion 은 XML, OAuth1 token 응답은 form-encoded 라 JSON 파서를 거치면 안 된다; project tag 에 공백·`/` 등이 들어가도 경로가 깨지면 안 된다. → Task 11 `saml2MetadataIsReturnedAsXml`, Task 12 `requestTokenParsesFormResponse`, Task 4 `tagWithSpacesIsEncodedInPath`.

---

## 실행 전 준비

- [ ] **P1: plan PR 머지** — 이 plan 을 `docs/identity-extensions-plan` 브랜치에 커밋해 `.superpowers/pr.sh` 로 머지한다.

### 공통 절차
- 각 Task 시작: `git switch main && git pull && git switch -c task/e<N>-<이름>`
- 끝: 전체 빌드 `./mvnw -B --no-transfer-progress install` 결과 `BUILD SUCCESS` 확인 → 커밋 → `.superpowers/pr.sh "<요약>"`.

### fixture 원본
개발용 Keystone 3.14 캡처: `/tmp/claude-1000/-home-seogineer-IdeaProjects/15c57ef7-653e-4e49-bba1-5d7ddc3e068a/scratchpad/keystone/live/*.json`(`capture.sh` 로 재생성). 테스트에는 필요한 부분만 인라인 JSON 으로 옮기고 호스트는 `127.0.0.1:5000` 으로 바꾼다.

---

### Task 1: 인증 보강 — application credential, TOTP/MFA, system·trust scope, 토큰 필드

**Files:**
- Modify: `core/src/main/java/org/openstack4j/openstack/common/Auth.java`(`Type.APPLICATION_CREDENTIAL`)
- Modify: `core/src/main/java/org/openstack4j/openstack/identity/v3/domain/KeystoneAuth.java`
- Modify: `core/src/main/java/org/openstack4j/model/identity/v3/Authentication.java`(필요 시 `Identity` 에 default getter)
- Modify: `core/src/main/java/org/openstack4j/api/client/IOSClientBuilder.java`(V3), `core/src/main/java/org/openstack4j/openstack/client/OSClientBuilder.java`(ClientV3)
- Modify: `core/src/main/java/org/openstack4j/openstack/internal/OSAuthenticator.java`(재인증 컨텍스트 분기)
- Modify: `core/src/main/java/org/openstack4j/model/identity/v3/Token.java`, `core/src/main/java/org/openstack4j/openstack/identity/v3/domain/KeystoneToken.java`
- Create: `core-test/src/main/java/org/openstack4j/api/identity/v3/ext/AbstractIdentityExtTest.java`, `IdentityAuthMethodTests.java`

**Interfaces:**
- Produces:
  - `IOSClientBuilder.V3`: `V3 applicationCredential(String id, String secret)`, `V3 applicationCredential(String name, String secret, Identifier user, Identifier userDomain)`, `V3 passcode(String passcode)`, `V3 scopeToSystem()`, `V3 scopeToTrust(String trustId)`
  - `KeystoneAuth`: 생성자 `KeystoneAuth(AuthIdentity identity, AuthScope scope, Type type)`; `AuthIdentity.createApplicationCredentialType(String id, String secret)`, `createApplicationCredentialType(String name, String secret, Identifier user, Identifier userDomain)`, `AuthIdentity withTotp(Identifier user, Identifier domain, String passcode)`(인스턴스, methods 에 `totp` 추가); `AuthScope.system()`
  - `Token`: `default Map<String, Object> getSystem()`, `default Map<String, Object> getApplicationCredential()`, `default Map<String, Object> getTrust()`
  - core-test `AbstractIdentityExtTest`: `service()` = IDENTITY, `body(RecordedRequest)`, `decodedPath(RecordedRequest)`, `assertNoMoreRequests()`, `static Map<String, String> tokenHeaders(String id)`

- [ ] **Step 1: 테스트 기반 클래스와 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/e1-auth-methods
mkdir -p core-test/src/main/java/org/openstack4j/api/identity/v3/ext core-test/src/main/resources/identity/v3/ext
```

`AbstractIdentityExtTest.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;

/** Base for the identity extension tests: Keystone on the mock server's identity port, v3 token. */
public abstract class AbstractIdentityExtTest extends AbstractTest {

    protected static final String USER = "e365357fdf4d4a37a07fde3209cac4aa";
    protected static final String PROJECT = "2580a7b51d564c1d848ee27fda2db713";

    @Override
    protected Service service() {
        return Service.IDENTITY;
    }

    protected static Map<String, String> tokenHeaders(String tokenId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Subject-Token", tokenId);
        headers.put("Content-Type", "application/json");
        return headers;
    }

    protected JsonNode body(RecordedRequest request) throws IOException {
        return new ObjectMapper().readTree(request.getBody().clone().readUtf8());
    }

    /** The request path with percent-escapes decoded; connectors encode reserved characters differently. */
    protected static String decodedPath(RecordedRequest request) {
        return URLDecoder.decode(request.getPath(), StandardCharsets.UTF_8);
    }

    protected void assertNoMoreRequests() throws InterruptedException {
        Assert.assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS), "unexpected extra request");
    }
}
```

`IdentityAuthMethodTests.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.identity.v3.domain.KeystoneAuth;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/AuthMethods")
public class IdentityAuthMethodTests extends AbstractIdentityExtTest {

    private static final String TOKEN = "gAAAAABqwMlkDR4imx9eF";
    /** Application credential token: project-scoped, methods [application_credential]. */
    private static final String APP_CRED_TOKEN = "{\"token\": {\"methods\": [\"application_credential\"], \"user\": {\"domain\": {\"id\": \"default\", \"name\": \"Default\"},"
            + " \"id\": \"" + USER + "\", \"name\": \"admin\", \"password_expires_at\": null}, \"audit_ids\": [\"9YW6EsOqSNuX0I2eleHe-Q\"],"
            + " \"expires_at\": \"2026-10-04T09:22:44.000000Z\", \"issued_at\": \"2026-10-03T09:22:44.000000Z\","
            + " \"project\": {\"domain\": {\"id\": \"default\", \"name\": \"Default\"}, \"id\": \"" + PROJECT + "\", \"name\": \"admin\"},"
            + " \"is_domain\": false, \"roles\": [{\"id\": \"1222de53b11f40e68497383c7636fbe0\", \"name\": \"admin\"}],"
            + " \"application_credential\": {\"id\": \"a05ec0a2f3ac4e8cbd1b3f1c467c0fc5\", \"name\": \"os4j-fixture\", \"restricted\": true},"
            + " \"catalog\": []}}";
    private static final String SYSTEM_TOKEN = "{\"token\": {\"methods\": [\"password\"], \"user\": {\"domain\": {\"id\": \"default\", \"name\": \"Default\"}, \"id\": \"" + USER + "\", \"name\": \"admin\"},"
            + " \"audit_ids\": [\"a\"], \"expires_at\": \"2026-10-04T09:22:44.000000Z\", \"issued_at\": \"2026-10-03T09:22:44.000000Z\","
            + " \"system\": {\"all\": true}, \"roles\": [{\"id\": \"1222de53b11f40e68497383c7636fbe0\", \"name\": \"admin\"}], \"catalog\": []}}";

    private JsonNode authBody() throws Exception {
        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/v3/auth/tokens"), request.getPath());
        return body(request).get("auth");
    }

    public void applicationCredentialById() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, APP_CRED_TOKEN);

        OSClientV3 os = OSFactory.builderV3().endpoint(authURL("/v3")).applicationCredential("a05ec0a2f3ac4e8cbd1b3f1c467c0fc5", "s3cret").authenticate();

        JsonNode auth = authBody();
        Assert.assertEquals(auth.get("identity").get("methods").get(0).asText(), "application_credential");
        JsonNode ac = auth.get("identity").get("application_credential");
        Assert.assertEquals(ac.get("id").asText(), "a05ec0a2f3ac4e8cbd1b3f1c467c0fc5");
        Assert.assertEquals(ac.get("secret").asText(), "s3cret");
        Assert.assertFalse(auth.has("scope"));
        Assert.assertEquals(os.getToken().getProject().getId(), PROJECT);
        Assert.assertEquals(os.getToken().getApplicationCredential().get("name"), "os4j-fixture");
    }

    public void applicationCredentialByNameAndUser() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, APP_CRED_TOKEN);

        OSFactory.builderV3().endpoint(authURL("/v3"))
                .applicationCredential("os4j-fixture", "s3cret", Identifier.byName("admin"), Identifier.byName("Default")).authenticate();

        JsonNode ac = authBody().get("identity").get("application_credential");
        Assert.assertEquals(ac.get("name").asText(), "os4j-fixture");
        Assert.assertEquals(ac.get("user").get("name").asText(), "admin");
        Assert.assertEquals(ac.get("user").get("domain").get("name").asText(), "Default");
        Assert.assertFalse(ac.has("id"));
    }

    public void applicationCredentialTokenKeepsCredentialsForReauth() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, APP_CRED_TOKEN);

        OSClientV3 os = OSFactory.builderV3().endpoint(authURL("/v3")).applicationCredential("a05ec0a2f3ac4e8cbd1b3f1c467c0fc5", "s3cret").authenticate();
        takeRequest();

        Assert.assertTrue(os.getToken().getCredentials() instanceof KeystoneAuth);
        KeystoneAuth stored = (KeystoneAuth) os.getToken().getCredentials();
        Assert.assertEquals(stored.getType(), org.openstack4j.openstack.common.Auth.Type.APPLICATION_CREDENTIAL);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*scope.*")
    public void applicationCredentialWithScopeIsRejected() throws Exception {
        try {
            OSFactory.builderV3().endpoint(authURL("/v3")).applicationCredential("a", "s").scopeToProject(Identifier.byId(PROJECT)).authenticate();
        } finally {
            assertNoMoreRequests();
        }
    }

    public void passwordWithTotpIsMultiFactor() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, SYSTEM_TOKEN.replace("\"system\": {\"all\": true}, ", ""));

        OSFactory.builderV3().endpoint(authURL("/v3")).credentials("admin", "pw", Identifier.byName("Default")).passcode("123456").authenticate();

        JsonNode identity = authBody().get("identity");
        Assert.assertEquals(identity.get("methods").size(), 2);
        Assert.assertEquals(identity.get("methods").get(0).asText(), "password");
        Assert.assertEquals(identity.get("methods").get(1).asText(), "totp");
        Assert.assertEquals(identity.get("password").get("user").get("password").asText(), "pw");
        JsonNode totp = identity.get("totp").get("user");
        Assert.assertEquals(totp.get("name").asText(), "admin");
        Assert.assertEquals(totp.get("domain").get("name").asText(), "Default");
        Assert.assertEquals(totp.get("passcode").asText(), "123456");
    }

    public void totpOnly() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, SYSTEM_TOKEN.replace("\"system\": {\"all\": true}, ", ""));

        OSFactory.builderV3().endpoint(authURL("/v3")).credentials(USER, null).passcode("654321").authenticate();

        JsonNode identity = authBody().get("identity");
        Assert.assertEquals(identity.get("methods").size(), 1);
        Assert.assertEquals(identity.get("methods").get(0).asText(), "totp");
        Assert.assertFalse(identity.has("password"));
        Assert.assertEquals(identity.get("totp").get("user").get("id").asText(), USER);
    }

    public void systemScope() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, SYSTEM_TOKEN);

        OSClientV3 os = OSFactory.builderV3().endpoint(authURL("/v3")).credentials(USER, "pw").scopeToSystem().authenticate();

        Assert.assertTrue(authBody().get("scope").get("system").get("all").asBoolean());
        Assert.assertEquals(os.getToken().getSystem().get("all"), Boolean.TRUE);
    }

    public void trustScope() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, APP_CRED_TOKEN.replace("\"application_credential\"", "\"password\""));

        OSFactory.builderV3().endpoint(authURL("/v3")).token("t0").scopeToTrust("c947c7f50eff44afaaa94782b90fd395").authenticate();

        Assert.assertEquals(authBody().get("scope").get("OS-TRUST:trust").get("id").asText(), "c947c7f50eff44afaaa94782b90fd395");
    }

    public void existingPasswordAuthBodyUnchanged() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, APP_CRED_TOKEN.replace("\"application_credential\"", "\"password\""));

        OSFactory.builderV3().endpoint(authURL("/v3")).credentials("admin", "pw", Identifier.byName("Default"))
                .scopeToProject(Identifier.byId(PROJECT)).authenticate();

        JsonNode auth = authBody();
        Assert.assertEquals(auth.toString(), "{\"identity\":{\"password\":{\"user\":{\"name\":\"admin\",\"domain\":{\"name\":\"Default\"},\"password\":\"pw\"}},"
                + "\"methods\":[\"password\"]},\"scope\":{\"project\":{\"id\":\"" + PROJECT + "\"}}}");
    }
}
```
주의: `existingPasswordAuthBodyUnchanged` 의 기대 문자열은 **변경 전 코드로 먼저 실행해서 얻은 실제 본문**으로 채운다(Step 2 에서 그 테스트만 통과해야 한다 — 필드 순서는 Jackson 이 정하므로 추측하지 않는다). 다르면 실제 값으로 고치고 Ruling 으로 남긴다.

- [ ] **Step 2: 실행 → 컴파일 실패 확인, 기준선 본문 확보**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -5`
Expected: `applicationCredential`, `passcode`, `scopeToSystem`, `getSystem` 등 없음.

기준선: 위 테스트 파일에서 `existingPasswordAuthBodyUnchanged` 를 제외한 메서드를 잠시 주석 처리하고 실행해 실제 본문을 확인한 뒤 기대 문자열을 맞추고 주석을 되돌린다.

- [ ] **Step 3: 모델 확장**

`Auth.java`: `public enum Type {CREDENTIALS, TOKEN, RAX_APIKEY, TOKENLESS, APPLICATION_CREDENTIAL}`.

`KeystoneAuth.java`:
- 생성자 추가:
  ```java
      public KeystoneAuth(AuthIdentity identity, AuthScope scope, Type type) {
          this.identity = identity;
          this.scope = scope;
          this.type = type;
      }
  ```
- `AuthIdentity` 필드 추가(직렬화 이름 그대로):
  ```java
          @JsonProperty("application_credential")
          private AuthApplicationCredential applicationCredential;
          @JsonProperty("totp")
          private AuthTotp totp;
  ```
  기존 `password`/`token` 필드는 그대로 두고, 위 두 필드는 null 이면 `NON_NULL` 로 빠진다.
- 팩토리:
  ```java
          public static AuthIdentity createApplicationCredentialType(String id, String secret) {
              AuthIdentity identity = new AuthIdentity();
              identity.methods.add("application_credential");
              identity.applicationCredential = AuthApplicationCredential.byId(id, secret);
              return identity;
          }

          public static AuthIdentity createApplicationCredentialType(String name, String secret, Identifier user, Identifier userDomain) {
              AuthIdentity identity = new AuthIdentity();
              identity.methods.add("application_credential");
              identity.applicationCredential = AuthApplicationCredential.byName(name, secret, user, userDomain);
              return identity;
          }

          /** Password+TOTP when a password was given, TOTP alone otherwise. */
          public static AuthIdentity createCredentialType(String user, String password, Identifier domain, String passcode) {
              AuthIdentity identity = password == null ? new AuthIdentity() : createCredentialType(user, password, domain);
              identity.methods.add("totp");
              identity.totp = new AuthTotp(user, domain, passcode);
              return identity;
          }
  ```
  (기존 `createCredentialType` 들은 package-private `static` 이다 — 새 메서드는 `public static` 으로 두고 builder 에서 쓴다.)
- 중첩 클래스:
  ```java
          public static final class AuthApplicationCredential implements Serializable {
              private static final long serialVersionUID = 1L;
              @JsonProperty("id") private String id;
              @JsonProperty("name") private String name;
              @JsonProperty("secret") private String secret;
              @JsonProperty("user") private AuthPassword.AuthUser user;

              static AuthApplicationCredential byId(String id, String secret) {
                  AuthApplicationCredential c = new AuthApplicationCredential();
                  c.id = id;
                  c.secret = secret;
                  return c;
              }

              static AuthApplicationCredential byName(String name, String secret, Identifier user, Identifier userDomain) {
                  AuthApplicationCredential c = new AuthApplicationCredential();
                  c.name = name;
                  c.secret = secret;
                  c.user = AuthPassword.AuthUser.of(user, userDomain, null);
                  return c;
              }

              public String getId() { return id; }
              public String getName() { return name; }
          }

          public static final class AuthTotp implements Serializable {
              private static final long serialVersionUID = 1L;
              @JsonProperty("user") private TotpUser user;

              AuthTotp(String user, Identifier domain, String passcode) {
                  this.user = new TotpUser(user, domain, passcode);
              }

              public static final class TotpUser extends BasicResourceEntity {
                  private static final long serialVersionUID = 1L;
                  @JsonProperty("domain") private AuthPassword.AuthUser.AuthDomain domain;
                  @JsonProperty("passcode") private String passcode;

                  TotpUser(String user, Identifier domainIdentifier, String passcode) {
                      this.passcode = passcode;
                      if (domainIdentifier != null) {
                          domain = new AuthPassword.AuthUser.AuthDomain();
                          if (domainIdentifier.isTypeID()) domain.setId(domainIdentifier.getId());
                          else domain.setName(domainIdentifier.getId());
                          setName(user);
                      } else
                          setId(user);
                  }
              }
          }
  ```
  `AuthPassword.AuthUser` 에 정적 팩토리 추가(application credential 의 user 용; password 는 null 이라 빠진다):
  ```java
                  static AuthUser of(Identifier user, Identifier domainIdentifier, String password) {
                      AuthUser u = new AuthUser();
                      u.password = password;
                      if (user.isTypeID()) u.setId(user.getId()); else u.setName(user.getId());
                      if (domainIdentifier != null) {
                          u.domain = new AuthDomain();
                          if (domainIdentifier.isTypeID()) u.domain.setId(domainIdentifier.getId());
                          else u.domain.setName(domainIdentifier.getId());
                      }
                      return u;
                  }
  ```
  `AuthDomain` 의 생성자가 private 이면 package 접근으로 바꾼다(같은 파일 안이라 문제 없음).
- `KeystoneAuth.getUsername()/getPassword()/getId()/getName()` 은 `identity.getPassword()` 가 null 일 때 NPE 를 내지 않도록 `identity == null || identity.getPassword() == null ? null : ...` 로 감싼다(application credential·TOTP 단독 인증에서 호출돼도 안전하게).
- `AuthScope`:
  ```java
          @JsonProperty("system")
          private Map<String, Boolean> system;

          /** System scope: {@code {"system": {"all": true}}}. */
          public static AuthScope system() {
              AuthScope scope = new AuthScope((ScopeProject) null);
              scope.system = Collections.singletonMap("all", Boolean.TRUE);
              return scope;
          }
  ```

`KeystoneToken.java`: 필드 `@JsonProperty("system") private Map<String, Object> system; @JsonProperty("application_credential") private Map<String, Object> applicationCredential; @JsonProperty("OS-TRUST:trust") private Map<String, Object> trust;` 와 `@Override` getter 3개. `Token.java`: 같은 이름의 `default` getter(`null`), Javadoc 한 줄씩.

- [ ] **Step 4: builder 와 재인증 분기**

`IOSClientBuilder.V3` 에 5개 메서드 선언(Javadoc: 동작과 제약 — application credential 은 scope 를 받지 않는다, passcode 는 TOTP).

`OSClientBuilder.ClientV3`:
```java
        String applicationCredentialId;
        String applicationCredentialName;
        String applicationCredentialSecret;
        Identifier applicationCredentialUser;
        Identifier applicationCredentialUserDomain;
        String passcode;

        @Override
        public ClientV3 applicationCredential(String id, String secret) {
            this.applicationCredentialId = id;
            this.applicationCredentialSecret = secret;
            return this;
        }

        @Override
        public ClientV3 applicationCredential(String name, String secret, Identifier user, Identifier userDomain) {
            this.applicationCredentialName = name;
            this.applicationCredentialSecret = secret;
            this.applicationCredentialUser = user;
            this.applicationCredentialUserDomain = userDomain;
            return this;
        }

        @Override
        public ClientV3 passcode(String passcode) {
            this.passcode = passcode;
            return this;
        }

        @Override
        public ClientV3 scopeToSystem() {
            this.scope = AuthScope.system();
            return this;
        }

        @Override
        public ClientV3 scopeToTrust(String trustId) {
            this.scope = AuthScope.trust(trustId);
            return this;
        }
```
`authenticate()` 에서 token 분기 다음, credentials 분기 앞에:
```java
            // application credential (its token is scoped by the credential itself)
            if (applicationCredentialSecret != null) {
                if (scope != null)
                    throw new IllegalStateException("An application credential cannot be combined with a scope; the credential is already scoped");
                AuthIdentity identity = applicationCredentialId != null
                        ? AuthIdentity.createApplicationCredentialType(applicationCredentialId, applicationCredentialSecret)
                        : AuthIdentity.createApplicationCredentialType(applicationCredentialName, applicationCredentialSecret, applicationCredentialUser, applicationCredentialUserDomain);
                return (OSClientV3) OSAuthenticator.invoke(new KeystoneAuth(identity, null, Auth.Type.APPLICATION_CREDENTIAL), endpoint, perspective, config, provider);
            }
            // password and/or TOTP
            if (user != null && user.length() > 0 && passcode != null)
                return (OSClientV3) OSAuthenticator.invoke(new KeystoneAuth(AuthIdentity.createCredentialType(user, password, domain, passcode), scope, Auth.Type.CREDENTIALS),
                        endpoint, perspective, config, provider);
```
(`AuthScope.trust(String)` 이 이미 있는지 확인 — 있으면 그대로, 없으면 `new AuthScope(new ScopeTrust(id))` 를 반환하는 static 을 추가한다.)

`OSAuthenticator.authenticateV3`: `if (auth.getType().equals(Type.CREDENTIALS))` 를 `if (auth.getType() == Type.CREDENTIALS || auth.getType() == Type.APPLICATION_CREDENTIAL)` 로 바꾼다(application credential 은 scope 가 없어 TokenAuth 분기의 `auth.getScope().getProject()` 가 NPE 를 낸다; 재인증도 같은 credential 로 해야 한다). TOTP 를 포함한 CREDENTIALS 의 재인증은 만료된 passcode 를 다시 보내 실패할 수 있다 — Javadoc 에 "MFA 세션은 토큰 만료 후 다시 `authenticate()` 해야 한다"고 적는다.

- [ ] **Step 5: 통과 확인 (Review Focus 1~3)**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='IdentityAuthMethodTests,KeystoneAuthenticationTests,KeystoneTokenlessTest' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Failures: 0, Errors: 0` (IdentityAuthMethodTests 9개, 기존 인증 테스트 그대로).

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/e1.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/e1.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/e1.log
git add -A && git commit -m "feat(identity): authenticate with application credentials, TOTP/MFA, system and trust scope

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "인증 보강: application credential(id / name+user), passcode(TOTP, password+TOTP MFA), scopeToSystem, scopeToTrust; 토큰 system/application_credential/trust 필드; application credential 재인증 컨텍스트."
```

---

### Task 2: 응답 모델 필드와 users/projects 목록 옵션

**Files:**
- Modify: `core/src/main/java/org/openstack4j/model/identity/v3/User.java`, `Project.java`, `Domain.java`, `Role.java`
- Modify: `core/src/main/java/org/openstack4j/openstack/identity/v3/domain/KeystoneUser.java`, `KeystoneProject.java`, `KeystoneDomain.java`, `KeystoneRole.java`
- Create: `core/src/main/java/org/openstack4j/model/identity/v3/options/UserListOptions.java`, `ProjectListOptions.java`
- Modify: `api/identity/v3/UserService.java`, `ProjectService.java` 와 구현
- Create: `core-test/src/main/java/org/openstack4j/api/identity/v3/ext/IdentityModelTests.java`

**Interfaces:**
- Consumes: Task 1 `AbstractIdentityExtTest`.
- Produces:
  - `User`: `default Map<String, Object> getOptions()`, `default Date getPasswordExpiresAt()`, `default List<Map<String, Object>> getFederated()`
  - `Project`: `default Boolean getIsDomain()`, `default Map<String, Object> getOptions()`, `default List<String> getTags()`
  - `Domain`: `default Map<String, Object> getOptions()`, `default List<String> getTags()`
  - `Role`: `default String getDescription()`, `default Map<String, Object> getOptions()`
  - `UserListOptions.create()`: `name`, `domainId`, `enabled(boolean)`, `idpId`, `protocolId`, `uniqueId`, `passwordExpiresAt(String op, String isoTime)`; `Map<String, String> toQueryParams()`
  - `ProjectListOptions.create()`: `name`, `domainId`, `parentId`, `enabled(boolean)`, `isDomain(boolean)`, `tags(String...)`, `tagsAny(String...)`, `notTags(String...)`, `notTagsAny(String...)`; `toQueryParams()`
  - `UserService.list(UserListOptions)`, `ProjectService.list(ProjectListOptions)`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/e2-models
```

`IdentityModelTests.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import java.util.Arrays;
import java.util.List;

import org.openstack4j.model.identity.v3.Domain;
import org.openstack4j.model.identity.v3.Project;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.model.identity.v3.User;
import org.openstack4j.model.identity.v3.options.ProjectListOptions;
import org.openstack4j.model.identity.v3.options.UserListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/Models")
public class IdentityModelTests extends AbstractIdentityExtTest {

    public void userReadsOptionsAndPasswordExpiry() throws Exception {
        respondWith(200, "{\"user\": {\"id\": \"" + USER + "\", \"name\": \"admin\", \"domain_id\": \"default\", \"enabled\": true,"
                + " \"password_expires_at\": \"2027-01-01T00:00:00.000000\", \"options\": {\"ignore_password_expiry\": true},"
                + " \"federated\": [{\"idp_id\": \"idp1\", \"protocols\": [{\"protocol_id\": \"saml2\", \"unique_id\": \"u1\"}]}], \"links\": {}}}");

        User user = osv3().identity().users().get(USER);
        takeRequest();

        Assert.assertNotNull(user.getPasswordExpiresAt());
        Assert.assertEquals(user.getOptions().get("ignore_password_expiry"), Boolean.TRUE);
        Assert.assertEquals(user.getFederated().get(0).get("idp_id"), "idp1");
    }

    public void projectReadsIsDomainOptionsTags() throws Exception {
        respondWith(200, "{\"project\": {\"id\": \"" + PROJECT + "\", \"name\": \"admin\", \"domain_id\": \"default\", \"enabled\": true,"
                + " \"is_domain\": false, \"parent_id\": \"default\", \"options\": {\"immutable\": false}, \"tags\": [\"prod\", \"web\"], \"links\": {}}}");

        Project project = osv3().identity().projects().get(PROJECT);
        takeRequest();

        Assert.assertEquals(project.getIsDomain(), Boolean.FALSE);
        Assert.assertEquals(project.getOptions().get("immutable"), Boolean.FALSE);
        Assert.assertEquals(project.getTags(), Arrays.asList("prod", "web"));
    }

    public void domainAndRoleReadOptions() throws Exception {
        respondWith(200, "{\"domain\": {\"id\": \"default\", \"name\": \"Default\", \"description\": \"The default domain\", \"enabled\": true, \"tags\": [\"t\"], \"options\": {\"immutable\": true}, \"links\": {}}}");
        respondWith(200, "{\"roles\": [{\"id\": \"1222de53b11f40e68497383c7636fbe0\", \"name\": \"admin\", \"domain_id\": null, \"description\": \"Administrator\", \"options\": {\"immutable\": true}, \"links\": {}}]}");

        Domain domain = osv3().identity().domains().get("default");
        List<? extends Role> roles = osv3().identity().roles().list();
        takeRequest();
        takeRequest();

        Assert.assertEquals(domain.getTags(), Arrays.asList("t"));
        Assert.assertEquals(domain.getOptions().get("immutable"), Boolean.TRUE);
        Assert.assertEquals(roles.get(0).getDescription(), "Administrator");
        Assert.assertEquals(roles.get(0).getOptions().get("immutable"), Boolean.TRUE);
    }

    public void listOptionsBecomeQuery() throws Exception {
        respondWith(200, "{\"users\": [], \"links\": {}}");
        respondWith(200, "{\"projects\": [], \"links\": {}}");

        osv3().identity().users().list(UserListOptions.create().domainId("default").enabled(true).idpId("idp1").passwordExpiresAt("lt", "2027-01-01T00:00:00Z"));
        osv3().identity().projects().list(ProjectListOptions.create().tags("prod", "web").notTagsAny("old").isDomain(false).parentId("default"));

        String users = decodedPath(takeRequest());
        for (String part : new String[] {"/v3/users?", "domain_id=default", "enabled=true", "idp_id=idp1", "password_expires_at=lt:2027-01-01T00:00:00Z"})
            Assert.assertTrue(users.contains(part), users + " lacks " + part);
        String projects = decodedPath(takeRequest());
        for (String part : new String[] {"/v3/projects?", "tags=prod,web", "not-tags-any=old", "is_domain=false", "parent_id=default"})
            Assert.assertTrue(projects.contains(part), projects + " lacks " + part);
    }

    public void existingProjectCreateBodyHasNoResponseOnlyFields() throws Exception {
        respondWith(201, "{\"project\": {\"id\": \"p1\", \"name\": \"n\", \"domain_id\": \"default\", \"enabled\": true, \"links\": {}}}");

        osv3().identity().projects().create(org.openstack4j.api.Builders.project().name("n").domainId("default").build());

        com.fasterxml.jackson.databind.JsonNode body = body(takeRequest()).get("project");
        Assert.assertFalse(body.has("options"));
        Assert.assertFalse(body.has("tags"));
        Assert.assertFalse(body.has("is_domain"));
    }
}
```
(`Builders.project()` 의 실제 이름을 확인한다. `osv3().identity()` 의 요청은 테스트 token 의 identity endpoint(`127.0.0.1:5000/v3`)로 간다.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `getOptions`, `UserListOptions` 등 없음.

- [ ] **Step 3: 모델 필드**

인터페이스에 Interfaces 의 `default` getter 를 추가한다(Javadoc 한 줄). 도메인 클래스:
- `KeystoneUser`: `@JsonProperty("options") private Map<String, Object> options; @JsonProperty("password_expires_at") private Date passwordExpiresAt; @JsonProperty("federated") private List<Map<String, Object>> federated;` + `@Override` getter. 이 클래스는 생성·수정 요청 본문으로도 쓰이므로, 세 필드는 빌더로 설정하지 않는 한 null 이라 `NON_NULL` 로 빠진다. 단 `KeystoneUser` 의 update 가 get 결과를 `toBuilder()` 로 재사용하면 `password_expires_at` 이 다시 전송될 수 있다 — Keystone 은 user update 의 `password_expires_at` 를 거부하지 않으므로(읽기 전용 무시) 그대로 둔다(Ruling 기록).
- `KeystoneProject`: `@JsonProperty("is_domain") private Boolean isDomain; @JsonProperty("options") private Map<String, Object> options; @JsonProperty("tags") private List<String> tags;` + getter.
- `KeystoneDomain`: `options`, `tags` + getter.
- `KeystoneRole`: `@JsonProperty("description") private String description; @JsonProperty("options") private Map<String, Object> options;` + getter. (`description` 은 role 생성에도 유효하다 — 빌더에 `description(String)` 이 이미 있는지 확인; 없으면 그대로.)

- [ ] **Step 4: 목록 옵션과 서비스**

`model/identity/v3/options/UserListOptions.java`:
```java
package org.openstack4j.model.identity.v3.options;

import java.util.LinkedHashMap;
import java.util.Map;

/** Filters for {@code GET /v3/users}. */
public class UserListOptions {

    private final Map<String, String> params = new LinkedHashMap<>();

    public static UserListOptions create() {
        return new UserListOptions();
    }

    private UserListOptions put(String key, Object value) {
        params.put(key, String.valueOf(value));
        return this;
    }

    public UserListOptions name(String name) { return put("name", name); }
    public UserListOptions domainId(String domainId) { return put("domain_id", domainId); }
    public UserListOptions enabled(boolean enabled) { return put("enabled", enabled); }
    /** Users federated through this identity provider. */
    public UserListOptions idpId(String idpId) { return put("idp_id", idpId); }
    public UserListOptions protocolId(String protocolId) { return put("protocol_id", protocolId); }
    public UserListOptions uniqueId(String uniqueId) { return put("unique_id", uniqueId); }
    /** Time comparison filter: {@code op} is lt, lte, gt, gte, eq or neq. */
    public UserListOptions passwordExpiresAt(String op, String isoTime) { return put("password_expires_at", op + ":" + isoTime); }

    public Map<String, String> toQueryParams() {
        return new LinkedHashMap<>(params);
    }
}
```
`ProjectListOptions.java`: 같은 구조, `name`, `domainId`(domain_id), `parentId`(parent_id), `enabled`, `isDomain`(is_domain), `tags(String...)`(tags, `String.join(",", ...)`), `tagsAny`(tags-any), `notTags`(not-tags), `notTagsAny`(not-tags-any).

`UserService`: `List<? extends User> list(UserListOptions options);` — 구현 `get(Users.class, uri(PATH_USERS)).params(options.toQueryParams()).execute().getList()`. `ProjectService`: 같은 방식(`Projects`, `PATH_PROJECTS`). 기존 `list()` 와의 null 모호성은 없다(기존에 1-인자 `list(...)` 오버로드가 없는지 확인; 있으면 MIGRATION 에 적는다).

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='IdentityModelTests,KeystoneUserServiceTests,KeystoneProjectServiceTests,KeystoneDomainServiceTests,KeystoneRoleServiceTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Failures: 0, Errors: 0` (IdentityModelTests 5개, 기존 테스트 그대로).

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/e2.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/e2.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/e2.log
git add -A && git commit -m "feat(identity): read options/tags/is_domain/password_expires_at and add typed user/project list filters

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "모델 필드(user options/password_expires_at/federated, project is_domain/options/tags, domain options/tags, role description/options), UserListOptions, ProjectListOptions(tags 필터)."
```

---

### Task 3: application credentials와 access rules

**Files:**
- Create (`core/src/main/java/org/openstack4j/`): `model/identity/v3/ApplicationCredential.java`, `model/identity/v3/AccessRule.java`, `model/identity/v3/options/ApplicationCredentialCreate.java`, `api/identity/v3/ApplicationCredentialService.java`, `openstack/identity/v3/domain/KeystoneApplicationCredential.java`, `openstack/identity/v3/domain/KeystoneAccessRule.java`, `openstack/identity/v3/internal/ApplicationCredentialServiceImpl.java`
- Modify: `api/identity/v3/IdentityService.java`, `openstack/identity/v3/internal/IdentityServiceImpl.java`, `openstack/provider/DefaultAPIProvider.java`, `api/identity/v3/UserService.java`, `openstack/identity/v3/internal/UserServiceImpl.java`
- Create: `core-test/src/main/java/org/openstack4j/api/identity/v3/ext/ApplicationCredentialTests.java`

**Interfaces:**
- Consumes: Task 1 `AbstractIdentityExtTest`; `JsonBody`(D).
- Produces:
  - `IdentityService.applicationCredentials()` → `ApplicationCredentialService`: `List<? extends ApplicationCredential> list(String userId)`, `list(String userId, String name)`, `ApplicationCredential get(String userId, String id)`, `ApplicationCredential create(String userId, ApplicationCredentialCreate create)`, `ActionResponse delete(String userId, String id)`
  - `ApplicationCredential`: `getId()`, `getName()`, `getDescription()`, `getUserId()`, `getProjectId()`, `Date getExpiresAt()`, `Boolean getUnrestricted()`, `List<? extends Role> getRoles()`, `List<? extends AccessRule> getAccessRules()`, `getSecret()`(생성 응답에만), `Map<String, Object> getSystem()`
  - `AccessRule`: `getId()`, `getService()`, `getPath()`, `getMethod()`
  - `ApplicationCredentialCreate.create(String name)`: `description`, `secret`(지정 안 하면 Keystone 이 생성), `expiresAt(Date)`, `unrestricted(boolean)`, `roleIds(String...)`, `roleNames(String...)`, `accessRule(String service, String method, String path)`; `Map<String, Object> toMap()`
  - `UserService`: `List<? extends AccessRule> accessRules(String userId)`, `AccessRule getAccessRule(String userId, String accessRuleId)`, `ActionResponse deleteAccessRule(String userId, String accessRuleId)`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/e3-application-credentials
```

`ApplicationCredentialTests.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.AccessRule;
import org.openstack4j.model.identity.v3.ApplicationCredential;
import org.openstack4j.model.identity.v3.options.ApplicationCredentialCreate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/ApplicationCredentials")
public class ApplicationCredentialTests extends AbstractIdentityExtTest {

    private static final String AC = "a05ec0a2f3ac4e8cbd1b3f1c467c0fc5";
    private static final String AC_JSON = "{\"id\": \"" + AC + "\", \"name\": \"os4j-fixture\", \"description\": \"fixture\", \"user_id\": \"" + USER + "\","
            + " \"project_id\": \"" + PROJECT + "\", \"system\": null, \"expires_at\": null, \"unrestricted\": null,"
            + " \"roles\": [{\"id\": \"1222de53b11f40e68497383c7636fbe0\", \"name\": \"admin\", \"domain_id\": null}],"
            + " \"access_rules\": [{\"id\": \"8bff75d3c41a426f9e29440584030920\", \"service\": \"compute\", \"path\": \"/v2.1/servers\", \"method\": \"GET\"}],"
            + " \"links\": {\"self\": \"http://127.0.0.1:5000/v3/users/" + USER + "/application_credentials/" + AC + "\"}";

    public void createListGetDelete() throws Exception {
        respondWith(201, "{\"application_credential\": " + AC_JSON + ", \"secret\": \"generated-secret\"}}");
        respondWith(200, "{\"application_credentials\": [" + AC_JSON + "}], \"links\": {}}");
        respondWith(200, "{\"application_credentials\": [], \"links\": {}}");
        respondWith(200, "{\"application_credential\": " + AC_JSON + "}}");
        respondWith(204);

        ApplicationCredential created = osv3().identity().applicationCredentials().create(USER, ApplicationCredentialCreate.create("os4j-fixture")
                .description("fixture").roleNames("admin").accessRule("compute", "GET", "/v2.1/servers"));
        List<? extends ApplicationCredential> all = osv3().identity().applicationCredentials().list(USER);
        osv3().identity().applicationCredentials().list(USER, "missing");
        ApplicationCredential one = osv3().identity().applicationCredentials().get(USER, AC);
        boolean deleted = osv3().identity().applicationCredentials().delete(USER, AC).isSuccess();

        RecordedRequest create = takeRequest();
        Assert.assertTrue(create.getPath().endsWith("/v3/users/" + USER + "/application_credentials"));
        JsonNode body = body(create).get("application_credential");
        Assert.assertEquals(body.get("name").asText(), "os4j-fixture");
        Assert.assertEquals(body.get("roles").get(0).get("name").asText(), "admin");
        Assert.assertEquals(body.get("access_rules").get(0).get("path").asText(), "/v2.1/servers");
        Assert.assertFalse(body.has("secret"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/application_credentials"));
        Assert.assertTrue(decodedPath(takeRequest()).endsWith("/application_credentials?name=missing"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/application_credentials/" + AC));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getSecret(), "generated-secret");
        Assert.assertEquals(created.getRoles().get(0).getName(), "admin");
        Assert.assertEquals(all.get(0).getAccessRules().get(0).getService(), "compute");
        Assert.assertNull(one.getSecret());
        Assert.assertEquals(one.getProjectId(), PROJECT);
        Assert.assertTrue(deleted);
    }

    public void accessRules() throws Exception {
        respondWith(200, "{\"access_rules\": [{\"id\": \"8bff75d3c41a426f9e29440584030920\", \"service\": \"compute\", \"path\": \"/v2.1/servers\", \"method\": \"GET\", \"links\": {}}], \"links\": {}}");
        respondWith(200, "{\"access_rule\": {\"id\": \"8bff75d3c41a426f9e29440584030920\", \"service\": \"compute\", \"path\": \"/v2.1/servers\", \"method\": \"GET\", \"links\": {}}}");
        respondWith(204);

        List<? extends AccessRule> rules = osv3().identity().users().accessRules(USER);
        AccessRule rule = osv3().identity().users().getAccessRule(USER, "8bff75d3c41a426f9e29440584030920");
        boolean deleted = osv3().identity().users().deleteAccessRule(USER, "8bff75d3c41a426f9e29440584030920").isSuccess();

        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/users/" + USER + "/access_rules"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/access_rules/8bff75d3c41a426f9e29440584030920"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(rules.get(0).getMethod(), "GET");
        Assert.assertEquals(rule.getPath(), "/v2.1/servers");
        Assert.assertTrue(deleted);
    }
}
```
(Keystone 의 생성 응답은 `secret` 을 `application_credential` 객체 **안**에 둔다 — 위 fixture 는 `AC_JSON + ", \"secret\": ..."` 로 객체 안에 넣는다.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `ApplicationCredential` 등 없음.

- [ ] **Step 3: 모델 구현**

`model/identity/v3/AccessRule.java`:
```java
package org.openstack4j.model.identity.v3;

import org.openstack4j.model.ModelEntity;

/** An access rule of an application credential: which API calls the credential may make. */
public interface AccessRule extends ModelEntity {
    String getId();
    String getService();
    String getPath();
    String getMethod();
}
```
`model/identity/v3/ApplicationCredential.java`:
```java
package org.openstack4j.model.identity.v3;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An application credential: a secret bound to a user, a project and a set of roles. */
public interface ApplicationCredential extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getUserId();
    String getProjectId();
    Map<String, Object> getSystem();
    Date getExpiresAt();
    /** @return whether the credential may create other credentials and trusts */
    Boolean getUnrestricted();
    List<? extends Role> getRoles();
    List<? extends AccessRule> getAccessRules();
    /** @return the secret; only present in the create response */
    String getSecret();
}
```
`openstack/identity/v3/domain/KeystoneAccessRule.java`:
```java
package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.AccessRule;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("access_rule")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneAccessRule implements AccessRule {

    private static final long serialVersionUID = 1L;

    private String id;
    private String service;
    private String path;
    private String method;

    @Override public String getId() { return id; }
    @Override public String getService() { return service; }
    @Override public String getPath() { return path; }
    @Override public String getMethod() { return method; }

    public static class AccessRules extends ListResult<KeystoneAccessRule> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("access_rules")
        private List<KeystoneAccessRule> list;

        @Override
        protected List<KeystoneAccessRule> value() {
            return list;
        }
    }
}
```
`openstack/identity/v3/domain/KeystoneApplicationCredential.java`:
```java
package org.openstack4j.openstack.identity.v3.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.ApplicationCredential;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("application_credential")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneApplicationCredential implements ApplicationCredential {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    @JsonProperty("user_id") private String userId;
    @JsonProperty("project_id") private String projectId;
    private Map<String, Object> system;
    @JsonProperty("expires_at") private Date expiresAt;
    private Boolean unrestricted;
    private List<KeystoneRole> roles;
    @JsonProperty("access_rules") private List<KeystoneAccessRule> accessRules;
    private String secret;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getUserId() { return userId; }
    @Override public String getProjectId() { return projectId; }
    @Override public Map<String, Object> getSystem() { return system; }
    @Override public Date getExpiresAt() { return expiresAt; }
    @Override public Boolean getUnrestricted() { return unrestricted; }
    @Override public List<KeystoneRole> getRoles() { return roles; }
    @Override public List<KeystoneAccessRule> getAccessRules() { return accessRules; }
    @Override public String getSecret() { return secret; }

    public static class ApplicationCredentials extends ListResult<KeystoneApplicationCredential> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("application_credentials")
        private List<KeystoneApplicationCredential> list;

        @Override
        protected List<KeystoneApplicationCredential> value() {
            return list;
        }
    }
}
```
`model/identity/v3/options/ApplicationCredentialCreate.java`:
```java
package org.openstack4j.model.identity.v3.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Body of {@code POST /users/{id}/application_credentials}. */
public class ApplicationCredentialCreate {

    private final Map<String, Object> fields = new LinkedHashMap<>();
    private final List<Map<String, String>> roles = new ArrayList<>();
    private final List<Map<String, String>> accessRules = new ArrayList<>();

    private ApplicationCredentialCreate(String name) {
        fields.put("name", name);
    }

    public static ApplicationCredentialCreate create(String name) {
        return new ApplicationCredentialCreate(name);
    }

    public ApplicationCredentialCreate description(String description) { fields.put("description", description); return this; }
    /** A secret of your own; Keystone generates one when omitted. */
    public ApplicationCredentialCreate secret(String secret) { fields.put("secret", secret); return this; }
    /** ISO 8601 expiry is generated from the date (UTC). */
    public ApplicationCredentialCreate expiresAt(Date expiresAt) { fields.put("expires_at", expiresAt.toInstant().toString()); return this; }
    /** Allows the credential to create other application credentials and trusts. */
    public ApplicationCredentialCreate unrestricted(boolean unrestricted) { fields.put("unrestricted", unrestricted); return this; }

    public ApplicationCredentialCreate roleIds(String... ids) {
        for (String id : ids) roles.add(Collections.singletonMap("id", id));
        return this;
    }

    public ApplicationCredentialCreate roleNames(String... names) {
        for (String name : names) roles.add(Collections.singletonMap("name", name));
        return this;
    }

    /** Restricts the credential to one API call, for example ("compute", "GET", "/v2.1/servers"). */
    public ApplicationCredentialCreate accessRule(String service, String method, String path) {
        Map<String, String> rule = new LinkedHashMap<>();
        rule.put("service", service);
        rule.put("method", method);
        rule.put("path", path);
        accessRules.add(rule);
        return this;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> body = new LinkedHashMap<>(fields);
        if (!roles.isEmpty()) body.put("roles", roles);
        if (!accessRules.isEmpty()) body.put("access_rules", accessRules);
        return body;
    }
}
```

- [ ] **Step 4: 서비스 구현**

`api/identity/v3/ApplicationCredentialService.java`(`extends RestService`, Interfaces 의 5개 메서드, Javadoc: 경로·`secret` 은 생성 응답에만).
`openstack/identity/v3/internal/ApplicationCredentialServiceImpl.java`:
```java
package org.openstack4j.openstack.identity.v3.internal;

import java.util.List;
import java.util.Objects;

import org.openstack4j.api.identity.v3.ApplicationCredentialService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.ApplicationCredential;
import org.openstack4j.model.identity.v3.options.ApplicationCredentialCreate;
import org.openstack4j.openstack.identity.v3.domain.KeystoneApplicationCredential;
import org.openstack4j.openstack.identity.v3.domain.KeystoneApplicationCredential.ApplicationCredentials;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class ApplicationCredentialServiceImpl extends BaseIdentityServices implements ApplicationCredentialService {

    @Override
    public List<? extends ApplicationCredential> list(String userId) {
        return get(ApplicationCredentials.class, uri("/users/%s/application_credentials", Objects.requireNonNull(userId))).execute().getList();
    }

    @Override
    public List<? extends ApplicationCredential> list(String userId, String name) {
        return get(ApplicationCredentials.class, uri("/users/%s/application_credentials", Objects.requireNonNull(userId)))
                .param("name", Objects.requireNonNull(name)).execute().getList();
    }

    @Override
    public ApplicationCredential get(String userId, String id) {
        return get(KeystoneApplicationCredential.class, uri("/users/%s/application_credentials/%s", Objects.requireNonNull(userId), Objects.requireNonNull(id))).execute();
    }

    @Override
    public ApplicationCredential create(String userId, ApplicationCredentialCreate create) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(create);
        return post(KeystoneApplicationCredential.class, uri("/users/%s/application_credentials", userId))
                .entity(JsonBody.of("application_credential", create.toMap())).execute();
    }

    @Override
    public ActionResponse delete(String userId, String id) {
        return deleteWithResponse(uri("/users/%s/application_credentials/%s", Objects.requireNonNull(userId), Objects.requireNonNull(id))).execute();
    }
}
```
`UserService`/`UserServiceImpl` 에 access rule 3개(`AccessRules` 목록 `/users/%s/access_rules`, `KeystoneAccessRule` 단건, `deleteWithResponse`).
`IdentityService.applicationCredentials()` + `IdentityServiceImpl`(`Apis.get`) + `DefaultAPIProvider` 의 v3 identity 바인딩 옆에 `bind(ApplicationCredentialService.class, ApplicationCredentialServiceImpl.class);`.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ApplicationCredentialTests,KeystoneUserServiceTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/e3.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/e3.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/e3.log
git add -A && git commit -m "feat(identity): add application credentials and access rules

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "applicationCredentials()(list/get/create/delete, roles·access rules·unrestricted·expires_at), users() access rules."
```

---

### Task 4: project tags와 `GET /auth/system`

**Files:**
- Modify: `api/identity/v3/ProjectService.java`, `openstack/identity/v3/internal/ProjectServiceImpl.java`, `api/identity/v3/TokenService.java`, `openstack/identity/v3/internal/TokenServiceImpl.java`
- Create: `openstack/identity/v3/domain/KeystoneProjectTags.java`, `KeystoneSystemScopes.java`
- Create: `core-test/src/main/java/org/openstack4j/api/identity/v3/ext/ProjectTagTests.java`

**Interfaces:**
- Produces:
  - `ProjectService`: `List<String> tags(String projectId)`, `ActionResponse hasTag(String projectId, String tag)`(200/204 → success, 404 → failure), `ActionResponse addTag(String projectId, String tag)`, `List<String> replaceTags(String projectId, List<String> tags)`, `ActionResponse removeTag(String projectId, String tag)`, `ActionResponse removeAllTags(String projectId)`
  - `TokenService.getSystemScopes(String tokenId)` → `List<Map<String, Object>>` (`GET /auth/system` with `X-Subject-Token`)

- [ ] **Step 1: 실패하는 테스트 작성 (Review Focus 5 일부)**

```bash
git switch main && git pull && git switch -c task/e4-project-tags
```

`ProjectTagTests.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/ProjectTags")
public class ProjectTagTests extends AbstractIdentityExtTest {

    public void tagLifecycle() throws Exception {
        respondWith(200, "{\"tags\": [\"prod\"], \"links\": {}}");
        respondWith(201);
        respondWith(204);
        respondWith(200, "{\"tags\": [\"a\", \"b\"], \"links\": {}}");
        respondWith(204);
        respondWith(204);

        List<String> tags = osv3().identity().projects().tags(PROJECT);
        boolean added = osv3().identity().projects().addTag(PROJECT, "web").isSuccess();
        boolean present = osv3().identity().projects().hasTag(PROJECT, "web").isSuccess();
        List<String> replaced = osv3().identity().projects().replaceTags(PROJECT, Arrays.asList("a", "b"));
        osv3().identity().projects().removeTag(PROJECT, "a");
        osv3().identity().projects().removeAllTags(PROJECT);

        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/projects/" + PROJECT + "/tags"));
        RecordedRequest add = takeRequest();
        Assert.assertEquals(add.getMethod(), "PUT");
        Assert.assertTrue(add.getPath().endsWith("/tags/web"));
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        RecordedRequest replace = takeRequest();
        Assert.assertEquals(replace.getMethod(), "PUT");
        Assert.assertEquals(body(replace).get("tags").get(1).asText(), "b");
        RecordedRequest remove = takeRequest();
        Assert.assertEquals(remove.getMethod(), "DELETE");
        Assert.assertTrue(remove.getPath().endsWith("/tags/a"));
        RecordedRequest removeAll = takeRequest();
        Assert.assertEquals(removeAll.getMethod(), "DELETE");
        Assert.assertTrue(removeAll.getPath().endsWith("/projects/" + PROJECT + "/tags"));
        Assert.assertEquals(tags, Arrays.asList("prod"));
        Assert.assertTrue(added);
        Assert.assertTrue(present);
        Assert.assertEquals(replaced, Arrays.asList("a", "b"));
    }

    public void missingTagIsNotSuccess() throws Exception {
        respondWith(404, "{\"error\": {\"code\": 404, \"message\": \"not found\"}}");
        Assert.assertFalse(osv3().identity().projects().hasTag(PROJECT, "nope").isSuccess());
        takeRequest();
    }

    public void tagWithSpacesIsEncodedInPath() throws Exception {
        respondWith(201);
        osv3().identity().projects().addTag(PROJECT, "my tag");
        RecordedRequest request = takeRequest();
        Assert.assertFalse(request.getPath().contains(" "), request.getPath());
        Assert.assertTrue(decodedPath(request).endsWith("/tags/my tag"));
    }

    public void systemScopes() throws Exception {
        respondWith(200, "{\"system\": [{\"all\": true}], \"links\": {}}");
        List<Map<String, Object>> scopes = osv3().identity().tokens().getSystemScopes("tok");
        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/v3/auth/system"));
        Assert.assertEquals(request.getHeader("X-Subject-Token"), "tok");
        Assert.assertEquals(scopes.get(0).get("all"), Boolean.TRUE);
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `tags`, `getSystemScopes` 등 없음.

- [ ] **Step 3: 구현**

`openstack/identity/v3/domain/KeystoneProjectTags.java`:
```java
package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"tags": [...]}} of a project. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneProjectTags implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("tags")
    private List<String> tags;

    public List<String> getTags() {
        return tags;
    }
}
```
`KeystoneSystemScopes.java` — 같은 모양, `@JsonProperty("system") private List<Map<String, Object>> system;` + `getSystem()`.

`ProjectServiceImpl`:
```java
    /** Tags may contain spaces and other reserved characters; encode them as one path segment. */
    private static String tagSegment(String tag) {
        return URLEncoder.encode(Objects.requireNonNull(tag), StandardCharsets.UTF_8).replace("+", "%20");
    }

    @Override
    public List<String> tags(String projectId) {
        KeystoneProjectTags tags = get(KeystoneProjectTags.class, uri("/projects/%s/tags", Objects.requireNonNull(projectId))).execute();
        return tags == null || tags.getTags() == null ? Collections.emptyList() : tags.getTags();
    }

    @Override
    public ActionResponse hasTag(String projectId, String tag) {
        return getWithResponse(uri("/projects/%s/tags/%s", Objects.requireNonNull(projectId), tagSegment(tag))).execute();
    }

    @Override
    public ActionResponse addTag(String projectId, String tag) {
        return putWithResponse(uri("/projects/%s/tags/%s", Objects.requireNonNull(projectId), tagSegment(tag))).execute();
    }

    @Override
    public List<String> replaceTags(String projectId, List<String> tags) {
        KeystoneProjectTags result = put(KeystoneProjectTags.class, uri("/projects/%s/tags", Objects.requireNonNull(projectId)))
                .entity(JsonBody.of(Collections.singletonMap("tags", Objects.requireNonNull(tags)))).execute();
        return result == null || result.getTags() == null ? Collections.emptyList() : result.getTags();
    }

    @Override
    public ActionResponse removeTag(String projectId, String tag) {
        return deleteWithResponse(uri("/projects/%s/tags/%s", Objects.requireNonNull(projectId), tagSegment(tag))).execute();
    }

    @Override
    public ActionResponse removeAllTags(String projectId) {
        return deleteWithResponse(uri("/projects/%s/tags", Objects.requireNonNull(projectId))).execute();
    }
```
(`uri(...)` 가 이미 인코딩된 `%20` 을 다시 인코딩하는지 `tagWithSpacesIsEncodedInPath` 로 확인한다 — 이중 인코딩(`%2520`)이 나오면 `tagSegment` 를 빼고 원문을 넘긴 뒤 connector 가 공백을 인코딩하는지 확인하고 Ruling 으로 남긴다.)

`TokenServiceImpl.getSystemScopes(tokenId)`: `get(KeystoneSystemScopes.class, uri("/auth/system")).header(HEADER_X_SUBJECT_TOKEN, tokenId).execute()` → 비면 빈 리스트. 인터페이스 선언 추가.

- [ ] **Step 4: 통과 확인 (세 connector — 경로 인코딩 확인)**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && for c in httpclient okhttp http-connector; do ./mvnw -B test -pl connectors/$c -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ProjectTagTests,KeystoneProjectServiceTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:' | tail -1; done`
Expected: 세 connector 모두 `Failures: 0, Errors: 0`.

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/e4.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/e4.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/e4.log
git add -A && git commit -m "feat(identity): add project tags and the system scopes lookup

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "projects() tags(list/check/add/replace/remove/removeAll, 경로 인코딩), tokens().getSystemScopes()."
```

---

### Task 5: 역할 보강 — OS-INHERIT, implied roles·role inferences, 누락 GET

**Files:**
- Create: `core/src/main/java/org/openstack4j/model/identity/v3/RoleInference.java`, `openstack/identity/v3/domain/KeystoneRoleInference.java`
- Modify: `api/identity/v3/RoleService.java`, `openstack/identity/v3/internal/RoleServiceImpl.java`
- Create: `core-test/src/main/java/org/openstack4j/api/identity/v3/ext/RoleExtensionTests.java`

**Interfaces:**
- Produces (`RoleService`):
  - 상속 할당(OS-INHERIT): `ActionResponse grantInheritedRoleToUserOnDomain(String domainId, String userId, String roleId)`, `revokeInheritedRoleFromUserOnDomain`, `checkInheritedRoleOfUserOnDomain`, `List<? extends Role> listInheritedRolesOfUserOnDomain(String domainId, String userId)`; 같은 4개의 group 판(`...GroupOnDomain`, `listInheritedRolesOfGroupOnDomain`); project 에서 user·group 의 `grant/revoke/check` 각 3개(`...UserOnProject`, `...GroupOnProject`)
  - implied roles: `List<? extends RoleInference> listImpliedRoles(String priorRoleId)`(한 prior 의 결과), `RoleInference getImpliedRole(String priorRoleId, String impliedRoleId)`, `ActionResponse checkImpliedRole(String priorRoleId, String impliedRoleId)`, `RoleInference createImpliedRole(String priorRoleId, String impliedRoleId)`, `ActionResponse deleteImpliedRole(String priorRoleId, String impliedRoleId)`, `List<? extends RoleInference> listRoleInferences()`
  - 누락 GET: `List<? extends Role> listProjectUserRoles(String projectId, String userId)`, `List<? extends Role> listDomainUserRoles(String domainId, String userId)`
  - `RoleInference`: `Role getPriorRole()`, `List<? extends Role> getImplies()`
- 결정: 스펙 4장의 `grantInheritedToUserOnDomain` 류 이름 대신 `grantInheritedRoleToUserOnDomain` 처럼 `Role` 을 넣는다 — 기존 `RoleService.grantProjectUserRole` 등과 읽는 순서가 맞다. 스펙의 "16개"는 api-ref 기준 14개(domain 8 + project 6)의 오기다.

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/e5-role-extensions
```

`RoleExtensionTests.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.model.identity.v3.RoleInference;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/RoleExtensions")
public class RoleExtensionTests extends AbstractIdentityExtTest {

    private static final String ROLE = "1222de53b11f40e68497383c7636fbe0";
    private static final String MEMBER = "f7ab1b9e582440e0b93aceec359aeaef";
    private static final String GROUP = "g1";
    private static final String ROLES = "{\"roles\": [{\"id\": \"" + ROLE + "\", \"name\": \"admin\", \"links\": {}}], \"links\": {}}";

    public void inheritedAssignments() throws Exception {
        for (int i = 0; i < 14; i++)
            respondWith(i == 3 || i == 7 ? 200 : 204, i == 3 || i == 7 ? ROLES : "");

        var roles = osv3().identity().roles();
        roles.grantInheritedRoleToUserOnDomain("default", USER, ROLE);
        roles.checkInheritedRoleOfUserOnDomain("default", USER, ROLE);
        roles.revokeInheritedRoleFromUserOnDomain("default", USER, ROLE);
        List<? extends Role> userRoles = roles.listInheritedRolesOfUserOnDomain("default", USER);
        roles.grantInheritedRoleToGroupOnDomain("default", GROUP, ROLE);
        roles.checkInheritedRoleOfGroupOnDomain("default", GROUP, ROLE);
        roles.revokeInheritedRoleFromGroupOnDomain("default", GROUP, ROLE);
        List<? extends Role> groupRoles = roles.listInheritedRolesOfGroupOnDomain("default", GROUP);
        roles.grantInheritedRoleToUserOnProject(PROJECT, USER, ROLE);
        roles.checkInheritedRoleOfUserOnProject(PROJECT, USER, ROLE);
        roles.revokeInheritedRoleFromUserOnProject(PROJECT, USER, ROLE);
        roles.grantInheritedRoleToGroupOnProject(PROJECT, GROUP, ROLE);
        roles.checkInheritedRoleOfGroupOnProject(PROJECT, GROUP, ROLE);
        roles.revokeInheritedRoleFromGroupOnProject(PROJECT, GROUP, ROLE);

        String[][] expected = {
                {"PUT", "/v3/OS-INHERIT/domains/default/users/" + USER + "/roles/" + ROLE + "/inherited_to_projects"},
                {"HEAD", "/v3/OS-INHERIT/domains/default/users/" + USER + "/roles/" + ROLE + "/inherited_to_projects"},
                {"DELETE", "/v3/OS-INHERIT/domains/default/users/" + USER + "/roles/" + ROLE + "/inherited_to_projects"},
                {"GET", "/v3/OS-INHERIT/domains/default/users/" + USER + "/roles/inherited_to_projects"},
                {"PUT", "/v3/OS-INHERIT/domains/default/groups/g1/roles/" + ROLE + "/inherited_to_projects"},
                {"HEAD", "/v3/OS-INHERIT/domains/default/groups/g1/roles/" + ROLE + "/inherited_to_projects"},
                {"DELETE", "/v3/OS-INHERIT/domains/default/groups/g1/roles/" + ROLE + "/inherited_to_projects"},
                {"GET", "/v3/OS-INHERIT/domains/default/groups/g1/roles/inherited_to_projects"},
                {"PUT", "/v3/OS-INHERIT/projects/" + PROJECT + "/users/" + USER + "/roles/" + ROLE + "/inherited_to_projects"},
                {"HEAD", "/v3/OS-INHERIT/projects/" + PROJECT + "/users/" + USER + "/roles/" + ROLE + "/inherited_to_projects"},
                {"DELETE", "/v3/OS-INHERIT/projects/" + PROJECT + "/users/" + USER + "/roles/" + ROLE + "/inherited_to_projects"},
                {"PUT", "/v3/OS-INHERIT/projects/" + PROJECT + "/groups/g1/roles/" + ROLE + "/inherited_to_projects"},
                {"HEAD", "/v3/OS-INHERIT/projects/" + PROJECT + "/groups/g1/roles/" + ROLE + "/inherited_to_projects"},
                {"DELETE", "/v3/OS-INHERIT/projects/" + PROJECT + "/groups/g1/roles/" + ROLE + "/inherited_to_projects"}};
        for (String[] e : expected) {
            RecordedRequest r = takeRequest();
            Assert.assertEquals(r.getMethod(), e[0], e[1]);
            Assert.assertTrue(r.getPath().endsWith(e[1]), r.getPath() + " vs " + e[1]);
        }
        Assert.assertEquals(userRoles.get(0).getName(), "admin");
        Assert.assertEquals(groupRoles.size(), 1);
    }

    public void impliedRolesAndInferences() throws Exception {
        String inference = "{\"role_inference\": {\"prior_role\": {\"id\": \"" + ROLE + "\", \"name\": \"admin\", \"links\": {}},"
                + " \"implies\": {\"id\": \"" + MEMBER + "\", \"name\": \"member\", \"links\": {}}}, \"links\": {}}";
        respondWith(201, inference);
        respondWith(200, inference);
        respondWith(204);
        respondWith(200, "{\"role_inference\": {\"prior_role\": {\"id\": \"" + ROLE + "\", \"name\": \"admin\"}, \"implies\": [{\"id\": \"" + MEMBER + "\", \"name\": \"member\"}]}, \"links\": {}}");
        respondWith(200, "{\"role_inferences\": [{\"prior_role\": {\"id\": \"" + ROLE + "\", \"name\": \"admin\"}, \"implies\": [{\"id\": \"" + MEMBER + "\", \"name\": \"member\"}]}], \"links\": {}}");
        respondWith(204);

        RoleInference created = osv3().identity().roles().createImpliedRole(ROLE, MEMBER);
        RoleInference got = osv3().identity().roles().getImpliedRole(ROLE, MEMBER);
        boolean exists = osv3().identity().roles().checkImpliedRole(ROLE, MEMBER).isSuccess();
        List<? extends RoleInference> implied = osv3().identity().roles().listImpliedRoles(ROLE);
        List<? extends RoleInference> all = osv3().identity().roles().listRoleInferences();
        boolean deleted = osv3().identity().roles().deleteImpliedRole(ROLE, MEMBER).isSuccess();

        RecordedRequest put = takeRequest();
        Assert.assertEquals(put.getMethod(), "PUT");
        Assert.assertTrue(put.getPath().endsWith("/v3/roles/" + ROLE + "/implies/" + MEMBER));
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertEquals(takeRequest().getMethod(), "HEAD");
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/roles/" + ROLE + "/implies"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/role_inferences"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getImplies().get(0).getName(), "member");
        Assert.assertEquals(got.getPriorRole().getId(), ROLE);
        Assert.assertTrue(exists);
        Assert.assertEquals(implied.get(0).getImplies().get(0).getId(), MEMBER);
        Assert.assertEquals(all.get(0).getPriorRole().getName(), "admin");
        Assert.assertTrue(deleted);
    }

    public void missingRoleListings() throws Exception {
        respondWith(200, ROLES);
        respondWith(200, ROLES);
        osv3().identity().roles().listProjectUserRoles(PROJECT, USER);
        osv3().identity().roles().listDomainUserRoles("default", USER);
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/projects/" + PROJECT + "/users/" + USER + "/roles"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/domains/default/users/" + USER + "/roles"));
    }
}
```
(`implies` 는 단건 응답(PUT/GET)에서는 객체, 목록 응답에서는 배열이다 — 전역 mapper 의 `ACCEPT_SINGLE_VALUE_AS_ARRAY` 로 둘 다 `List` 로 읽는다.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `grantInheritedRoleToUserOnDomain`, `RoleInference` 등 없음.

- [ ] **Step 3: 구현**

`model/identity/v3/RoleInference.java`:
```java
package org.openstack4j.model.identity.v3;

import java.util.List;

import org.openstack4j.model.ModelEntity;

/** A prior role and the roles it implies. */
public interface RoleInference extends ModelEntity {
    Role getPriorRole();
    List<? extends Role> getImplies();
}
```
`openstack/identity/v3/domain/KeystoneRoleInference.java`:
```java
package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.RoleInference;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("role_inference")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneRoleInference implements RoleInference {

    private static final long serialVersionUID = 1L;

    @JsonProperty("prior_role") private KeystoneRole priorRole;
    @JsonProperty("implies") private List<KeystoneRole> implies;

    @Override public KeystoneRole getPriorRole() { return priorRole; }
    @Override public List<KeystoneRole> getImplies() { return implies; }

    public static class RoleInferences extends ListResult<KeystoneRoleInference> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("role_inferences")
        private List<KeystoneRoleInference> list;

        @Override
        protected List<KeystoneRoleInference> value() {
            return list;
        }
    }
}
```
`listImpliedRoles(prior)` 의 응답은 `{"role_inference": {...}}` 하나이므로 `get(KeystoneRoleInference.class, ...)` 결과를 `Collections.singletonList(...)` 로 감싼다(null 이면 빈 리스트).

`RoleServiceImpl`:
```java
    private static String inherited(String targetType, String targetId, String actorType, String actorId, String roleId) {
        return String.format("/OS-INHERIT/%s/%s/%s/%s/roles/%s/inherited_to_projects", targetType, Objects.requireNonNull(targetId),
                actorType, Objects.requireNonNull(actorId), Objects.requireNonNull(roleId));
    }

    @Override public ActionResponse grantInheritedRoleToUserOnDomain(String d, String u, String r) { return putWithResponse(inherited("domains", d, "users", u, r)).execute(); }
    @Override public ActionResponse revokeInheritedRoleFromUserOnDomain(String d, String u, String r) { return deleteWithResponse(inherited("domains", d, "users", u, r)).execute(); }
    @Override public ActionResponse checkInheritedRoleOfUserOnDomain(String d, String u, String r) { return head(ActionResponse.class, inherited("domains", d, "users", u, r)).execute(); }
    @Override public List<? extends Role> listInheritedRolesOfUserOnDomain(String d, String u) { return get(Roles.class, uri("/OS-INHERIT/domains/%s/users/%s/roles/inherited_to_projects", d, u)).execute().getList(); }
    @Override public ActionResponse grantInheritedRoleToGroupOnDomain(String d, String g, String r) { return putWithResponse(inherited("domains", d, "groups", g, r)).execute(); }
    @Override public ActionResponse revokeInheritedRoleFromGroupOnDomain(String d, String g, String r) { return deleteWithResponse(inherited("domains", d, "groups", g, r)).execute(); }
    @Override public ActionResponse checkInheritedRoleOfGroupOnDomain(String d, String g, String r) { return head(ActionResponse.class, inherited("domains", d, "groups", g, r)).execute(); }
    @Override public List<? extends Role> listInheritedRolesOfGroupOnDomain(String d, String g) { return get(Roles.class, uri("/OS-INHERIT/domains/%s/groups/%s/roles/inherited_to_projects", d, g)).execute().getList(); }
    @Override public ActionResponse grantInheritedRoleToUserOnProject(String p, String u, String r) { return putWithResponse(inherited("projects", p, "users", u, r)).execute(); }
    @Override public ActionResponse revokeInheritedRoleFromUserOnProject(String p, String u, String r) { return deleteWithResponse(inherited("projects", p, "users", u, r)).execute(); }
    @Override public ActionResponse checkInheritedRoleOfUserOnProject(String p, String u, String r) { return head(ActionResponse.class, inherited("projects", p, "users", u, r)).execute(); }
    @Override public ActionResponse grantInheritedRoleToGroupOnProject(String p, String g, String r) { return putWithResponse(inherited("projects", p, "groups", g, r)).execute(); }
    @Override public ActionResponse revokeInheritedRoleFromGroupOnProject(String p, String g, String r) { return deleteWithResponse(inherited("projects", p, "groups", g, r)).execute(); }
    @Override public ActionResponse checkInheritedRoleOfGroupOnProject(String p, String g, String r) { return head(ActionResponse.class, inherited("projects", p, "groups", g, r)).execute(); }

    @Override
    public List<? extends RoleInference> listImpliedRoles(String priorRoleId) {
        KeystoneRoleInference one = get(KeystoneRoleInference.class, uri("/roles/%s/implies", Objects.requireNonNull(priorRoleId))).execute();
        return one == null ? Collections.emptyList() : Collections.singletonList(one);
    }

    @Override public RoleInference getImpliedRole(String prior, String implied) { return get(KeystoneRoleInference.class, uri("/roles/%s/implies/%s", prior, implied)).execute(); }
    @Override public ActionResponse checkImpliedRole(String prior, String implied) { return head(ActionResponse.class, uri("/roles/%s/implies/%s", prior, implied)).execute(); }
    @Override public RoleInference createImpliedRole(String prior, String implied) { return put(KeystoneRoleInference.class, uri("/roles/%s/implies/%s", prior, implied)).execute(); }
    @Override public ActionResponse deleteImpliedRole(String prior, String implied) { return deleteWithResponse(uri("/roles/%s/implies/%s", prior, implied)).execute(); }
    @Override public List<? extends RoleInference> listRoleInferences() { return get(RoleInferences.class, uri("/role_inferences")).execute().getList(); }
    @Override public List<? extends Role> listProjectUserRoles(String p, String u) { return get(Roles.class, uri("/projects/%s/users/%s/roles", p, u)).execute().getList(); }
    @Override public List<? extends Role> listDomainUserRoles(String d, String u) { return get(Roles.class, uri("/domains/%s/users/%s/roles", d, u)).execute().getList(); }
```
(`putWithResponse(String...)` 가 엔티티 없이 PUT 하는지 확인 — 기존 `grantProjectUserRole` 은 `put(ActionResponse.class, ...)` 를 쓴다; 같은 방식이 더 안전하면 그것을 따른다.) `RoleService` 에 Interfaces 의 선언을 Javadoc(경로)과 함께 추가한다.

- [ ] **Step 4: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='RoleExtensionTests,KeystoneRoleServiceTests,KeystoneGroupServiceTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Failures: 0, Errors: 0` (RoleExtensionTests 3개).

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/e5.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/e5.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/e5.log
git add -A && git commit -m "feat(identity): add inherited role assignments, implied roles and role inferences

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "roles(): OS-INHERIT 14개(domain/project × user/group × grant/check/revoke, domain 목록), implied roles(CRUD/check), role inferences, project/domain user 역할 목록."
```

---

### Task 6: system 역할 할당

**Files:**
- Create: `core/src/main/java/org/openstack4j/api/identity/v3/SystemRoleService.java`, `openstack/identity/v3/internal/SystemRoleServiceImpl.java`
- Modify: `api/identity/v3/IdentityService.java`, `IdentityServiceImpl.java`, `DefaultAPIProvider.java`
- Create: `core-test/src/main/java/org/openstack4j/api/identity/v3/ext/SystemRoleTests.java`

**Interfaces:**
- Produces: `IdentityService.systemRoles()` → `SystemRoleService`: `List<? extends Role> listUserRoles(String userId)`, `Role getUserRole(String userId, String roleId)`, `ActionResponse checkUserRole(String userId, String roleId)`, `ActionResponse grantUserRole(String userId, String roleId)`, `ActionResponse revokeUserRole(String userId, String roleId)`, 그리고 같은 5개의 group 판(`listGroupRoles`, `getGroupRole`, `checkGroupRole`, `grantGroupRole`, `revokeGroupRole`)

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/e6-system-roles
```

`SystemRoleTests.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.Role;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/SystemRoles")
public class SystemRoleTests extends AbstractIdentityExtTest {

    private static final String ROLE = "1222de53b11f40e68497383c7636fbe0";
    private static final String ROLE_JSON = "{\"id\": \"" + ROLE + "\", \"name\": \"admin\", \"domain_id\": null, \"links\": {}}";

    public void userAndGroupSystemRoles() throws Exception {
        for (String actor : new String[] {"users", "groups"}) {
            respondWith(204);
            respondWith(204);
            respondWith(200, "{\"role\": " + ROLE_JSON + "}");
            respondWith(200, "{\"roles\": [" + ROLE_JSON + "], \"links\": {}}");
            respondWith(204);
        }
        var system = osv3().identity().systemRoles();
        system.grantUserRole(USER, ROLE);
        system.checkUserRole(USER, ROLE);
        Role userRole = system.getUserRole(USER, ROLE);
        List<? extends Role> userRoles = system.listUserRoles(USER);
        system.revokeUserRole(USER, ROLE);
        system.grantGroupRole("g1", ROLE);
        system.checkGroupRole("g1", ROLE);
        system.getGroupRole("g1", ROLE);
        List<? extends Role> groupRoles = system.listGroupRoles("g1");
        system.revokeGroupRole("g1", ROLE);

        for (String prefix : new String[] {"/v3/system/users/" + USER, "/v3/system/groups/g1"}) {
            String[][] expected = {{"PUT", prefix + "/roles/" + ROLE}, {"HEAD", prefix + "/roles/" + ROLE}, {"GET", prefix + "/roles/" + ROLE},
                    {"GET", prefix + "/roles"}, {"DELETE", prefix + "/roles/" + ROLE}};
            for (String[] e : expected) {
                RecordedRequest r = takeRequest();
                Assert.assertEquals(r.getMethod(), e[0]);
                Assert.assertTrue(r.getPath().endsWith(e[1]), r.getPath());
            }
        }
        Assert.assertEquals(userRole.getName(), "admin");
        Assert.assertEquals(userRoles.size(), 1);
        Assert.assertEquals(groupRoles.get(0).getId(), ROLE);
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -3`
Expected: `systemRoles()` 없음.

- [ ] **Step 3: 구현**

`api/identity/v3/SystemRoleService.java`(`extends RestService`, Javadoc: system 역할은 배포 전체 관리 권한, `/system/{users,groups}/{id}/roles`). `SystemRoleServiceImpl`:
```java
package org.openstack4j.openstack.identity.v3.internal;

import java.util.List;
import java.util.Objects;

import org.openstack4j.api.identity.v3.SystemRoleService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRole;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRole.Roles;

public class SystemRoleServiceImpl extends BaseIdentityServices implements SystemRoleService {

    private static String path(String actor, String actorId, String roleId) {
        String base = "/system/" + actor + "/" + Objects.requireNonNull(actorId) + "/roles";
        return roleId == null ? base : base + "/" + roleId;
    }

    @Override public List<? extends Role> listUserRoles(String userId) { return get(Roles.class, path("users", userId, null)).execute().getList(); }
    @Override public Role getUserRole(String userId, String roleId) { return get(KeystoneRole.class, path("users", userId, Objects.requireNonNull(roleId))).execute(); }
    @Override public ActionResponse checkUserRole(String userId, String roleId) { return head(ActionResponse.class, path("users", userId, Objects.requireNonNull(roleId))).execute(); }
    @Override public ActionResponse grantUserRole(String userId, String roleId) { return put(ActionResponse.class, path("users", userId, Objects.requireNonNull(roleId))).execute(); }
    @Override public ActionResponse revokeUserRole(String userId, String roleId) { return deleteWithResponse(path("users", userId, Objects.requireNonNull(roleId))).execute(); }
    @Override public List<? extends Role> listGroupRoles(String groupId) { return get(Roles.class, path("groups", groupId, null)).execute().getList(); }
    @Override public Role getGroupRole(String groupId, String roleId) { return get(KeystoneRole.class, path("groups", groupId, Objects.requireNonNull(roleId))).execute(); }
    @Override public ActionResponse checkGroupRole(String groupId, String roleId) { return head(ActionResponse.class, path("groups", groupId, Objects.requireNonNull(roleId))).execute(); }
    @Override public ActionResponse grantGroupRole(String groupId, String roleId) { return put(ActionResponse.class, path("groups", groupId, Objects.requireNonNull(roleId))).execute(); }
    @Override public ActionResponse revokeGroupRole(String groupId, String roleId) { return deleteWithResponse(path("groups", groupId, Objects.requireNonNull(roleId))).execute(); }
}
```
accessor·binding 추가.

- [ ] **Step 4: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='SystemRoleTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Tests run: 1, Failures: 0, Errors: 0`

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/e6.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/e6.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/e6.log
git add -A && git commit -m "feat(identity): add system role assignments for users and groups

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "systemRoles(): user/group 의 system 역할 list/get/check/grant/revoke."
```

---

### Task 7: domain configuration (도메인별·기본값)

**Files:**
- Create: `core/src/main/java/org/openstack4j/openstack/identity/v3/domain/KeystoneDomainConfig.java`
- Modify: `api/identity/v3/DomainService.java`, `openstack/identity/v3/internal/DomainServiceImpl.java`
- Create: `core-test/src/main/java/org/openstack4j/api/identity/v3/ext/DomainConfigTests.java`

**Interfaces:**
- Produces (`DomainService`; 설정은 `Map<String, Map<String, Object>>` = group → option → value):
  - `Map<String, Map<String, Object>> config(String domainId)`, `Map<String, Object> configGroup(String domainId, String group)`, `Object configOption(String domainId, String group, String option)`
  - `Map<String, Map<String, Object>> createConfig(String domainId, Map<String, Map<String, Object>> config)`(PUT), `updateConfig(domainId, config)`(PATCH), `Map<String, Object> updateConfigGroup(String domainId, String group, Map<String, Object> options)`(PATCH), `Object updateConfigOption(String domainId, String group, String option, Object value)`(PATCH)
  - `ActionResponse deleteConfig(String domainId)`, `deleteConfigGroup(domainId, group)`, `deleteConfigOption(domainId, group, option)`
  - `Map<String, Map<String, Object>> defaultConfig()`, `Map<String, Object> defaultConfigGroup(String group)`, `Object defaultConfigOption(String group, String option)`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/e7-domain-config
```

`DomainConfigTests.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import java.util.Collections;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/DomainConfig")
public class DomainConfigTests extends AbstractIdentityExtTest {

    private static final String CONFIG = "{\"config\": {\"identity\": {\"driver\": \"ldap\"}, \"ldap\": {\"url\": \"ldap://ldap.example.com\", \"user_tree_dn\": \"ou=Users,dc=example,dc=com\"}}}";

    public void domainConfigLifecycle() throws Exception {
        respondWith(201, CONFIG);
        respondWith(200, CONFIG);
        respondWith(200, "{\"config\": {\"url\": \"ldap://ldap.example.com\", \"user_tree_dn\": \"ou=Users,dc=example,dc=com\"}}");
        respondWith(200, "{\"url\": \"ldap://ldap.example.com\"}");
        respondWith(200, CONFIG);
        respondWith(200, "{\"config\": {\"url\": \"ldap://ldap2.example.com\"}}");
        respondWith(200, "{\"config\": {\"url\": \"ldap://ldap3.example.com\"}}");
        respondWith(204);
        respondWith(204);
        respondWith(204);

        var domains = osv3().identity().domains();
        Map<String, Map<String, Object>> created = domains.createConfig("d1", Map.of("identity", Map.of("driver", "ldap")));
        Map<String, Map<String, Object>> all = domains.config("d1");
        Map<String, Object> ldap = domains.configGroup("d1", "ldap");
        Object url = domains.configOption("d1", "ldap", "url");
        domains.updateConfig("d1", Map.of("ldap", Map.of("url", "ldap://ldap.example.com")));
        Map<String, Object> group = domains.updateConfigGroup("d1", "ldap", Map.of("url", "ldap://ldap2.example.com"));
        Object option = domains.updateConfigOption("d1", "ldap", "url", "ldap://ldap3.example.com");
        domains.deleteConfigOption("d1", "ldap", "url");
        domains.deleteConfigGroup("d1", "ldap");
        domains.deleteConfig("d1");

        RecordedRequest create = takeRequest();
        Assert.assertEquals(create.getMethod(), "PUT");
        Assert.assertTrue(create.getPath().endsWith("/v3/domains/d1/config"));
        Assert.assertEquals(body(create).get("config").get("identity").get("driver").asText(), "ldap");
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertTrue(takeRequest().getPath().endsWith("/config/ldap"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/config/ldap/url"));
        Assert.assertEquals(takeRequest().getMethod(), "PATCH");
        RecordedRequest patchGroup = takeRequest();
        Assert.assertEquals(body(patchGroup).get("config").get("url").asText(), "ldap://ldap2.example.com");
        RecordedRequest patchOption = takeRequest();
        Assert.assertTrue(patchOption.getPath().endsWith("/config/ldap/url"));
        Assert.assertEquals(body(patchOption).get("config").get("url").asText(), "ldap://ldap3.example.com");
        for (String suffix : new String[] {"/config/ldap/url", "/config/ldap", "/config"}) {
            RecordedRequest r = takeRequest();
            Assert.assertEquals(r.getMethod(), "DELETE");
            Assert.assertTrue(r.getPath().endsWith("/v3/domains/d1" + suffix), r.getPath());
        }
        Assert.assertEquals(created.get("identity").get("driver"), "ldap");
        Assert.assertEquals(all.get("ldap").get("url"), "ldap://ldap.example.com");
        Assert.assertEquals(ldap.get("user_tree_dn"), "ou=Users,dc=example,dc=com");
        Assert.assertEquals(url, "ldap://ldap.example.com");
        Assert.assertEquals(group.get("url"), "ldap://ldap2.example.com");
        Assert.assertEquals(option, "ldap://ldap3.example.com");
    }

    public void defaultConfig() throws Exception {
        respondWith(200, "{\"config\": {\"identity\": {\"driver\": \"sql\", \"list_limit\": null}, \"ldap\": {\"url\": \"ldap://localhost\", \"user\": null}}}");
        respondWith(200, "{\"config\": {\"url\": \"ldap://localhost\", \"user\": null}}");
        respondWith(200, "{\"driver\": \"sql\"}");

        Map<String, Map<String, Object>> all = osv3().identity().domains().defaultConfig();
        Map<String, Object> ldap = osv3().identity().domains().defaultConfigGroup("ldap");
        Object driver = osv3().identity().domains().defaultConfigOption("identity", "driver");

        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/domains/config/default"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/domains/config/ldap/default"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/domains/config/identity/driver/default"));
        Assert.assertEquals(all.get("identity").get("driver"), "sql");
        Assert.assertTrue(all.get("identity").containsKey("list_limit"));
        Assert.assertEquals(ldap.get("url"), "ldap://localhost");
        Assert.assertEquals(driver, "sql");
    }
}
```
(`Map.of(...)` 는 null 값을 못 넣으므로 테스트 요청에는 null 이 없다. 응답의 null 은 `containsKey` 로 확인한다.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -3`
Expected: `createConfig` 등 없음.

- [ ] **Step 3: 구현**

`openstack/identity/v3/domain/KeystoneDomainConfig.java`:
```java
package org.openstack4j.openstack.identity.v3.domain;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"config": {...}}}: a whole configuration (group → option → value) or one group (option → value). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneDomainConfig implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("config")
    private Map<String, Object> config;

    public Map<String, Object> getConfig() {
        return config;
    }
}
```
`DomainServiceImpl`:
```java
    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> groups(KeystoneDomainConfig result) {
        if (result == null || result.getConfig() == null)
            return Collections.emptyMap();
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        result.getConfig().forEach((k, v) -> out.put(k, v instanceof Map ? (Map<String, Object>) v : Collections.emptyMap()));
        return out;
    }

    private static Map<String, Object> options(KeystoneDomainConfig result) {
        return result == null || result.getConfig() == null ? Collections.emptyMap() : result.getConfig();
    }

    /** A single option comes back as {@code {"<option>": value}}. */
    @SuppressWarnings("unchecked")
    private static Object single(Map<String, Object> response, String option) {
        return response == null ? null : response.get(option);
    }

    @Override public Map<String, Map<String, Object>> config(String domainId) { return groups(get(KeystoneDomainConfig.class, uri("/domains/%s/config", domainId)).execute()); }
    @Override public Map<String, Object> configGroup(String domainId, String group) { return options(get(KeystoneDomainConfig.class, uri("/domains/%s/config/%s", domainId, group)).execute()); }

    @Override
    @SuppressWarnings("unchecked")
    public Object configOption(String domainId, String group, String option) {
        return single(get(HashMap.class, uri("/domains/%s/config/%s/%s", domainId, group, option)).execute(), option);
    }

    @Override
    public Map<String, Map<String, Object>> createConfig(String domainId, Map<String, Map<String, Object>> config) {
        return groups(put(KeystoneDomainConfig.class, uri("/domains/%s/config", domainId)).entity(JsonBody.of("config", config)).execute());
    }

    @Override
    public Map<String, Map<String, Object>> updateConfig(String domainId, Map<String, Map<String, Object>> config) {
        return groups(patch(KeystoneDomainConfig.class, uri("/domains/%s/config", domainId)).entity(JsonBody.of("config", config)).execute());
    }

    @Override
    public Map<String, Object> updateConfigGroup(String domainId, String group, Map<String, Object> options) {
        return options(patch(KeystoneDomainConfig.class, uri("/domains/%s/config/%s", domainId, group)).entity(JsonBody.of("config", options)).execute());
    }

    @Override
    public Object updateConfigOption(String domainId, String group, String option, Object value) {
        Map<String, Object> body = new HashMap<>();
        body.put(option, value);
        return options(patch(KeystoneDomainConfig.class, uri("/domains/%s/config/%s/%s", domainId, group, option)).entity(JsonBody.of("config", body)).execute()).get(option);
    }

    @Override public ActionResponse deleteConfig(String domainId) { return deleteWithResponse(uri("/domains/%s/config", domainId)).execute(); }
    @Override public ActionResponse deleteConfigGroup(String domainId, String group) { return deleteWithResponse(uri("/domains/%s/config/%s", domainId, group)).execute(); }
    @Override public ActionResponse deleteConfigOption(String domainId, String group, String option) { return deleteWithResponse(uri("/domains/%s/config/%s/%s", domainId, group, option)).execute(); }
    @Override public Map<String, Map<String, Object>> defaultConfig() { return groups(get(KeystoneDomainConfig.class, uri("/domains/config/default")).execute()); }
    @Override public Map<String, Object> defaultConfigGroup(String group) { return options(get(KeystoneDomainConfig.class, uri("/domains/config/%s/default", group)).execute()); }

    @Override
    @SuppressWarnings("unchecked")
    public Object defaultConfigOption(String group, String option) {
        return single(get(HashMap.class, uri("/domains/config/%s/%s/default", group, option)).execute(), option);
    }
```
`DomainService` 에 Interfaces 의 13개 선언(Javadoc: 경로, 관리자 전용, domain-specific drivers 설정이 켜져 있어야 함).

- [ ] **Step 4: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='DomainConfigTests,KeystoneDomainServiceTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Failures: 0, Errors: 0` (DomainConfigTests 2개).

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/e7.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/e7.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/e7.log
git add -A && git commit -m "feat(identity): add domain-specific configuration and default configuration

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "domains(): config(도메인별 create/get/update/delete 의 config·group·option 단위)와 default config 조회."
```

---

### Task 8: OS-TRUST

**Files:**
- Create (`core/src/main/java/org/openstack4j/`): `model/identity/v3/Trust.java`, `model/identity/v3/options/TrustCreate.java`, `api/identity/v3/TrustService.java`, `openstack/identity/v3/domain/KeystoneTrust.java`, `openstack/identity/v3/internal/TrustServiceImpl.java`
- Modify: `IdentityService.java`, `IdentityServiceImpl.java`, `DefaultAPIProvider.java`
- Create: `core-test/src/main/java/org/openstack4j/api/identity/v3/ext/TrustTests.java`

**Interfaces:**
- Produces: `IdentityService.trusts()` → `TrustService`: `List<? extends Trust> list()`, `List<? extends Trust> list(String trustorUserId, String trusteeUserId)`(null 인 쪽은 필터 생략), `Trust get(String trustId)`, `Trust create(TrustCreate create)`, `ActionResponse delete(String trustId)`, `List<? extends Role> roles(String trustId)`, `Role getRole(String trustId, String roleId)`, `ActionResponse checkRole(String trustId, String roleId)`
- `Trust`: `getId()`, `getTrustorUserId()`, `getTrusteeUserId()`, `getProjectId()`, `Boolean getImpersonation()`, `Date getExpiresAt()`, `Integer getRemainingUses()`, `Integer getRedelegationCount()`, `String getRedelegatedTrustId()`, `List<? extends Role> getRoles()`
- `TrustCreate.create(String trustorUserId, String trusteeUserId, boolean impersonation)`: `projectId(String)`, `roleIds(String...)`, `roleNames(String...)`, `expiresAt(Date)`, `remainingUses(int)`, `allowRedelegation(boolean)`, `redelegationCount(int)`; `Map<String, Object> toMap()`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/e8-trusts
```

`TrustTests.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.model.identity.v3.Trust;
import org.openstack4j.model.identity.v3.options.TrustCreate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/Trusts")
public class TrustTests extends AbstractIdentityExtTest {

    private static final String TRUST = "c947c7f50eff44afaaa94782b90fd395";
    private static final String ROLE = "1222de53b11f40e68497383c7636fbe0";
    private static final String ROLE_JSON = "{\"id\": \"" + ROLE + "\", \"name\": \"admin\", \"domain_id\": null, \"description\": null, \"options\": {\"immutable\": true}, \"links\": {}}";
    private static final String TRUST_JSON = "{\"roles\": [" + ROLE_JSON + "], \"id\": \"" + TRUST + "\", \"trustor_user_id\": \"" + USER + "\","
            + " \"trustee_user_id\": \"" + USER + "\", \"project_id\": \"" + PROJECT + "\", \"impersonation\": false, \"expires_at\": null,"
            + " \"remaining_uses\": null, \"deleted_at\": null, \"redelegated_trust_id\": null, \"redelegation_count\": 0,"
            + " \"roles_links\": {\"self\": \"x\", \"next\": null, \"previous\": null}, \"links\": {}}";

    public void trustLifecycle() throws Exception {
        respondWith(201, "{\"trust\": " + TRUST_JSON + "}");
        respondWith(200, "{\"trusts\": [" + TRUST_JSON + "], \"links\": {}}");
        respondWith(200, "{\"trusts\": [], \"links\": {}}");
        respondWith(200, "{\"trust\": " + TRUST_JSON + "}");
        respondWith(200, "{\"roles\": [" + ROLE_JSON + "], \"links\": {}}");
        respondWith(200, "{\"role\": " + ROLE_JSON + "}");
        respondWith(200);
        respondWith(204);

        Trust created = osv3().identity().trusts().create(TrustCreate.create(USER, USER, false).projectId(PROJECT).roleNames("admin").remainingUses(3));
        List<? extends Trust> all = osv3().identity().trusts().list();
        osv3().identity().trusts().list(USER, null);
        Trust one = osv3().identity().trusts().get(TRUST);
        List<? extends Role> roles = osv3().identity().trusts().roles(TRUST);
        Role role = osv3().identity().trusts().getRole(TRUST, ROLE);
        boolean has = osv3().identity().trusts().checkRole(TRUST, ROLE).isSuccess();
        boolean deleted = osv3().identity().trusts().delete(TRUST).isSuccess();

        RecordedRequest create = takeRequest();
        Assert.assertTrue(create.getPath().endsWith("/v3/OS-TRUST/trusts"));
        JsonNode body = body(create).get("trust");
        Assert.assertEquals(body.get("trustor_user_id").asText(), USER);
        Assert.assertFalse(body.get("impersonation").asBoolean());
        Assert.assertEquals(body.get("project_id").asText(), PROJECT);
        Assert.assertEquals(body.get("roles").get(0).get("name").asText(), "admin");
        Assert.assertEquals(body.get("remaining_uses").asInt(), 3);
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-TRUST/trusts"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-TRUST/trusts?trustor_user_id=" + USER));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-TRUST/trusts/" + TRUST));
        Assert.assertTrue(takeRequest().getPath().endsWith("/trusts/" + TRUST + "/roles"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/trusts/" + TRUST + "/roles/" + ROLE));
        Assert.assertEquals(takeRequest().getMethod(), "HEAD");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getTrustorUserId(), USER);
        Assert.assertEquals(created.getRedelegationCount(), Integer.valueOf(0));
        Assert.assertEquals(all.get(0).getRoles().get(0).getName(), "admin");
        Assert.assertFalse(one.getImpersonation());
        Assert.assertNull(one.getRemainingUses());
        Assert.assertEquals(roles.size(), 1);
        Assert.assertEquals(role.getId(), ROLE);
        Assert.assertTrue(has);
        Assert.assertTrue(deleted);
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -3`
Expected: `Trust`, `TrustCreate`, `trusts()` 없음.

- [ ] **Step 3: 구현**

`model/identity/v3/Trust.java`:
```java
package org.openstack4j.model.identity.v3;

import java.util.Date;
import java.util.List;

import org.openstack4j.model.ModelEntity;

/** A trust (OS-TRUST): the trustor delegates roles on a project to the trustee. */
public interface Trust extends ModelEntity {
    String getId();
    String getTrustorUserId();
    String getTrusteeUserId();
    String getProjectId();
    Boolean getImpersonation();
    Date getExpiresAt();
    Integer getRemainingUses();
    Integer getRedelegationCount();
    String getRedelegatedTrustId();
    List<? extends Role> getRoles();
}
```
`openstack/identity/v3/domain/KeystoneTrust.java`:
```java
package org.openstack4j.openstack.identity.v3.domain;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.Trust;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("trust")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneTrust implements Trust {

    private static final long serialVersionUID = 1L;

    private String id;
    @JsonProperty("trustor_user_id") private String trustorUserId;
    @JsonProperty("trustee_user_id") private String trusteeUserId;
    @JsonProperty("project_id") private String projectId;
    private Boolean impersonation;
    @JsonProperty("expires_at") private Date expiresAt;
    @JsonProperty("remaining_uses") private Integer remainingUses;
    @JsonProperty("redelegation_count") private Integer redelegationCount;
    @JsonProperty("redelegated_trust_id") private String redelegatedTrustId;
    private List<KeystoneRole> roles;

    @Override public String getId() { return id; }
    @Override public String getTrustorUserId() { return trustorUserId; }
    @Override public String getTrusteeUserId() { return trusteeUserId; }
    @Override public String getProjectId() { return projectId; }
    @Override public Boolean getImpersonation() { return impersonation; }
    @Override public Date getExpiresAt() { return expiresAt; }
    @Override public Integer getRemainingUses() { return remainingUses; }
    @Override public Integer getRedelegationCount() { return redelegationCount; }
    @Override public String getRedelegatedTrustId() { return redelegatedTrustId; }
    @Override public List<KeystoneRole> getRoles() { return roles; }

    public static class Trusts extends ListResult<KeystoneTrust> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("trusts")
        private List<KeystoneTrust> list;

        @Override
        protected List<KeystoneTrust> value() {
            return list;
        }
    }
}
```
`model/identity/v3/options/TrustCreate.java`:
```java
package org.openstack4j.model.identity.v3.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Body of {@code POST /OS-TRUST/trusts}. */
public class TrustCreate {

    private final Map<String, Object> fields = new LinkedHashMap<>();
    private final List<Map<String, String>> roles = new ArrayList<>();

    private TrustCreate(String trustorUserId, String trusteeUserId, boolean impersonation) {
        fields.put("trustor_user_id", Objects.requireNonNull(trustorUserId));
        fields.put("trustee_user_id", Objects.requireNonNull(trusteeUserId));
        fields.put("impersonation", impersonation);
    }

    public static TrustCreate create(String trustorUserId, String trusteeUserId, boolean impersonation) {
        return new TrustCreate(trustorUserId, trusteeUserId, impersonation);
    }

    public TrustCreate projectId(String projectId) { fields.put("project_id", projectId); return this; }
    public TrustCreate expiresAt(Date expiresAt) { fields.put("expires_at", expiresAt.toInstant().toString()); return this; }
    public TrustCreate remainingUses(int remainingUses) { fields.put("remaining_uses", remainingUses); return this; }
    public TrustCreate allowRedelegation(boolean allow) { fields.put("allow_redelegation", allow); return this; }
    public TrustCreate redelegationCount(int count) { fields.put("redelegation_count", count); return this; }

    public TrustCreate roleIds(String... ids) {
        for (String id : ids) roles.add(Collections.singletonMap("id", id));
        return this;
    }

    public TrustCreate roleNames(String... names) {
        for (String name : names) roles.add(Collections.singletonMap("name", name));
        return this;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> body = new LinkedHashMap<>(fields);
        if (!roles.isEmpty()) body.put("roles", roles);
        return body;
    }
}
```
`TrustServiceImpl`:
```java
package org.openstack4j.openstack.identity.v3.internal;

import java.util.List;
import java.util.Objects;

import org.openstack4j.api.identity.v3.TrustService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.model.identity.v3.Trust;
import org.openstack4j.model.identity.v3.options.TrustCreate;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRole;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRole.Roles;
import org.openstack4j.openstack.identity.v3.domain.KeystoneTrust;
import org.openstack4j.openstack.identity.v3.domain.KeystoneTrust.Trusts;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class TrustServiceImpl extends BaseIdentityServices implements TrustService {

    private static final String TRUSTS = "/OS-TRUST/trusts";

    @Override
    public List<? extends Trust> list() {
        return get(Trusts.class, TRUSTS).execute().getList();
    }

    @Override
    public List<? extends Trust> list(String trustorUserId, String trusteeUserId) {
        return get(Trusts.class, TRUSTS).param(trustorUserId != null, "trustor_user_id", trustorUserId)
                .param(trusteeUserId != null, "trustee_user_id", trusteeUserId).execute().getList();
    }

    @Override
    public Trust get(String trustId) {
        return get(KeystoneTrust.class, TRUSTS, "/", Objects.requireNonNull(trustId)).execute();
    }

    @Override
    public Trust create(TrustCreate create) {
        return post(KeystoneTrust.class, TRUSTS).entity(JsonBody.of("trust", Objects.requireNonNull(create).toMap())).execute();
    }

    @Override
    public ActionResponse delete(String trustId) {
        return deleteWithResponse(TRUSTS, "/", Objects.requireNonNull(trustId)).execute();
    }

    @Override
    public List<? extends Role> roles(String trustId) {
        return get(Roles.class, TRUSTS, "/", Objects.requireNonNull(trustId), "/roles").execute().getList();
    }

    @Override
    public Role getRole(String trustId, String roleId) {
        return get(KeystoneRole.class, TRUSTS, "/", Objects.requireNonNull(trustId), "/roles/", Objects.requireNonNull(roleId)).execute();
    }

    @Override
    public ActionResponse checkRole(String trustId, String roleId) {
        return head(ActionResponse.class, TRUSTS, "/", Objects.requireNonNull(trustId), "/roles/", Objects.requireNonNull(roleId)).execute();
    }
}
```
`api/identity/v3/TrustService.java`(Interfaces 의 8개 선언 + Javadoc), accessor·binding 추가.

- [ ] **Step 4: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='TrustTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Tests run: 1, Failures: 0, Errors: 0`

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/e8.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/e8.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/e8.log
git add -A && git commit -m "feat(identity): add OS-TRUST trusts

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "trusts(): list(필터)/get/create/delete, trust 역할 list/get/check."
```

---

### Task 9: OS-EP-FILTER와 OS-ENDPOINT-POLICY

**Files:**
- Create (`core/src/main/java/org/openstack4j/`): `model/identity/v3/EndpointGroup.java`, `openstack/identity/v3/domain/KeystoneEndpointGroup.java`, `api/identity/v3/EndpointFilterService.java`, `openstack/identity/v3/internal/EndpointFilterServiceImpl.java`, `api/identity/v3/EndpointPolicyService.java`, `openstack/identity/v3/internal/EndpointPolicyServiceImpl.java`
- Modify: `IdentityService.java`, `IdentityServiceImpl.java`, `DefaultAPIProvider.java`
- Create: `core-test/src/main/java/org/openstack4j/api/identity/v3/ext/EndpointFilterTests.java`, `EndpointPolicyTests.java`

**Interfaces:**
- Produces:
  - `EndpointGroup`: `getId()`, `getName()`, `getDescription()`, `Map<String, Object> getFilters()`
  - `IdentityService.endpointFilter()` → `EndpointFilterService`:
    - `List<? extends EndpointGroup> listEndpointGroups()`, `EndpointGroup getEndpointGroup(String id)`, `ActionResponse checkEndpointGroup(String id)`, `EndpointGroup createEndpointGroup(String name, String description, Map<String, Object> filters)`, `EndpointGroup updateEndpointGroup(String id, String name, String description, Map<String, Object> filters)`(null 필드 생략), `ActionResponse deleteEndpointGroup(String id)`
    - `List<? extends Endpoint> endpointGroupEndpoints(String endpointGroupId)`, `List<? extends Project> endpointGroupProjects(String endpointGroupId)`, `ActionResponse addProjectToEndpointGroup(String endpointGroupId, String projectId)`, `ActionResponse checkProjectInEndpointGroup(...)`, `ActionResponse removeProjectFromEndpointGroup(...)`, `Project getProjectEndpointGroup(String endpointGroupId, String projectId)`
    - `List<? extends EndpointGroup> projectEndpointGroups(String projectId)`, `List<? extends Endpoint> projectEndpoints(String projectId)`, `ActionResponse addEndpointToProject(String projectId, String endpointId)`, `ActionResponse checkEndpointInProject(...)`, `ActionResponse removeEndpointFromProject(...)`, `List<? extends Project> endpointProjects(String endpointId)`
  - `IdentityService.endpointPolicies()` → `EndpointPolicyService`:
    - `associateWithEndpoint(String policyId, String endpointId)`, `checkEndpointAssociation(...)`, `disassociateFromEndpoint(...)`
    - `associateWithService(String policyId, String serviceId)`, `checkServiceAssociation(...)`, `disassociateFromService(...)`
    - `associateWithServiceInRegion(String policyId, String serviceId, String regionId)`, `checkServiceInRegionAssociation(...)`, `disassociateFromServiceInRegion(...)` — 모두 `ActionResponse`
    - `List<? extends Endpoint> endpointsForPolicy(String policyId)`, `Policy policyForEndpoint(String endpointId)`, `ActionResponse checkPolicyAssociations(String policyId)`(HEAD `/policies/{id}/OS-ENDPOINT-POLICY/policy`)

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/e9-endpoint-filter
```

`EndpointFilterTests.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.EndpointGroup;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/EndpointFilter")
public class EndpointFilterTests extends AbstractIdentityExtTest {

    private static final String EG = "5e4b09fcc4bd4cb781e47a0fd65d1da5";
    private static final String EP = "e1";
    private static final String EG_JSON = "{\"id\": \"" + EG + "\", \"name\": \"os4j-fixture\", \"description\": \"fixture\", \"filters\": {\"interface\": \"public\"}, \"links\": {}}";
    private static final String ENDPOINTS = "{\"endpoints\": [{\"id\": \"e1\", \"interface\": \"public\", \"url\": \"http://x\", \"service_id\": \"s1\", \"region_id\": \"RegionOne\", \"enabled\": true, \"links\": {}}], \"links\": {}}";
    private static final String PROJECTS = "{\"projects\": [{\"id\": \"" + PROJECT + "\", \"name\": \"admin\", \"domain_id\": \"default\", \"enabled\": true, \"links\": {}}], \"links\": {}}";

    public void endpointGroups() throws Exception {
        respondWith(201, "{\"endpoint_group\": " + EG_JSON + "}");
        respondWith(200, "{\"endpoint_groups\": [" + EG_JSON + "], \"links\": {}}");
        respondWith(200, "{\"endpoint_group\": " + EG_JSON + "}");
        respondWith(200);
        respondWith(200, "{\"endpoint_group\": " + EG_JSON + "}");
        respondWith(204);

        var filter = osv3().identity().endpointFilter();
        EndpointGroup created = filter.createEndpointGroup("os4j-fixture", "fixture", Map.of("interface", "public"));
        List<? extends EndpointGroup> all = filter.listEndpointGroups();
        filter.getEndpointGroup(EG);
        boolean exists = filter.checkEndpointGroup(EG).isSuccess();
        filter.updateEndpointGroup(EG, null, "changed", null);
        filter.deleteEndpointGroup(EG);

        RecordedRequest create = takeRequest();
        Assert.assertTrue(create.getPath().endsWith("/v3/OS-EP-FILTER/endpoint_groups"));
        Assert.assertEquals(body(create).get("endpoint_group").get("filters").get("interface").asText(), "public");
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertTrue(takeRequest().getPath().endsWith("/endpoint_groups/" + EG));
        Assert.assertEquals(takeRequest().getMethod(), "HEAD");
        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PATCH");
        Assert.assertEquals(body(update).get("endpoint_group").get("description").asText(), "changed");
        Assert.assertFalse(body(update).get("endpoint_group").has("name"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getFilters().get("interface"), "public");
        Assert.assertEquals(all.get(0).getName(), "os4j-fixture");
        Assert.assertTrue(exists);
    }

    public void associations() throws Exception {
        respondWith(200, ENDPOINTS);
        respondWith(200, PROJECTS);
        respondWith(204);
        respondWith(204);
        respondWith(200, "{\"project\": {\"id\": \"" + PROJECT + "\", \"name\": \"admin\", \"links\": {}}}");
        respondWith(204);
        respondWith(200, "{\"endpoint_groups\": [" + EG_JSON + "], \"links\": {}}");
        respondWith(200, ENDPOINTS);
        respondWith(204);
        respondWith(204);
        respondWith(204);
        respondWith(200, PROJECTS);

        var filter = osv3().identity().endpointFilter();
        Assert.assertEquals(filter.endpointGroupEndpoints(EG).get(0).getId(), "e1");
        Assert.assertEquals(filter.endpointGroupProjects(EG).get(0).getId(), PROJECT);
        filter.addProjectToEndpointGroup(EG, PROJECT);
        filter.checkProjectInEndpointGroup(EG, PROJECT);
        Assert.assertEquals(filter.getProjectEndpointGroup(EG, PROJECT).getName(), "admin");
        filter.removeProjectFromEndpointGroup(EG, PROJECT);
        Assert.assertEquals(filter.projectEndpointGroups(PROJECT).size(), 1);
        Assert.assertEquals(filter.projectEndpoints(PROJECT).size(), 1);
        filter.addEndpointToProject(PROJECT, EP);
        filter.checkEndpointInProject(PROJECT, EP);
        filter.removeEndpointFromProject(PROJECT, EP);
        Assert.assertEquals(filter.endpointProjects(EP).size(), 1);

        String g = "/v3/OS-EP-FILTER/endpoint_groups/" + EG;
        String p = "/v3/OS-EP-FILTER/projects/" + PROJECT;
        String[][] expected = {{"GET", g + "/endpoints"}, {"GET", g + "/projects"}, {"PUT", g + "/projects/" + PROJECT},
                {"HEAD", g + "/projects/" + PROJECT}, {"GET", g + "/projects/" + PROJECT}, {"DELETE", g + "/projects/" + PROJECT},
                {"GET", p + "/endpoint_groups"}, {"GET", p + "/endpoints"}, {"PUT", p + "/endpoints/e1"}, {"HEAD", p + "/endpoints/e1"},
                {"DELETE", p + "/endpoints/e1"}, {"GET", "/v3/OS-EP-FILTER/endpoints/e1/projects"}};
        for (String[] e : expected) {
            RecordedRequest r = takeRequest();
            Assert.assertEquals(r.getMethod(), e[0], e[1]);
            Assert.assertTrue(r.getPath().endsWith(e[1]), r.getPath() + " vs " + e[1]);
        }
    }
}
```
`EndpointPolicyTests.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import okhttp3.mockwebserver.RecordedRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/EndpointPolicy")
public class EndpointPolicyTests extends AbstractIdentityExtTest {

    public void associations() throws Exception {
        for (int i = 0; i < 9; i++)
            respondWith(204);
        respondWith(200, "{\"endpoints\": [{\"id\": \"e1\", \"interface\": \"public\", \"url\": \"http://x\", \"links\": {}}], \"links\": {}}");
        respondWith(200, "{\"policy\": {\"id\": \"p1\", \"type\": \"application/json\", \"blob\": \"{}\", \"links\": {}}}");
        respondWith(200);

        var policies = osv3().identity().endpointPolicies();
        policies.associateWithEndpoint("p1", "e1");
        policies.checkEndpointAssociation("p1", "e1");
        policies.disassociateFromEndpoint("p1", "e1");
        policies.associateWithService("p1", "s1");
        policies.checkServiceAssociation("p1", "s1");
        policies.disassociateFromService("p1", "s1");
        policies.associateWithServiceInRegion("p1", "s1", "RegionOne");
        policies.checkServiceInRegionAssociation("p1", "s1", "RegionOne");
        policies.disassociateFromServiceInRegion("p1", "s1", "RegionOne");
        Assert.assertEquals(policies.endpointsForPolicy("p1").get(0).getId(), "e1");
        Assert.assertEquals(policies.policyForEndpoint("e1").getId(), "p1");
        Assert.assertTrue(policies.checkPolicyAssociations("p1").isSuccess());

        String base = "/v3/policies/p1/OS-ENDPOINT-POLICY";
        String[][] expected = {{"PUT", base + "/endpoints/e1"}, {"GET", base + "/endpoints/e1"}, {"DELETE", base + "/endpoints/e1"},
                {"PUT", base + "/services/s1"}, {"GET", base + "/services/s1"}, {"DELETE", base + "/services/s1"},
                {"PUT", base + "/services/s1/regions/RegionOne"}, {"GET", base + "/services/s1/regions/RegionOne"},
                {"DELETE", base + "/services/s1/regions/RegionOne"}, {"GET", base + "/endpoints"},
                {"GET", "/v3/endpoints/e1/OS-ENDPOINT-POLICY/policy"}, {"HEAD", base + "/policy"}};
        for (String[] e : expected) {
            RecordedRequest r = takeRequest();
            Assert.assertEquals(r.getMethod(), e[0], e[1]);
            Assert.assertTrue(r.getPath().endsWith(e[1]), r.getPath() + " vs " + e[1]);
        }
    }
}
```
(Keystone api-ref 의 endpoint policy "verify" 는 GET 이다 — `checkXxxAssociation` 은 `getWithResponse` 로 보낸다.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -3`
Expected: `endpointFilter()`, `EndpointGroup` 등 없음.

- [ ] **Step 3: 구현**

`model/identity/v3/EndpointGroup.java`:
```java
package org.openstack4j.model.identity.v3;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An endpoint group (OS-EP-FILTER): a filter that selects catalog endpoints. */
public interface EndpointGroup extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    /** @return the filters, for example {@code {"interface": "public", "service_id": "..."}} */
    Map<String, Object> getFilters();
}
```
`openstack/identity/v3/domain/KeystoneEndpointGroup.java`:
```java
package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.EndpointGroup;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("endpoint_group")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneEndpointGroup implements EndpointGroup {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private Map<String, Object> filters;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public Map<String, Object> getFilters() { return filters; }

    public static class EndpointGroups extends ListResult<KeystoneEndpointGroup> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("endpoint_groups")
        private List<KeystoneEndpointGroup> list;

        @Override
        protected List<KeystoneEndpointGroup> value() {
            return list;
        }
    }
}
```
`EndpointFilterServiceImpl`:
```java
package org.openstack4j.openstack.identity.v3.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.identity.v3.EndpointFilterService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Endpoint;
import org.openstack4j.model.identity.v3.EndpointGroup;
import org.openstack4j.model.identity.v3.Project;
import org.openstack4j.openstack.identity.v3.domain.KeystoneEndpoint.Endpoints;
import org.openstack4j.openstack.identity.v3.domain.KeystoneEndpointGroup;
import org.openstack4j.openstack.identity.v3.domain.KeystoneEndpointGroup.EndpointGroups;
import org.openstack4j.openstack.identity.v3.domain.KeystoneProject;
import org.openstack4j.openstack.identity.v3.domain.KeystoneProject.Projects;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class EndpointFilterServiceImpl extends BaseIdentityServices implements EndpointFilterService {

    private static final String GROUPS = "/OS-EP-FILTER/endpoint_groups/";
    private static final String PROJECTS = "/OS-EP-FILTER/projects/";

    private static String id(String value) {
        return Objects.requireNonNull(value);
    }

    private static Map<String, Object> group(String name, String description, Map<String, Object> filters) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (filters != null) body.put("filters", filters);
        return body;
    }

    @Override public List<? extends EndpointGroup> listEndpointGroups() { return get(EndpointGroups.class, "/OS-EP-FILTER/endpoint_groups").execute().getList(); }
    @Override public EndpointGroup getEndpointGroup(String id) { return get(KeystoneEndpointGroup.class, GROUPS, id(id)).execute(); }
    @Override public ActionResponse checkEndpointGroup(String id) { return head(ActionResponse.class, GROUPS, id(id)).execute(); }

    @Override
    public EndpointGroup createEndpointGroup(String name, String description, Map<String, Object> filters) {
        return post(KeystoneEndpointGroup.class, "/OS-EP-FILTER/endpoint_groups")
                .entity(JsonBody.of("endpoint_group", group(id(name), description, id(filters)))).execute();
    }

    @Override
    public EndpointGroup updateEndpointGroup(String id, String name, String description, Map<String, Object> filters) {
        return patch(KeystoneEndpointGroup.class, GROUPS, id(id)).entity(JsonBody.of("endpoint_group", group(name, description, filters))).execute();
    }

    @Override public ActionResponse deleteEndpointGroup(String id) { return deleteWithResponse(GROUPS, id(id)).execute(); }
    @Override public List<? extends Endpoint> endpointGroupEndpoints(String eg) { return get(Endpoints.class, GROUPS, id(eg), "/endpoints").execute().getList(); }
    @Override public List<? extends Project> endpointGroupProjects(String eg) { return get(Projects.class, GROUPS, id(eg), "/projects").execute().getList(); }
    @Override public ActionResponse addProjectToEndpointGroup(String eg, String p) { return put(ActionResponse.class, GROUPS, id(eg), "/projects/", id(p)).execute(); }
    @Override public ActionResponse checkProjectInEndpointGroup(String eg, String p) { return head(ActionResponse.class, GROUPS, id(eg), "/projects/", id(p)).execute(); }
    @Override public ActionResponse removeProjectFromEndpointGroup(String eg, String p) { return deleteWithResponse(GROUPS, id(eg), "/projects/", id(p)).execute(); }
    @Override public Project getProjectEndpointGroup(String eg, String p) { return get(KeystoneProject.class, GROUPS, id(eg), "/projects/", id(p)).execute(); }
    @Override public List<? extends EndpointGroup> projectEndpointGroups(String p) { return get(EndpointGroups.class, PROJECTS, id(p), "/endpoint_groups").execute().getList(); }
    @Override public List<? extends Endpoint> projectEndpoints(String p) { return get(Endpoints.class, PROJECTS, id(p), "/endpoints").execute().getList(); }
    @Override public ActionResponse addEndpointToProject(String p, String e) { return put(ActionResponse.class, PROJECTS, id(p), "/endpoints/", id(e)).execute(); }
    @Override public ActionResponse checkEndpointInProject(String p, String e) { return head(ActionResponse.class, PROJECTS, id(p), "/endpoints/", id(e)).execute(); }
    @Override public ActionResponse removeEndpointFromProject(String p, String e) { return deleteWithResponse(PROJECTS, id(p), "/endpoints/", id(e)).execute(); }
    @Override public List<? extends Project> endpointProjects(String e) { return get(Projects.class, "/OS-EP-FILTER/endpoints/", id(e), "/projects").execute().getList(); }
}
```
`EndpointPolicyServiceImpl`:
```java
package org.openstack4j.openstack.identity.v3.internal;

import java.util.List;
import java.util.Objects;

import org.openstack4j.api.identity.v3.EndpointPolicyService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Endpoint;
import org.openstack4j.model.identity.v3.Policy;
import org.openstack4j.openstack.identity.v3.domain.KeystoneEndpoint.Endpoints;
import org.openstack4j.openstack.identity.v3.domain.KeystonePolicy;

public class EndpointPolicyServiceImpl extends BaseIdentityServices implements EndpointPolicyService {

    private static String base(String policyId) {
        return "/policies/" + Objects.requireNonNull(policyId) + "/OS-ENDPOINT-POLICY";
    }

    private static String endpoint(String policyId, String endpointId) {
        return base(policyId) + "/endpoints/" + Objects.requireNonNull(endpointId);
    }

    private static String service(String policyId, String serviceId) {
        return base(policyId) + "/services/" + Objects.requireNonNull(serviceId);
    }

    private static String region(String policyId, String serviceId, String regionId) {
        return service(policyId, serviceId) + "/regions/" + Objects.requireNonNull(regionId);
    }

    @Override public ActionResponse associateWithEndpoint(String p, String e) { return put(ActionResponse.class, endpoint(p, e)).execute(); }
    @Override public ActionResponse checkEndpointAssociation(String p, String e) { return getWithResponse(endpoint(p, e)).execute(); }
    @Override public ActionResponse disassociateFromEndpoint(String p, String e) { return deleteWithResponse(endpoint(p, e)).execute(); }
    @Override public ActionResponse associateWithService(String p, String s) { return put(ActionResponse.class, service(p, s)).execute(); }
    @Override public ActionResponse checkServiceAssociation(String p, String s) { return getWithResponse(service(p, s)).execute(); }
    @Override public ActionResponse disassociateFromService(String p, String s) { return deleteWithResponse(service(p, s)).execute(); }
    @Override public ActionResponse associateWithServiceInRegion(String p, String s, String r) { return put(ActionResponse.class, region(p, s, r)).execute(); }
    @Override public ActionResponse checkServiceInRegionAssociation(String p, String s, String r) { return getWithResponse(region(p, s, r)).execute(); }
    @Override public ActionResponse disassociateFromServiceInRegion(String p, String s, String r) { return deleteWithResponse(region(p, s, r)).execute(); }
    @Override public List<? extends Endpoint> endpointsForPolicy(String p) { return get(Endpoints.class, base(p) + "/endpoints").execute().getList(); }
    @Override public Policy policyForEndpoint(String e) { return get(KeystonePolicy.class, "/endpoints/" + Objects.requireNonNull(e) + "/OS-ENDPOINT-POLICY/policy").execute(); }
    @Override public ActionResponse checkPolicyAssociations(String p) { return head(ActionResponse.class, base(p) + "/policy").execute(); }
}
```
두 인터페이스(`extends RestService`, Javadoc), accessor 2개, binding 2개 추가.

- [ ] **Step 4: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='EndpointFilterTests,EndpointPolicyTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/e9.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/e9.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/e9.log
git add -A && git commit -m "feat(identity): add OS-EP-FILTER endpoint groups and OS-ENDPOINT-POLICY associations

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "endpointFilter(): endpoint group CRUD/HEAD, project·endpoint 연결 18개. endpointPolicies(): endpoint/service/service+region 연결 9개와 조회 3개."
```

---

### Task 10: unified limits — registered limits, limits, enforcement model

**Files:**
- Create (`core/src/main/java/org/openstack4j/`): `model/identity/v3/Limit.java`, `RegisteredLimit.java`, `LimitModel.java`, `model/identity/v3/options/LimitCreate.java`, `RegisteredLimitCreate.java`, `LimitListOptions.java`, `RegisteredLimitListOptions.java`, `api/identity/v3/LimitService.java`, `RegisteredLimitService.java`, `openstack/identity/v3/domain/KeystoneLimit.java`, `KeystoneRegisteredLimit.java`, `KeystoneLimitModel.java`, `openstack/identity/v3/internal/LimitServiceImpl.java`, `RegisteredLimitServiceImpl.java`
- Modify: `IdentityService.java`, `IdentityServiceImpl.java`, `DefaultAPIProvider.java`
- Create: `core-test/src/main/java/org/openstack4j/api/identity/v3/ext/LimitTests.java`

**Interfaces:**
- Produces:
  - `RegisteredLimit`: `getId()`, `getServiceId()`, `getRegionId()`, `getResourceName()`, `Integer getDefaultLimit()`, `getDescription()`
  - `Limit`: `getId()`, `getServiceId()`, `getRegionId()`, `getResourceName()`, `Integer getResourceLimit()`, `getDescription()`, `getProjectId()`, `getDomainId()`
  - `LimitModel`: `getName()`, `getDescription()`
  - `RegisteredLimitCreate.create(String serviceId, String resourceName, int defaultLimit)`: `regionId`, `description`; `toMap()`
  - `LimitCreate.forProject(String projectId, String serviceId, String resourceName, int resourceLimit)` / `forDomain(String domainId, ...)`: `regionId`, `description`; `toMap()`
  - `RegisteredLimitListOptions.create()`: `serviceId`, `regionId`, `resourceName`; `Map<String, Object> getOptions()`
  - `LimitListOptions.create()`: `serviceId`, `regionId`, `resourceName`, `projectId`, `domainId`; `getOptions()`
  - `IdentityService.registeredLimits()` → `RegisteredLimitService`: `List<? extends RegisteredLimit> list()`, `list(RegisteredLimitListOptions)`, `RegisteredLimit get(String id)`, `List<? extends RegisteredLimit> create(List<RegisteredLimitCreate> limits)`, `RegisteredLimit update(String id, Integer defaultLimit, String description)`(null 필드 생략), `ActionResponse delete(String id)`
  - `IdentityService.limits()` → `LimitService`: `LimitModel model()`, `List<? extends Limit> list()`, `list(LimitListOptions)`, `Limit get(String id)`, `List<? extends Limit> create(List<LimitCreate> limits)`, `Limit update(String id, Integer resourceLimit, String description)`, `ActionResponse delete(String id)`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/e10-limits
```

`LimitTests.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.Limit;
import org.openstack4j.model.identity.v3.RegisteredLimit;
import org.openstack4j.model.identity.v3.options.LimitCreate;
import org.openstack4j.model.identity.v3.options.LimitListOptions;
import org.openstack4j.model.identity.v3.options.RegisteredLimitCreate;
import org.openstack4j.model.identity.v3.options.RegisteredLimitListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/Limits")
public class LimitTests extends AbstractIdentityExtTest {

    private static final String SERVICE = "78d4693e713c43abb7f24f39020f9d76";
    private static final String RL = "3facd55c5d7d4ac88a8b4177a9c4e9d0";
    private static final String RL_JSON = "{\"id\": \"" + RL + "\", \"service_id\": \"" + SERVICE + "\", \"region_id\": null, \"resource_name\": \"os4j_fixture\", \"default_limit\": 5, \"description\": \"fixture\", \"links\": {}}";
    private static final String L_JSON = "{\"id\": \"l1\", \"service_id\": \"" + SERVICE + "\", \"region_id\": \"RegionOne\", \"resource_name\": \"os4j_fixture\", \"resource_limit\": 10, \"description\": null, \"project_id\": \"" + PROJECT + "\", \"domain_id\": null, \"links\": {}}";

    public void registeredLimits() throws Exception {
        respondWith(201, "{\"registered_limits\": [" + RL_JSON + "]}");
        respondWith(200, "{\"registered_limits\": [" + RL_JSON + "], \"links\": {}}");
        respondWith(200, "{\"registered_limits\": [], \"links\": {}}");
        respondWith(200, "{\"registered_limit\": " + RL_JSON + "}");
        respondWith(200, "{\"registered_limit\": " + RL_JSON + "}");
        respondWith(204);

        var limits = osv3().identity().registeredLimits();
        List<? extends RegisteredLimit> created = limits.create(Collections.singletonList(
                RegisteredLimitCreate.create(SERVICE, "os4j_fixture", 5).description("fixture")));
        limits.list();
        limits.list(RegisteredLimitListOptions.create().serviceId(SERVICE).resourceName("os4j_fixture"));
        RegisteredLimit one = limits.get(RL);
        limits.update(RL, 7, null);
        limits.delete(RL);

        RecordedRequest create = takeRequest();
        Assert.assertTrue(create.getPath().endsWith("/v3/registered_limits"));
        JsonNode first = body(create).get("registered_limits").get(0);
        Assert.assertEquals(first.get("service_id").asText(), SERVICE);
        Assert.assertEquals(first.get("default_limit").asInt(), 5);
        Assert.assertFalse(first.has("region_id"));
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        String query = decodedPath(takeRequest());
        Assert.assertTrue(query.contains("service_id=" + SERVICE) && query.contains("resource_name=os4j_fixture"), query);
        Assert.assertTrue(takeRequest().getPath().endsWith("/registered_limits/" + RL));
        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PATCH");
        Assert.assertEquals(body(update).get("registered_limit").get("default_limit").asInt(), 7);
        Assert.assertFalse(body(update).get("registered_limit").has("description"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.get(0).getDefaultLimit(), Integer.valueOf(5));
        Assert.assertNull(one.getRegionId());
    }

    public void projectLimitsAndModel() throws Exception {
        respondWith(200, "{\"model\": {\"name\": \"flat\", \"description\": \"Limit enforcement and validation does not take project hierarchy into consideration.\"}}");
        respondWith(201, "{\"limits\": [" + L_JSON + "]}");
        respondWith(200, "{\"limits\": [" + L_JSON + "], \"links\": {}}");
        respondWith(200, "{\"limit\": " + L_JSON + "}");
        respondWith(200, "{\"limit\": " + L_JSON + "}");
        respondWith(204);

        var limits = osv3().identity().limits();
        Assert.assertEquals(limits.model().getName(), "flat");
        List<? extends Limit> created = limits.create(Arrays.asList(
                LimitCreate.forProject(PROJECT, SERVICE, "os4j_fixture", 10).regionId("RegionOne")));
        limits.list(LimitListOptions.create().projectId(PROJECT));
        Limit one = limits.get("l1");
        limits.update("l1", null, "note");
        limits.delete("l1");

        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/limits/model"));
        JsonNode first = body(takeRequest()).get("limits").get(0);
        Assert.assertEquals(first.get("project_id").asText(), PROJECT);
        Assert.assertEquals(first.get("resource_limit").asInt(), 10);
        Assert.assertEquals(first.get("region_id").asText(), "RegionOne");
        Assert.assertTrue(decodedPath(takeRequest()).endsWith("/v3/limits?project_id=" + PROJECT));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/limits/l1"));
        RecordedRequest update = takeRequest();
        Assert.assertEquals(body(update).get("limit").get("description").asText(), "note");
        Assert.assertFalse(body(update).get("limit").has("resource_limit"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.get(0).getResourceLimit(), Integer.valueOf(10));
        Assert.assertEquals(one.getProjectId(), PROJECT);
        Assert.assertNull(one.getDomainId());
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -3`
Expected: `Limit`, `RegisteredLimit` 등 없음.

- [ ] **Step 3: 모델 구현**

인터페이스 3개(`Limit`, `RegisteredLimit`, `LimitModel`)는 Interfaces 의 getter 를 그대로 선언한다(`extends ModelEntity`, Javadoc 한 줄). Keystone 도메인 클래스:
```java
package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.RegisteredLimit;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("registered_limit")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneRegisteredLimit implements RegisteredLimit {

    private static final long serialVersionUID = 1L;

    private String id;
    @JsonProperty("service_id") private String serviceId;
    @JsonProperty("region_id") private String regionId;
    @JsonProperty("resource_name") private String resourceName;
    @JsonProperty("default_limit") private Integer defaultLimit;
    private String description;

    @Override public String getId() { return id; }
    @Override public String getServiceId() { return serviceId; }
    @Override public String getRegionId() { return regionId; }
    @Override public String getResourceName() { return resourceName; }
    @Override public Integer getDefaultLimit() { return defaultLimit; }
    @Override public String getDescription() { return description; }

    public static class RegisteredLimits extends ListResult<KeystoneRegisteredLimit> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("registered_limits")
        private List<KeystoneRegisteredLimit> list;

        @Override
        protected List<KeystoneRegisteredLimit> value() {
            return list;
        }
    }
}
```
`KeystoneLimit` 은 같은 모양에 `@JsonRootName("limit")`, `resource_limit`, `project_id`, `domain_id` 필드와 `Limits`(`@JsonProperty("limits")`) 목록. `KeystoneLimitModel` 은 `@JsonRootName("model")`, `name`, `description`.

옵션:
```java
package org.openstack4j.model.identity.v3.options;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** One entry of {@code POST /registered_limits}. */
public class RegisteredLimitCreate {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    private RegisteredLimitCreate(String serviceId, String resourceName, int defaultLimit) {
        fields.put("service_id", Objects.requireNonNull(serviceId));
        fields.put("resource_name", Objects.requireNonNull(resourceName));
        fields.put("default_limit", defaultLimit);
    }

    public static RegisteredLimitCreate create(String serviceId, String resourceName, int defaultLimit) {
        return new RegisteredLimitCreate(serviceId, resourceName, defaultLimit);
    }

    public RegisteredLimitCreate regionId(String regionId) { fields.put("region_id", regionId); return this; }
    public RegisteredLimitCreate description(String description) { fields.put("description", description); return this; }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(fields);
    }
}
```
```java
package org.openstack4j.model.identity.v3.options;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** One entry of {@code POST /limits}: a project or domain override of a registered limit. */
public class LimitCreate {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    private LimitCreate(String ownerKey, String ownerId, String serviceId, String resourceName, int resourceLimit) {
        fields.put(ownerKey, Objects.requireNonNull(ownerId));
        fields.put("service_id", Objects.requireNonNull(serviceId));
        fields.put("resource_name", Objects.requireNonNull(resourceName));
        fields.put("resource_limit", resourceLimit);
    }

    public static LimitCreate forProject(String projectId, String serviceId, String resourceName, int resourceLimit) {
        return new LimitCreate("project_id", projectId, serviceId, resourceName, resourceLimit);
    }

    public static LimitCreate forDomain(String domainId, String serviceId, String resourceName, int resourceLimit) {
        return new LimitCreate("domain_id", domainId, serviceId, resourceName, resourceLimit);
    }

    public LimitCreate regionId(String regionId) { fields.put("region_id", regionId); return this; }
    public LimitCreate description(String description) { fields.put("description", description); return this; }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(fields);
    }
}
```
```java
package org.openstack4j.model.identity.v3.options;

import java.util.LinkedHashMap;
import java.util.Map;

/** Filters of {@code GET /limits}. */
public class LimitListOptions {

    private final Map<String, Object> options = new LinkedHashMap<>();

    public static LimitListOptions create() {
        return new LimitListOptions();
    }

    public LimitListOptions serviceId(String serviceId) { options.put("service_id", serviceId); return this; }
    public LimitListOptions regionId(String regionId) { options.put("region_id", regionId); return this; }
    public LimitListOptions resourceName(String resourceName) { options.put("resource_name", resourceName); return this; }
    public LimitListOptions projectId(String projectId) { options.put("project_id", projectId); return this; }
    public LimitListOptions domainId(String domainId) { options.put("domain_id", domainId); return this; }

    public Map<String, Object> getOptions() {
        return options;
    }
}
```
`RegisteredLimitListOptions` 는 같은 모양에 `serviceId`, `regionId`, `resourceName` 세 개.

- [ ] **Step 4: 서비스 구현**

```java
package org.openstack4j.openstack.identity.v3.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openstack4j.api.identity.v3.RegisteredLimitService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.RegisteredLimit;
import org.openstack4j.model.identity.v3.options.RegisteredLimitCreate;
import org.openstack4j.model.identity.v3.options.RegisteredLimitListOptions;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRegisteredLimit;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRegisteredLimit.RegisteredLimits;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class RegisteredLimitServiceImpl extends BaseIdentityServices implements RegisteredLimitService {

    @Override
    public List<? extends RegisteredLimit> list() {
        return get(RegisteredLimits.class, "/registered_limits").execute().getList();
    }

    @Override
    public List<? extends RegisteredLimit> list(RegisteredLimitListOptions options) {
        return get(RegisteredLimits.class, "/registered_limits").params(Objects.requireNonNull(options).getOptions()).execute().getList();
    }

    @Override
    public RegisteredLimit get(String id) {
        return get(KeystoneRegisteredLimit.class, "/registered_limits/", Objects.requireNonNull(id)).execute();
    }

    @Override
    public List<? extends RegisteredLimit> create(List<RegisteredLimitCreate> limits) {
        List<Map<String, Object>> entries = limits.stream().map(RegisteredLimitCreate::toMap).collect(Collectors.toList());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("registered_limits", entries);
        return post(RegisteredLimits.class, "/registered_limits").entity(JsonBody.of(body)).execute().getList();
    }

    @Override
    public RegisteredLimit update(String id, Integer defaultLimit, String description) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (defaultLimit != null) fields.put("default_limit", defaultLimit);
        if (description != null) fields.put("description", description);
        return patch(KeystoneRegisteredLimit.class, "/registered_limits/", Objects.requireNonNull(id))
                .entity(JsonBody.of("registered_limit", fields)).execute();
    }

    @Override
    public ActionResponse delete(String id) {
        return deleteWithResponse("/registered_limits/", Objects.requireNonNull(id)).execute();
    }
}
```
`LimitServiceImpl` 은 같은 모양으로 `/limits`, 루트 `limits`/`limit`, update 필드 `resource_limit`, 그리고:
```java
    @Override
    public LimitModel model() {
        return get(KeystoneLimitModel.class, "/limits/model").execute();
    }
```
인터페이스 2개, accessor 2개, binding 2개 추가.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='LimitTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/e10.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/e10.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/e10.log
git add -A && git commit -m "feat(identity): add unified limits (registered limits, limits, enforcement model)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "registeredLimits()·limits(): list(필터)/get/create(일괄)/update/delete, limits().model()."
```

---

### Task 11: OS-FEDERATION

**Files:**
- Create (`core/src/main/java/org/openstack4j/`):
  - 모델: `model/identity/v3/IdentityProvider.java`, `FederationProtocol.java`, `Mapping.java`, `ServiceProvider.java`
  - 도메인: `openstack/identity/v3/domain/KeystoneIdentityProvider.java`, `KeystoneFederationProtocol.java`, `KeystoneMapping.java`, `KeystoneServiceProvider.java`
  - API: `api/identity/v3/FederationService.java`, `IdentityProviderService.java`, `MappingService.java`, `ServiceProviderService.java`
  - 구현: `openstack/identity/v3/internal/FederationServiceImpl.java`, `IdentityProviderServiceImpl.java`, `MappingServiceImpl.java`, `ServiceProviderServiceImpl.java`, `IdentityResponses.java`(본문을 문자열로 읽는 helper — Task 12 도 쓴다)
- Modify: `IdentityService.java`, `IdentityServiceImpl.java`, `DefaultAPIProvider.java`
- Create: `core-test/src/main/java/org/openstack4j/api/identity/v3/ext/FederationTests.java`

**Interfaces:**
- Produces:
  - `IdentityProvider`: `getId()`, `getDescription()`, `Boolean isEnabled()`, `getDomainId()`, `List<String> getRemoteIds()`, `Integer getAuthorizationTtl()`
  - `FederationProtocol`: `getId()`, `getMappingId()`, `getRemoteIdAttribute()`
  - `Mapping`: `getId()`, `List<Map<String, Object>> getRules()`, `getSchemaVersion()`
  - `ServiceProvider`: `getId()`, `getDescription()`, `Boolean isEnabled()`, `getAuthUrl()`, `getSpUrl()`, `getRelayStatePrefix()`
  - `IdentityService.federation()` → `FederationService`:
    - `IdentityProviderService identityProviders()`: `list()`, `get(id)`, `create(String id, Map<String, Object> attributes)`(PUT; 키: `description`, `enabled`, `domain_id`, `remote_ids`, `authorization_ttl`), `update(String id, Map<String, Object> attributes)`(PATCH), `delete(id)`; `protocols(String idpId)`, `getProtocol(idpId, protocolId)`, `createProtocol(String idpId, String protocolId, String mappingId)`(PUT), `updateProtocol(idpId, protocolId, mappingId)`(PATCH), `deleteProtocol(idpId, protocolId)`
    - `MappingService mappings()`: `list()`, `get(id)`, `create(String id, List<Map<String, Object>> rules)`(PUT), `update(String id, List<Map<String, Object>> rules)`(PATCH), `delete(id)`
    - `ServiceProviderService serviceProviders()`: `list()`, `get(id)`, `create(String id, Map<String, Object> attributes)`(PUT; 키: `auth_url`, `sp_url`, `description`, `enabled`, `relay_state_prefix`), `update(id, attributes)`(PATCH), `delete(id)`
    - `List<? extends Project> projects()`, `List<? extends Domain> domains()`(`/OS-FEDERATION/projects|domains`)
    - `String saml2Metadata()`(XML), `String saml2Assertion(String tokenId, String serviceProviderId)`(XML), `String ecpAssertion(String tokenId, String serviceProviderId)`(SOAP XML)
    - `Token federatedToken(String idpId, String protocolId, Map<String, String> headers)` — `GET /OS-FEDERATION/identity_providers/{idp}/protocols/{p}/auth`, 토큰 id 는 `X-Subject-Token` 헤더에서
  - `IdentityResponses.text(HttpResponse)`: 상태 ≥ 400 이면 `ClientResponseException`, 아니면 본문을 UTF-8 문자열로 읽고 응답을 닫는다.
- 결정: IdP·service provider 생성/수정 본문은 `Map<String, Object>` 로 받는다 — 필드가 서버 버전마다 늘고(`authorization_ttl` 은 2023.1 추가) 모두 선택값이라 builder 보다 Map 이 덜 깨진다. Javadoc 에 키를 적는다.

- [ ] **Step 1: 실패하는 테스트 작성 (Review Focus 5 일부)**

```bash
git switch main && git pull && git switch -c task/e11-federation
```

`FederationTests.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.IdentityProvider;
import org.openstack4j.model.identity.v3.Mapping;
import org.openstack4j.model.identity.v3.ServiceProvider;
import org.openstack4j.model.identity.v3.Token;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/Federation")
public class FederationTests extends AbstractIdentityExtTest {

    private static final String IDP_JSON = "{\"id\": \"os4j-fixture\", \"domain_id\": \"f9381a82bc9a4805bc5c4370f3215857\", \"enabled\": false,"
            + " \"description\": \"fixture\", \"remote_ids\": [\"https://idp.example.com/os4j\"], \"authorization_ttl\": null, \"links\": {}}";
    private static final String MAPPING_JSON = "{\"id\": \"os4j-fixture\", \"rules\": [{\"local\": [{\"user\": {\"name\": \"{0}\"}}],"
            + " \"remote\": [{\"type\": \"REMOTE_USER\"}]}], \"schema_version\": \"1.0\", \"links\": {}}";
    private static final String PROTOCOL_JSON = "{\"id\": \"saml2\", \"mapping_id\": \"os4j-fixture\", \"links\": {}}";
    private static final String SP_JSON = "{\"id\": \"sp1\", \"auth_url\": \"https://sp/v3/OS-FEDERATION/identity_providers/acme/protocols/saml2/auth\","
            + " \"sp_url\": \"https://sp/Shibboleth.sso/SAML2/ECP\", \"description\": null, \"enabled\": true, \"relay_state_prefix\": \"ss:mem:\", \"links\": {}}";

    public void identityProvidersAndProtocols() throws Exception {
        respondWith(201, "{\"identity_provider\": " + IDP_JSON + "}");
        respondWith(200, "{\"identity_providers\": [" + IDP_JSON + "], \"links\": {}}");
        respondWith(200, "{\"identity_provider\": " + IDP_JSON + "}");
        respondWith(200, "{\"identity_provider\": " + IDP_JSON + "}");
        respondWith(201, "{\"protocol\": " + PROTOCOL_JSON + "}");
        respondWith(200, "{\"protocols\": [" + PROTOCOL_JSON + "], \"links\": {}}");
        respondWith(200, "{\"protocol\": " + PROTOCOL_JSON + "}");
        respondWith(200, "{\"protocol\": " + PROTOCOL_JSON + "}");
        respondWith(204);
        respondWith(204);

        var idps = osv3().identity().federation().identityProviders();
        IdentityProvider created = idps.create("os4j-fixture", Map.of("enabled", false, "remote_ids", List.of("https://idp.example.com/os4j")));
        List<? extends IdentityProvider> all = idps.list();
        idps.get("os4j-fixture");
        idps.update("os4j-fixture", Map.of("description", "changed"));
        idps.createProtocol("os4j-fixture", "saml2", "os4j-fixture");
        Assert.assertEquals(idps.protocols("os4j-fixture").get(0).getMappingId(), "os4j-fixture");
        idps.getProtocol("os4j-fixture", "saml2");
        idps.updateProtocol("os4j-fixture", "saml2", "other");
        idps.deleteProtocol("os4j-fixture", "saml2");
        idps.delete("os4j-fixture");

        String idp = "/v3/OS-FEDERATION/identity_providers/os4j-fixture";
        RecordedRequest create = takeRequest();
        Assert.assertEquals(create.getMethod(), "PUT");
        Assert.assertTrue(create.getPath().endsWith(idp));
        Assert.assertEquals(body(create).get("identity_provider").get("remote_ids").get(0).asText(), "https://idp.example.com/os4j");
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-FEDERATION/identity_providers"));
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertEquals(takeRequest().getMethod(), "PATCH");
        RecordedRequest protocol = takeRequest();
        Assert.assertEquals(protocol.getMethod(), "PUT");
        Assert.assertTrue(protocol.getPath().endsWith(idp + "/protocols/saml2"));
        Assert.assertEquals(body(protocol).get("protocol").get("mapping_id").asText(), "os4j-fixture");
        Assert.assertTrue(takeRequest().getPath().endsWith(idp + "/protocols"));
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertEquals(body(takeRequest()).get("protocol").get("mapping_id").asText(), "other");
        Assert.assertTrue(takeRequest().getPath().endsWith(idp + "/protocols/saml2"));
        Assert.assertTrue(takeRequest().getPath().endsWith(idp));
        Assert.assertFalse(created.isEnabled());
        Assert.assertEquals(created.getRemoteIds(), Arrays.asList("https://idp.example.com/os4j"));
        Assert.assertNull(all.get(0).getAuthorizationTtl());
    }

    public void mappingsAndServiceProviders() throws Exception {
        respondWith(201, "{\"mapping\": " + MAPPING_JSON + "}");
        respondWith(200, "{\"mappings\": [" + MAPPING_JSON + "], \"links\": {}}");
        respondWith(200, "{\"mapping\": " + MAPPING_JSON + "}");
        respondWith(200, "{\"mapping\": " + MAPPING_JSON + "}");
        respondWith(204);
        respondWith(201, "{\"service_provider\": " + SP_JSON + "}");
        respondWith(200, "{\"service_providers\": [" + SP_JSON + "], \"links\": {}}");
        respondWith(200, "{\"service_provider\": " + SP_JSON + "}");
        respondWith(200, "{\"service_provider\": " + SP_JSON + "}");
        respondWith(204);

        List<Map<String, Object>> rules = List.of(Map.of("local", List.of(Map.of("user", Map.of("name", "{0}"))), "remote", List.of(Map.of("type", "REMOTE_USER"))));
        var mappings = osv3().identity().federation().mappings();
        Mapping mapping = mappings.create("os4j-fixture", rules);
        Assert.assertEquals(mappings.list().size(), 1);
        mappings.get("os4j-fixture");
        mappings.update("os4j-fixture", rules);
        mappings.delete("os4j-fixture");
        var sps = osv3().identity().federation().serviceProviders();
        ServiceProvider sp = sps.create("sp1", Map.of("auth_url", "https://sp/auth", "sp_url", "https://sp/ecp"));
        Assert.assertEquals(sps.list().size(), 1);
        sps.get("sp1");
        sps.update("sp1", Map.of("enabled", false));
        sps.delete("sp1");

        RecordedRequest create = takeRequest();
        Assert.assertEquals(create.getMethod(), "PUT");
        Assert.assertTrue(create.getPath().endsWith("/v3/OS-FEDERATION/mappings/os4j-fixture"));
        JsonNode rule = body(create).get("mapping").get("rules").get(0);
        Assert.assertEquals(rule.get("remote").get(0).get("type").asText(), "REMOTE_USER");
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-FEDERATION/mappings"));
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertEquals(takeRequest().getMethod(), "PATCH");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        RecordedRequest spCreate = takeRequest();
        Assert.assertTrue(spCreate.getPath().endsWith("/v3/OS-FEDERATION/service_providers/sp1"));
        Assert.assertEquals(body(spCreate).get("service_provider").get("sp_url").asText(), "https://sp/ecp");
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-FEDERATION/service_providers"));
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertFalse(body(takeRequest()).get("service_provider").get("enabled").asBoolean());
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(mapping.getRules().get(0).get("remote"), List.of(Map.of("type", "REMOTE_USER")));
        Assert.assertEquals(mapping.getSchemaVersion(), "1.0");
        Assert.assertEquals(sp.getRelayStatePrefix(), "ss:mem:");
        Assert.assertTrue(sp.isEnabled());
    }

    public void saml2MetadataIsReturnedAsXml() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>\n<ns0:EntityDescriptor xmlns:ns0=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"k2k\"/>";
        respondWith(Collections.singletonMap("Content-Type", "text/xml"), 200, xml);
        respondWith(Collections.singletonMap("Content-Type", "text/xml"), 200, "<samlp:Response/>");
        respondWith(Collections.singletonMap("Content-Type", "text/xml"), 200, "<soap11:Envelope/>");

        Assert.assertEquals(osv3().identity().federation().saml2Metadata(), xml);
        Assert.assertEquals(osv3().identity().federation().saml2Assertion("tok", "sp1"), "<samlp:Response/>");
        Assert.assertEquals(osv3().identity().federation().ecpAssertion("tok", "sp1"), "<soap11:Envelope/>");

        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-FEDERATION/saml2/metadata"));
        RecordedRequest saml = takeRequest();
        Assert.assertTrue(saml.getPath().endsWith("/v3/auth/OS-FEDERATION/saml2"));
        JsonNode auth = body(saml).get("auth");
        Assert.assertEquals(auth.get("identity").get("methods").get(0).asText(), "token");
        Assert.assertEquals(auth.get("identity").get("token").get("id").asText(), "tok");
        Assert.assertEquals(auth.get("scope").get("service_provider").get("id").asText(), "sp1");
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/auth/OS-FEDERATION/saml2/ecp"));
    }

    public void saml2ErrorIsRaised() throws Exception {
        respondWith(404, "{\"error\": {\"code\": 404, \"message\": \"not found\", \"title\": \"Not Found\"}}");
        try {
            osv3().identity().federation().saml2Metadata();
            Assert.fail("expected an exception");
        } catch (RuntimeException expected) {
            Assert.assertTrue(expected.getMessage() != null);
        }
        takeRequest();
    }

    public void projectsDomainsAndFederatedToken() throws Exception {
        respondWith(200, "{\"projects\": [{\"id\": \"" + PROJECT + "\", \"name\": \"admin\", \"links\": {}}], \"links\": {}}");
        respondWith(200, "{\"domains\": [{\"id\": \"default\", \"name\": \"Default\", \"links\": {}}], \"links\": {}}");
        respondWith(Collections.singletonMap("X-Subject-Token", "fedtok"), 201,
                "{\"token\": {\"methods\": [\"saml2\"], \"user\": {\"id\": \"u1\", \"name\": \"fed\", \"OS-FEDERATION\": {\"identity_provider\": {\"id\": \"acme\"}, \"protocol\": {\"id\": \"saml2\"}, \"groups\": []}},"
                        + " \"expires_at\": \"2026-10-03T12:00:00.000000Z\", \"issued_at\": \"2026-10-03T11:00:00.000000Z\", \"audit_ids\": [\"a\"]}}");

        Assert.assertEquals(osv3().identity().federation().projects().get(0).getId(), PROJECT);
        Assert.assertEquals(osv3().identity().federation().domains().get(0).getName(), "Default");
        Token token = osv3().identity().federation().federatedToken("acme", "saml2", Map.of("REMOTE_USER", "fed"));

        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-FEDERATION/projects"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-FEDERATION/domains"));
        RecordedRequest auth = takeRequest();
        Assert.assertTrue(auth.getPath().endsWith("/v3/OS-FEDERATION/identity_providers/acme/protocols/saml2/auth"));
        Assert.assertEquals(auth.getHeader("REMOTE_USER"), "fed");
        Assert.assertEquals(token.getId(), "fedtok");
        Assert.assertEquals(token.getUser().getName(), "fed");
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -3`
Expected: `federation()`, `IdentityProvider` 등 없음.

- [ ] **Step 3: 모델 구현**

인터페이스 4개는 Interfaces 의 getter 를 선언한다(`extends ModelEntity`). 도메인 클래스는 Task 3 의 `KeystoneAccessRule` 과 같은 모양으로 쓴다 — `@JsonRootName`, `@JsonIgnoreProperties(ignoreUnknown = true)`, `@JsonProperty` 필드, 목록 클래스:

| 클래스 | 루트 / 목록 키 | 필드(JSON) |
|---|---|---|
| `KeystoneIdentityProvider` | `identity_provider` / `identity_providers`(`IdentityProviders`) | `id`, `description`, `enabled`(Boolean), `domain_id`, `remote_ids`(List<String>), `authorization_ttl`(Integer) |
| `KeystoneFederationProtocol` | `protocol` / `protocols`(`Protocols`) | `id`, `mapping_id`, `remote_id_attribute` |
| `KeystoneMapping` | `mapping` / `mappings`(`Mappings`) | `id`, `rules`(List<Map<String, Object>>), `schema_version` |
| `KeystoneServiceProvider` | `service_provider` / `service_providers`(`ServiceProviders`) | `id`, `description`, `enabled`(Boolean), `auth_url`, `sp_url`, `relay_state_prefix` |

예 — `KeystoneMapping`:
```java
package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.Mapping;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("mapping")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneMapping implements Mapping {

    private static final long serialVersionUID = 1L;

    private String id;
    private List<Map<String, Object>> rules;
    @JsonProperty("schema_version") private String schemaVersion;

    @Override public String getId() { return id; }
    @Override public List<Map<String, Object>> getRules() { return rules; }
    @Override public String getSchemaVersion() { return schemaVersion; }

    public static class Mappings extends ListResult<KeystoneMapping> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("mappings")
        private List<KeystoneMapping> list;

        @Override
        protected List<KeystoneMapping> value() {
            return list;
        }
    }
}
```

- [ ] **Step 4: 서비스 구현**

`openstack/identity/v3/internal/IdentityResponses.java`:
```java
package org.openstack4j.openstack.identity.v3.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.core.transport.HttpResponse;

/** Reads Keystone responses that are not JSON (SAML XML, OAuth1 form bodies). */
final class IdentityResponses {

    private IdentityResponses() {
    }

    static String text(HttpResponse response) {
        try (HttpResponse r = response) {
            if (r.getStatus() >= 400)
                throw new ClientResponseException(r.getStatusMessage() + " (" + r.getStatus() + ")", r.getStatus());
            try (InputStream in = r.getInputStream()) {
                return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new ClientResponseException(e.getMessage(), 0, e);
        }
    }
}
```
`IdentityProviderServiceImpl`:
```java
package org.openstack4j.openstack.identity.v3.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.identity.v3.IdentityProviderService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.FederationProtocol;
import org.openstack4j.model.identity.v3.IdentityProvider;
import org.openstack4j.openstack.identity.v3.domain.KeystoneFederationProtocol;
import org.openstack4j.openstack.identity.v3.domain.KeystoneFederationProtocol.Protocols;
import org.openstack4j.openstack.identity.v3.domain.KeystoneIdentityProvider;
import org.openstack4j.openstack.identity.v3.domain.KeystoneIdentityProvider.IdentityProviders;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class IdentityProviderServiceImpl extends BaseIdentityServices implements IdentityProviderService {

    private static final String IDPS = "/OS-FEDERATION/identity_providers";

    private static String idp(String id) {
        return IDPS + "/" + Objects.requireNonNull(id);
    }

    private static String protocol(String idpId, String protocolId) {
        return idp(idpId) + "/protocols/" + Objects.requireNonNull(protocolId);
    }

    @Override public List<? extends IdentityProvider> list() { return get(IdentityProviders.class, IDPS).execute().getList(); }
    @Override public IdentityProvider get(String id) { return get(KeystoneIdentityProvider.class, idp(id)).execute(); }

    @Override
    public IdentityProvider create(String id, Map<String, Object> attributes) {
        return put(KeystoneIdentityProvider.class, idp(id)).entity(JsonBody.of("identity_provider", attributes == null ? Collections.emptyMap() : attributes)).execute();
    }

    @Override
    public IdentityProvider update(String id, Map<String, Object> attributes) {
        return patch(KeystoneIdentityProvider.class, idp(id)).entity(JsonBody.of("identity_provider", Objects.requireNonNull(attributes))).execute();
    }

    @Override public ActionResponse delete(String id) { return deleteWithResponse(idp(id)).execute(); }
    @Override public List<? extends FederationProtocol> protocols(String idpId) { return get(Protocols.class, idp(idpId) + "/protocols").execute().getList(); }
    @Override public FederationProtocol getProtocol(String idpId, String protocolId) { return get(KeystoneFederationProtocol.class, protocol(idpId, protocolId)).execute(); }

    @Override
    public FederationProtocol createProtocol(String idpId, String protocolId, String mappingId) {
        return put(KeystoneFederationProtocol.class, protocol(idpId, protocolId))
                .entity(JsonBody.of("protocol", Collections.singletonMap("mapping_id", Objects.requireNonNull(mappingId)))).execute();
    }

    @Override
    public FederationProtocol updateProtocol(String idpId, String protocolId, String mappingId) {
        return patch(KeystoneFederationProtocol.class, protocol(idpId, protocolId))
                .entity(JsonBody.of("protocol", Collections.singletonMap("mapping_id", Objects.requireNonNull(mappingId)))).execute();
    }

    @Override public ActionResponse deleteProtocol(String idpId, String protocolId) { return deleteWithResponse(protocol(idpId, protocolId)).execute(); }
}
```
`MappingServiceImpl`(`/OS-FEDERATION/mappings`, 본문 `{"mapping": {"rules": [...]}}`)와 `ServiceProviderServiceImpl`(`/OS-FEDERATION/service_providers`, 루트 `service_provider`)은 같은 모양이다.

`FederationServiceImpl`:
```java
package org.openstack4j.openstack.identity.v3.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.Apis;
import org.openstack4j.api.identity.v3.FederationService;
import org.openstack4j.api.identity.v3.IdentityProviderService;
import org.openstack4j.api.identity.v3.MappingService;
import org.openstack4j.api.identity.v3.ServiceProviderService;
import org.openstack4j.core.transport.ClientConstants;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.model.identity.v3.Domain;
import org.openstack4j.model.identity.v3.Project;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.openstack.identity.v3.domain.KeystoneDomain.Domains;
import org.openstack4j.openstack.identity.v3.domain.KeystoneProject.Projects;
import org.openstack4j.openstack.identity.v3.domain.KeystoneToken;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class FederationServiceImpl extends BaseIdentityServices implements FederationService {

    @Override public IdentityProviderService identityProviders() { return Apis.get(IdentityProviderService.class); }
    @Override public MappingService mappings() { return Apis.get(MappingService.class); }
    @Override public ServiceProviderService serviceProviders() { return Apis.get(ServiceProviderService.class); }
    @Override public List<? extends Project> projects() { return get(Projects.class, "/OS-FEDERATION/projects").execute().getList(); }
    @Override public List<? extends Domain> domains() { return get(Domains.class, "/OS-FEDERATION/domains").execute().getList(); }

    @Override
    public String saml2Metadata() {
        return IdentityResponses.text(get(Void.class, "/OS-FEDERATION/saml2/metadata").executeWithResponse());
    }

    @Override
    public String saml2Assertion(String tokenId, String serviceProviderId) {
        return IdentityResponses.text(post(Void.class, "/auth/OS-FEDERATION/saml2").entity(assertionRequest(tokenId, serviceProviderId)).executeWithResponse());
    }

    @Override
    public String ecpAssertion(String tokenId, String serviceProviderId) {
        return IdentityResponses.text(post(Void.class, "/auth/OS-FEDERATION/saml2/ecp").entity(assertionRequest(tokenId, serviceProviderId)).executeWithResponse());
    }

    @Override
    public Token federatedToken(String idpId, String protocolId, Map<String, String> headers) {
        HttpResponse response = get(Void.class, "/OS-FEDERATION/identity_providers/", Objects.requireNonNull(idpId), "/protocols/",
                Objects.requireNonNull(protocolId), "/auth").headers(headers == null ? Map.of() : headers).executeWithResponse();
        KeystoneToken token = response.getEntity(KeystoneToken.class);
        if (token != null)
            token.setId(response.header(ClientConstants.HEADER_X_SUBJECT_TOKEN));
        return token;
    }

    /** {@code {"auth": {"identity": {"methods": ["token"], "token": {"id": ...}}, "scope": {"service_provider": {"id": ...}}}}} */
    private static JsonBody assertionRequest(String tokenId, String serviceProviderId) {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("methods", List.of("token"));
        identity.put("token", Map.of("id", Objects.requireNonNull(tokenId)));
        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("identity", identity);
        auth.put("scope", Map.of("service_provider", Map.of("id", Objects.requireNonNull(serviceProviderId))));
        return JsonBody.of("auth", auth);
    }
}
```
(`federatedToken` 의 `getEntity` 가 오류 상태에서 예외를 던지는지 `saml2ErrorIsRaised` 와 같은 방식으로 확인 — 기존 `HttpEntityHandler` 가 처리한다. `KeystoneToken` 은 `@JsonRootName("token")` 이다.)

API 인터페이스 4개(Javadoc: 경로, 관리자 전용, 키 목록), `IdentityService.federation()`, binding 4개(`FederationService`, `IdentityProviderService`, `MappingService`, `ServiceProviderService`).

- [ ] **Step 5: 통과 확인 (세 connector — 문자열 응답)**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && for c in httpclient okhttp http-connector; do ./mvnw -B test -pl connectors/$c -Dsurefire.failIfNoSpecifiedTests=false -Dtest='FederationTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:' | tail -1; done`
Expected: 세 connector 모두 `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/e11.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/e11.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/e11.log
git add -A && git commit -m "feat(identity): add OS-FEDERATION identity providers, protocols, mappings, service providers and SAML

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "federation(): IdP·protocol·mapping·service provider CRUD, federated projects/domains, SAML2 metadata·assertion·ECP(XML 문자열), federated token."
```

---

### Task 12: OS-OAUTH1(서명), OS-OAUTH2, OS-REVOKE

**Files:**
- Create (`core/src/main/java/org/openstack4j/`):
  - 서명: `openstack/identity/v3/internal/OAuth1Signer.java`
  - 모델: `model/identity/v3/OAuth1Consumer.java`, `OAuth1Token.java`, `OAuth1AccessToken.java`, `OAuth2AccessToken.java`, `RevocationEvent.java`
  - 도메인: `openstack/identity/v3/domain/KeystoneOAuth1Consumer.java`, `KeystoneOAuth1Token.java`(폼 응답 파싱), `KeystoneOAuth1AccessToken.java`, `KeystoneOAuth2AccessToken.java`, `KeystoneRevocationEvent.java`
  - API·구현: `api/identity/v3/OAuth1Service.java`, `OAuth2Service.java`, `RevocationEventService.java`, `openstack/identity/v3/internal/OAuth1ServiceImpl.java`, `OAuth2ServiceImpl.java`, `RevocationEventServiceImpl.java`
- Modify: `IdentityService.java`, `IdentityServiceImpl.java`, `DefaultAPIProvider.java`
- Create: `core-test/src/main/java/org/openstack4j/api/identity/v3/ext/OAuthTests.java`

**Interfaces:**
- Consumes: Task 11 `IdentityResponses.text(HttpResponse)`.
- Produces:
  - `OAuth1Signer`(public final, 내부 패키지): `static String signature(String method, String url, Map<String, String> oauthParams, String consumerSecret, String tokenSecret)`(HMAC-SHA1, RFC 5849 3.4), `static String header(Map<String, String> oauthParams, String signature)` → `OAuth k="v", ..., oauth_signature="..."`, `static String encode(String)`(RFC 3986)
  - `OAuth1Consumer`: `getId()`, `getDescription()`, `getSecret()`(생성 응답에만)
  - `OAuth1Token`: `getKey()`, `getSecret()`, `String getExpiresAt()`; `KeystoneOAuth1Token.parse(String formBody)`
  - `OAuth1AccessToken`: `getId()`, `getConsumerId()`, `getProjectId()`, `getAuthorizingUserId()`, `String getExpiresAt()`
  - `OAuth2AccessToken`: `getAccessToken()`, `getTokenType()`, `Integer getExpiresIn()`
  - `RevocationEvent`: `getUserId()`, `getProjectId()`, `getDomainId()`, `getAuditId()`, `getAuditChainId()`, `getRoleId()`, `getTrustId()`, `getConsumerId()`, `getAccessTokenId()`, `getExpiresAt()`, `Date getIssuedBefore()`, `Date getRevokedAt()`
  - `IdentityService.oauth1()` → `OAuth1Service`:
    - consumers: `listConsumers()`, `getConsumer(id)`, `createConsumer(String description)`, `updateConsumer(String id, String description)`, `deleteConsumer(id)`
    - 위임 흐름: `OAuth1Token requestToken(String consumerKey, String consumerSecret, String projectId)`, `String authorize(String requestTokenKey, List<String> roleIds)`(→ verifier), `OAuth1Token accessToken(String consumerKey, String consumerSecret, String requestTokenKey, String requestTokenSecret, String verifier)`
    - 사용자 access token: `listAccessTokens(userId)`, `getAccessToken(userId, tokenId)`, `deleteAccessToken(userId, tokenId)`, `List<? extends Role> accessTokenRoles(userId, tokenId)`, `Role getAccessTokenRole(userId, tokenId, roleId)`
  - `IdentityService.oauth2()` → `OAuth2Service.token(String clientId, String clientSecret)` → `OAuth2AccessToken`(`POST /OS-OAUTH2/token`, Basic 인증, `grant_type=client_credentials` 폼 본문)
  - `IdentityService.revocationEvents()` → `RevocationEventService`: `List<? extends RevocationEvent> list()`, `list(Date since)`(`?since=` ISO 8601 UTC)

- [ ] **Step 1: 실패하는 테스트 작성 (Review Focus 4, 5)**

```bash
git switch main && git pull && git switch -c task/e12-oauth-revoke
```

`OAuthTests.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.OAuth1Consumer;
import org.openstack4j.model.identity.v3.OAuth1Token;
import org.openstack4j.model.identity.v3.OAuth2AccessToken;
import org.openstack4j.model.identity.v3.RevocationEvent;
import org.openstack4j.openstack.identity.v3.internal.OAuth1Signer;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/OAuth")
public class OAuthTests extends AbstractIdentityExtTest {

    private static Map<String, String> baseParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("oauth_consumer_key", "7fea2d");
        params.put("oauth_nonce", "abc123");
        params.put("oauth_signature_method", "HMAC-SHA1");
        params.put("oauth_timestamp", "1700000000");
        params.put("oauth_version", "1.0");
        return params;
    }

    /** Vectors computed independently with Python hmac/hashlib (RFC 5849 base string). */
    public void signatureMatchesIndependentVector() {
        Map<String, String> request = baseParams();
        request.put("oauth_callback", "oob");
        Assert.assertEquals(OAuth1Signer.signature("POST", "http://127.0.0.1:5000/v3/OS-OAUTH1/request_token", request, "secret1", ""),
                "xd9vbzZ5lHemn1aq0L+A42gPi8w=");

        Map<String, String> access = baseParams();
        access.put("oauth_token", "reqkey");
        access.put("oauth_verifier", "8171");
        Assert.assertEquals(OAuth1Signer.signature("POST", "http://127.0.0.1:5000/v3/OS-OAUTH1/access_token", access, "secret1", "reqsecret"),
                "kPRcl1Abl9xTcBkQWXpKOtuqWuA=");
    }

    public void headerEncodesValues() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("oauth_consumer_key", "a b");
        String header = OAuth1Signer.header(params, "x+y/=");
        Assert.assertEquals(header, "OAuth oauth_consumer_key=\"a%20b\", oauth_signature=\"x%2By%2F%3D\"");
    }

    public void requestTokenParsesFormResponse() throws Exception {
        respondWith(java.util.Collections.singletonMap("Content-Type", "application/x-www-form-urlencoded"), 201,
                "oauth_token=29971f&oauth_token_secret=238eb8&oauth_expires_at=2026-10-03T12%3A00%3A00.000000Z");

        OAuth1Token token = osv3().identity().oauth1().requestToken("7fea2d", "secret1", PROJECT);

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "POST");
        Assert.assertTrue(request.getPath().endsWith("/v3/OS-OAUTH1/request_token"));
        Assert.assertEquals(request.getHeader("Requested-Project-Id"), PROJECT);
        String authorization = request.getHeader("Authorization");
        Assert.assertTrue(authorization.startsWith("OAuth "), authorization);
        Assert.assertTrue(authorization.contains("oauth_consumer_key=\"7fea2d\""), authorization);
        Assert.assertTrue(authorization.contains("oauth_callback=\"oob\""), authorization);
        Assert.assertTrue(authorization.contains("oauth_signature=\""), authorization);
        Assert.assertEquals(token.getKey(), "29971f");
        Assert.assertEquals(token.getSecret(), "238eb8");
        Assert.assertEquals(token.getExpiresAt(), "2026-10-03T12:00:00.000000Z");
    }

    public void authorizeAndAccessToken() throws Exception {
        respondWith(200, "{\"token\": {\"oauth_verifier\": \"8171\"}}");
        respondWith(java.util.Collections.singletonMap("Content-Type", "application/x-www-form-urlencoded"), 201,
                "oauth_token=accesskey&oauth_token_secret=accesssecret&oauth_expires_at=");

        String verifier = osv3().identity().oauth1().authorize("29971f", List.of("r1"));
        OAuth1Token access = osv3().identity().oauth1().accessToken("7fea2d", "secret1", "29971f", "238eb8", verifier);

        RecordedRequest authorize = takeRequest();
        Assert.assertEquals(authorize.getMethod(), "PUT");
        Assert.assertTrue(authorize.getPath().endsWith("/v3/OS-OAUTH1/authorize/29971f"));
        Assert.assertEquals(body(authorize).get("roles").get(0).get("id").asText(), "r1");
        RecordedRequest token = takeRequest();
        Assert.assertTrue(token.getPath().endsWith("/v3/OS-OAUTH1/access_token"));
        String authorization = token.getHeader("Authorization");
        Assert.assertTrue(authorization.contains("oauth_token=\"29971f\"") && authorization.contains("oauth_verifier=\"8171\""), authorization);
        Assert.assertEquals(verifier, "8171");
        Assert.assertEquals(access.getKey(), "accesskey");
        Assert.assertEquals(access.getSecret(), "accesssecret");
    }

    public void consumersAndUserAccessTokens() throws Exception {
        String consumer = "{\"id\": \"9c467b\", \"description\": \"os4j fixture\", \"secret\": \"2fc164\", \"links\": {}}";
        String accessToken = "{\"id\": \"at1\", \"consumer_id\": \"9c467b\", \"project_id\": \"" + PROJECT + "\", \"authorizing_user_id\": \"" + USER + "\", \"expires_at\": null, \"links\": {}}";
        String role = "{\"id\": \"r1\", \"name\": \"member\", \"links\": {}}";
        respondWith(201, "{\"consumer\": " + consumer + "}");
        respondWith(200, "{\"consumers\": [" + consumer + "], \"links\": {}}");
        respondWith(200, "{\"consumer\": " + consumer + "}");
        respondWith(200, "{\"consumer\": " + consumer + "}");
        respondWith(204);
        respondWith(200, "{\"access_tokens\": [" + accessToken + "], \"links\": {}}");
        respondWith(200, "{\"access_token\": " + accessToken + "}");
        respondWith(200, "{\"roles\": [" + role + "], \"links\": {}}");
        respondWith(200, "{\"role\": " + role + "}");
        respondWith(204);

        var oauth1 = osv3().identity().oauth1();
        OAuth1Consumer created = oauth1.createConsumer("os4j fixture");
        Assert.assertEquals(oauth1.listConsumers().size(), 1);
        oauth1.getConsumer("9c467b");
        oauth1.updateConsumer("9c467b", "changed");
        oauth1.deleteConsumer("9c467b");
        Assert.assertEquals(oauth1.listAccessTokens(USER).get(0).getConsumerId(), "9c467b");
        Assert.assertEquals(oauth1.getAccessToken(USER, "at1").getAuthorizingUserId(), USER);
        Assert.assertEquals(oauth1.accessTokenRoles(USER, "at1").get(0).getName(), "member");
        Assert.assertEquals(oauth1.getAccessTokenRole(USER, "at1", "r1").getId(), "r1");
        oauth1.deleteAccessToken(USER, "at1");

        RecordedRequest create = takeRequest();
        Assert.assertTrue(create.getPath().endsWith("/v3/OS-OAUTH1/consumers"));
        Assert.assertEquals(body(create).get("consumer").get("description").asText(), "os4j fixture");
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertTrue(takeRequest().getPath().endsWith("/consumers/9c467b"));
        Assert.assertEquals(body(takeRequest()).get("consumer").get("description").asText(), "changed");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        String tokens = "/v3/users/" + USER + "/OS-OAUTH1/access_tokens";
        Assert.assertTrue(takeRequest().getPath().endsWith(tokens));
        Assert.assertTrue(takeRequest().getPath().endsWith(tokens + "/at1"));
        Assert.assertTrue(takeRequest().getPath().endsWith(tokens + "/at1/roles"));
        Assert.assertTrue(takeRequest().getPath().endsWith(tokens + "/at1/roles/r1"));
        RecordedRequest delete = takeRequest();
        Assert.assertEquals(delete.getMethod(), "DELETE");
        Assert.assertTrue(delete.getPath().endsWith(tokens + "/at1"));
        Assert.assertEquals(created.getSecret(), "2fc164");
    }

    public void oauth2ClientCredentials() throws Exception {
        respondWith(200, "{\"access_token\": \"tok2\", \"token_type\": \"Bearer\", \"expires_in\": 3600}");

        OAuth2AccessToken token = osv3().identity().oauth2().token("client1", "s3cret");

        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/v3/OS-OAUTH2/token"));
        Assert.assertEquals(request.getHeader("Authorization"),
                "Basic " + Base64.getEncoder().encodeToString("client1:s3cret".getBytes(StandardCharsets.UTF_8)));
        Assert.assertTrue(request.getHeader("Content-Type").startsWith("application/x-www-form-urlencoded"), request.getHeader("Content-Type"));
        Assert.assertEquals(request.getBody().readUtf8(), "grant_type=client_credentials");
        Assert.assertEquals(token.getAccessToken(), "tok2");
        Assert.assertEquals(token.getTokenType(), "Bearer");
        Assert.assertEquals(token.getExpiresIn(), Integer.valueOf(3600));
    }

    public void revocationEvents() throws Exception {
        String events = "{\"events\": [{\"project_id\": \"ed6351\", \"issued_before\": \"2026-10-02T00:07:59.000000Z\", \"revoked_at\": \"2026-10-02T00:07:59.000000Z\"},"
                + " {\"audit_id\": \"7OpmWuy8QQKA_K38Sio9sQ\", \"issued_before\": \"2026-10-02T03:43:48.000000Z\", \"revoked_at\": \"2026-10-02T03:43:48.000000Z\"}], \"links\": {}}";
        respondWith(200, events);
        respondWith(200, "{\"events\": [], \"links\": {}}");

        List<? extends RevocationEvent> all = osv3().identity().revocationEvents().list();
        osv3().identity().revocationEvents().list(new Date(1759363200000L));

        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-REVOKE/events"));
        Assert.assertTrue(decodedPath(takeRequest()).endsWith("/v3/OS-REVOKE/events?since=2025-10-02T00:00:00Z"));
        Assert.assertEquals(all.get(0).getProjectId(), "ed6351");
        Assert.assertEquals(all.get(1).getAuditId(), "7OpmWuy8QQKA_K38Sio9sQ");
        Assert.assertNotNull(all.get(1).getRevokedAt());
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -3`
Expected: `OAuth1Signer`, `oauth1()` 등 없음.

- [ ] **Step 3: 서명 구현**

`openstack/identity/v3/internal/OAuth1Signer.java`:
```java
package org.openstack4j.openstack.identity.v3.internal;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** OAuth 1.0a HMAC-SHA1 signing (RFC 5849) for Keystone OS-OAUTH1. */
public final class OAuth1Signer {

    private OAuth1Signer() {
    }

    /** Percent-encodes per RFC 3986: unreserved characters stay, everything else is %XX (upper case). */
    public static String encode(String value) {
        StringBuilder out = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '.' || c == '_' || c == '~')
                out.append(c);
            else
                out.append('%').append(String.format("%02X", b & 0xFF));
        }
        return out.toString();
    }

    /** Scheme and host lower case, default port dropped, no query or fragment (RFC 5849 3.4.1.2). */
    static String baseUrl(String url) {
        URI uri = URI.create(url);
        String scheme = uri.getScheme().toLowerCase();
        int port = uri.getPort();
        boolean defaultPort = port == -1 || ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + uri.getHost().toLowerCase() + (defaultPort ? "" : ":" + port) + uri.getRawPath();
    }

    public static String signature(String method, String url, Map<String, String> oauthParams, String consumerSecret, String tokenSecret) {
        StringJoiner params = new StringJoiner("&");
        Map<String, String> sorted = new TreeMap<>();
        oauthParams.forEach((k, v) -> sorted.put(encode(k), encode(v)));
        sorted.forEach((k, v) -> params.add(k + "=" + v));
        String base = method.toUpperCase() + "&" + encode(baseUrl(url)) + "&" + encode(params.toString());
        String key = encode(consumerSecret) + "&" + encode(tokenSecret == null ? "" : tokenSecret);
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(base.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 is not available", e);
        }
    }

    public static String header(Map<String, String> oauthParams, String signature) {
        StringJoiner header = new StringJoiner(", ", "OAuth ", "");
        oauthParams.forEach((k, v) -> header.add(encode(k) + "=\"" + encode(v) + "\""));
        header.add("oauth_signature=\"" + encode(signature) + "\"");
        return header.toString();
    }
}
```
(같은 이름의 query 파라미터가 여러 개인 경우는 Keystone OAuth1 경로에 없으므로 다루지 않는다.)

- [ ] **Step 4: 모델 구현**

인터페이스 5개는 Interfaces 의 getter 를 선언한다. 도메인 클래스는 Task 3 의 `KeystoneAccessRule` 과 같은 모양이다:

| 클래스 | 루트 / 목록 키 | 필드(JSON) |
|---|---|---|
| `KeystoneOAuth1Consumer` | `consumer` / `consumers`(`Consumers`) | `id`, `description`, `secret` |
| `KeystoneOAuth1AccessToken` | `access_token` / `access_tokens`(`AccessTokens`) | `id`, `consumer_id`, `project_id`, `authorizing_user_id`, `expires_at`(String) |
| `KeystoneOAuth2AccessToken` | 루트 없음 | `access_token`, `token_type`, `expires_in`(Integer) |
| `KeystoneRevocationEvent` | 목록 `events`(`RevocationEvents`) | `user_id`, `project_id`, `domain_id`, `audit_id`, `audit_chain_id`, `role_id`, `trust_id`, `consumer_id`, `access_token_id`, `expires_at`(String), `issued_before`(Date), `revoked_at`(Date) |

`KeystoneOAuth2AccessToken` 은 루트 래핑이 없으므로 `@JsonRootName` 을 붙이지 않는다 — 전역 mapper 가 `UNWRAP_ROOT_VALUE` 를 루트 이름 있는 클래스에만 적용하는지 테스트(`oauth2ClientCredentials`)로 확인하고, 래핑 해제가 걸리면 `JsonNode` 로 받아 직접 읽는다(Ruling 기록).

`KeystoneOAuth1Token`:
```java
package org.openstack4j.openstack.identity.v3.domain;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.openstack4j.model.identity.v3.OAuth1Token;

/** An OAuth1 request or access token from Keystone's form-encoded response. */
public class KeystoneOAuth1Token implements OAuth1Token {

    private static final long serialVersionUID = 1L;

    private String key;
    private String secret;
    private String expiresAt;

    /** Parses {@code oauth_token=..&oauth_token_secret=..&oauth_expires_at=..}. */
    public static KeystoneOAuth1Token parse(String formBody) {
        KeystoneOAuth1Token token = new KeystoneOAuth1Token();
        for (String pair : formBody.trim().split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0)
                continue;
            String name = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            switch (name) {
                case "oauth_token": token.key = value; break;
                case "oauth_token_secret": token.secret = value; break;
                case "oauth_expires_at": token.expiresAt = value.isEmpty() ? null : value; break;
                default: break;
            }
        }
        return token;
    }

    @Override public String getKey() { return key; }
    @Override public String getSecret() { return secret; }
    @Override public String getExpiresAt() { return expiresAt; }
}
```
(`authorizeAndAccessToken` 의 `oauth_expires_at=` 빈 값 → null.)

- [ ] **Step 5: 서비스 구현**

`OAuth1ServiceImpl`:
```java
package org.openstack4j.openstack.identity.v3.internal;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openstack4j.api.identity.v3.OAuth1Service;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.OAuth1AccessToken;
import org.openstack4j.model.identity.v3.OAuth1Consumer;
import org.openstack4j.model.identity.v3.OAuth1Token;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.openstack.identity.v3.domain.KeystoneOAuth1AccessToken;
import org.openstack4j.openstack.identity.v3.domain.KeystoneOAuth1AccessToken.AccessTokens;
import org.openstack4j.openstack.identity.v3.domain.KeystoneOAuth1Consumer;
import org.openstack4j.openstack.identity.v3.domain.KeystoneOAuth1Consumer.Consumers;
import org.openstack4j.openstack.identity.v3.domain.KeystoneOAuth1Token;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRole;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRole.Roles;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class OAuth1ServiceImpl extends BaseIdentityServices implements OAuth1Service {

    private static final String CONSUMERS = "/OS-OAUTH1/consumers";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override public List<? extends OAuth1Consumer> listConsumers() { return get(Consumers.class, CONSUMERS).execute().getList(); }
    @Override public OAuth1Consumer getConsumer(String id) { return get(KeystoneOAuth1Consumer.class, CONSUMERS, "/", Objects.requireNonNull(id)).execute(); }

    @Override
    public OAuth1Consumer createConsumer(String description) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (description != null) fields.put("description", description);
        return post(KeystoneOAuth1Consumer.class, CONSUMERS).entity(JsonBody.of("consumer", fields)).execute();
    }

    @Override
    public OAuth1Consumer updateConsumer(String id, String description) {
        return patch(KeystoneOAuth1Consumer.class, CONSUMERS, "/", Objects.requireNonNull(id))
                .entity(JsonBody.of("consumer", Collections.singletonMap("description", description))).execute();
    }

    @Override public ActionResponse deleteConsumer(String id) { return deleteWithResponse(CONSUMERS, "/", Objects.requireNonNull(id)).execute(); }

    private static Map<String, String> oauthParams(String consumerKey) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("oauth_consumer_key", Objects.requireNonNull(consumerKey));
        params.put("oauth_nonce", Long.toHexString(RANDOM.nextLong()) + Long.toHexString(RANDOM.nextLong()));
        params.put("oauth_signature_method", "HMAC-SHA1");
        params.put("oauth_timestamp", Long.toString(System.currentTimeMillis() / 1000));
        params.put("oauth_version", "1.0");
        return params;
    }

    /** Signs against the exact URL the request goes to, then sends it with an empty body. */
    private OAuth1Token signedPost(String path, Map<String, String> params, String consumerSecret, String tokenSecret, String projectId) {
        BaseOpenStackService.Invocation<Void> invocation = post(Void.class, path);
        String url = invocation.getRequest().getUrl();
        invocation.header("Authorization", OAuth1Signer.header(params, OAuth1Signer.signature("POST", url, params, consumerSecret, tokenSecret)));
        if (projectId != null)
            invocation.header("Requested-Project-Id", projectId);
        return KeystoneOAuth1Token.parse(IdentityResponses.text(invocation.executeWithResponse()));
    }

    @Override
    public OAuth1Token requestToken(String consumerKey, String consumerSecret, String projectId) {
        Map<String, String> params = oauthParams(consumerKey);
        params.put("oauth_callback", "oob");
        return signedPost("/OS-OAUTH1/request_token", params, Objects.requireNonNull(consumerSecret), "", Objects.requireNonNull(projectId));
    }

    @Override
    @SuppressWarnings("unchecked")
    public String authorize(String requestTokenKey, List<String> roleIds) {
        List<Map<String, String>> roles = roleIds.stream().map(id -> Collections.singletonMap("id", id)).collect(Collectors.toList());
        Map<String, Object> response = put(Map.class, "/OS-OAUTH1/authorize/", Objects.requireNonNull(requestTokenKey))
                .entity(JsonBody.of(Collections.singletonMap("roles", roles))).execute();
        Object token = response == null ? null : response.get("token");
        return token instanceof Map ? (String) ((Map<String, Object>) token).get("oauth_verifier") : null;
    }

    @Override
    public OAuth1Token accessToken(String consumerKey, String consumerSecret, String requestTokenKey, String requestTokenSecret, String verifier) {
        Map<String, String> params = oauthParams(consumerKey);
        params.put("oauth_token", Objects.requireNonNull(requestTokenKey));
        params.put("oauth_verifier", Objects.requireNonNull(verifier));
        return signedPost("/OS-OAUTH1/access_token", params, Objects.requireNonNull(consumerSecret), Objects.requireNonNull(requestTokenSecret), null);
    }

    private static String tokens(String userId) {
        return "/users/" + Objects.requireNonNull(userId) + "/OS-OAUTH1/access_tokens";
    }

    @Override public List<? extends OAuth1AccessToken> listAccessTokens(String userId) { return get(AccessTokens.class, tokens(userId)).execute().getList(); }
    @Override public OAuth1AccessToken getAccessToken(String userId, String tokenId) { return get(KeystoneOAuth1AccessToken.class, tokens(userId), "/", Objects.requireNonNull(tokenId)).execute(); }
    @Override public ActionResponse deleteAccessToken(String userId, String tokenId) { return deleteWithResponse(tokens(userId), "/", Objects.requireNonNull(tokenId)).execute(); }
    @Override public List<? extends Role> accessTokenRoles(String userId, String tokenId) { return get(Roles.class, tokens(userId), "/", Objects.requireNonNull(tokenId), "/roles").execute().getList(); }
    @Override public Role getAccessTokenRole(String userId, String tokenId, String roleId) { return get(KeystoneRole.class, tokens(userId), "/", Objects.requireNonNull(tokenId), "/roles/", Objects.requireNonNull(roleId)).execute(); }
}
```
(`Invocation` 은 `BaseOpenStackService` 의 protected 중첩 클래스다 — 하위 클래스 안이므로 접근 가능하다. `getRequest().getUrl()` 이 endpoint 함수까지 적용된 최종 URL 인지 `requestTokenParsesFormResponse` 의 서명 재계산으로 확인하려면 테스트에서 nonce 를 알 수 없으므로, 대신 테스트가 `oauth_signature` 존재만 보고 서명 정확성은 `signatureMatchesIndependentVector` 와 Task 13 실환경 테스트가 맡는다.)

`OAuth2ServiceImpl`:
```java
package org.openstack4j.openstack.identity.v3.internal;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

import org.openstack4j.api.identity.v3.OAuth2Service;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.identity.v3.OAuth2AccessToken;
import org.openstack4j.openstack.identity.v3.domain.KeystoneOAuth2AccessToken;

public class OAuth2ServiceImpl extends BaseIdentityServices implements OAuth2Service {

    @Override
    public OAuth2AccessToken token(String clientId, String clientSecret) {
        String basic = Base64.getEncoder().encodeToString((Objects.requireNonNull(clientId) + ":" + Objects.requireNonNull(clientSecret))
                .getBytes(StandardCharsets.UTF_8));
        byte[] form = "grant_type=client_credentials".getBytes(StandardCharsets.UTF_8);
        return post(KeystoneOAuth2AccessToken.class, "/OS-OAUTH2/token")
                .header("Authorization", "Basic " + basic)
                .contentType("application/x-www-form-urlencoded")
                .entity(Payloads.create(new ByteArrayInputStream(form)))
                .execute();
    }
}
```
(InputStream 엔티티는 세 connector 모두 `request.getContentType()` 으로 보낸다. 세션 토큰 헤더(`X-Auth-Token`)가 함께 가도 Keystone 은 Basic 인증으로 판단한다.)

`RevocationEventServiceImpl`:
```java
    @Override
    public List<? extends RevocationEvent> list() {
        return get(RevocationEvents.class, "/OS-REVOKE/events").execute().getList();
    }

    @Override
    public List<? extends RevocationEvent> list(Date since) {
        return get(RevocationEvents.class, "/OS-REVOKE/events")
                .param("since", Objects.requireNonNull(since).toInstant().truncatedTo(ChronoUnit.SECONDS).toString()).execute().getList();
    }
```
API 인터페이스 3개(Javadoc: OAuth1 흐름 순서 — consumer → requestToken → authorize(사용자) → accessToken; 서명은 HMAC-SHA1), accessor 3개, binding 3개.

- [ ] **Step 6: 통과 확인 (세 connector — 폼 본문·폼 응답)**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && for c in httpclient okhttp http-connector; do ./mvnw -B test -pl connectors/$c -Dsurefire.failIfNoSpecifiedTests=false -Dtest='OAuthTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:' | tail -1; done`
Expected: 세 connector 모두 `Tests run: 7, Failures: 0, Errors: 0`

- [ ] **Step 7: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/e12.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/e12.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/e12.log
git add -A && git commit -m "feat(identity): add OS-OAUTH1 with HMAC-SHA1 signing, OS-OAUTH2 client credentials and OS-REVOKE events

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "oauth1(): consumer CRUD, request token/authorize/access token(RFC 5849 서명, 독립 벡터 검증), 사용자 access token 조회·삭제·역할. oauth2().token(). revocationEvents().list([since])."
```

---

### Task 13: 실환경 통합 테스트와 문서

**Files:**
- Create: `core-test/src/main/java/org/openstack4j/api/identity/v3/ext/IdentityExtensionsLiveTests.java`
- Modify: `README.md`(identity 절), `MIGRATION.md`, `CHANGELOG.md`

**Interfaces:**
- Consumes: Task 1~12 의 공개 API 전부.
- 환경 변수(`BlockStorageLiveTests` 와 같은 이름): `OS_AUTH_URL`, `OS_USERNAME`, `OS_PASSWORD`, `OS_PROJECT_NAME`, `OS_USER_DOMAIN_NAME`·`OS_PROJECT_DOMAIN_NAME`(기본 `Default`). `OS_AUTH_URL` 이 없으면 `SkipException`. 비밀번호는 저장하지 않고 실행할 때만 넘긴다.

- [ ] **Step 1: live test 의 기존 모양 확인**

Run: `sed -n 27,56p core-test/src/main/java/org/openstack4j/api/storage/microversion/BlockStorageLiveTests.java`
Expected: `OS_AUTH_URL` 없으면 skip, `env(name, fallback)` helper, `groups = "block-storage-live"`. 아래 테스트도 같은 방식(`groups = "identity-live"`)을 쓴다.

- [ ] **Step 2: 실환경 테스트 작성**

`IdentityExtensionsLiveTests.java`:
```java
package org.openstack4j.api.identity.v3.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.identity.v3.ApplicationCredential;
import org.openstack4j.model.identity.v3.EndpointGroup;
import org.openstack4j.model.identity.v3.RegisteredLimit;
import org.openstack4j.model.identity.v3.Service;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.model.identity.v3.Trust;
import org.openstack4j.model.identity.v3.options.ApplicationCredentialCreate;
import org.openstack4j.model.identity.v3.options.RegisteredLimitCreate;
import org.openstack4j.model.identity.v3.options.TrustCreate;
import org.openstack4j.openstack.OSFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Runs against a real Keystone when OS_AUTH_URL is set; every temporary resource is removed in finally. */
@Test(suiteName = "Identity/V3/Live", groups = "identity-live", singleThreaded = true)
public class IdentityExtensionsLiveTests {

    private String authUrl;
    private String user;
    private String password;
    private String project;
    private String domain;
    private OSClientV3 os;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live identity tests");
        authUrl = url.replaceAll("/+$", "").endsWith("/v3") ? url.replaceAll("/+$", "") : url.replaceAll("/+$", "") + "/v3";
        user = env("OS_USERNAME", null);
        password = env("OS_PASSWORD", null);
        project = env("OS_PROJECT_NAME", null);
        domain = env("OS_USER_DOMAIN_NAME", "Default");
        os = OSFactory.builderV3().endpoint(authUrl).credentials(user, password, Identifier.byName(domain))
                .scopeToProject(Identifier.byName(project), Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default"))).authenticate();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live identity tests");
        }
        return value;
    }

    private OSClientV3 session() {
        return OSFactory.clientFromToken(os.getToken());
    }

    public void applicationCredentialAuthenticatesAndCallsApi() {
        OSClientV3 client = session();
        String userId = client.getToken().getUser().getId();
        ApplicationCredential credential = client.identity().applicationCredentials().create(userId,
                ApplicationCredentialCreate.create("os4j-live-" + System.nanoTime()).description("openstack4j live test"));
        final String credentialId = credential.getId();
        try {
            Assert.assertNotNull(credential.getSecret());
            OSClientV3 viaCredential = OSFactory.builderV3().endpoint(authUrl).applicationCredential(credentialId, credential.getSecret()).authenticate();
            Assert.assertEquals(viaCredential.getToken().getApplicationCredential().get("id"), credentialId);
            Assert.assertNotNull(viaCredential.getToken().getProject());
            Assert.assertFalse(viaCredential.identity().serviceEndpoints().list().isEmpty());
        } finally {
            client.identity().applicationCredentials().delete(userId, credentialId);
        }
    }

    public void systemScope() {
        OSClientV3 system = OSFactory.builderV3().endpoint(authUrl).credentials(user, password, Identifier.byName(domain)).scopeToSystem().authenticate();
        Assert.assertEquals(system.getToken().getSystem().get("all"), Boolean.TRUE);
        Assert.assertFalse(session().identity().tokens().getSystemScopes(os.getToken().getId()).isEmpty());
    }

    public void projectTags() {
        OSClientV3 client = session();
        String projectId = client.getToken().getProject().getId();
        String tag = "os4j live " + System.nanoTime();
        try {
            Assert.assertTrue(client.identity().projects().addTag(projectId, tag).isSuccess());
            Assert.assertTrue(client.identity().projects().tags(projectId).contains(tag));
            Assert.assertTrue(client.identity().projects().hasTag(projectId, tag).isSuccess());
        } finally {
            client.identity().projects().removeTag(projectId, tag);
        }
        Assert.assertFalse(client.identity().projects().hasTag(projectId, tag).isSuccess());
    }

    public void endpointGroup() {
        OSClientV3 client = session();
        EndpointGroup group = client.identity().endpointFilter().createEndpointGroup("os4j-live", "openstack4j live test", Map.of("interface", "public"));
        try {
            Assert.assertEquals(client.identity().endpointFilter().getEndpointGroup(group.getId()).getFilters().get("interface"), "public");
        } finally {
            client.identity().endpointFilter().deleteEndpointGroup(group.getId());
        }
    }

    public void registeredLimit() {
        OSClientV3 client = session();
        String computeId = client.identity().serviceEndpoints().list().stream().filter(s -> "compute".equals(s.getType()))
                .map(Service::getId).findFirst().orElseThrow(() -> new SkipException("no compute service"));
        List<? extends RegisteredLimit> created = client.identity().registeredLimits().create(List.of(
                RegisteredLimitCreate.create(computeId, "os4j_live_" + System.nanoTime(), 5).description("openstack4j live test")));
        try {
            Assert.assertEquals(client.identity().registeredLimits().get(created.get(0).getId()).getDefaultLimit(), Integer.valueOf(5));
        } finally {
            client.identity().registeredLimits().delete(created.get(0).getId());
        }
    }

    public void trustToSelf() {
        OSClientV3 client = session();
        Token token = client.getToken();
        Trust trust = client.identity().trusts().create(TrustCreate.create(token.getUser().getId(), token.getUser().getId(), false)
                .projectId(token.getProject().getId()).roleNames(token.getRoles().get(0).getName()));
        try {
            Assert.assertEquals(client.identity().trusts().get(trust.getId()).getTrusteeUserId(), token.getUser().getId());
            Assert.assertFalse(client.identity().trusts().roles(trust.getId()).isEmpty());
        } finally {
            client.identity().trusts().delete(trust.getId());
        }
    }

    public void federationObjects() {
        OSClientV3 client = session();
        var federation = client.identity().federation();
        String id = "os4j-live-" + System.nanoTime();
        federation.mappings().create(id, List.of(Map.of("local", List.of(Map.of("user", Map.of("name", "{0}"))), "remote", List.of(Map.of("type", "REMOTE_USER")))));
        try {
            federation.identityProviders().create(id, Map.of("enabled", false));
            try {
                federation.identityProviders().createProtocol(id, "saml2", id);
                Assert.assertEquals(federation.identityProviders().getProtocol(id, "saml2").getMappingId(), id);
                federation.identityProviders().deleteProtocol(id, "saml2");
            } finally {
                federation.identityProviders().delete(id);
            }
        } finally {
            federation.mappings().delete(id);
        }
    }

    public void oauth1Consumer() {
        OSClientV3 client = session();
        var consumer = client.identity().oauth1().createConsumer("openstack4j live test");
        try {
            Assert.assertNotNull(consumer.getSecret());
            Assert.assertEquals(client.identity().oauth1().getConsumer(consumer.getId()).getDescription(), "openstack4j live test");
        } finally {
            client.identity().oauth1().deleteConsumer(consumer.getId());
        }
    }

    public void readOnlyListings() {
        OSClientV3 client = session();
        Assert.assertNotNull(client.identity().roles().listRoleInferences());
        Assert.assertNotNull(client.identity().limits().model().getName());
        Assert.assertNotNull(client.identity().revocationEvents().list());
        Assert.assertFalse(client.identity().domains().defaultConfig().isEmpty());
    }
}
```
(`OSFactory.clientFromToken` 은 기존 API 다. `serviceEndpoints().list()` 의 반환형과 `Service.getType()` 이름은 기존 코드에서 확인해 맞춘다.)

- [ ] **Step 3: 실환경 실행**

Run(비밀번호는 셸 변수로만 넘긴다): `./mvnw -B -q install -DskipTests -pl core,core-test && OS_AUTH_URL=http://192.168.140.12:5000/v3 OS_USERNAME=admin OS_PASSWORD="$OS_PASSWORD" OS_PROJECT_NAME=admin ./mvnw -B test -pl connectors/okhttp -Dsurefire.failIfNoSpecifiedTests=false -Dtest='IdentityExtensionsLiveTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL|Skipped' | tail -3`
Expected: `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`. 이어서 `openstack application credential list`, `openstack registered limit list`, `openstack trust list`, `openstack mapping list`, `openstack identity provider list` 에 `os4j-live` 자원이 남지 않았는지 확인한다(curl 로 같은 API 조회도 된다).

- [ ] **Step 4: 문서**

`README.md` 의 identity 절에 추가(기존 절 형식 유지):
```markdown
#### Identity v3 extensions (4.4.0)

```java
// application credential (no password, no scope)
OSClientV3 os = OSFactory.builderV3()
        .endpoint("https://keystone:5000/v3")
        .applicationCredential(credentialId, credentialSecret)
        .authenticate();

// MFA: password + TOTP; system scope; trust scope
OSFactory.builderV3().endpoint(url).credentials(user, password, Identifier.byName("Default")).passcode("123456").authenticate();
OSFactory.builderV3().endpoint(url).credentials(user, password, Identifier.byName("Default")).scopeToSystem().authenticate();
OSFactory.builderV3().endpoint(url).token(tokenId).scopeToTrust(trustId).authenticate();

os.identity().applicationCredentials().create(userId, ApplicationCredentialCreate.create("ci").roleNames("member"));
os.identity().projects().addTag(projectId, "prod");
os.identity().trusts().create(TrustCreate.create(trustorId, trusteeId, false).projectId(projectId).roleNames("member"));
os.identity().registeredLimits().create(List.of(RegisteredLimitCreate.create(computeServiceId, "cores", 20)));
os.identity().federation().identityProviders().list();
```

New accessors: `applicationCredentials()`, `trusts()`, `endpointFilter()`, `endpointPolicies()`, `limits()`, `registeredLimits()`,
`federation()`, `oauth1()`, `oauth2()`, `revocationEvents()`, `systemRoles()`. Existing services gained project tags, OS-INHERIT,
implied roles, domain configuration, access rules, and list options for users and projects.
```
`MIGRATION.md` 에 4.3 → 4.4 절: 기존 API 변경 없음; TOTP(`passcode`) 세션은 토큰 만료 후 같은 passcode 로 재인증할 수 없으므로(일회용) 만료 전에 새로 인증해야 한다; application credential 인증에 scope 를 함께 주면 요청 전에 `IllegalStateException`; Keystone 이 410 을 돌려주는 OS-SIMPLE-CERT·OS-PKI 와 브라우저용 websso 두 경로는 지원하지 않는다. `CHANGELOG.md` 에 4.4.0 항목(추가된 accessor·인증 방식·메서드 수 152).

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/e13.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/e13.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/e13.log
git add -A && git commit -m "test(identity): add live Keystone extension tests; docs for identity extensions

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "IdentityExtensionsLiveTests(개발 Keystone 3.14 에서 9/9 통과, 임시 자원 없음), README identity 절, MIGRATION 4.3→4.4, CHANGELOG 4.4.0."
```

---

### Task 14: 전체 리뷰, 4.4.0 릴리스, 4.5.0-SNAPSHOT

- [ ] **Step 1: 최종 리뷰** — `review-package PLAN_FILE $(git merge-base <E 시작 커밋>^ HEAD) HEAD` 로 패키지를 만들고, opus 리뷰어에게 code-reviewer 템플릿, 이 계획서·스펙 경로, Review Focus 5개, ledger 의 `Ruling:` 줄을 준다. Critical/Important 는 한 번의 수정 패스(각각 RED→GREEN 테스트 + 전체 빌드)로 고치고 PR 로 머지한다. Minor 는 ledger 에 `Final: minor (deferred)` 로 남긴다.

- [ ] **Step 2: main CI 확인 후 태그**

```bash
git switch main && git pull
gh run list -R seogineer/openstack4j --branch main --limit 1 --json status,conclusion,headSha
git tag v4.4.0 && git push origin v4.4.0
gh run watch -R seogineer/openstack4j $(gh run list -R seogineer/openstack4j --workflow release.yml --limit 1 --json databaseId -q '.[0].databaseId') --exit-status
```
Expected: main 최신 커밋의 CI `success`, release workflow `success`.

- [ ] **Step 3: 다음 SNAPSHOT**

```bash
git switch -c chore/4.5.0-snapshot
./mvnw -B -q versions:set -DnewVersion=4.5.0-SNAPSHOT -DgenerateBackupPoms=false
sed -i 's#<openstack4j.version>4.4.0-SNAPSHOT</openstack4j.version>#<openstack4j.version>4.5.0-SNAPSHOT</openstack4j.version>#' examples/spring-boot-smoke/pom.xml
grep -rn "4.4.0-SNAPSHOT" --include=pom.xml . ; ./mvnw -B -q install -DskipTests -pl .,connectors,core,core-test
git add -A && git commit -m "chore: bump to 4.5.0-SNAPSHOT

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "4.4.0 릴리스 후 4.5.0-SNAPSHOT."
```
Expected: `grep` 출력 없음(남은 4.4.0-SNAPSHOT 없음). spring-boot-smoke pom 의 버전 표기가 다르면 그 파일을 열어 실제 표기에 맞춘다.

- [ ] **Step 4: Central 확인** — `~/openstack4j-check/pom.xml` 의 버전을 4.4.0 으로 바꾸고 `Check.java` 에 `os.identity().limits().model().getName()` 과 `os.identity().tokens().getSystemScopes(...)` 출력을 더해 실행한다. Central 반영까지 시간이 걸리면 `ScheduleWakeup` 없이 Monitor 로 `curl -s -o /dev/null -w '%{http_code}' https://repo1.maven.org/maven2/io/github/seogineer/openstack4j-core/4.4.0/` 가 200 이 될 때까지 기다린다.
Expected: 의존성 해석 성공, 출력에 `flat`.

- [ ] **Step 5: 메모리 갱신** — `project_openstack4j_fork.md` 에 E 완료(4.4.0), deferred minors, 다음 F(Neutron) 를 적는다.
