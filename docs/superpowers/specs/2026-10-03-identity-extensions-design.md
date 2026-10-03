# openstack4j 후속 포크 — E. Keystone(identity v3) 확장과 인증 방식 설계

- 작성일: 2026-10-03
- 상태: 확정(사용자 지시 "질문 없이 권장안으로 진행"에 따라 작성자가 결정하고 근거를 기록)
- 대상 버전: 4.4.0 (기능 추가, 하위 호환)
- 선행: A~D(4.0.0~4.3.0), 서비스별 지원 현황 분석(`docs/superpowers/analysis/2026-10-02-api-coverage-gap-analysis.md`)

## 1. 배경과 목적

openstack4j 의 identity v3 는 핵심 CRUD(users, groups, projects, domains, roles, role assignments, credentials, policies, services, endpoints, regions, tokens)만 지원한다. Keystone api-ref(`api-ref/source/v3/*.inc`, `v3-ext/*.inc`, `v3-ext/federation/**`)의 메서드 233개 중 81개만 구현돼 있고, 누락 152개는 다음과 같다.

| 영역 | 누락 메서드 |
|---|---|
| OS-EP-FILTER(endpoint groups, project↔endpoint) | 18 |
| OS-INHERIT(상속 역할 할당) | 14 |
| OS-OAUTH1(consumers, request/access token, 사용자 access token) | 13 |
| OS-ENDPOINT-POLICY | 13 |
| domain configuration(`/domains/{id}/config`, `/domains/config/default`) | 13 |
| system 역할 할당(`/system/{users,groups}/{id}/roles`) | 10 |
| OS-TRUST | 7 |
| unified limits(`/limits`, `/limits/model`) | 6 |
| project tags | 6 |
| registered limits | 5 |
| implied roles(`/roles/{id}/implies`, `/role_inferences`) | 6 |
| application credentials, access rules | 7 |
| OS-REVOKE events | 1 |
| `GET /auth/system` | 1 |
| OAuth2 client credentials(`POST /OS-OAUTH2/token`) | 1 |
| OS-FEDERATION(identity providers, protocols, mappings, service providers, projects/domains, SAML2 metadata·assertion·ECP, websso) | 28 |
| OS-SIMPLE-CERT(2), OS-PKI revoked(1) | 3 — Keystone 이 410 Gone 을 돌려준다 |

인증도 password·token 두 방식과 project·domain scope 뿐이다. application credential, TOTP(MFA), system scope, trust scope 로 토큰을 받을 수 없다.

또 응답 모델에 `options`, `password_expires_at`(user), `is_domain`·`options`·`tags`(project), `options`·`tags`(domain), `description`·`options`(role) 등이 없다.

이 하위 프로젝트는 위 누락을 채우고, 인증 방식을 보강한다.

### 제약

- **기존 코드는 유지하고 추가만 한다.** 기존 메서드의 시그니처·반환 타입·동작은 바꾸지 않는다. `@Deprecated` 도 붙이지 않는다. 기존 identity 테스트(core-test `api/identity/v3/*`)는 수정 없이 통과해야 한다.
- 410 API(OS-SIMPLE-CERT, OS-PKI revoked)와 브라우저 리다이렉트 전용 엔드포인트(websso 2개: `GET /auth/OS-FEDERATION/websso/{protocol}`, `GET /auth/OS-FEDERATION/identity_providers/{idp}/protocols/{protocol}/websso`)는 추가하지 않는다. federated 인증(`GET /OS-FEDERATION/identity_providers/{idp}/protocols/{protocol}/auth`)은 IdP 가 앞단에서 SAML/OIDC 를 처리한 환경에서만 의미가 있어 "토큰 발급" 대신 **unscoped federated token 을 받는 저수준 메서드**로만 제공한다.
- 회사 코드는 참고·복사하지 않는다. Keystone api-ref 를 기준으로 구현한다.

### 참고 수치

- Keystone 은 microversion 이 없다(identity API 3.x 는 추가만 한다). 개발용 OpenStack(epoxy) Keystone 은 `v3.14`. Hibiscus 도 3.14.
- 개발용에서 확장 동작 확인: OS-EP-FILTER, OS-TRUST, OS-OAUTH1, OS-FEDERATION, OS-REVOKE, registered/unified limits, limits model, role inferences, application credentials·access rules·application credential 인증, project tags, OS-INHERIT, domain config default, `/auth/system` 모두 200. OS-SIMPLE-CERT·OS-PKI 는 410.

## 2. 결정 사항

| 항목 | 결정 | 이유 |
|---|---|---|
| 구조 | 기존 `os.identity()` 에 accessor 를 추가하고, 기존 서비스(`users()`, `projects()`, `roles()`, `domains()`)에는 그 리소스의 하위 API 를 메서드로 추가 | 기존 패턴(서비스당 리소스) 유지 |
| 새 accessor | `applicationCredentials()`, `trusts()`, `endpointFilter()`, `endpointPolicies()`, `limits()`, `registeredLimits()`, `federation()`, `oauth1()`, `oauth2()`, `revocationEvents()`, `systemRoles()` | 확장(OS-*)은 독립 서비스로 |
| 인증 | `IOSClientBuilder.V3` 에 `applicationCredential(id, secret)`, `applicationCredential(name, secret, Identifier user, Identifier userDomain)`, `passcode(String)`(password+totp 또는 totp 단독), `scopeToSystem()`, `scopeToTrust(String)` 추가. `KeystoneAuth`/`AuthIdentity`/`AuthScope` 를 확장해 재인증도 같은 방식으로 | `OSAuthenticator.reAuthenticate()` 가 토큰의 `KeystoneAuth` 를 재사용하므로 모델만 확장하면 만료 후 재인증이 된다 |
| OAuth1 | consumer·access token·authorize 관리와 함께 `requestToken`/`accessToken` 을 OAuth1 HMAC-SHA1 서명으로 구현(서명기는 `openstack.identity.v3.internal.OAuth1Signer`, 외부 의존 없음). 응답은 form-encoded 라 직접 파싱 | api-ref 전체 지원 목표. 서명은 결정적(nonce·timestamp 주입)이라 단위 테스트 가능 |
| OAuth2 | `oauth2().token(clientId, clientSecret)` → access token 문자열과 만료 시간. Keystone 의 client credentials grant(`application/x-www-form-urlencoded`, HTTP Basic) | api-ref 에 있는 유일한 메서드 |
| federation | IdP·protocol·mapping·SP CRUD, `GET /OS-FEDERATION/projects|domains`, SAML2 metadata(XML 문자열), SAML2 assertion·ECP(XML 문자열, `POST /auth/OS-FEDERATION/saml2[/ecp]`), federated token(`.../protocols/{p}/auth` → `Token`) | 위 제약 |
| 응답 모델 | 기존 모델에 `default` getter 로 필드 추가(Map/List/Date/Boolean 은 null 허용) | D 와 같은 정책 |
| 버전 | **4.4.0** | 기존 동작 불변, 기능 추가 |

## 3. 인증 보강

### 3.1 모델 (`openstack/identity/v3/domain/KeystoneAuth.java`)

- `AuthIdentity`:
  - `createApplicationCredentialType(String id, String secret)` → `{"methods": ["application_credential"], "application_credential": {"id": ..., "secret": ...}}`
  - `createApplicationCredentialType(String name, String secret, Identifier user, Identifier userDomain)` → `{"application_credential": {"name": ..., "secret": ..., "user": {"id"|"name": ..., "domain": {"id"|"name": ...}}}}`
  - `withTotp(String userId|name, Identifier domain, String passcode)`: methods 에 `totp` 를 추가하고 `{"totp": {"user": {..., "passcode": ...}}}`. password 와 함께면 `["password", "totp"]`.
- `AuthScope.system()` → `{"system": {"all": true}}`. 기존 `AuthScope.trust(id)` 는 이미 `{"OS-TRUST:trust": {"id": ...}}` 로 직렬화된다(`@JsonProperty("OS-TRUST:trust")`) — builder 에서 노출만 한다.
- `Auth.Type` 에 `APPLICATION_CREDENTIAL` 을 추가한다. application credential 토큰은 scope 를 지정할 수 없다(Keystone 이 400) — builder 는 scope 와 함께 쓰이면 요청 전에 `IllegalStateException` 을 던진다.

### 3.2 builder (`IOSClientBuilder.V3`, `OSClientBuilder.ClientV3`)

| 메서드 | 동작 |
|---|---|
| `applicationCredential(String id, String secret)` | application credential id 로 인증 |
| `applicationCredential(String name, String secret, Identifier user, Identifier userDomain)` | 이름 + 사용자로 인증 |
| `passcode(String passcode)` | `credentials(...)` 와 함께면 password+totp MFA, 단독이면 totp(사용자는 `credentials(user, null, domain)` 로 지정) |
| `scopeToSystem()` | system scope(`{"system": {"all": true}}`) |
| `scopeToTrust(String trustId)` | trust scope |

`authenticate()` 의 분기 순서: token → application credential → credentials(+passcode) → tokenless.

### 3.3 토큰 모델

`KeystoneToken` 에 `system`(Map), `application_credential`(id/name/restricted), `OS-TRUST:trust`(id, impersonation, trustor/trustee) 필드를 추가하고 `Token` 에 `default` getter(`getSystem()`, `getApplicationCredential()`, `getTrust()`)를 둔다.

## 4. 기존 서비스에 메서드 추가

| 서비스 | 메서드 | 엔드포인트 |
|---|---|---|
| `users()` | `applicationCredentials` 는 별도 accessor. `accessRules(userId)`, `getAccessRule(userId, id)`, `deleteAccessRule(userId, id)` | `/users/{id}/access_rules[/{id}]` |
| | OAuth1 사용자 access token 은 `oauth1()` 로 | |
| `projects()` | `tags(projectId)`, `hasTag(projectId, tag)`, `addTag(projectId, tag)`, `replaceTags(projectId, List)`, `removeTag(projectId, tag)`, `removeAllTags(projectId)`; `list(ProjectListOptions)`(tags, tags-any, not-tags, not-tags-any, is_domain, parent_id, enabled, name, domain_id) | `/projects/{id}/tags[/{tag}]`, `GET /projects` |
| `roles()` | 상속 할당 16개(OS-INHERIT): `grantInheritedToUserOnDomain/Project`, `revoke...`, `checkInherited...`, `listInherited...`(domain 만 목록 존재), group 판 동일; implied roles: `listImpliedRoles(priorRoleId)`, `getImpliedRole(prior, implied)`, `checkImpliedRole`, `createImpliedRole`, `deleteImpliedRole`, `listRoleInferences()`; `listProjectUserRoles(projectId, userId)`, `listDomainUserRoles(domainId, userId)`(누락이던 GET) | `/OS-INHERIT/...`, `/roles/{id}/implies[/{id}]`, `/role_inferences`, `/projects/{p}/users/{u}/roles`, `/domains/{d}/users/{u}/roles` |
| `domains()` | 도메인 config 13개: `config(domainId)`, `configGroup(domainId, group)`, `configOption(domainId, group, option)`, `createConfig(domainId, Map)`, `updateConfig`, `updateConfigGroup`, `updateConfigOption`, `deleteConfig`, `deleteConfigGroup`, `deleteConfigOption`, `defaultConfig()`, `defaultConfigGroup(group)`, `defaultConfigOption(group, option)` | `/domains/{id}/config[/{g}[/{o}]]`, `/domains/config/default[/{g}[/{o}]]` |
| `tokens()` | `getSystemScopes()`(`GET /auth/system`) | |
| `users()` 목록 | `list(UserListOptions)`(name, domain_id, enabled, idp_id, protocol_id, unique_id, password_expires_at) | `GET /users` |

## 5. 새 서비스

| accessor | 메서드 | 엔드포인트 |
|---|---|---|
| `applicationCredentials()` | `list(userId)`, `list(userId, name)`, `get(userId, id)`, `create(userId, ApplicationCredentialCreate)`, `delete(userId, id)` | `/users/{id}/application_credentials[/{id}]` |
| `trusts()` | `list()`, `list(trustorUserId, trusteeUserId)`, `get(id)`, `create(TrustCreate)`, `delete(id)`, `roles(trustId)`, `getRole(trustId, roleId)`, `checkRole(trustId, roleId)` | `/OS-TRUST/trusts[/{id}[/roles[/{id}]]]` |
| `endpointFilter()` | endpoint groups CRUD(+HEAD), `endpointGroupEndpoints(id)`, `endpointGroupProjects(id)`, `addProjectToEndpointGroup`, `checkProjectInEndpointGroup`, `removeProjectFromEndpointGroup`, `getProjectEndpointGroup(egId, projectId)`, `projectEndpointGroups(projectId)`, `projectEndpoints(projectId)`, `addEndpointToProject`, `checkEndpointInProject`, `removeEndpointFromProject`, `endpointProjects(endpointId)` | `/OS-EP-FILTER/...` |
| `endpointPolicies()` | `associateWithEndpoint/Service/ServiceInRegion`, `check...`, `disassociate...`, `policyForEndpoint(endpointId)`, `endpointsForPolicy(policyId)`, `checkPolicyAssociations(policyId)`(HEAD .../policy) | `/policies/{id}/OS-ENDPOINT-POLICY/...`, `/endpoints/{id}/OS-ENDPOINT-POLICY/policy` |
| `systemRoles()` | user/group 별 `list`, `grant`, `check`(HEAD), `get`(GET), `revoke` | `/system/{users,groups}/{id}/roles[/{id}]` |
| `limits()` | `model()`, `list(LimitListOptions)`, `get(id)`, `create(List<LimitCreate>)`, `update(id, Integer resourceLimit, String description)`, `delete(id)` | `/limits[/model|/{id}]` |
| `registeredLimits()` | `list(RegisteredLimitListOptions)`, `get(id)`, `create(List<RegisteredLimitCreate>)`, `update(id, ...)`, `delete(id)` | `/registered_limits[/{id}]` |
| `federation()` | `identityProviders()`(CRUD, `protocols(idp)`, protocol CRUD), `mappings()`(CRUD), `serviceProviders()`(CRUD), `projects()`, `domains()`, `saml2Metadata()`, `saml2Assertion(tokenId, spId)`, `ecpAssertion(tokenId, spId)`, `federatedToken(idp, protocol, Map<String,String> headers)` | `/OS-FEDERATION/...`, `/auth/OS-FEDERATION/saml2[/ecp]` |
| `oauth1()` | consumers CRUD, `requestToken(consumerKey, consumerSecret, projectId)`, `authorize(requestTokenKey, List<roleId>)` → verifier, `accessToken(consumerKey, consumerSecret, requestToken, requestSecret, verifier)`, user access tokens: `list(userId)`, `get(userId, id)`, `delete(userId, id)`, `roles(userId, tokenId)`, `getRole(...)` | `/OS-OAUTH1/...`, `/users/{id}/OS-OAUTH1/access_tokens/...` |
| `oauth2()` | `token(clientId, clientSecret)` → `OAuth2AccessToken`(`getAccessToken()`, `getTokenType()`, `getExpiresIn()`) | `POST /OS-OAUTH2/token` |
| `revocationEvents()` | `list()`, `list(Date since)` | `GET /OS-REVOKE/events` |

## 6. 응답 모델 필드 추가 (모두 nullable, `default` getter)

- User: `options`(Map), `password_expires_at`(Date), `federated`(List<Map>)
- Project: `is_domain`(Boolean), `options`(Map), `tags`(List<String>)
- Domain: `options`(Map), `tags`(List<String>)
- Role: `description`, `options`(Map)
- Group: 변경 없음
- Token: 3.3 절

## 7. 테스트

### 7.1 단위 테스트 (core-test)
- 기존 identity 테스트 12 클래스는 **변경 없이** 통과해야 한다.
- 새 테스트는 `core-test/src/main/java/org/openstack4j/api/identity/v3/ext/` 에 둔다. 기존 `AbstractTest`(Service.IDENTITY, 포트 5000)와 v3 token fixture 를 쓴다.
- 인증 테스트는 `OSFactory.builderV3().endpoint(authURL("/v3"))...authenticate()` 로 MockWebServer 에 요청을 보내고 본문을 검사한다(기존 `KeystoneAuthenticationTests` 방식).
- fixture 는 개발용 Keystone 3.14 의 실제 응답(`scratchpad/keystone/live/*.json`)과 api-ref 예시로 만든다.

### 7.2 실환경 통합 테스트 (`IdentityExtensionsLiveTests`, 환경 변수 있을 때만)
application credential 생성 → 그 credential 로 인증(새 클라이언트) → 삭제; system scope 인증; project tag 추가·조회·삭제; endpoint group 생성·삭제; registered limit 생성·삭제(compute 서비스); trust 생성·조회·삭제(본인→본인); IdP·mapping·protocol 생성·삭제; OAuth1 consumer 생성·삭제; role inferences·limits model·revoke events·domain default config 조회. 임시 자원은 `finally` 에서 삭제한다.

## 8. 작업 순서

| # | 작업 |
|---|---|
| 1 | 인증 보강: application credential, TOTP/MFA, system·trust scope, 토큰 모델 필드 |
| 2 | 응답 모델 필드(user/project/domain/role) + `users().list(UserListOptions)`, `projects().list(ProjectListOptions)` |
| 3 | application credentials, access rules |
| 4 | project tags, `GET /auth/system` |
| 5 | roles 보강: OS-INHERIT, implied roles·role inferences, 누락 GET 2개 |
| 6 | system 역할 할당 |
| 7 | domain configuration(도메인별·기본값) |
| 8 | OS-TRUST |
| 9 | OS-EP-FILTER, OS-ENDPOINT-POLICY |
| 10 | unified limits(registered limits, limits, model) |
| 11 | OS-FEDERATION |
| 12 | OS-OAUTH1(서명 포함), OS-OAUTH2, OS-REVOKE |
| 13 | 실환경 통합 테스트, README identity 절, MIGRATION/CHANGELOG |
| 14 | 전체 리뷰 → 4.4.0 릴리스 → 4.5.0-SNAPSHOT |

## 9. 완료 기준

1. 기존 identity 테스트가 전부 변경 없이 통과한다.
2. 4·5·6장의 모든 메서드·필드와 3장의 인증 방식이 구현되고 단위 테스트가 세 connector 에서 통과한다.
3. 실환경 테스트가 개발용 Keystone(3.14)에서 통과하고 임시 자원이 남지 않는다. application credential 로 받은 클라이언트가 실제 API 를 호출할 수 있다.
4. `io.github.seogineer:openstack4j:4.4.0` 이 Maven Central 에 배포된다.
