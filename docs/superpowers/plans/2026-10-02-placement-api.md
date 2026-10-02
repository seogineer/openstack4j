# Placement API 전체 지원 (B) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** OpenStack Placement API 전체(1.28~1.39)를 microversion 협상과 함께 openstack4j 에 추가하고 4.1.0 으로 배포한다.

**Architecture:** 새 코드는 모두 `*.placement.v1` 패키지에 두고 기존 Placement 코드는 건드리지 않는다. 공통 기반 `BasePlacementV1Service` 가 microversion 협상(세션·endpoint 별 캐시), `OpenStack-API-Version` 헤더, 기능별 버전 검사, Placement 오류 → 예외 변환을 담당하고, 서비스 10개가 이를 상속한다. 단위 테스트는 core-test(MockWebServer)에 두어 connector 3개 모두에서 실행되고, 실환경 테스트는 환경 변수가 있을 때만 돈다.

**Tech Stack:** Java 17, Jackson 2.x, TestNG 7, OkHttp MockWebServer 4.12, Maven Wrapper.

**Spec:** `docs/superpowers/specs/2026-10-02-placement-api-design.md`

## Global Constraints

- 기존 코드(`PlacementService.resourceProviders()`, `org.openstack4j.{api,model,openstack}.placement.ext`, `openstack.placement.internal.BasePlacementServices`, `openstack.placement.domain.*`)는 **수정하지 않고 `@Deprecated` 도 붙이지 않는다.** 예외: `PlacementService` 인터페이스와 `PlacementServiceImpl` 에 accessor 를 **추가**하는 것, `DefaultAPIProvider` 에 binding 을 **추가**하는 것.
- 새 코드 패키지: 서비스 `org.openstack4j.api.placement.v1`, 모델·옵션·상수 `org.openstack4j.model.placement.v1`, 예외 `org.openstack4j.api.placement.v1.exceptions`, 구현 `org.openstack4j.openstack.placement.v1.internal`, Jackson 모델 `org.openstack4j.openstack.placement.v1.domain`.
- microversion: 라이브러리 최고 `1.39`, 새 API 최소 `1.28`. 헤더 `OpenStack-API-Version: placement <버전>`.
- 기능별 최소 버전: 1.30 reshaper, 1.31 candidates `in_tree`, 1.32 `member_of=!`, 1.33 숫자 아닌 group suffix, 1.35 `root_required`, 1.36 `same_subtree`, 1.37 provider 부모 해제(unparent), 1.38 consumer type, 1.39 trait `in:` 문법.
- openstack4j 관례: `get` 404 → `null`, `delete` → `ActionResponse`, 존재 확인 → `boolean`, 그 밖의 쓰기 실패 → `PlacementException`.
- 버전 검사 실패는 **서버 요청 없이** `PlacementMicroVersionException`.
- 회사 코드(`~/IdeaProjects/openstackit-java`)는 참고·복사하지 않는다.
- 각 Task = 브랜치 하나 + PR 하나. CI(JDK 17/21/25, 아티팩트 검사, 스모크) 통과 후 squash 머지(사용자 사전 승인, 2026-10-01). tag push(배포)는 매번 사용자 확인.
- 커밋 메시지 끝 `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, PR 본문 끝 `🤖 Generated with [Claude Code](https://claude.com/claude-code)`.
- 빌드는 `./mvnw`. 단위 테스트 실행: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='<클래스명>' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test` (core-test 의 테스트는 connector 모듈에서 실행된다).

## Review Focus

1. **세션 간 microversion 캐시 오염** — 같은 JVM 의 다른 사용자 세션·다른 endpoint(리전)가 서로의 협상 결과나 수동 지정 값을 쓰면 안 된다. → Task 1 `pinIsPerSessionAndEndpoint` 테스트.
2. **오류 본문이 Placement 형식이 아닌 경우**(프록시의 HTML 502, 빈 본문) — 파싱 실패로 원래 상태 코드를 잃지 말고 `PlacementException(status)` 로 알려야 한다. → Task 1 `nonPlacementErrorBodyStillMapsStatus`.
3. **resource class/trait 이름·UUID 에 URL 특수문자** — 경로 조각은 그대로 붙지만 사용자 실수(`"CUSTOM_A/B"`)로 다른 경로를 호출하면 안 된다. 이름은 Placement 규칙(`^[A-Z0-9_]+$`)으로 클라이언트에서 검사한다. → Task 4·5 `rejectsInvalidName`.
4. **generation 이 0** — 0 은 유효한 값이다. `long` 으로 받고 생략·null 처리를 하지 않는다. → Task 3 `replaceSendsZeroGeneration`.
5. **1.38 미만 서버의 project usages 응답 형식**(consumer type 그룹 없음) — 같은 모델로 읽혀야 한다. → Task 7 `projectUsagesBefore138`.

---

## 실행 전 준비

- [ ] **P1: spec·plan PR 머지** — 브랜치 `docs/placement-spec` 에 spec(커밋 `bf3e939e`)과 이 plan 을 커밋하고 PR → CI → 머지.

```bash
cd ~/IdeaProjects/openstack4j && git switch docs/placement-spec
git add docs/superpowers/plans/2026-10-02-placement-api.md
git commit -m "docs: add Placement API implementation plan

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```
그다음 공통 PR 절차.

### 공통 PR 절차

```bash
git push -u origin HEAD
gh pr create -R seogineer/openstack4j --base main --fill-first --body "<요약>

🤖 Generated with [Claude Code](https://claude.com/claude-code)"
gh pr checks -R seogineer/openstack4j --watch
gh pr merge -R seogineer/openstack4j --squash --delete-branch && git switch main && git pull
```
각 Task 는 `git switch main && git pull && git switch -c task/b<N>-<이름>` 으로 시작한다.

### 실행 메모 (fixture 원본)

`core-test/src/main/resources/placement/v1/*.json` 은 개발용 OpenStack(epoxy, placement 1.39)의 실제 응답을 바탕으로 이 plan 에 그대로 적어 두었다. 호스트명은 `compute-1` 로 일반화했다.

---

### Task 1: 기반 — microversion 협상, 오류 매핑, 재시도 도우미, versions()

**Files:**
- Create (core): `model/placement/v1/PlacementMicroVersions.java`, `model/placement/v1/PlacementVersion.java`, `api/placement/v1/VersionService.java`, `api/placement/v1/Placement.java`, `api/placement/v1/exceptions/PlacementException.java`, `api/placement/v1/exceptions/PlacementConcurrentUpdateException.java`, `api/placement/v1/exceptions/PlacementMicroVersionException.java`, `openstack/placement/v1/domain/PlacementRoot.java`, `openstack/placement/v1/domain/PlacementErrorResponse.java`, `openstack/placement/v1/domain/PlacementVersionInfo.java`, `openstack/placement/v1/internal/PlacementSessionState.java`, `openstack/placement/v1/internal/PlacementErrors.java`, `openstack/placement/v1/internal/BasePlacementV1Service.java`, `openstack/placement/v1/internal/VersionServiceImpl.java` (모두 `core/src/main/java/org/openstack4j/` 아래)
- Modify (core): `api/placement/PlacementService.java` (accessor 추가), `openstack/placement/internal/PlacementServiceImpl.java` (구현 추가), `openstack/provider/DefaultAPIProvider.java` (binding 추가)
- Create (test): `core/src/test/java/org/openstack4j/test/placement/PlacementRetryTest.java`, `core-test/src/main/java/org/openstack4j/api/placement/v1/AbstractPlacementTest.java`, `core-test/src/main/java/org/openstack4j/api/placement/v1/PlacementVersionTests.java`
- Modify (test): `core-test/src/main/java/org/openstack4j/api/AbstractTest.java` (`Service.PLACEMENT(8780)`), `core-test/src/main/resources/identity/v3/authv3_project.json` (placement 카탈로그 항목)

**Interfaces:**
- Produces:
  - `PlacementMicroVersions.MINIMUM` (1.28), `LATEST` (1.39), `V1_30`, `V1_31`, `V1_32`, `V1_33`, `V1_35`, `V1_36`, `V1_37`, `V1_38`, `V1_39` — 모두 `org.openstack4j.openstack.internal.MicroVersion`
  - `BasePlacementV1Service`: `protected MicroVersion microVersion()`, `protected void requireMicroVersion(String feature, MicroVersion required)`, `protected <R> Invocation<R> placement(HttpMethod method, Class<R> type, String path)`, `protected <R> R executeOrNull(Invocation<R>)`, `protected <R> R executeOrThrow(Invocation<R>)`, `protected ActionResponse executeAction(Invocation<ActionResponse>)`, `protected static String requireName(String kind, String name)`
  - `PlacementService`: `VersionService versions()`, `void useMicroVersion(String version)` (이후 Task 들이 accessor 를 하나씩 추가)
  - core-test `AbstractPlacementTest`: `RP` 상수, `respondWithVersions(String max)`, `takeVersionAndRequest()`, `respondWithError(int status, String code)`, `body(RecordedRequest)`, `assertNoMoreRequests()`

- [ ] **Step 1: 테스트 인프라 — Service.PLACEMENT 와 카탈로그 항목**

```bash
git switch main && git pull && git switch -c task/b1-placement-foundation
```
`AbstractTest.java` 의 `Service` enum 에서 `WORKFLOW(8989);` 를 `WORKFLOW(8989),` 로 바꾸고 다음 줄에 `PLACEMENT(8780);` 를 추가한다.

`authv3_project.json` 의 `"type": "load-balancer"` 서비스 객체 바로 뒤(닫는 `},` 뒤)에 추가:
```json
            {
                "endpoints": [
                    {
                        "region_id": "RegionOne",
                        "url": "http://127.0.0.1:8780",
                        "region": "RegionOne",
                        "interface": "public",
                        "id": "a3e6f0f0c1d64c4f9d0e3c3b2e8f8780"
                    },
                    {
                        "region_id": "RegionOne",
                        "url": "http://127.0.0.1:8780",
                        "region": "RegionOne",
                        "interface": "internal",
                        "id": "b3e6f0f0c1d64c4f9d0e3c3b2e8f8780"
                    }
                ],
                "type": "placement",
                "id": "c3e6f0f0c1d64c4f9d0e3c3b2e8f8780",
                "name": "placement"
            },
```
확인: `python3 -c "import json;d=json.load(open('core-test/src/main/resources/identity/v3/authv3_project.json'));print([s['type'] for s in d['token']['catalog'] if s['type']=='placement'])"` → `['placement']`.

- [ ] **Step 2: 재시도 도우미 테스트 작성 (core 단위)**

`core/src/test/java/org/openstack4j/test/placement/PlacementRetryTest.java`:
```java
package org.openstack4j.test.placement;

import java.util.concurrent.atomic.AtomicInteger;

import org.openstack4j.api.placement.v1.Placement;
import org.openstack4j.api.placement.v1.exceptions.PlacementConcurrentUpdateException;
import org.openstack4j.api.placement.v1.exceptions.PlacementException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PlacementRetryTest {

    private static PlacementConcurrentUpdateException conflict() {
        return new PlacementConcurrentUpdateException("conflict", 409, "placement.concurrent_update", "detail", "req-1");
    }

    @Test
    public void retriesUntilSuccess() {
        AtomicInteger calls = new AtomicInteger();
        String result = Placement.retryOnConcurrentUpdate(3, () -> {
            if (calls.incrementAndGet() < 3) throw conflict();
            return "ok";
        });
        Assert.assertEquals(result, "ok");
        Assert.assertEquals(calls.get(), 3);
    }

    @Test
    public void rethrowsLastConflictAfterMaxAttempts() {
        AtomicInteger calls = new AtomicInteger();
        try {
            Placement.retryOnConcurrentUpdate(2, () -> {
                calls.incrementAndGet();
                throw conflict();
            });
            Assert.fail("expected PlacementConcurrentUpdateException");
        } catch (PlacementConcurrentUpdateException expected) {
            Assert.assertEquals(calls.get(), 2);
        }
    }

    @Test
    public void otherExceptionsAreNotRetried() {
        AtomicInteger calls = new AtomicInteger();
        try {
            Placement.retryOnConcurrentUpdate(5, () -> {
                calls.incrementAndGet();
                throw new PlacementException("in use", 409, "placement.inventory.inuse", "detail", "req-1");
            });
            Assert.fail("expected PlacementException");
        } catch (PlacementConcurrentUpdateException unexpected) {
            Assert.fail("must not be a concurrent update");
        } catch (PlacementException expected) {
            Assert.assertEquals(calls.get(), 1);
            Assert.assertEquals(expected.getErrorCode(), "placement.inventory.inuse");
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void maxAttemptsMustBePositive() {
        Placement.retryOnConcurrentUpdate(0, () -> "never");
    }
}
```

- [ ] **Step 3: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q test -pl core -Dtest=PlacementRetryTest 2>&1 | grep -E 'cannot find symbol|ERROR' | head -3`
Expected: `cannot find symbol` (Placement, 예외 클래스 없음).

- [ ] **Step 4: 예외 3종과 재시도 도우미 구현**

`api/placement/v1/exceptions/PlacementException.java`:
```java
package org.openstack4j.api.placement.v1.exceptions;

import org.openstack4j.api.exceptions.ClientResponseException;

/**
 * A Placement API request failed. Carries the Placement error code (for example {@code placement.inventory.inuse}).
 */
public class PlacementException extends ClientResponseException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String detail;
    private final String requestId;

    public PlacementException(String message, int status, String errorCode, String detail, String requestId) {
        super(message, status);
        this.errorCode = errorCode;
        this.detail = detail;
        this.requestId = requestId;
    }

    /** @return the Placement error code, or {@code null} when the response body was not a Placement error */
    public String getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detail;
    }

    public String getRequestId() {
        return requestId;
    }
}
```

`api/placement/v1/exceptions/PlacementConcurrentUpdateException.java`:
```java
package org.openstack4j.api.placement.v1.exceptions;

/**
 * The resource provider or consumer generation sent with a write no longer matches the server
 * ({@code placement.concurrent_update}). Re-read the current state and apply the change again, for example with
 * {@link org.openstack4j.api.placement.v1.Placement#retryOnConcurrentUpdate}.
 */
public class PlacementConcurrentUpdateException extends PlacementException {

    private static final long serialVersionUID = 1L;

    public PlacementConcurrentUpdateException(String message, int status, String errorCode, String detail, String requestId) {
        super(message, status, errorCode, detail, requestId);
    }
}
```

`api/placement/v1/exceptions/PlacementMicroVersionException.java`:
```java
package org.openstack4j.api.placement.v1.exceptions;

import org.openstack4j.api.exceptions.OS4JException;

/**
 * The requested Placement feature or microversion is not available on this server. Raised before any request is sent.
 */
public class PlacementMicroVersionException extends OS4JException {

    private static final long serialVersionUID = 1L;

    public PlacementMicroVersionException(String message) {
        super(message);
    }
}
```

`api/placement/v1/Placement.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.function.Supplier;

import org.openstack4j.api.placement.v1.exceptions.PlacementConcurrentUpdateException;

/**
 * Helpers for working with the Placement API.
 */
public final class Placement {

    private Placement() {
    }

    /**
     * Runs {@code action} and runs it again when it fails with {@link PlacementConcurrentUpdateException}, up to
     * {@code maxAttempts} times in total. The action must re-read the current generation itself.
     */
    public static <T> T retryOnConcurrentUpdate(int maxAttempts, Supplier<T> action) {
        if (maxAttempts < 1)
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (PlacementConcurrentUpdateException e) {
                if (attempt >= maxAttempts)
                    throw e;
            }
        }
    }
}
```

- [ ] **Step 5: 재시도 테스트 통과 확인**

Run: `./mvnw -B test -pl core -Dtest=PlacementRetryTest 2>&1 | grep -E 'Tests run:' | tail -1`
Expected: `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 6: microversion 상수와 모델**

`model/placement/v1/PlacementMicroVersions.java`:
```java
package org.openstack4j.model.placement.v1;

import org.openstack4j.openstack.internal.MicroVersion;

/**
 * Placement microversions this library knows about.
 */
public final class PlacementMicroVersions {

    /** The oldest microversion the {@code placement().v1} services support (Rocky). */
    public static final MicroVersion MINIMUM = new MicroVersion(1, 28);
    /** The newest microversion this library understands. */
    public static final MicroVersion LATEST = new MicroVersion(1, 39);

    public static final MicroVersion V1_30 = new MicroVersion(1, 30);
    public static final MicroVersion V1_31 = new MicroVersion(1, 31);
    public static final MicroVersion V1_32 = new MicroVersion(1, 32);
    public static final MicroVersion V1_33 = new MicroVersion(1, 33);
    public static final MicroVersion V1_35 = new MicroVersion(1, 35);
    public static final MicroVersion V1_36 = new MicroVersion(1, 36);
    public static final MicroVersion V1_37 = new MicroVersion(1, 37);
    public static final MicroVersion V1_38 = new MicroVersion(1, 38);
    public static final MicroVersion V1_39 = new MicroVersion(1, 39);

    private PlacementMicroVersions() {
    }
}
```

`model/placement/v1/PlacementVersion.java`:
```java
package org.openstack4j.model.placement.v1;

import org.openstack4j.model.ModelEntity;

/**
 * The Placement microversion range of the server and the microversion this session uses.
 */
public interface PlacementVersion extends ModelEntity {

    String getServerMinVersion();

    String getServerMaxVersion();

    /** @return the microversion sent with requests, or {@code null} when the server is older than 1.28 */
    String getMicroVersion();

    /** @return {@code true} when the version was set with {@code useMicroVersion} instead of negotiated */
    boolean isPinned();
}
```

`api/placement/v1/VersionService.java`:
```java
package org.openstack4j.api.placement.v1;

import org.openstack4j.common.RestService;
import org.openstack4j.model.placement.v1.PlacementVersion;

public interface VersionService extends RestService {

    /** @return the server's microversion range and the version this session uses */
    PlacementVersion get();
}
```

- [ ] **Step 7: Jackson 모델 (root, error, version info)**

`openstack/placement/v1/domain/PlacementRoot.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.openstack.internal.MicroVersion;

/** Response of {@code GET /} on the Placement endpoint. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementRoot implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("versions")
    private List<Entry> versions;

    public MicroVersion getMinVersion() {
        return parse(versions == null || versions.isEmpty() ? null : versions.get(0).minVersion);
    }

    public MicroVersion getMaxVersion() {
        return parse(versions == null || versions.isEmpty() ? null : versions.get(0).maxVersion);
    }

    private static MicroVersion parse(String value) {
        return value == null || value.isEmpty() ? new MicroVersion(1, 0) : new MicroVersion(value);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("min_version")
        String minVersion;
        @JsonProperty("max_version")
        String maxVersion;
    }
}
```

`openstack/placement/v1/domain/PlacementErrorResponse.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** {@code {"errors": [{"status": 409, "code": "...", "title": "...", "detail": "...", "request_id": "..."}]}} */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementErrorResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("errors")
    private List<Error> errors;

    public Error first() {
        return errors == null || errors.isEmpty() ? null : errors.get(0);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Error implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("code")
        public String code;
        @JsonProperty("title")
        public String title;
        @JsonProperty("detail")
        public String detail;
        @JsonProperty("request_id")
        public String requestId;
    }
}
```

`openstack/placement/v1/domain/PlacementVersionInfo.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import org.openstack4j.model.placement.v1.PlacementVersion;

public class PlacementVersionInfo implements PlacementVersion {

    private static final long serialVersionUID = 1L;

    private final String serverMinVersion;
    private final String serverMaxVersion;
    private final String microVersion;
    private final boolean pinned;

    public PlacementVersionInfo(String serverMinVersion, String serverMaxVersion, String microVersion, boolean pinned) {
        this.serverMinVersion = serverMinVersion;
        this.serverMaxVersion = serverMaxVersion;
        this.microVersion = microVersion;
        this.pinned = pinned;
    }

    @Override
    public String getServerMinVersion() {
        return serverMinVersion;
    }

    @Override
    public String getServerMaxVersion() {
        return serverMaxVersion;
    }

    @Override
    public String getMicroVersion() {
        return microVersion;
    }

    @Override
    public boolean isPinned() {
        return pinned;
    }
}
```
- [ ] **Step 8: 세션 상태, 오류 매핑, 기반 클래스**

`openstack/placement/v1/internal/PlacementSessionState.java`:
```java
package org.openstack4j.openstack.placement.v1.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.openstack4j.openstack.internal.MicroVersion;

/**
 * Negotiated Placement microversion state, kept per client session and per Placement endpoint. Weakly keyed so a
 * discarded session releases its state.
 */
public final class PlacementSessionState {

    private static final Map<Object, Map<String, State>> STATES = new WeakHashMap<>();

    private PlacementSessionState() {
    }

    static final class State {
        final MicroVersion serverMin;
        final MicroVersion serverMax;
        volatile MicroVersion pinned;

        State(MicroVersion serverMin, MicroVersion serverMax) {
            this.serverMin = serverMin;
            this.serverMax = serverMax;
        }
    }

    static synchronized State get(Object session, String endpoint) {
        Map<String, State> byEndpoint = STATES.get(session);
        return byEndpoint == null ? null : byEndpoint.get(endpoint);
    }

    static synchronized State putIfAbsent(Object session, String endpoint, State state) {
        return STATES.computeIfAbsent(session, s -> new HashMap<>()).merge(endpoint, state, (existing, ignored) -> existing);
    }

    /** Forgets every negotiated and pinned version. Intended for tests. */
    public static synchronized void clearAll() {
        STATES.clear();
    }
}
```

`openstack/placement/v1/internal/PlacementErrors.java`:
```java
package org.openstack4j.openstack.placement.v1.internal;

import org.openstack4j.api.placement.v1.exceptions.PlacementConcurrentUpdateException;
import org.openstack4j.api.placement.v1.exceptions.PlacementException;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.PropagateResponse;
import org.openstack4j.openstack.placement.v1.domain.PlacementErrorResponse;

/** Turns Placement error responses into {@link PlacementException}s. */
final class PlacementErrors {

    static final String CONCURRENT_UPDATE = "placement.concurrent_update";

    /** For writes: every error status throws. */
    static final PropagateResponse THROW_ALL = response -> {
        if (response.getStatus() >= 400)
            throw toException(response);
    };

    /** For reads: 404 is left to openstack4j, which returns {@code null}; other errors throw. */
    static final PropagateResponse THROW_EXCEPT_404 = response -> {
        if (response.getStatus() >= 400 && response.getStatus() != 404)
            throw toException(response);
    };

    private PlacementErrors() {
    }

    static PlacementException toException(HttpResponse response) {
        PlacementErrorResponse.Error error = null;
        try {
            PlacementErrorResponse body = response.readEntity(PlacementErrorResponse.class);
            error = body == null ? null : body.first();
        } catch (RuntimeException notPlacementJson) {
            // proxies and load balancers answer with HTML or an empty body; keep the HTTP status
        }
        int status = response.getStatus();
        String code = error == null ? null : error.code;
        String detail = error == null ? response.getStatusMessage() : (error.detail != null ? error.detail.trim() : error.title);
        String requestId = error == null ? null : error.requestId;
        String message = "Placement request failed with " + status + (code == null ? "" : " " + code)
                + (detail == null || detail.isEmpty() ? "" : ": " + detail);
        if (CONCURRENT_UPDATE.equals(code))
            return new PlacementConcurrentUpdateException(message, status, code, detail, requestId);
        return new PlacementException(message, status, code, detail, requestId);
    }
}
```

`openstack/placement/v1/internal/BasePlacementV1Service.java`:
```java
package org.openstack4j.openstack.placement.v1.internal;

import java.util.regex.Pattern;

import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.PlacementMicroVersions;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.OSClientSession;
import org.openstack4j.openstack.placement.v1.domain.PlacementRoot;

/**
 * Base of the {@code placement.v1} services: microversion negotiation, the {@code OpenStack-API-Version} header,
 * per-feature version checks and Placement error mapping.
 */
public abstract class BasePlacementV1Service extends BaseOpenStackService {

    static final String API_VERSION_HEADER = "OpenStack-API-Version";
    private static final Pattern NAME = Pattern.compile("^[A-Z0-9_]+$");

    protected BasePlacementV1Service() {
        super(ServiceType.PLACEMENT);
    }

    /** Returns the cached server range for this session and endpoint, fetching {@code GET /} the first time. */
    PlacementSessionState.State state() {
        OSClientSession<?, ?> session = OSClientSession.getCurrent();
        String endpoint = session.getEndpoint(ServiceType.PLACEMENT);
        PlacementSessionState.State state = PlacementSessionState.get(session, endpoint);
        if (state != null)
            return state;
        PlacementRoot root = request(HttpMethod.GET, PlacementRoot.class, "/")
                .execute(ExecutionOptions.create(PlacementErrors.THROW_ALL));
        return PlacementSessionState.putIfAbsent(session, endpoint,
                new PlacementSessionState.State(root.getMinVersion(), root.getMaxVersion()));
    }

    /** The microversion sent with requests: the pinned one, otherwise min(library latest, server max). */
    protected MicroVersion microVersion() {
        PlacementSessionState.State state = state();
        if (state.pinned != null)
            return state.pinned;
        MicroVersion version = min(PlacementMicroVersions.LATEST, state.serverMax);
        if (version.compareTo(PlacementMicroVersions.MINIMUM) < 0)
            throw new PlacementMicroVersionException(String.format(
                    "The placement v1 API requires placement microversion %s or later, but the server supports up to %s",
                    PlacementMicroVersions.MINIMUM, state.serverMax));
        return version;
    }

    protected void requireMicroVersion(String feature, MicroVersion required) {
        MicroVersion version = microVersion();
        if (version.compareTo(required) < 0)
            throw new PlacementMicroVersionException(String.format(
                    "%s requires placement microversion %s, but the negotiated version is %s (server max %s)",
                    feature, required, version, state().serverMax));
    }

    protected <R> Invocation<R> placement(HttpMethod method, Class<R> type, String path) {
        return request(method, type, path).header(API_VERSION_HEADER, "placement " + microVersion());
    }

    protected <R> R executeOrNull(Invocation<R> invocation) {
        return invocation.execute(ExecutionOptions.create(PlacementErrors.THROW_EXCEPT_404));
    }

    protected <R> R executeOrThrow(Invocation<R> invocation) {
        return invocation.execute(ExecutionOptions.create(PlacementErrors.THROW_ALL));
    }

    protected ActionResponse executeAction(Invocation<ActionResponse> invocation) {
        return invocation.execute();
    }

    /** Placement names (resource classes, traits) are upper case letters, digits and underscores. */
    protected static String requireName(String kind, String name) {
        if (name == null || !NAME.matcher(name).matches())
            throw new IllegalArgumentException(kind + " name must match " + NAME.pattern() + ": " + name);
        return name;
    }

    static MicroVersion min(MicroVersion a, MicroVersion b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    static MicroVersion max(MicroVersion a, MicroVersion b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
}
```

`openstack/placement/v1/internal/VersionServiceImpl.java`:
```java
package org.openstack4j.openstack.placement.v1.internal;

import org.openstack4j.api.placement.v1.VersionService;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.placement.v1.PlacementMicroVersions;
import org.openstack4j.model.placement.v1.PlacementVersion;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.placement.v1.domain.PlacementVersionInfo;

public class VersionServiceImpl extends BasePlacementV1Service implements VersionService {

    @Override
    public PlacementVersion get() {
        PlacementSessionState.State state = state();
        String version;
        try {
            version = microVersion().toString();
        } catch (PlacementMicroVersionException tooOld) {
            version = null;
        }
        return new PlacementVersionInfo(state.serverMin.toString(), state.serverMax.toString(), version, state.pinned != null);
    }

    /**
     * Pins the microversion for the current session and endpoint, or returns to negotiation when {@code version} is
     * {@code null}.
     */
    public void pin(String version) {
        PlacementSessionState.State state = state();
        if (version == null) {
            state.pinned = null;
            return;
        }
        MicroVersion requested;
        try {
            requested = new MicroVersion(version);
        } catch (IllegalArgumentException e) {
            throw new PlacementMicroVersionException("Invalid placement microversion '" + version + "': " + e.getMessage());
        }
        MicroVersion lowest = max(PlacementMicroVersions.MINIMUM, state.serverMin);
        MicroVersion highest = min(PlacementMicroVersions.LATEST, state.serverMax);
        if (requested.compareTo(lowest) < 0 || requested.compareTo(highest) > 0)
            throw new PlacementMicroVersionException(String.format(
                    "Placement microversion %s is outside the usable range %s - %s (library %s - %s, server %s - %s)",
                    requested, lowest, highest, PlacementMicroVersions.MINIMUM, PlacementMicroVersions.LATEST,
                    state.serverMin, state.serverMax));
        state.pinned = requested;
    }
}
```

- [ ] **Step 9: PlacementService 에 accessor 추가와 binding**

`api/placement/PlacementService.java` — 기존 메서드 아래에 추가(기존 내용은 그대로):
```java
    /**
     * @return the Placement microversion range of the server and the version this session uses
     */
    org.openstack4j.api.placement.v1.VersionService versions();

    /**
     * Pins the Placement microversion used by the {@code v1} services for this session, or returns to automatic
     * negotiation when {@code version} is {@code null}.
     *
     * @throws org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException if the version is outside
     *         the range supported by both the library and the server
     */
    void useMicroVersion(String version);
```

`openstack/placement/internal/PlacementServiceImpl.java` — 기존 메서드 아래에 추가:
```java
    @Override
    public org.openstack4j.api.placement.v1.VersionService versions() {
        return Apis.get(org.openstack4j.api.placement.v1.VersionService.class);
    }

    @Override
    public void useMicroVersion(String version) {
        new org.openstack4j.openstack.placement.v1.internal.VersionServiceImpl().pin(version);
    }
```

`openstack/provider/DefaultAPIProvider.java` — `bind(ResourceProviderService.class, ResourceProviderServiceImpl.class);` 줄 바로 아래에 추가:
```java
        bind(org.openstack4j.api.placement.v1.VersionService.class, org.openstack4j.openstack.placement.v1.internal.VersionServiceImpl.class);
```

- [ ] **Step 10: core-test 기반 클래스와 협상 테스트 작성**

`core-test/src/main/java/org/openstack4j/api/placement/v1/AbstractPlacementTest.java`:
```java
package org.openstack4j.api.placement.v1;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.openstack.placement.v1.internal.PlacementSessionState;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

public abstract class AbstractPlacementTest extends AbstractTest {

    /** The resource provider used throughout the fixtures. */
    protected static final String RP = "3626308f-38dd-4da8-8f0f-6697b05d8f6c";

    @Override
    protected Service service() {
        return Service.PLACEMENT;
    }

    @BeforeMethod
    public void forgetNegotiatedVersions() {
        PlacementSessionState.clearAll();
    }

    /** Enqueues the response of {@code GET /} advertising {@code max} as the server's newest microversion. */
    protected void respondWithVersions(String max) {
        respondWith(200, "{\"versions\":[{\"id\":\"v1.0\",\"max_version\":\"" + max
                + "\",\"min_version\":\"1.0\",\"status\":\"CURRENT\",\"links\":[{\"rel\":\"self\",\"href\":\"\"}]}]}");
    }

    /** Takes the version discovery request and returns the request that followed it. */
    protected RecordedRequest takeVersionAndRequest() throws InterruptedException {
        RecordedRequest root = takeRequest();
        Assert.assertEquals(root.getPath(), "/");
        return takeRequest();
    }

    protected void respondWithError(int status, String code) {
        respondWith(status, "{\"errors\":[{\"status\":" + status + ",\"title\":\"Error\",\"detail\":\"detail for "
                + code + "\",\"code\":\"" + code + "\",\"request_id\":\"req-test\"}]}");
    }

    protected JsonNode body(RecordedRequest request) throws IOException {
        return new ObjectMapper().readTree(request.getBody().readUtf8());
    }

    protected void assertNoMoreRequests() throws InterruptedException {
        Assert.assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS), "unexpected extra request");
    }
}
```

`core-test/src/main/java/org/openstack4j/api/placement/v1/PlacementVersionTests.java`:
```java
package org.openstack4j.api.placement.v1;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.placement.v1.PlacementVersion;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/Versions")
public class PlacementVersionTests extends AbstractPlacementTest {

    public void negotiatesLibraryLatestWhenServerIsNewer() throws Exception {
        respondWithVersions("1.45");

        PlacementVersion version = osv3().placement().versions().get();

        Assert.assertEquals(version.getMicroVersion(), "1.39");
        Assert.assertEquals(version.getServerMaxVersion(), "1.45");
        Assert.assertFalse(version.isPinned());
        Assert.assertEquals(takeRequest().getPath(), "/");
    }

    public void usesServerMaxWhenServerIsOlder() throws Exception {
        respondWithVersions("1.30");

        Assert.assertEquals(osv3().placement().versions().get().getMicroVersion(), "1.30");
        takeRequest();
    }

    public void serverOlderThanMinimumHasNoUsableVersion() throws Exception {
        respondWithVersions("1.27");

        PlacementVersion version = osv3().placement().versions().get();

        Assert.assertNull(version.getMicroVersion());
        Assert.assertEquals(version.getServerMaxVersion(), "1.27");
        takeRequest();
    }

    public void rootIsFetchedOncePerSession() throws Exception {
        respondWithVersions("1.39");

        osv3().placement().versions().get();
        osv3().placement().versions().get();

        takeRequest();
        assertNoMoreRequests();
    }

    public void pinWithinRangeIsUsedAndCanBeCleared() throws Exception {
        respondWithVersions("1.39");

        osv3().placement().useMicroVersion("1.30");
        PlacementVersion pinned = osv3().placement().versions().get();
        osv3().placement().useMicroVersion(null);
        PlacementVersion negotiated = osv3().placement().versions().get();

        Assert.assertEquals(pinned.getMicroVersion(), "1.30");
        Assert.assertTrue(pinned.isPinned());
        Assert.assertEquals(negotiated.getMicroVersion(), "1.39");
        Assert.assertFalse(negotiated.isPinned());
        takeRequest();
        assertNoMoreRequests();
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.40.*outside.*")
    public void pinAboveRangeIsRejected() throws Exception {
        respondWithVersions("1.39");
        try {
            osv3().placement().useMicroVersion("1.40");
        } finally {
            takeRequest();
        }
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class)
    public void pinBelowMinimumIsRejected() throws Exception {
        respondWithVersions("1.39");
        try {
            osv3().placement().useMicroVersion("1.27");
        } finally {
            takeRequest();
        }
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = "Invalid placement microversion 'latest'.*")
    public void pinWithInvalidFormatIsRejected() throws Exception {
        respondWithVersions("1.39");
        try {
            osv3().placement().useMicroVersion("latest");
        } finally {
            takeRequest();
        }
    }

    public void pinIsPerSessionAndEndpoint() throws Exception {
        respondWithVersions("1.39");
        osv3().placement().useMicroVersion("1.30");
        takeRequest();

        // a second, independent session (same token, new client) must negotiate on its own
        org.openstack4j.api.OSClient.OSClientV3 other = org.openstack4j.openstack.OSFactory.clientFromToken(osv3().getToken());
        respondWithVersions("1.39");
        PlacementVersion otherVersion = other.placement().versions().get();
        RecordedRequest root = takeRequest();

        Assert.assertEquals(root.getPath(), "/");
        Assert.assertEquals(otherVersion.getMicroVersion(), "1.39");
        Assert.assertFalse(otherVersion.isPinned());
    }
}
```
`pinIsPerSessionAndEndpoint` 는 Review Focus 1번을 고정한다. `OSFactory.clientFromToken` 은 새 세션을 만들면서 현재 스레드의 세션을 그 세션으로 바꾸지만, 토큰·endpoint 가 같고 `@BeforeMethod` 의 `clearAll()` 이 협상 상태를 매번 지우므로 이후 테스트에 영향이 없다.

오류 매핑 테스트(`nonPlacementErrorBodyStillMapsStatus` 포함)는 첫 실제 엔드포인트가 생기는 Task 2 에 둔다.

- [ ] **Step 11: 실행 → 실패 확인 후 전체 통과 확인**

Step 9 를 적용하기 전에 Step 10 테스트만 먼저 추가해 실행하면 `versions()` 가 없어 컴파일 실패해야 한다. Step 6~9 적용 후:

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest=PlacementVersionTests -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Tests run: 9, Failures: 0, Errors: 0`

- [ ] **Step 12: 전체 빌드와 커밋**

Run: `./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'`
Expected: `BUILD SUCCESS`. core 25(21+4), connector 별 기존 수 + 9.

```bash
git add -A
git commit -m "feat(placement): add microversion negotiation and error mapping for placement v1

Negotiates min(library 1.39, server max) per session and endpoint, lets a
session pin a version, maps Placement error bodies to PlacementException
(PlacementConcurrentUpdateException for generation conflicts) and adds
Placement.retryOnConcurrentUpdate.

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```
공통 PR 절차.

---

### Task 2: Resource providers

**Files:**
- Create (core, `core/src/main/java/org/openstack4j/` 아래): `model/placement/v1/ResourceProvider.java`, `model/placement/v1/ResourceProviderCreate.java`, `model/placement/v1/ResourceProviderUpdate.java`, `model/placement/v1/ResourceProviderListOptions.java`, `api/placement/v1/ResourceProviderService.java`, `openstack/placement/v1/domain/PlacementResourceProvider.java`, `openstack/placement/v1/internal/ResourceProviderServiceImpl.java`
- Modify (core): `api/placement/PlacementService.java`, `openstack/placement/internal/PlacementServiceImpl.java`, `openstack/provider/DefaultAPIProvider.java`
- Create (test): `core-test/src/main/resources/placement/v1/rp_list.json`, `rp_get.json`, `core-test/src/main/java/org/openstack4j/api/placement/v1/ResourceProviderTests.java`

**Interfaces:**
- Consumes: Task 1 의 `BasePlacementV1Service`(`placement()`, `executeOrNull/OrThrow/Action`, `requireMicroVersion`), `AbstractPlacementTest`, `PlacementMicroVersions.V1_32/V1_37/V1_39`.
- Produces: `PlacementService.providers()` → `org.openstack4j.api.placement.v1.ResourceProviderService`; 모델 `org.openstack4j.model.placement.v1.ResourceProvider` (getUuid, getName, getGeneration Long, getParentProviderUuid, getRootProviderUuid). 주의: 기존 `model.placement.ext.ResourceProvider` 와 단순 이름이 같다 — 패키지가 다르므로 import 로 구분한다.

- [ ] **Step 1: fixture 와 테스트 작성**

```bash
git switch main && git pull && git switch -c task/b2-resource-providers
mkdir -p core-test/src/main/resources/placement/v1
```

`core-test/src/main/resources/placement/v1/rp_get.json`:
```json
{
  "uuid": "3626308f-38dd-4da8-8f0f-6697b05d8f6c",
  "name": "compute-1",
  "generation": 557,
  "links": [
    {"rel": "self", "href": "/resource_providers/3626308f-38dd-4da8-8f0f-6697b05d8f6c"},
    {"rel": "inventories", "href": "/resource_providers/3626308f-38dd-4da8-8f0f-6697b05d8f6c/inventories"},
    {"rel": "usages", "href": "/resource_providers/3626308f-38dd-4da8-8f0f-6697b05d8f6c/usages"},
    {"rel": "aggregates", "href": "/resource_providers/3626308f-38dd-4da8-8f0f-6697b05d8f6c/aggregates"},
    {"rel": "traits", "href": "/resource_providers/3626308f-38dd-4da8-8f0f-6697b05d8f6c/traits"},
    {"rel": "allocations", "href": "/resource_providers/3626308f-38dd-4da8-8f0f-6697b05d8f6c/allocations"}
  ],
  "parent_provider_uuid": null,
  "root_provider_uuid": "3626308f-38dd-4da8-8f0f-6697b05d8f6c"
}
```

`core-test/src/main/resources/placement/v1/rp_list.json`:
```json
{
  "resource_providers": [
    {
      "uuid": "3626308f-38dd-4da8-8f0f-6697b05d8f6c",
      "name": "compute-1",
      "generation": 557,
      "links": [{"rel": "self", "href": "/resource_providers/3626308f-38dd-4da8-8f0f-6697b05d8f6c"}],
      "parent_provider_uuid": null,
      "root_provider_uuid": "3626308f-38dd-4da8-8f0f-6697b05d8f6c"
    },
    {
      "uuid": "7b0c4d61-1d6b-4a3e-9d3b-8c1f2e5a9b11",
      "name": "compute-1_NET_AGENT",
      "generation": 3,
      "links": [{"rel": "self", "href": "/resource_providers/7b0c4d61-1d6b-4a3e-9d3b-8c1f2e5a9b11"}],
      "parent_provider_uuid": "3626308f-38dd-4da8-8f0f-6697b05d8f6c",
      "root_provider_uuid": "3626308f-38dd-4da8-8f0f-6697b05d8f6c"
    }
  ]
}
```

`core-test/src/main/java/org/openstack4j/api/placement/v1/ResourceProviderTests.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.placement.v1.exceptions.PlacementConcurrentUpdateException;
import org.openstack4j.api.placement.v1.exceptions.PlacementException;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.ResourceProvider;
import org.openstack4j.model.placement.v1.ResourceProviderCreate;
import org.openstack4j.model.placement.v1.ResourceProviderListOptions;
import org.openstack4j.model.placement.v1.ResourceProviderUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/ResourceProviders")
public class ResourceProviderTests extends AbstractPlacementTest {

    private static final String CHILD = "7b0c4d61-1d6b-4a3e-9d3b-8c1f2e5a9b11";

    public void listSendsVersionHeaderAndParsesTree() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_list.json");

        List<? extends ResourceProvider> providers = osv3().placement().providers().list();
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getMethod(), "GET");
        Assert.assertEquals(request.getPath(), "/resource_providers");
        Assert.assertEquals(request.getHeader("OpenStack-API-Version"), "placement 1.39");
        Assert.assertEquals(providers.size(), 2);
        Assert.assertEquals(providers.get(0).getName(), "compute-1");
        Assert.assertEquals(providers.get(0).getGeneration(), Long.valueOf(557));
        Assert.assertNull(providers.get(0).getParentProviderUuid());
        Assert.assertEquals(providers.get(1).getParentProviderUuid(), RP);
        Assert.assertEquals(providers.get(1).getRootProviderUuid(), RP);
    }

    public void listOptionsBecomeQueryParameters() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_list.json");

        osv3().placement().providers().list(ResourceProviderListOptions.create()
                .name("compute-1")
                .resources("VCPU", 2).resources("MEMORY_MB", 1024)
                .memberOf("agg-1")
                .inTree(RP)
                .required("HW_CPU_X86_AVX2", "!CUSTOM_SLOW"));
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getRequestUrl().queryParameter("name"), "compute-1");
        Assert.assertEquals(request.getRequestUrl().queryParameter("resources"), "VCPU:2,MEMORY_MB:1024");
        Assert.assertEquals(request.getRequestUrl().queryParameter("member_of"), "agg-1");
        Assert.assertEquals(request.getRequestUrl().queryParameter("in_tree"), RP);
        Assert.assertEquals(request.getRequestUrl().queryParameter("required"), "HW_CPU_X86_AVX2,!CUSTOM_SLOW");
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.32.*")
    public void forbiddenAggregateRequires132() throws Exception {
        respondWithVersions("1.31");
        try {
            osv3().placement().providers().list(ResourceProviderListOptions.create().memberOf("!agg-1"));
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.39.*")
    public void traitInSyntaxRequires139() throws Exception {
        respondWithVersions("1.38");
        try {
            osv3().placement().providers().list(ResourceProviderListOptions.create().required("in:A,B"));
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    public void getReturnsProvider() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_get.json");

        ResourceProvider provider = osv3().placement().providers().get(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP);
        Assert.assertEquals(provider.getUuid(), RP);
        Assert.assertEquals(provider.getGeneration(), Long.valueOf(557));
    }

    public void getReturnsNullOn404() throws Exception {
        respondWithVersions("1.39");
        respondWithError(404, "placement.undefined_code");

        Assert.assertNull(osv3().placement().providers().get("00000000-0000-0000-0000-000000000000"));
        takeVersionAndRequest();
    }

    public void createSendsBodyAndReturnsProvider() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_get.json");

        ResourceProvider created = osv3().placement().providers().create(
                ResourceProviderCreate.builder().name("compute-1").uuid(RP).build());
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getMethod(), "POST");
        Assert.assertEquals(request.getPath(), "/resource_providers");
        Assert.assertEquals(body(request).get("name").asText(), "compute-1");
        Assert.assertEquals(body(request).get("uuid").asText(), RP);
        Assert.assertFalse(body(request).has("parent_provider_uuid"));
        Assert.assertEquals(created.getUuid(), RP);
    }

    public void updateRenamesProvider() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_get.json");

        osv3().placement().providers().update(CHILD, ResourceProviderUpdate.builder().name("compute-1").build());
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertEquals(request.getPath(), "/resource_providers/" + CHILD);
        Assert.assertEquals(body(request).get("name").asText(), "compute-1");
        Assert.assertFalse(body(request).has("parent_provider_uuid"));
    }

    public void unparentSendsExplicitNull() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_get.json");

        osv3().placement().providers().update(CHILD, ResourceProviderUpdate.builder().name("x").unparent().build());

        Assert.assertTrue(body(takeVersionAndRequest()).get("parent_provider_uuid").isNull());
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.37.*")
    public void unparentRequires137() throws Exception {
        respondWithVersions("1.36");
        try {
            osv3().placement().providers().update(CHILD, ResourceProviderUpdate.builder().name("x").unparent().build());
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    public void deleteReturnsActionResponse() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        ActionResponse response = osv3().placement().providers().delete(CHILD);

        Assert.assertEquals(takeVersionAndRequest().getMethod(), "DELETE");
        Assert.assertTrue(response.isSuccess());
    }

    public void deleteConflictIsReportedInActionResponse() throws Exception {
        respondWithVersions("1.39");
        respondWithError(409, "placement.resource_provider.inuse");

        ActionResponse response = osv3().placement().providers().delete(RP);

        takeVersionAndRequest();
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(response.getCode(), 409);
    }

    public void concurrentUpdateIsMappedToItsOwnException() throws Exception {
        respondWithVersions("1.39");
        respondWithError(409, "placement.concurrent_update");
        try {
            osv3().placement().providers().update(RP, ResourceProviderUpdate.builder().name("x").build());
            Assert.fail("expected PlacementConcurrentUpdateException");
        } catch (PlacementConcurrentUpdateException e) {
            Assert.assertEquals(e.getErrorCode(), "placement.concurrent_update");
            Assert.assertEquals(e.getStatus(), 409);
            Assert.assertEquals(e.getRequestId(), "req-test");
        }
        takeVersionAndRequest();
    }

    public void otherPlacementErrorsKeepTheirCode() throws Exception {
        respondWithVersions("1.39");
        respondWithError(400, "placement.duplicate_name");
        try {
            osv3().placement().providers().create(ResourceProviderCreate.builder().name("dup").build());
            Assert.fail("expected PlacementException");
        } catch (PlacementConcurrentUpdateException e) {
            Assert.fail("must not be a concurrent update");
        } catch (PlacementException e) {
            Assert.assertEquals(e.getErrorCode(), "placement.duplicate_name");
            Assert.assertEquals(e.getDetail(), "detail for placement.duplicate_name");
        }
        takeVersionAndRequest();
    }

    public void nonPlacementErrorBodyStillMapsStatus() throws Exception {
        respondWithVersions("1.39");
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("Content-Type", "text/html");
        respondWith(headers, 502, "<html><body>Bad Gateway</body></html>");
        try {
            osv3().placement().providers().get(RP);
            Assert.fail("expected PlacementException");
        } catch (PlacementException e) {
            Assert.assertEquals(e.getStatus(), 502);
            Assert.assertNull(e.getErrorCode());
        }
        takeVersionAndRequest();
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core && ./mvnw -B -q compile -pl core-test 2>&1 | grep -E 'cannot find symbol' | head -3`
Expected: `cannot find symbol` (`providers()`, 모델 클래스 없음).

- [ ] **Step 3: 모델 인터페이스와 옵션**

`model/placement/v1/ResourceProvider.java`:
```java
package org.openstack4j.model.placement.v1;

import org.openstack4j.model.ModelEntity;

/** A Placement resource provider (for example a compute node or one of its child providers). */
public interface ResourceProvider extends ModelEntity {

    String getUuid();

    String getName();

    /** Incremented by Placement on every change to the provider's inventories, traits or aggregates. */
    Long getGeneration();

    /** @return the parent provider UUID, or {@code null} for a root provider */
    String getParentProviderUuid();

    String getRootProviderUuid();
}
```

`model/placement/v1/ResourceProviderCreate.java`:
```java
package org.openstack4j.model.placement.v1;

import java.util.Objects;

import org.openstack4j.model.ModelEntity;

/** Request body of {@code POST /resource_providers}. */
public final class ResourceProviderCreate implements ModelEntity {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String uuid;
    private final String parentProviderUuid;

    private ResourceProviderCreate(String name, String uuid, String parentProviderUuid) {
        this.name = Objects.requireNonNull(name, "name");
        this.uuid = uuid;
        this.parentProviderUuid = parentProviderUuid;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    /** @return the UUID to create the provider with, or {@code null} to let Placement choose one */
    public String getUuid() {
        return uuid;
    }

    public String getParentProviderUuid() {
        return parentProviderUuid;
    }

    public static final class Builder {
        private String name;
        private String uuid;
        private String parentProviderUuid;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder parentProviderUuid(String parentProviderUuid) {
            this.parentProviderUuid = parentProviderUuid;
            return this;
        }

        public ResourceProviderCreate build() {
            return new ResourceProviderCreate(name, uuid, parentProviderUuid);
        }
    }
}
```

`model/placement/v1/ResourceProviderUpdate.java`:
```java
package org.openstack4j.model.placement.v1;

import java.util.Objects;

import org.openstack4j.model.ModelEntity;

/** Request body of {@code PUT /resource_providers/{uuid}}. The name is always required by Placement. */
public final class ResourceProviderUpdate implements ModelEntity {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String parentProviderUuid;
    private final boolean unparent;

    private ResourceProviderUpdate(String name, String parentProviderUuid, boolean unparent) {
        this.name = Objects.requireNonNull(name, "name");
        this.parentProviderUuid = parentProviderUuid;
        this.unparent = unparent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    /** @return the new parent, or {@code null} when the parent is left alone (or removed, see {@link #isUnparent()}) */
    public String getParentProviderUuid() {
        return parentProviderUuid;
    }

    /** @return {@code true} to make the provider a root provider again (placement 1.37) */
    public boolean isUnparent() {
        return unparent;
    }

    public static final class Builder {
        private String name;
        private String parentProviderUuid;
        private boolean unparent;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** Re-parents the provider (placement 1.37 when it already has a different parent). */
        public Builder parentProviderUuid(String parentProviderUuid) {
            this.parentProviderUuid = parentProviderUuid;
            this.unparent = false;
            return this;
        }

        /** Removes the parent (placement 1.37). */
        public Builder unparent() {
            this.unparent = true;
            this.parentProviderUuid = null;
            return this;
        }

        public ResourceProviderUpdate build() {
            return new ResourceProviderUpdate(name, parentProviderUuid, unparent);
        }
    }
}
```

`model/placement/v1/ResourceProviderListOptions.java`:
```java
package org.openstack4j.model.placement.v1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Filters for {@code GET /resource_providers}. */
public final class ResourceProviderListOptions {

    private String name;
    private String uuid;
    private String inTree;
    private final List<String> memberOf = new ArrayList<>();
    private final Map<String, Long> resources = new LinkedHashMap<>();
    private final List<String> required = new ArrayList<>();

    private ResourceProviderListOptions() {
    }

    public static ResourceProviderListOptions create() {
        return new ResourceProviderListOptions();
    }

    public ResourceProviderListOptions name(String name) {
        this.name = name;
        return this;
    }

    public ResourceProviderListOptions uuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    /** Providers in the same tree as the given provider. */
    public ResourceProviderListOptions inTree(String providerUuid) {
        this.inTree = providerUuid;
        return this;
    }

    /**
     * Providers in the given aggregate(s). Each call adds one {@code member_of} parameter (AND). A value may be
     * {@code in:a,b} (any of) or start with {@code !} (not in; placement 1.32).
     */
    public ResourceProviderListOptions memberOf(String... aggregateExpressions) {
        for (String expression : aggregateExpressions) memberOf.add(expression);
        return this;
    }

    /** Providers with at least this much free capacity of the resource class. */
    public ResourceProviderListOptions resources(String resourceClass, long amount) {
        resources.put(resourceClass, amount);
        return this;
    }

    /**
     * Required traits. {@code !TRAIT} forbids a trait; {@code in:A,B} requires any of them (placement 1.39).
     * Values given in one call are joined with commas into one {@code required} parameter.
     */
    public ResourceProviderListOptions required(String... traitExpressions) {
        for (String expression : traitExpressions) required.add(expression);
        return this;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public String getInTree() {
        return inTree;
    }

    public List<String> getMemberOf() {
        return memberOf;
    }

    public Map<String, Long> getResources() {
        return resources;
    }

    public List<String> getRequired() {
        return required;
    }

    /** @return {@code VCPU:2,MEMORY_MB:1024}, or {@code null} when no resources were given */
    public String resourcesParameter() {
        return resources.isEmpty() ? null
                : resources.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(","));
    }

    /** @return the {@code required} parameter value, or {@code null} when no traits were given */
    public String requiredParameter() {
        return required.isEmpty() ? null : String.join(",", required);
    }

    public boolean usesForbiddenAggregate() {
        return memberOf.stream().anyMatch(m -> m.startsWith("!"));
    }

    public boolean usesTraitInSyntax() {
        return required.stream().anyMatch(t -> t.startsWith("in:"));
    }
}
```

- [ ] **Step 4: 서비스 인터페이스, Jackson 모델, 구현**

`api/placement/v1/ResourceProviderService.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.ResourceProvider;
import org.openstack4j.model.placement.v1.ResourceProviderCreate;
import org.openstack4j.model.placement.v1.ResourceProviderListOptions;
import org.openstack4j.model.placement.v1.ResourceProviderUpdate;

/** Placement resource providers ({@code /resource_providers}). */
public interface ResourceProviderService extends RestService {

    List<? extends ResourceProvider> list();

    List<? extends ResourceProvider> list(ResourceProviderListOptions options);

    /** @return the provider, or {@code null} when it does not exist */
    ResourceProvider get(String providerUuid);

    ResourceProvider create(ResourceProviderCreate request);

    ResourceProvider update(String providerUuid, ResourceProviderUpdate request);

    /** Fails (409) while the provider still has inventories, allocations or child providers. */
    ActionResponse delete(String providerUuid);
}
```

`openstack/placement/v1/domain/PlacementResourceProvider.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.ResourceProvider;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.util.ToStringHelper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementResourceProvider implements ResourceProvider {

    private static final long serialVersionUID = 1L;

    @JsonProperty("uuid")
    private String uuid;
    @JsonProperty("name")
    private String name;
    @JsonProperty("generation")
    private Long generation;
    @JsonProperty("parent_provider_uuid")
    private String parentProviderUuid;
    @JsonProperty("root_provider_uuid")
    private String rootProviderUuid;

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Long getGeneration() {
        return generation;
    }

    @Override
    public String getParentProviderUuid() {
        return parentProviderUuid;
    }

    @Override
    public String getRootProviderUuid() {
        return rootProviderUuid;
    }

    @Override
    public String toString() {
        return new ToStringHelper(this).add("uuid", uuid).add("name", name).add("generation", generation)
                .add("parentProviderUuid", parentProviderUuid).add("rootProviderUuid", rootProviderUuid).toString();
    }

    public static class ResourceProviders extends ListResult<PlacementResourceProvider> {

        private static final long serialVersionUID = 1L;

        @JsonProperty("resource_providers")
        private List<PlacementResourceProvider> resourceProviders;

        @Override
        protected List<PlacementResourceProvider> value() {
            return resourceProviders;
        }
    }
}
```

`openstack/placement/v1/internal/ResourceProviderServiceImpl.java`:
```java
package org.openstack4j.openstack.placement.v1.internal;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openstack4j.api.placement.v1.ResourceProviderService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.PlacementMicroVersions;
import org.openstack4j.model.placement.v1.ResourceProvider;
import org.openstack4j.model.placement.v1.ResourceProviderCreate;
import org.openstack4j.model.placement.v1.ResourceProviderListOptions;
import org.openstack4j.model.placement.v1.ResourceProviderUpdate;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceProvider;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceProvider.ResourceProviders;

public class ResourceProviderServiceImpl extends BasePlacementV1Service implements ResourceProviderService {

    @Override
    public List<? extends ResourceProvider> list() {
        return list(ResourceProviderListOptions.create());
    }

    @Override
    public List<? extends ResourceProvider> list(ResourceProviderListOptions options) {
        Objects.requireNonNull(options, "options");
        if (options.usesForbiddenAggregate())
            requireMicroVersion("forbidden aggregates (member_of=!...)", PlacementMicroVersions.V1_32);
        if (options.usesTraitInSyntax())
            requireMicroVersion("the trait 'in:' syntax", PlacementMicroVersions.V1_39);
        Invocation<ResourceProviders> invocation = placement(HttpMethod.GET, ResourceProviders.class, "/resource_providers")
                .param("name", options.getName())
                .param("uuid", options.getUuid())
                .param("in_tree", options.getInTree())
                .param("resources", options.resourcesParameter())
                .param("required", options.requiredParameter());
        for (String aggregate : options.getMemberOf())
            invocation.param("member_of", aggregate);
        return executeOrThrow(invocation).getList();
    }

    @Override
    public ResourceProvider get(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeOrNull(placement(HttpMethod.GET, PlacementResourceProvider.class, uri("/resource_providers/%s", providerUuid)));
    }

    @Override
    public ResourceProvider create(ResourceProviderCreate request) {
        Objects.requireNonNull(request, "request");
        ObjectNode body = ObjectMapperSingleton.getContext(Object.class).createObjectNode();
        body.put("name", request.getName());
        if (request.getUuid() != null) body.put("uuid", request.getUuid());
        if (request.getParentProviderUuid() != null) body.put("parent_provider_uuid", request.getParentProviderUuid());
        return executeOrThrow(placement(HttpMethod.POST, PlacementResourceProvider.class, "/resource_providers").json(body.toString()));
    }

    @Override
    public ResourceProvider update(String providerUuid, ResourceProviderUpdate request) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        Objects.requireNonNull(request, "request");
        if (request.isUnparent())
            requireMicroVersion("removing a provider's parent", PlacementMicroVersions.V1_37);
        ObjectNode body = ObjectMapperSingleton.getContext(Object.class).createObjectNode();
        body.put("name", request.getName());
        if (request.isUnparent()) body.putNull("parent_provider_uuid");
        else if (request.getParentProviderUuid() != null) body.put("parent_provider_uuid", request.getParentProviderUuid());
        return executeOrThrow(placement(HttpMethod.PUT, PlacementResourceProvider.class, uri("/resource_providers/%s", providerUuid))
                .json(body.toString()));
    }

    @Override
    public ActionResponse delete(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeAction(placement(HttpMethod.DELETE, ActionResponse.class, uri("/resource_providers/%s", providerUuid)));
    }
}
```
`Invocation.param(name, value)` 은 `value == null` 이면 아무것도 넣지 않는다(`HttpRequest.queryParam` 이 null 을 무시한다).

accessor 와 binding:
- `PlacementService`: `org.openstack4j.api.placement.v1.ResourceProviderService providers();` 추가 (Javadoc: "Placement resource providers with the full v1 API; see also the older {@link #resourceProviders()}").
- `PlacementServiceImpl`: `public org.openstack4j.api.placement.v1.ResourceProviderService providers() { return Apis.get(org.openstack4j.api.placement.v1.ResourceProviderService.class); }`
- `DefaultAPIProvider`: `bind(org.openstack4j.api.placement.v1.ResourceProviderService.class, org.openstack4j.openstack.placement.v1.internal.ResourceProviderServiceImpl.class);`

- [ ] **Step 5: 테스트 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ResourceProviderTests -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Tests run: 15, Failures: 0, Errors: 0`

`nonPlacementErrorBodyStillMapsStatus` 가 `ClientResponseException`(JSON 파싱 실패)으로 실패하면 `PlacementErrors.toException` 의 `catch (RuntimeException)` 이 connector 의 `readEntity` 예외를 잡고 있는지 확인한다(connector 는 파싱 실패를 `ClientResponseException` 으로 던진다 — RuntimeException 이므로 잡힌다).

- [ ] **Step 6: 전체 빌드와 커밋**

Run: `./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'`
Expected: `BUILD SUCCESS`

```bash
git add -A
git commit -m "feat(placement): add resource provider CRUD and list filters (v1)

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```
공통 PR 절차.

---

### Task 3: Inventories

**Files:**
- Create (core): `model/placement/v1/Inventory.java`, `model/placement/v1/ResourceProviderInventories.java`, `api/placement/v1/InventoryService.java`, `openstack/placement/v1/domain/PlacementInventory.java`, `openstack/placement/v1/domain/PlacementResourceProviderInventories.java`, `openstack/placement/v1/internal/InventoryServiceImpl.java`
- Modify (core): `PlacementService`, `PlacementServiceImpl`, `DefaultAPIProvider` (accessor `inventories()` + binding)
- Create (test): `core-test/src/main/resources/placement/v1/inventories.json`, `inventory_vcpu.json`, `core-test/src/main/java/org/openstack4j/api/placement/v1/InventoryTests.java`

**Interfaces:**
- Produces: `Inventory` (getTotal long, getReserved Long, getMinUnit Long, getMaxUnit Long, getStepSize Long, getAllocationRatio Float, getResourceProviderGeneration Long; `Inventory.builder()`), `ResourceProviderInventories` (getResourceProviderGeneration long, `Map<String, ? extends Inventory> getInventories()`). Task 7(capacity)와 Task 10(reshaper)이 `Inventory` 를 쓴다.

- [ ] **Step 1: fixture 와 테스트**

```bash
git switch main && git pull && git switch -c task/b3-inventories
```

`core-test/src/main/resources/placement/v1/inventories.json`:
```json
{
  "resource_provider_generation": 557,
  "inventories": {
    "VCPU": {"total": 12, "reserved": 0, "min_unit": 1, "max_unit": 12, "step_size": 1, "allocation_ratio": 4.0},
    "MEMORY_MB": {"total": 15584, "reserved": 512, "min_unit": 1, "max_unit": 15584, "step_size": 1, "allocation_ratio": 1.0},
    "DISK_GB": {"total": 467, "reserved": 0, "min_unit": 1, "max_unit": 467, "step_size": 1, "allocation_ratio": 1.0}
  }
}
```

`core-test/src/main/resources/placement/v1/inventory_vcpu.json`:
```json
{"total": 12, "reserved": 0, "min_unit": 1, "max_unit": 12, "step_size": 1, "allocation_ratio": 4.0, "resource_provider_generation": 557}
```

`InventoryTests.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.ResourceProviderInventories;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/Inventories")
public class InventoryTests extends AbstractPlacementTest {

    public void listParsesAllResourceClasses() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/inventories.json");

        ResourceProviderInventories inventories = osv3().placement().inventories().list(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/inventories");
        Assert.assertEquals(inventories.getResourceProviderGeneration(), 557L);
        Assert.assertEquals(inventories.getInventories().keySet(), new java.util.HashSet<>(java.util.Arrays.asList("VCPU", "MEMORY_MB", "DISK_GB")));
        Inventory memory = inventories.getInventories().get("MEMORY_MB");
        Assert.assertEquals(memory.getTotal(), 15584L);
        Assert.assertEquals(memory.getReserved(), Long.valueOf(512));
        Assert.assertEquals(memory.getAllocationRatio(), Float.valueOf(1.0f));
    }

    public void getReturnsSingleInventory() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/inventory_vcpu.json");

        Inventory vcpu = osv3().placement().inventories().get(RP, "VCPU");

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/inventories/VCPU");
        Assert.assertEquals(vcpu.getTotal(), 12L);
        Assert.assertEquals(vcpu.getMaxUnit(), Long.valueOf(12));
        Assert.assertEquals(vcpu.getResourceProviderGeneration(), Long.valueOf(557));
    }

    public void getReturnsNullForUnknownResourceClass() throws Exception {
        respondWithVersions("1.39");
        respondWithError(404, "placement.undefined_code");

        Assert.assertNull(osv3().placement().inventories().get(RP, "CUSTOM_NOPE"));
        takeVersionAndRequest();
    }

    public void replaceSendsZeroGeneration() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/inventories.json");

        osv3().placement().inventories().replace(RP, 0L,
                Collections.singletonMap("VCPU", Inventory.builder().total(12).allocationRatio(4.0f).build()));
        RecordedRequest request = takeVersionAndRequest();
        JsonNode body = body(request);

        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertEquals(request.getPath(), "/resource_providers/" + RP + "/inventories");
        Assert.assertEquals(body.get("resource_provider_generation").asLong(), 0L);
        Assert.assertEquals(body.get("inventories").get("VCPU").get("total").asLong(), 12L);
        Assert.assertEquals(body.get("inventories").get("VCPU").get("allocation_ratio").asDouble(), 4.0, 0.0001);
        Assert.assertFalse(body.get("inventories").get("VCPU").has("reserved"), "unset fields must be omitted");
    }

    public void createPostsInventoryWithResourceClass() throws Exception {
        respondWithVersions("1.39");
        respondWith(201, "{\"total\": 8, \"reserved\": 0, \"min_unit\": 1, \"max_unit\": 8, \"step_size\": 1, \"allocation_ratio\": 1.0, \"resource_provider_generation\": 558}");

        Inventory created = osv3().placement().inventories().create(RP, 557L, "CUSTOM_GPU",
                Inventory.builder().total(8).maxUnit(8).build());
        RecordedRequest request = takeVersionAndRequest();
        JsonNode body = body(request);

        Assert.assertEquals(request.getMethod(), "POST");
        Assert.assertEquals(request.getPath(), "/resource_providers/" + RP + "/inventories");
        Assert.assertEquals(body.get("resource_class").asText(), "CUSTOM_GPU");
        Assert.assertEquals(body.get("resource_provider_generation").asLong(), 557L);
        Assert.assertEquals(body.get("total").asLong(), 8L);
        Assert.assertEquals(created.getResourceProviderGeneration(), Long.valueOf(558));
    }

    public void updatePutsSingleInventory() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/inventory_vcpu.json");

        osv3().placement().inventories().update(RP, 557L, "VCPU", Inventory.builder().total(12).reserved(2).build());
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertEquals(request.getPath(), "/resource_providers/" + RP + "/inventories/VCPU");
        Assert.assertEquals(body(request).get("reserved").asLong(), 2L);
        Assert.assertFalse(body(request).has("resource_class"));
    }

    public void deleteOneAndAll() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);
        respondWith(204);

        ActionResponse one = osv3().placement().inventories().delete(RP, "VCPU");
        ActionResponse all = osv3().placement().inventories().deleteAll(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/inventories/VCPU");
        Assert.assertEquals(takeRequest().getPath(), "/resource_providers/" + RP + "/inventories");
        Assert.assertTrue(one.isSuccess());
        Assert.assertTrue(all.isSuccess());
    }

    public void deleteWhileInUseIsReported() throws Exception {
        respondWithVersions("1.39");
        respondWithError(409, "placement.inventory.inuse");

        ActionResponse response = osv3().placement().inventories().delete(RP, "VCPU");

        takeVersionAndRequest();
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(response.getCode(), 409);
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core && ./mvnw -B -q compile -pl core-test 2>&1 | grep -E 'cannot find symbol' | head -2`
Expected: `cannot find symbol` (`inventories()`).

- [ ] **Step 3: 모델**

`model/placement/v1/Inventory.java`:
```java
package org.openstack4j.model.placement.v1;

import org.openstack4j.model.ModelEntity;

/**
 * Inventory of one resource class on a resource provider. Capacity available for allocation is
 * {@code (total - reserved) * allocationRatio}.
 */
public interface Inventory extends ModelEntity {

    long getTotal();

    Long getReserved();

    Long getMinUnit();

    Long getMaxUnit();

    Long getStepSize();

    Float getAllocationRatio();

    /** @return the provider generation after the change, present on responses only */
    Long getResourceProviderGeneration();

    static Builder builder() {
        return new org.openstack4j.openstack.placement.v1.domain.PlacementInventory.Builder();
    }

    interface Builder {
        Builder total(long total);

        Builder reserved(long reserved);

        Builder minUnit(long minUnit);

        Builder maxUnit(long maxUnit);

        Builder stepSize(long stepSize);

        Builder allocationRatio(float allocationRatio);

        Inventory build();
    }
}
```

`model/placement/v1/ResourceProviderInventories.java`:
```java
package org.openstack4j.model.placement.v1;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** All inventories of a resource provider together with the provider generation they belong to. */
public interface ResourceProviderInventories extends ModelEntity {

    long getResourceProviderGeneration();

    /** @return inventories keyed by resource class name */
    Map<String, ? extends Inventory> getInventories();
}
```

`openstack/placement/v1/domain/PlacementInventory.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.util.ToStringHelper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlacementInventory implements Inventory {

    private static final long serialVersionUID = 1L;

    @JsonProperty("total")
    private long total;
    @JsonProperty("reserved")
    private Long reserved;
    @JsonProperty("min_unit")
    private Long minUnit;
    @JsonProperty("max_unit")
    private Long maxUnit;
    @JsonProperty("step_size")
    private Long stepSize;
    @JsonProperty("allocation_ratio")
    private Float allocationRatio;
    @JsonProperty("resource_provider_generation")
    private Long resourceProviderGeneration;

    @Override
    public long getTotal() {
        return total;
    }

    @Override
    public Long getReserved() {
        return reserved;
    }

    @Override
    public Long getMinUnit() {
        return minUnit;
    }

    @Override
    public Long getMaxUnit() {
        return maxUnit;
    }

    @Override
    public Long getStepSize() {
        return stepSize;
    }

    @Override
    public Float getAllocationRatio() {
        return allocationRatio;
    }

    @Override
    public Long getResourceProviderGeneration() {
        return resourceProviderGeneration;
    }

    /** Copy without the generation, for use inside request bodies. */
    public PlacementInventory withoutGeneration() {
        PlacementInventory copy = new PlacementInventory();
        copy.total = total;
        copy.reserved = reserved;
        copy.minUnit = minUnit;
        copy.maxUnit = maxUnit;
        copy.stepSize = stepSize;
        copy.allocationRatio = allocationRatio;
        return copy;
    }

    public static PlacementInventory from(Inventory inventory) {
        if (inventory instanceof PlacementInventory)
            return ((PlacementInventory) inventory).withoutGeneration();
        PlacementInventory copy = new PlacementInventory();
        copy.total = inventory.getTotal();
        copy.reserved = inventory.getReserved();
        copy.minUnit = inventory.getMinUnit();
        copy.maxUnit = inventory.getMaxUnit();
        copy.stepSize = inventory.getStepSize();
        copy.allocationRatio = inventory.getAllocationRatio();
        return copy;
    }

    @Override
    public String toString() {
        return new ToStringHelper(this).add("total", total).add("reserved", reserved).add("minUnit", minUnit)
                .add("maxUnit", maxUnit).add("stepSize", stepSize).add("allocationRatio", allocationRatio)
                .add("resourceProviderGeneration", resourceProviderGeneration).toString();
    }

    public static class Builder implements Inventory.Builder {
        private final PlacementInventory inventory = new PlacementInventory();

        @Override
        public Builder total(long total) {
            inventory.total = total;
            return this;
        }

        @Override
        public Builder reserved(long reserved) {
            inventory.reserved = reserved;
            return this;
        }

        @Override
        public Builder minUnit(long minUnit) {
            inventory.minUnit = minUnit;
            return this;
        }

        @Override
        public Builder maxUnit(long maxUnit) {
            inventory.maxUnit = maxUnit;
            return this;
        }

        @Override
        public Builder stepSize(long stepSize) {
            inventory.stepSize = stepSize;
            return this;
        }

        @Override
        public Builder allocationRatio(float allocationRatio) {
            inventory.allocationRatio = allocationRatio;
            return this;
        }

        @Override
        public Inventory build() {
            return inventory;
        }
    }
}
```

`openstack/placement/v1/domain/PlacementResourceProviderInventories.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.ResourceProviderInventories;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementResourceProviderInventories implements ResourceProviderInventories {

    private static final long serialVersionUID = 1L;

    @JsonProperty("resource_provider_generation")
    private long resourceProviderGeneration;
    @JsonProperty("inventories")
    private Map<String, PlacementInventory> inventories;

    public PlacementResourceProviderInventories() {
    }

    public PlacementResourceProviderInventories(long resourceProviderGeneration, Map<String, PlacementInventory> inventories) {
        this.resourceProviderGeneration = resourceProviderGeneration;
        this.inventories = inventories;
    }

    @Override
    public long getResourceProviderGeneration() {
        return resourceProviderGeneration;
    }

    @Override
    public Map<String, ? extends Inventory> getInventories() {
        return inventories;
    }
}
```

- [ ] **Step 4: 서비스 인터페이스와 구현**

`api/placement/v1/InventoryService.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.ResourceProviderInventories;

/**
 * Inventories of a resource provider ({@code /resource_providers/{uuid}/inventories}). Writes take the provider
 * generation read beforehand; a stale generation fails with
 * {@link org.openstack4j.api.placement.v1.exceptions.PlacementConcurrentUpdateException}.
 */
public interface InventoryService extends RestService {

    ResourceProviderInventories list(String providerUuid);

    /** @return the inventory, or {@code null} when the provider has none for the resource class */
    Inventory get(String providerUuid, String resourceClass);

    /** Replaces every inventory of the provider. An empty map removes them all. */
    ResourceProviderInventories replace(String providerUuid, long generation, Map<String, ? extends Inventory> inventories);

    Inventory create(String providerUuid, long generation, String resourceClass, Inventory inventory);

    Inventory update(String providerUuid, long generation, String resourceClass, Inventory inventory);

    /** Fails (409) while allocations exist against the inventory. */
    ActionResponse delete(String providerUuid, String resourceClass);

    ActionResponse deleteAll(String providerUuid);
}
```

`openstack/placement/v1/internal/InventoryServiceImpl.java`:
```java
package org.openstack4j.openstack.placement.v1.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openstack4j.api.placement.v1.InventoryService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.ResourceProviderInventories;
import org.openstack4j.openstack.placement.v1.domain.PlacementInventory;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceProviderInventories;

public class InventoryServiceImpl extends BasePlacementV1Service implements InventoryService {

    private static final ObjectMapper MAPPER = ObjectMapperSingleton.getContext(Object.class);

    @Override
    public ResourceProviderInventories list(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeOrThrow(placement(HttpMethod.GET, PlacementResourceProviderInventories.class,
                uri("/resource_providers/%s/inventories", providerUuid)));
    }

    @Override
    public Inventory get(String providerUuid, String resourceClass) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        requireName("resource class", resourceClass);
        return executeOrNull(placement(HttpMethod.GET, PlacementInventory.class,
                uri("/resource_providers/%s/inventories/%s", providerUuid, resourceClass)));
    }

    @Override
    public ResourceProviderInventories replace(String providerUuid, long generation, Map<String, ? extends Inventory> inventories) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        Objects.requireNonNull(inventories, "inventories");
        Map<String, PlacementInventory> body = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends Inventory> entry : inventories.entrySet())
            body.put(requireName("resource class", entry.getKey()), PlacementInventory.from(entry.getValue()));
        return executeOrThrow(placement(HttpMethod.PUT, PlacementResourceProviderInventories.class,
                uri("/resource_providers/%s/inventories", providerUuid))
                .entity(new PlacementResourceProviderInventories(generation, body)));
    }

    @Override
    public Inventory create(String providerUuid, long generation, String resourceClass, Inventory inventory) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        requireName("resource class", resourceClass);
        ObjectNode body = inventoryBody(generation, inventory);
        body.put("resource_class", resourceClass);
        return executeOrThrow(placement(HttpMethod.POST, PlacementInventory.class,
                uri("/resource_providers/%s/inventories", providerUuid)).json(body.toString()));
    }

    @Override
    public Inventory update(String providerUuid, long generation, String resourceClass, Inventory inventory) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        requireName("resource class", resourceClass);
        return executeOrThrow(placement(HttpMethod.PUT, PlacementInventory.class,
                uri("/resource_providers/%s/inventories/%s", providerUuid, resourceClass))
                .json(inventoryBody(generation, inventory).toString()));
    }

    @Override
    public ActionResponse delete(String providerUuid, String resourceClass) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        requireName("resource class", resourceClass);
        return executeAction(placement(HttpMethod.DELETE, ActionResponse.class,
                uri("/resource_providers/%s/inventories/%s", providerUuid, resourceClass)));
    }

    @Override
    public ActionResponse deleteAll(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeAction(placement(HttpMethod.DELETE, ActionResponse.class,
                uri("/resource_providers/%s/inventories", providerUuid)));
    }

    private static ObjectNode inventoryBody(long generation, Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        ObjectNode body = MAPPER.valueToTree(PlacementInventory.from(inventory));
        body.put("resource_provider_generation", generation);
        return body;
    }
}
```
accessor `inventories()` 와 binding 을 Task 2 와 같은 방식으로 추가한다.

- [ ] **Step 5: 테스트 통과 확인**

Run: 공통 테스트 명령에 `-Dtest=InventoryTests`
Expected: `Tests run: 8, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
git add -A
git commit -m "feat(placement): add inventory read/write API (v1)

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 4: Resource classes

**Files:**
- Create (core): `model/placement/v1/ResourceClasses.java`, `api/placement/v1/ResourceClassService.java`, `openstack/placement/v1/domain/PlacementResourceClass.java`, `openstack/placement/v1/internal/ResourceClassServiceImpl.java`
- Modify (core): accessor `resourceClasses()` + binding
- Create (test): `core-test/src/main/resources/placement/v1/resource_classes.json`, `core-test/src/main/java/org/openstack4j/api/placement/v1/ResourceClassTests.java`

**Interfaces:**
- Produces: `ResourceClassService` (`List<String> list()`, `boolean exists(String)`, `void create(String)`, `void ensure(String)`, `ActionResponse delete(String)`), 상수 `ResourceClasses.VCPU` 등.

- [ ] **Step 1: fixture 와 테스트**

```bash
git switch main && git pull && git switch -c task/b4-resource-classes
```

`core-test/src/main/resources/placement/v1/resource_classes.json`:
```json
{
  "resource_classes": [
    {"name": "VCPU", "links": [{"rel": "self", "href": "/resource_classes/VCPU"}]},
    {"name": "MEMORY_MB", "links": [{"rel": "self", "href": "/resource_classes/MEMORY_MB"}]},
    {"name": "DISK_GB", "links": [{"rel": "self", "href": "/resource_classes/DISK_GB"}]},
    {"name": "CUSTOM_GPU", "links": [{"rel": "self", "href": "/resource_classes/CUSTOM_GPU"}]}
  ]
}
```

`ResourceClassTests.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.Arrays;
import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.ResourceClasses;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/ResourceClasses")
public class ResourceClassTests extends AbstractPlacementTest {

    public void listReturnsNames() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/resource_classes.json");

        List<String> names = osv3().placement().resourceClasses().list();

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_classes");
        Assert.assertEquals(names, Arrays.asList("VCPU", "MEMORY_MB", "DISK_GB", "CUSTOM_GPU"));
        Assert.assertTrue(names.contains(ResourceClasses.VCPU));
    }

    public void existsIsTrueOn200AndFalseOn404() throws Exception {
        respondWithVersions("1.39");
        respondWith(200, "{\"name\": \"CUSTOM_GPU\", \"links\": []}");
        respondWithError(404, "placement.undefined_code");

        boolean gpu = osv3().placement().resourceClasses().exists("CUSTOM_GPU");
        boolean nope = osv3().placement().resourceClasses().exists("CUSTOM_NOPE");

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_classes/CUSTOM_GPU");
        takeRequest();
        Assert.assertTrue(gpu);
        Assert.assertFalse(nope);
    }

    public void createPostsName() throws Exception {
        respondWithVersions("1.39");
        respondWith(201);

        osv3().placement().resourceClasses().create("CUSTOM_GPU");
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getMethod(), "POST");
        Assert.assertEquals(request.getPath(), "/resource_classes");
        Assert.assertEquals(body(request).get("name").asText(), "CUSTOM_GPU");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createRequiresCustomPrefix() throws Exception {
        respondWithVersions("1.39");
        osv3().placement().resourceClasses().create("GPU");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsInvalidName() throws Exception {
        respondWithVersions("1.39");
        osv3().placement().resourceClasses().exists("CUSTOM_A/B");
    }

    public void ensurePutsNameAndAcceptsBothStatuses() throws Exception {
        respondWithVersions("1.39");
        respondWith(201);
        respondWith(204);

        osv3().placement().resourceClasses().ensure("CUSTOM_GPU");
        osv3().placement().resourceClasses().ensure("CUSTOM_GPU");

        RecordedRequest first = takeVersionAndRequest();
        Assert.assertEquals(first.getMethod(), "PUT");
        Assert.assertEquals(first.getPath(), "/resource_classes/CUSTOM_GPU");
        Assert.assertEquals(takeRequest().getMethod(), "PUT");
    }

    public void deleteReturnsActionResponse() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        ActionResponse response = osv3().placement().resourceClasses().delete("CUSTOM_GPU");

        Assert.assertEquals(takeVersionAndRequest().getMethod(), "DELETE");
        Assert.assertTrue(response.isSuccess());
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인** (Task 3 Step 2 와 같은 명령, `resourceClasses()` 없음)

- [ ] **Step 3: 구현**

`model/placement/v1/ResourceClasses.java`:
```java
package org.openstack4j.model.placement.v1;

/** Standard Placement resource class names. Custom classes start with {@link #CUSTOM_PREFIX}. */
public final class ResourceClasses {

    public static final String VCPU = "VCPU";
    public static final String PCPU = "PCPU";
    public static final String MEMORY_MB = "MEMORY_MB";
    public static final String DISK_GB = "DISK_GB";
    public static final String PCI_DEVICE = "PCI_DEVICE";
    public static final String SRIOV_NET_VF = "SRIOV_NET_VF";
    public static final String NUMA_SOCKET = "NUMA_SOCKET";
    public static final String NUMA_CORE = "NUMA_CORE";
    public static final String NUMA_THREAD = "NUMA_THREAD";
    public static final String NUMA_MEMORY_MB = "NUMA_MEMORY_MB";
    public static final String IPV4_ADDRESS = "IPV4_ADDRESS";
    public static final String VGPU = "VGPU";
    public static final String VGPU_DISPLAY_HEAD = "VGPU_DISPLAY_HEAD";
    public static final String NET_BW_EGR_KILOBIT_PER_SEC = "NET_BW_EGR_KILOBIT_PER_SEC";
    public static final String NET_BW_IGR_KILOBIT_PER_SEC = "NET_BW_IGR_KILOBIT_PER_SEC";
    public static final String NET_PACKET_RATE_KILOPACKET_PER_SEC = "NET_PACKET_RATE_KILOPACKET_PER_SEC";
    public static final String NET_PACKET_RATE_EGR_KILOPACKET_PER_SEC = "NET_PACKET_RATE_EGR_KILOPACKET_PER_SEC";
    public static final String NET_PACKET_RATE_IGR_KILOPACKET_PER_SEC = "NET_PACKET_RATE_IGR_KILOPACKET_PER_SEC";
    public static final String MEM_ENCRYPTION_CONTEXT = "MEM_ENCRYPTION_CONTEXT";
    public static final String FPGA = "FPGA";
    public static final String PGPU = "PGPU";

    public static final String CUSTOM_PREFIX = "CUSTOM_";

    private ResourceClasses() {
    }

    public static boolean isCustom(String name) {
        return name != null && name.startsWith(CUSTOM_PREFIX);
    }
}
```

`api/placement/v1/ResourceClassService.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/** Resource classes ({@code /resource_classes}). Only {@code CUSTOM_*} classes can be created or deleted. */
public interface ResourceClassService extends RestService {

    List<String> list();

    boolean exists(String name);

    /** Creates a custom resource class; fails (409) when it exists. */
    void create(String name);

    /** Creates the custom resource class if missing (idempotent). */
    void ensure(String name);

    /** Fails (409) while inventories use the class. */
    ActionResponse delete(String name);
}
```

`openstack/placement/v1/domain/PlacementResourceClass.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementResourceClass implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("name")
    private String name;

    public String getName() {
        return name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceClasses implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("resource_classes")
        private List<PlacementResourceClass> resourceClasses;

        public List<String> names() {
            return resourceClasses == null ? java.util.Collections.emptyList()
                    : resourceClasses.stream().map(PlacementResourceClass::getName).collect(Collectors.toList());
        }
    }
}
```

`openstack/placement/v1/internal/ResourceClassServiceImpl.java`:
```java
package org.openstack4j.openstack.placement.v1.internal;

import java.util.List;

import org.openstack4j.api.placement.v1.ResourceClassService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.ResourceClasses;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceClass;

public class ResourceClassServiceImpl extends BasePlacementV1Service implements ResourceClassService {

    @Override
    public List<String> list() {
        return executeOrThrow(placement(HttpMethod.GET, PlacementResourceClass.ResourceClasses.class, "/resource_classes")).names();
    }

    @Override
    public boolean exists(String name) {
        requireName("resource class", name);
        return executeOrNull(placement(HttpMethod.GET, PlacementResourceClass.class, uri("/resource_classes/%s", name))) != null;
    }

    @Override
    public void create(String name) {
        requireCustom(name);
        executeOrThrow(placement(HttpMethod.POST, Void.class, "/resource_classes").json("{\"name\":\"" + name + "\"}"));
    }

    @Override
    public void ensure(String name) {
        requireCustom(name);
        executeOrThrow(placement(HttpMethod.PUT, Void.class, uri("/resource_classes/%s", name)));
    }

    @Override
    public ActionResponse delete(String name) {
        requireCustom(name);
        return executeAction(placement(HttpMethod.DELETE, ActionResponse.class, uri("/resource_classes/%s", name)));
    }

    private static void requireCustom(String name) {
        requireName("resource class", name);
        if (!ResourceClasses.isCustom(name))
            throw new IllegalArgumentException("Only custom resource classes (CUSTOM_*) can be created or deleted: " + name);
    }
}
```
accessor `resourceClasses()` 와 binding 추가.

- [ ] **Step 4: 테스트 통과 확인** — `-Dtest=ResourceClassTests`, Expected `Tests run: 7, Failures: 0, Errors: 0`

- [ ] **Step 5: 전체 빌드, 커밋, PR** — `feat(placement): add resource class API (v1)`

---

### Task 5: Traits

**Files:**
- Create (core): `model/placement/v1/TraitListOptions.java`, `model/placement/v1/ResourceProviderTraits.java`, `api/placement/v1/TraitService.java`, `openstack/placement/v1/domain/PlacementTraits.java`, `openstack/placement/v1/domain/PlacementResourceProviderTraits.java`, `openstack/placement/v1/internal/TraitServiceImpl.java`
- Modify (core): accessor `traits()` + binding
- Create (test): `core-test/src/main/resources/placement/v1/rp_traits.json`, `TraitTests.java`

**Interfaces:**
- Produces: `TraitService` (`List<String> list()`, `list(TraitListOptions)`, `boolean exists(String)`, `void create(String)`, `ActionResponse delete(String)`, `ResourceProviderTraits listForProvider(String rp)`, `replaceForProvider(String rp, long gen, Collection<String>)`, `ActionResponse deleteForProvider(String rp)`), `ResourceProviderTraits` (getResourceProviderGeneration long, `List<String> getTraits()`).

- [ ] **Step 1: fixture 와 테스트**

```bash
git switch main && git pull && git switch -c task/b5-traits
```

`core-test/src/main/resources/placement/v1/rp_traits.json`:
```json
{"resource_provider_generation": 557, "traits": ["HW_CPU_X86_AVX2", "COMPUTE_NET_ATTACH_INTERFACE", "CUSTOM_GOLD"]}
```

`TraitTests.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.Arrays;
import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.ResourceProviderTraits;
import org.openstack4j.model.placement.v1.TraitListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/Traits")
public class TraitTests extends AbstractPlacementTest {

    public void listWithFilters() throws Exception {
        respondWithVersions("1.39");
        respondWith(200, "{\"traits\": [\"HW_CPU_X86_AVX\", \"HW_CPU_X86_AVX2\"]}");

        List<String> traits = osv3().placement().traits().list(TraitListOptions.create().nameStartsWith("HW_CPU_X86_AVX").associated(true));
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getRequestUrl().encodedPath(), "/traits");
        Assert.assertEquals(request.getRequestUrl().queryParameter("name"), "startswith:HW_CPU_X86_AVX");
        Assert.assertEquals(request.getRequestUrl().queryParameter("associated"), "true");
        Assert.assertEquals(traits, Arrays.asList("HW_CPU_X86_AVX", "HW_CPU_X86_AVX2"));
    }

    public void existsUses204And404() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);
        respondWithError(404, "placement.undefined_code");

        Assert.assertTrue(osv3().placement().traits().exists("HW_CPU_X86_AVX"));
        Assert.assertFalse(osv3().placement().traits().exists("CUSTOM_NOPE"));
        Assert.assertEquals(takeVersionAndRequest().getPath(), "/traits/HW_CPU_X86_AVX");
        takeRequest();
    }

    public void createPutsTrait() throws Exception {
        respondWithVersions("1.39");
        respondWith(201);

        osv3().placement().traits().create("CUSTOM_GOLD");
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertEquals(request.getPath(), "/traits/CUSTOM_GOLD");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createRequiresCustomPrefix() throws Exception {
        respondWithVersions("1.39");
        osv3().placement().traits().create("GOLD");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsInvalidName() throws Exception {
        respondWithVersions("1.39");
        osv3().placement().traits().exists("CUSTOM_A/B");
    }

    public void deleteTrait() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        ActionResponse response = osv3().placement().traits().delete("CUSTOM_GOLD");

        Assert.assertEquals(takeVersionAndRequest().getMethod(), "DELETE");
        Assert.assertTrue(response.isSuccess());
    }

    public void providerTraitsRoundTrip() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_traits.json");
        respondWith("/placement/v1/rp_traits.json");
        respondWith(204);

        ResourceProviderTraits current = osv3().placement().traits().listForProvider(RP);
        ResourceProviderTraits replaced = osv3().placement().traits().replaceForProvider(RP, current.getResourceProviderGeneration(),
                Arrays.asList("HW_CPU_X86_AVX2", "CUSTOM_GOLD"));
        ActionResponse cleared = osv3().placement().traits().deleteForProvider(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/traits");
        RecordedRequest put = takeRequest();
        Assert.assertEquals(put.getMethod(), "PUT");
        Assert.assertEquals(body(put).get("resource_provider_generation").asLong(), 557L);
        Assert.assertEquals(body(put).get("traits").size(), 2);
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(current.getTraits().size(), 3);
        Assert.assertEquals(replaced.getResourceProviderGeneration(), 557L);
        Assert.assertTrue(cleared.isSuccess());
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

- [ ] **Step 3: 구현**

`model/placement/v1/TraitListOptions.java`:
```java
package org.openstack4j.model.placement.v1;

/** Filters for {@code GET /traits}. */
public final class TraitListOptions {

    private String name;
    private Boolean associated;

    private TraitListOptions() {
    }

    public static TraitListOptions create() {
        return new TraitListOptions();
    }

    /** Raw {@code name} filter, for example {@code startswith:HW_} or {@code in:A,B}. */
    public TraitListOptions name(String filter) {
        this.name = filter;
        return this;
    }

    public TraitListOptions nameStartsWith(String prefix) {
        return name("startswith:" + prefix);
    }

    public TraitListOptions nameIn(String... traits) {
        return name("in:" + String.join(",", traits));
    }

    /** Only traits that are associated with at least one resource provider. */
    public TraitListOptions associated(boolean associated) {
        this.associated = associated;
        return this;
    }

    public String getName() {
        return name;
    }

    public Boolean getAssociated() {
        return associated;
    }
}
```

`model/placement/v1/ResourceProviderTraits.java`:
```java
package org.openstack4j.model.placement.v1;

import java.util.List;

import org.openstack4j.model.ModelEntity;

public interface ResourceProviderTraits extends ModelEntity {

    long getResourceProviderGeneration();

    List<String> getTraits();
}
```

`openstack/placement/v1/domain/PlacementTraits.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** {@code {"traits": [...]}} */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementTraits implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("traits")
    private List<String> traits;

    public List<String> getTraits() {
        return traits == null ? Collections.emptyList() : traits;
    }
}
```

`openstack/placement/v1/domain/PlacementResourceProviderTraits.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.ResourceProviderTraits;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementResourceProviderTraits implements ResourceProviderTraits {

    private static final long serialVersionUID = 1L;

    @JsonProperty("resource_provider_generation")
    private long resourceProviderGeneration;
    @JsonProperty("traits")
    private List<String> traits;

    public PlacementResourceProviderTraits() {
    }

    public PlacementResourceProviderTraits(long resourceProviderGeneration, Collection<String> traits) {
        this.resourceProviderGeneration = resourceProviderGeneration;
        this.traits = new ArrayList<>(traits);
    }

    @Override
    public long getResourceProviderGeneration() {
        return resourceProviderGeneration;
    }

    @Override
    public List<String> getTraits() {
        return traits == null ? Collections.emptyList() : traits;
    }
}
```

`api/placement/v1/TraitService.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.Collection;
import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.ResourceProviderTraits;
import org.openstack4j.model.placement.v1.TraitListOptions;

/** Traits ({@code /traits}) and the traits of a resource provider ({@code /resource_providers/{uuid}/traits}). */
public interface TraitService extends RestService {

    List<String> list();

    List<String> list(TraitListOptions options);

    boolean exists(String trait);

    /** Creates a custom trait ({@code CUSTOM_*}); idempotent. */
    void create(String trait);

    /** Fails (409) while a provider has the trait. */
    ActionResponse delete(String trait);

    ResourceProviderTraits listForProvider(String providerUuid);

    /** Replaces the provider's traits; traits must already exist. */
    ResourceProviderTraits replaceForProvider(String providerUuid, long generation, Collection<String> traits);

    ActionResponse deleteForProvider(String providerUuid);
}
```

`openstack/placement/v1/internal/TraitServiceImpl.java`:
```java
package org.openstack4j.openstack.placement.v1.internal;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.openstack4j.api.placement.v1.TraitService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.ResourceProviderTraits;
import org.openstack4j.model.placement.v1.TraitListOptions;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceProviderTraits;
import org.openstack4j.openstack.placement.v1.domain.PlacementTraits;

public class TraitServiceImpl extends BasePlacementV1Service implements TraitService {

    private static final String CUSTOM_PREFIX = "CUSTOM_";

    @Override
    public List<String> list() {
        return list(TraitListOptions.create());
    }

    @Override
    public List<String> list(TraitListOptions options) {
        Objects.requireNonNull(options, "options");
        return executeOrThrow(placement(HttpMethod.GET, PlacementTraits.class, "/traits")
                .param("name", options.getName())
                .param("associated", options.getAssociated())).getTraits();
    }

    @Override
    public boolean exists(String trait) {
        requireName("trait", trait);
        return executeAction(placement(HttpMethod.GET, ActionResponse.class, uri("/traits/%s", trait))).isSuccess();
    }

    @Override
    public void create(String trait) {
        requireCustom(trait);
        executeOrThrow(placement(HttpMethod.PUT, Void.class, uri("/traits/%s", trait)));
    }

    @Override
    public ActionResponse delete(String trait) {
        requireCustom(trait);
        return executeAction(placement(HttpMethod.DELETE, ActionResponse.class, uri("/traits/%s", trait)));
    }

    @Override
    public ResourceProviderTraits listForProvider(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeOrThrow(placement(HttpMethod.GET, PlacementResourceProviderTraits.class, uri("/resource_providers/%s/traits", providerUuid)));
    }

    @Override
    public ResourceProviderTraits replaceForProvider(String providerUuid, long generation, Collection<String> traits) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        Objects.requireNonNull(traits, "traits");
        for (String trait : traits) requireName("trait", trait);
        return executeOrThrow(placement(HttpMethod.PUT, PlacementResourceProviderTraits.class, uri("/resource_providers/%s/traits", providerUuid))
                .entity(new PlacementResourceProviderTraits(generation, traits)));
    }

    @Override
    public ActionResponse deleteForProvider(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeAction(placement(HttpMethod.DELETE, ActionResponse.class, uri("/resource_providers/%s/traits", providerUuid)));
    }

    private static void requireCustom(String trait) {
        requireName("trait", trait);
        if (!trait.startsWith(CUSTOM_PREFIX))
            throw new IllegalArgumentException("Only custom traits (CUSTOM_*) can be created or deleted: " + trait);
    }
}
```
accessor `traits()` 와 binding 추가. `PlacementResourceProviderTraits` 는 `ModelEntity` 를 구현하므로(`ResourceProviderTraits extends ModelEntity`) `.entity(...)` 로 보낼 수 있다.

- [ ] **Step 4: 테스트 통과 확인** — `-Dtest=TraitTests`, Expected `Tests run: 7, Failures: 0, Errors: 0`
- [ ] **Step 5: 전체 빌드, 커밋, PR** — `feat(placement): add trait API (v1)`

---

### Task 6: Aggregates

**Files:**
- Create (core): `model/placement/v1/ResourceProviderAggregates.java`, `api/placement/v1/AggregateService.java`, `openstack/placement/v1/domain/PlacementResourceProviderAggregates.java`, `openstack/placement/v1/internal/AggregateServiceImpl.java`
- Modify (core): accessor `aggregates()` + binding
- Create (test): `AggregateTests.java`

**Interfaces:**
- Produces: `AggregateService` (`ResourceProviderAggregates listForProvider(String rp)`, `replaceForProvider(String rp, long gen, Collection<String> aggregateUuids)`), `ResourceProviderAggregates` (getResourceProviderGeneration long, `List<String> getAggregates()`).

- [ ] **Step 1: 테스트**

```bash
git switch main && git pull && git switch -c task/b6-aggregates
```

`AggregateTests.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.Arrays;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.placement.v1.ResourceProviderAggregates;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/Aggregates")
public class AggregateTests extends AbstractPlacementTest {

    public void listForProviderParsesEmptyList() throws Exception {
        respondWithVersions("1.39");
        respondWith(200, "{\"aggregates\": [], \"resource_provider_generation\": 557}");

        ResourceProviderAggregates aggregates = osv3().placement().aggregates().listForProvider(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/aggregates");
        Assert.assertTrue(aggregates.getAggregates().isEmpty());
        Assert.assertEquals(aggregates.getResourceProviderGeneration(), 557L);
    }

    public void replaceForProviderSendsGenerationAndUuids() throws Exception {
        respondWithVersions("1.39");
        respondWith(200, "{\"aggregates\": [\"a1\", \"a2\"], \"resource_provider_generation\": 558}");

        ResourceProviderAggregates result = osv3().placement().aggregates().replaceForProvider(RP, 557L, Arrays.asList("a1", "a2"));
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertEquals(body(request).get("resource_provider_generation").asLong(), 557L);
        Assert.assertEquals(body(request).get("aggregates").size(), 2);
        Assert.assertEquals(result.getAggregates(), Arrays.asList("a1", "a2"));
        Assert.assertEquals(result.getResourceProviderGeneration(), 558L);
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

- [ ] **Step 3: 구현**

`model/placement/v1/ResourceProviderAggregates.java`:
```java
package org.openstack4j.model.placement.v1;

import java.util.List;

import org.openstack4j.model.ModelEntity;

public interface ResourceProviderAggregates extends ModelEntity {

    long getResourceProviderGeneration();

    /** @return aggregate UUIDs the provider belongs to */
    List<String> getAggregates();
}
```

`openstack/placement/v1/domain/PlacementResourceProviderAggregates.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.ResourceProviderAggregates;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementResourceProviderAggregates implements ResourceProviderAggregates {

    private static final long serialVersionUID = 1L;

    @JsonProperty("resource_provider_generation")
    private long resourceProviderGeneration;
    @JsonProperty("aggregates")
    private List<String> aggregates;

    public PlacementResourceProviderAggregates() {
    }

    public PlacementResourceProviderAggregates(long resourceProviderGeneration, Collection<String> aggregates) {
        this.resourceProviderGeneration = resourceProviderGeneration;
        this.aggregates = new ArrayList<>(aggregates);
    }

    @Override
    public long getResourceProviderGeneration() {
        return resourceProviderGeneration;
    }

    @Override
    public List<String> getAggregates() {
        return aggregates == null ? Collections.emptyList() : aggregates;
    }
}
```

`api/placement/v1/AggregateService.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.Collection;

import org.openstack4j.common.RestService;
import org.openstack4j.model.placement.v1.ResourceProviderAggregates;

/** Aggregate membership of a resource provider ({@code /resource_providers/{uuid}/aggregates}). */
public interface AggregateService extends RestService {

    ResourceProviderAggregates listForProvider(String providerUuid);

    /** Replaces the provider's aggregates. Aggregates are plain UUIDs; Placement does not know Nova aggregates. */
    ResourceProviderAggregates replaceForProvider(String providerUuid, long generation, Collection<String> aggregateUuids);
}
```

`openstack/placement/v1/internal/AggregateServiceImpl.java`:
```java
package org.openstack4j.openstack.placement.v1.internal;

import java.util.Collection;
import java.util.Objects;

import org.openstack4j.api.placement.v1.AggregateService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.placement.v1.ResourceProviderAggregates;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceProviderAggregates;

public class AggregateServiceImpl extends BasePlacementV1Service implements AggregateService {

    @Override
    public ResourceProviderAggregates listForProvider(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeOrThrow(placement(HttpMethod.GET, PlacementResourceProviderAggregates.class,
                uri("/resource_providers/%s/aggregates", providerUuid)));
    }

    @Override
    public ResourceProviderAggregates replaceForProvider(String providerUuid, long generation, Collection<String> aggregateUuids) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        Objects.requireNonNull(aggregateUuids, "aggregateUuids");
        return executeOrThrow(placement(HttpMethod.PUT, PlacementResourceProviderAggregates.class,
                uri("/resource_providers/%s/aggregates", providerUuid))
                .entity(new PlacementResourceProviderAggregates(generation, aggregateUuids)));
    }
}
```
accessor `aggregates()` 와 binding 추가.

- [ ] **Step 4: 테스트 통과 확인** — `-Dtest=AggregateTests`, Expected `Tests run: 2, Failures: 0, Errors: 0`
- [ ] **Step 5: 전체 빌드, 커밋, PR** — `feat(placement): add aggregate API (v1)`

---

### Task 7: Usages and capacity

**Files:**
- Create (core): `model/placement/v1/ResourceProviderUsages.java`, `model/placement/v1/ProjectUsages.java`, `model/placement/v1/ResourceCapacity.java`, `api/placement/v1/UsageService.java`, `openstack/placement/v1/domain/PlacementResourceProviderUsages.java`, `openstack/placement/v1/domain/PlacementProjectUsages.java`, `openstack/placement/v1/domain/PlacementResourceCapacity.java`, `openstack/placement/v1/internal/UsageServiceImpl.java`
- Modify (core): accessor `usages()` + binding
- Create (test): `core-test/src/main/resources/placement/v1/rp_usages.json`, `usages_project.json`, `UsageTests.java`

**Interfaces:**
- Consumes: Task 3 `InventoryService.list(rp)` / `Inventory`.
- Produces: `UsageService` (`ResourceProviderUsages forProvider(String rp)`, `ProjectUsages forProject(String projectId, String userId, String consumerType)`, `Map<String, ResourceCapacity> capacity(String rp)`), `ProjectUsages` (`Map<String, Long> getUsages()` 합계, `Map<String, ? extends ConsumerTypeUsage> getByConsumerType()`), `ResourceCapacity` (getResourceClass, getTotal, getReserved, getAllocationRatio, getCapacity, getUsed, getFree).

- [ ] **Step 1: fixture 와 테스트**

```bash
git switch main && git pull && git switch -c task/b7-usages
```

`core-test/src/main/resources/placement/v1/rp_usages.json`:
```json
{"resource_provider_generation": 557, "usages": {"VCPU": 1, "MEMORY_MB": 1024, "DISK_GB": 0}}
```

`core-test/src/main/resources/placement/v1/usages_project.json` (placement 1.38+ 형식):
```json
{"usages": {"INSTANCE": {"VCPU": 4, "consumer_count": 2, "MEMORY_MB": 4096}, "unknown": {"VCPU": 1, "consumer_count": 1, "MEMORY_MB": 1024}}}
```

`UsageTests.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.placement.v1.ProjectUsages;
import org.openstack4j.model.placement.v1.ResourceCapacity;
import org.openstack4j.model.placement.v1.ResourceProviderUsages;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/Usages")
public class UsageTests extends AbstractPlacementTest {

    public void forProviderParsesUsages() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_usages.json");

        ResourceProviderUsages usages = osv3().placement().usages().forProvider(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/usages");
        Assert.assertEquals(usages.getResourceProviderGeneration(), 557L);
        Assert.assertEquals(usages.getUsages().get("MEMORY_MB"), Long.valueOf(1024));
        Assert.assertEquals(usages.getUsages().get("DISK_GB"), Long.valueOf(0));
    }

    public void forProjectGroupsByConsumerTypeOn138() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/usages_project.json");

        ProjectUsages usages = osv3().placement().usages().forProject("proj-1", "user-1", "INSTANCE");
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getRequestUrl().encodedPath(), "/usages");
        Assert.assertEquals(request.getRequestUrl().queryParameter("project_id"), "proj-1");
        Assert.assertEquals(request.getRequestUrl().queryParameter("user_id"), "user-1");
        Assert.assertEquals(request.getRequestUrl().queryParameter("consumer_type"), "INSTANCE");
        Assert.assertEquals(usages.getUsages().get("VCPU"), Long.valueOf(5));
        Assert.assertEquals(usages.getUsages().get("MEMORY_MB"), Long.valueOf(5120));
        Assert.assertEquals(usages.getByConsumerType().get("INSTANCE").getConsumerCount(), Long.valueOf(2));
        Assert.assertEquals(usages.getByConsumerType().get("INSTANCE").getUsages().get("VCPU"), Long.valueOf(4));
        Assert.assertEquals(usages.getByConsumerType().get("unknown").getUsages().get("VCPU"), Long.valueOf(1));
    }

    public void projectUsagesBefore138() throws Exception {
        respondWithVersions("1.37");
        respondWith(200, "{\"usages\": {\"VCPU\": 3, \"MEMORY_MB\": 2048}}");

        ProjectUsages usages = osv3().placement().usages().forProject("proj-1", null, null);
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getHeader("OpenStack-API-Version"), "placement 1.37");
        Assert.assertNull(request.getRequestUrl().queryParameter("user_id"));
        Assert.assertEquals(usages.getUsages().get("VCPU"), Long.valueOf(3));
        Assert.assertEquals(usages.getByConsumerType().size(), 1);
        Assert.assertNull(usages.getByConsumerType().get(ProjectUsages.ALL_CONSUMER_TYPES).getConsumerCount());
        Assert.assertEquals(usages.getByConsumerType().get(ProjectUsages.ALL_CONSUMER_TYPES).getUsages().get("MEMORY_MB"), Long.valueOf(2048));
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.38.*")
    public void consumerTypeFilterRequires138() throws Exception {
        respondWithVersions("1.37");
        try {
            osv3().placement().usages().forProject("proj-1", null, "INSTANCE");
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    public void capacityCombinesInventoriesAndUsages() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/inventories.json");
        respondWith("/placement/v1/rp_usages.json");

        Map<String, ResourceCapacity> capacity = osv3().placement().usages().capacity(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/inventories");
        Assert.assertEquals(takeRequest().getPath(), "/resource_providers/" + RP + "/usages");
        ResourceCapacity vcpu = capacity.get("VCPU");
        Assert.assertEquals(vcpu.getCapacity(), 48L);   // (12 - 0) * 4.0
        Assert.assertEquals(vcpu.getUsed(), 1L);
        Assert.assertEquals(vcpu.getFree(), 47L);
        ResourceCapacity memory = capacity.get("MEMORY_MB");
        Assert.assertEquals(memory.getCapacity(), 15072L); // (15584 - 512) * 1.0
        Assert.assertEquals(memory.getFree(), 14048L);
        Assert.assertEquals(capacity.get("DISK_GB").getUsed(), 0L);
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

- [ ] **Step 3: 모델**

`model/placement/v1/ResourceProviderUsages.java`:
```java
package org.openstack4j.model.placement.v1;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

public interface ResourceProviderUsages extends ModelEntity {

    long getResourceProviderGeneration();

    /** @return used amount per resource class */
    Map<String, Long> getUsages();
}
```

`model/placement/v1/ProjectUsages.java`:
```java
package org.openstack4j.model.placement.v1;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** Usage of a project (optionally one user) across all providers. */
public interface ProjectUsages extends ModelEntity {

    /** Key of {@link #getByConsumerType()} on servers older than 1.38, which do not group by consumer type. */
    String ALL_CONSUMER_TYPES = "all";

    /** @return used amount per resource class, summed over consumer types */
    Map<String, Long> getUsages();

    /** @return usage per consumer type (placement 1.38+); one entry {@link #ALL_CONSUMER_TYPES} on older servers */
    Map<String, ? extends ConsumerTypeUsage> getByConsumerType();

    interface ConsumerTypeUsage {
        /** @return number of consumers, or {@code null} on servers older than 1.38 */
        Long getConsumerCount();

        Map<String, Long> getUsages();
    }
}
```

`model/placement/v1/ResourceCapacity.java`:
```java
package org.openstack4j.model.placement.v1;

import org.openstack4j.model.ModelEntity;

/**
 * Capacity of one resource class on a provider, computed from its inventory and usage:
 * {@code capacity = floor((total - reserved) * allocationRatio)}, {@code free = capacity - used}.
 */
public interface ResourceCapacity extends ModelEntity {

    String getResourceClass();

    long getTotal();

    long getReserved();

    float getAllocationRatio();

    long getCapacity();

    long getUsed();

    long getFree();
}
```

- [ ] **Step 4: Jackson 모델과 구현**

`openstack/placement/v1/domain/PlacementResourceProviderUsages.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.ResourceProviderUsages;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementResourceProviderUsages implements ResourceProviderUsages {

    private static final long serialVersionUID = 1L;

    @JsonProperty("resource_provider_generation")
    private long resourceProviderGeneration;
    @JsonProperty("usages")
    private Map<String, Long> usages;

    @Override
    public long getResourceProviderGeneration() {
        return resourceProviderGeneration;
    }

    @Override
    public Map<String, Long> getUsages() {
        return usages == null ? Collections.emptyMap() : usages;
    }
}
```

`openstack/placement/v1/domain/PlacementProjectUsages.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.openstack4j.model.placement.v1.ProjectUsages;

/**
 * Reads both shapes of {@code GET /usages}: {@code {"usages": {"VCPU": 3}}} (before 1.38) and
 * {@code {"usages": {"INSTANCE": {"VCPU": 3, "consumer_count": 1}}}} (1.38 and later).
 */
public class PlacementProjectUsages implements ProjectUsages {

    private static final long serialVersionUID = 1L;

    private final Map<String, Long> usages = new LinkedHashMap<>();
    private final Map<String, Usage> byConsumerType = new LinkedHashMap<>();

    @JsonCreator
    public PlacementProjectUsages(@JsonProperty("usages") JsonNode node) {
        if (node == null || !node.isObject())
            return;
        boolean grouped = false;
        for (Iterator<JsonNode> it = node.elements(); it.hasNext(); )
            if (it.next().isObject()) grouped = true;
        if (grouped) {
            node.fields().forEachRemaining(group -> {
                Usage usage = new Usage();
                group.getValue().fields().forEachRemaining(field -> {
                    if ("consumer_count".equals(field.getKey())) usage.consumerCount = field.getValue().asLong();
                    else usage.usages.put(field.getKey(), field.getValue().asLong());
                });
                byConsumerType.put(group.getKey(), usage);
                usage.usages.forEach((rc, amount) -> usages.merge(rc, amount, Long::sum));
            });
        } else {
            Usage usage = new Usage();
            node.fields().forEachRemaining(field -> usage.usages.put(field.getKey(), field.getValue().asLong()));
            byConsumerType.put(ALL_CONSUMER_TYPES, usage);
            usages.putAll(usage.usages);
        }
    }

    @Override
    public Map<String, Long> getUsages() {
        return Collections.unmodifiableMap(usages);
    }

    @Override
    public Map<String, ? extends ConsumerTypeUsage> getByConsumerType() {
        return Collections.unmodifiableMap(byConsumerType);
    }

    public static class Usage implements ConsumerTypeUsage, java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private Long consumerCount;
        private final Map<String, Long> usages = new LinkedHashMap<>();

        @Override
        public Long getConsumerCount() {
            return consumerCount;
        }

        @Override
        public Map<String, Long> getUsages() {
            return Collections.unmodifiableMap(usages);
        }
    }
}
```

`openstack/placement/v1/domain/PlacementResourceCapacity.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.ResourceCapacity;
import org.openstack4j.util.ToStringHelper;

public class PlacementResourceCapacity implements ResourceCapacity {

    private static final long serialVersionUID = 1L;

    private final String resourceClass;
    private final long total;
    private final long reserved;
    private final float allocationRatio;
    private final long capacity;
    private final long used;

    public PlacementResourceCapacity(String resourceClass, Inventory inventory, long used) {
        this.resourceClass = resourceClass;
        this.total = inventory.getTotal();
        this.reserved = inventory.getReserved() == null ? 0 : inventory.getReserved();
        this.allocationRatio = inventory.getAllocationRatio() == null ? 1.0f : inventory.getAllocationRatio();
        this.capacity = (long) Math.floor((total - reserved) * (double) allocationRatio);
        this.used = used;
    }

    @Override
    public String getResourceClass() {
        return resourceClass;
    }

    @Override
    public long getTotal() {
        return total;
    }

    @Override
    public long getReserved() {
        return reserved;
    }

    @Override
    public float getAllocationRatio() {
        return allocationRatio;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    @Override
    public long getUsed() {
        return used;
    }

    @Override
    public long getFree() {
        return capacity - used;
    }

    @Override
    public String toString() {
        return new ToStringHelper(this).add("resourceClass", resourceClass).add("capacity", capacity).add("used", used)
                .add("free", getFree()).toString();
    }
}
```

`api/placement/v1/UsageService.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.placement.v1.ProjectUsages;
import org.openstack4j.model.placement.v1.ResourceCapacity;
import org.openstack4j.model.placement.v1.ResourceProviderUsages;

public interface UsageService extends RestService {

    ResourceProviderUsages forProvider(String providerUuid);

    /**
     * @param userId optional user filter
     * @param consumerType optional consumer type filter (placement 1.38)
     */
    ProjectUsages forProject(String projectId, String userId, String consumerType);

    /**
     * Capacity per resource class of a provider, from its inventories and usages (two requests; a change between
     * them is not reflected).
     */
    Map<String, ResourceCapacity> capacity(String providerUuid);
}
```

`openstack/placement/v1/internal/UsageServiceImpl.java`:
```java
package org.openstack4j.openstack.placement.v1.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.placement.v1.UsageService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.PlacementMicroVersions;
import org.openstack4j.model.placement.v1.ProjectUsages;
import org.openstack4j.model.placement.v1.ResourceCapacity;
import org.openstack4j.model.placement.v1.ResourceProviderInventories;
import org.openstack4j.model.placement.v1.ResourceProviderUsages;
import org.openstack4j.openstack.placement.v1.domain.PlacementProjectUsages;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceCapacity;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceProviderUsages;

public class UsageServiceImpl extends BasePlacementV1Service implements UsageService {

    @Override
    public ResourceProviderUsages forProvider(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeOrThrow(placement(HttpMethod.GET, PlacementResourceProviderUsages.class, uri("/resource_providers/%s/usages", providerUuid)));
    }

    @Override
    public ProjectUsages forProject(String projectId, String userId, String consumerType) {
        Objects.requireNonNull(projectId, "projectId");
        if (consumerType != null)
            requireMicroVersion("filtering usages by consumer type", PlacementMicroVersions.V1_38);
        return executeOrThrow(placement(HttpMethod.GET, PlacementProjectUsages.class, "/usages")
                .param("project_id", projectId)
                .param("user_id", userId)
                .param("consumer_type", consumerType));
    }

    @Override
    public Map<String, ResourceCapacity> capacity(String providerUuid) {
        ResourceProviderInventories inventories = new InventoryServiceImpl().list(providerUuid);
        Map<String, Long> used = forProvider(providerUuid).getUsages();
        Map<String, ResourceCapacity> result = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends Inventory> entry : inventories.getInventories().entrySet())
            result.put(entry.getKey(), new PlacementResourceCapacity(entry.getKey(), entry.getValue(), used.getOrDefault(entry.getKey(), 0L)));
        return result;
    }
}
```
accessor `usages()` 와 binding 추가.

- [ ] **Step 5: 테스트 통과 확인** — `-Dtest=UsageTests`, Expected `Tests run: 5, Failures: 0, Errors: 0`
- [ ] **Step 6: 전체 빌드, 커밋, PR** — `feat(placement): add usage API and capacity helper (v1)`

---

### Task 8: Allocations

**Files:**
- Create (core): `model/placement/v1/ConsumerAllocations.java`, `model/placement/v1/AllocationRequest.java`, `model/placement/v1/ResourceProviderAllocations.java`, `api/placement/v1/AllocationService.java`, `openstack/placement/v1/domain/PlacementConsumerAllocations.java`, `openstack/placement/v1/domain/PlacementResourceProviderAllocations.java`, `openstack/placement/v1/internal/AllocationBodies.java`, `openstack/placement/v1/internal/AllocationServiceImpl.java`
- Modify (core): accessor `allocations()` + binding
- Create (test): `core-test/src/main/resources/placement/v1/consumer_allocations.json`, `rp_allocations.json`, `AllocationTests.java`

**Interfaces:**
- Produces: `AllocationRequest` (builder: projectId, userId, consumerGeneration(Long), consumerType, allocation(rp, rc, amount), allocations(rp, Map); getters `getAllocations()` → `Map<String, Map<String, Long>>`), `AllocationBodies.toJson(ObjectNode target, AllocationRequest, MicroVersion)` (Task 10 reshaper 재사용), `AllocationService` (`ConsumerAllocations get(String consumer)`, `void set(String consumer, AllocationRequest)`, `void setMany(Map<String, AllocationRequest>)`, `ActionResponse delete(String consumer)`, `ResourceProviderAllocations listForProvider(String rp)`).

- [ ] **Step 1: fixture 와 테스트**

```bash
git switch main && git pull && git switch -c task/b8-allocations
```

`core-test/src/main/resources/placement/v1/consumer_allocations.json`:
```json
{
  "allocations": {
    "3626308f-38dd-4da8-8f0f-6697b05d8f6c": {"resources": {"VCPU": 1, "MEMORY_MB": 1024}, "generation": 557}
  },
  "project_id": "2580a7b51d564c1d848ee27fda2db713",
  "user_id": "e365357fdf4d4a37a07fde3209cac4aa",
  "consumer_generation": 1,
  "consumer_type": "INSTANCE"
}
```

`core-test/src/main/resources/placement/v1/rp_allocations.json`:
```json
{
  "allocations": {
    "96a38bed-26b5-410b-8cef-1913a2e0e0b8": {"resources": {"MEMORY_MB": 1024, "VCPU": 1}, "consumer_generation": 1}
  },
  "resource_provider_generation": 557
}
```

`AllocationTests.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.placement.v1.exceptions.PlacementConcurrentUpdateException;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.AllocationRequest;
import org.openstack4j.model.placement.v1.ConsumerAllocations;
import org.openstack4j.model.placement.v1.ResourceProviderAllocations;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/Allocations")
public class AllocationTests extends AbstractPlacementTest {

    private static final String CONSUMER = "96a38bed-26b5-410b-8cef-1913a2e0e0b8";

    private static AllocationRequest.Builder request() {
        return AllocationRequest.builder().projectId("proj-1").userId("user-1").allocation(RP, "VCPU", 2).allocation(RP, "MEMORY_MB", 2048);
    }

    public void getParsesConsumerAllocations() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/consumer_allocations.json");

        ConsumerAllocations allocations = osv3().placement().allocations().get(CONSUMER);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/allocations/" + CONSUMER);
        Assert.assertEquals(allocations.getAllocations().get(RP).getResources().get("MEMORY_MB"), Long.valueOf(1024));
        Assert.assertEquals(allocations.getAllocations().get(RP).getGeneration(), Long.valueOf(557));
        Assert.assertEquals(allocations.getConsumerGeneration(), Long.valueOf(1));
        Assert.assertEquals(allocations.getConsumerType(), "INSTANCE");
        Assert.assertEquals(allocations.getProjectId(), "2580a7b51d564c1d848ee27fda2db713");
    }

    public void getOfUnknownConsumerIsEmpty() throws Exception {
        respondWithVersions("1.39");
        respondWith(200, "{\"allocations\": {}}");

        ConsumerAllocations allocations = osv3().placement().allocations().get("00000000-0000-0000-0000-000000000000");

        takeVersionAndRequest();
        Assert.assertTrue(allocations.getAllocations().isEmpty());
        Assert.assertNull(allocations.getConsumerGeneration());
    }

    public void setNewConsumerSendsNullGenerationAndType() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        osv3().placement().allocations().set(CONSUMER, request().consumerType("INSTANCE").build());
        RecordedRequest put = takeVersionAndRequest();
        JsonNode body = body(put);

        Assert.assertEquals(put.getMethod(), "PUT");
        Assert.assertEquals(put.getPath(), "/allocations/" + CONSUMER);
        Assert.assertTrue(body.has("consumer_generation"), "consumer_generation must be present");
        Assert.assertTrue(body.get("consumer_generation").isNull());
        Assert.assertEquals(body.get("consumer_type").asText(), "INSTANCE");
        Assert.assertEquals(body.get("project_id").asText(), "proj-1");
        Assert.assertEquals(body.get("allocations").get(RP).get("resources").get("VCPU").asLong(), 2L);
    }

    public void setExistingConsumerSendsGeneration() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        osv3().placement().allocations().set(CONSUMER, request().consumerGeneration(1L).consumerType("INSTANCE").build());

        Assert.assertEquals(body(takeVersionAndRequest()).get("consumer_generation").asLong(), 1L);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*consumer type.*1\\.38.*")
    public void consumerTypeIsRequiredFrom138() throws Exception {
        respondWithVersions("1.39");
        try {
            osv3().placement().allocations().set(CONSUMER, request().build());
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.38.*")
    public void consumerTypeIsRejectedBefore138() throws Exception {
        respondWithVersions("1.37");
        try {
            osv3().placement().allocations().set(CONSUMER, request().consumerType("INSTANCE").build());
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    public void setBefore138OmitsConsumerType() throws Exception {
        respondWithVersions("1.37");
        respondWith(204);

        osv3().placement().allocations().set(CONSUMER, request().build());
        JsonNode body = body(takeVersionAndRequest());

        Assert.assertFalse(body.has("consumer_type"));
        Assert.assertTrue(body.get("consumer_generation").isNull());
    }

    public void setManyPostsAllConsumers() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        osv3().placement().allocations().setMany(Collections.singletonMap(CONSUMER, request().consumerType("INSTANCE").build()));
        RecordedRequest post = takeVersionAndRequest();

        Assert.assertEquals(post.getMethod(), "POST");
        Assert.assertEquals(post.getPath(), "/allocations");
        Assert.assertEquals(body(post).get(CONSUMER).get("allocations").get(RP).get("resources").get("MEMORY_MB").asLong(), 2048L);
        Assert.assertEquals(body(post).get(CONSUMER).get("consumer_type").asText(), "INSTANCE");
    }

    public void deleteConsumer() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        ActionResponse response = osv3().placement().allocations().delete(CONSUMER);

        Assert.assertEquals(takeVersionAndRequest().getMethod(), "DELETE");
        Assert.assertTrue(response.isSuccess());
    }

    public void listForProvider() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_allocations.json");

        ResourceProviderAllocations allocations = osv3().placement().allocations().listForProvider(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/allocations");
        Assert.assertEquals(allocations.getResourceProviderGeneration(), 557L);
        Assert.assertEquals(allocations.getAllocations().get(CONSUMER).getResources().get("VCPU"), Long.valueOf(1));
        Assert.assertEquals(allocations.getAllocations().get(CONSUMER).getConsumerGeneration(), Long.valueOf(1));
    }

    @Test(expectedExceptions = PlacementConcurrentUpdateException.class)
    public void staleConsumerGenerationIsAConcurrentUpdate() throws Exception {
        respondWithVersions("1.39");
        respondWithError(409, "placement.concurrent_update");
        try {
            osv3().placement().allocations().set(CONSUMER, request().consumerGeneration(1L).consumerType("INSTANCE").build());
        } finally {
            takeVersionAndRequest();
        }
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

- [ ] **Step 3: 모델**

`model/placement/v1/ConsumerAllocations.java`:
```java
package org.openstack4j.model.placement.v1;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** Allocations of one consumer (for example a server) across resource providers. */
public interface ConsumerAllocations extends ModelEntity {

    /** @return allocations keyed by resource provider UUID; empty for an unknown consumer */
    Map<String, ? extends ProviderAllocation> getAllocations();

    String getProjectId();

    String getUserId();

    /** @return the consumer generation to send with the next write, or {@code null} for an unknown consumer */
    Long getConsumerGeneration();

    /** @return the consumer type (placement 1.38), otherwise {@code null} */
    String getConsumerType();

    interface ProviderAllocation {
        /** @return amount per resource class */
        Map<String, Long> getResources();

        /** @return the provider generation at the time of the read */
        Long getGeneration();
    }
}
```

`model/placement/v1/AllocationRequest.java`:
```java
package org.openstack4j.model.placement.v1;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.model.ModelEntity;

/** Body of {@code PUT /allocations/{consumer}} (and one entry of {@code POST /allocations}). */
public final class AllocationRequest implements ModelEntity {

    private static final long serialVersionUID = 1L;

    private final Map<String, Map<String, Long>> allocations;
    private final String projectId;
    private final String userId;
    private final Long consumerGeneration;
    private final String consumerType;

    private AllocationRequest(Builder b) {
        this.allocations = Collections.unmodifiableMap(b.allocations);
        this.projectId = Objects.requireNonNull(b.projectId, "projectId");
        this.userId = Objects.requireNonNull(b.userId, "userId");
        this.consumerGeneration = b.consumerGeneration;
        this.consumerType = b.consumerType;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @return resource provider UUID → (resource class → amount) */
    public Map<String, Map<String, Long>> getAllocations() {
        return allocations;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getUserId() {
        return userId;
    }

    /** @return the generation read before, or {@code null} for a consumer that has no allocations yet */
    public Long getConsumerGeneration() {
        return consumerGeneration;
    }

    /** @return the consumer type (placement 1.38, where it is required), or {@code null} */
    public String getConsumerType() {
        return consumerType;
    }

    public static final class Builder {
        private final Map<String, Map<String, Long>> allocations = new LinkedHashMap<>();
        private String projectId;
        private String userId;
        private Long consumerGeneration;
        private String consumerType;

        public Builder allocation(String providerUuid, String resourceClass, long amount) {
            allocations.computeIfAbsent(providerUuid, k -> new LinkedHashMap<>()).put(resourceClass, amount);
            return this;
        }

        public Builder allocations(String providerUuid, Map<String, Long> resources) {
            allocations.computeIfAbsent(providerUuid, k -> new LinkedHashMap<>()).putAll(resources);
            return this;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder consumerGeneration(Long consumerGeneration) {
            this.consumerGeneration = consumerGeneration;
            return this;
        }

        public Builder consumerType(String consumerType) {
            this.consumerType = consumerType;
            return this;
        }

        public AllocationRequest build() {
            return new AllocationRequest(this);
        }
    }
}
```

`model/placement/v1/ResourceProviderAllocations.java`:
```java
package org.openstack4j.model.placement.v1;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** All allocations against one resource provider, keyed by consumer UUID. */
public interface ResourceProviderAllocations extends ModelEntity {

    long getResourceProviderGeneration();

    Map<String, ? extends ConsumerAllocation> getAllocations();

    interface ConsumerAllocation {
        Map<String, Long> getResources();

        Long getConsumerGeneration();
    }
}
```

- [ ] **Step 4: Jackson 모델, 요청 직렬화, 구현**

`openstack/placement/v1/domain/PlacementConsumerAllocations.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.ConsumerAllocations;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementConsumerAllocations implements ConsumerAllocations {

    private static final long serialVersionUID = 1L;

    @JsonProperty("allocations")
    private Map<String, Allocation> allocations;
    @JsonProperty("project_id")
    private String projectId;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("consumer_generation")
    private Long consumerGeneration;
    @JsonProperty("consumer_type")
    private String consumerType;

    @Override
    public Map<String, ? extends ProviderAllocation> getAllocations() {
        return allocations == null ? Collections.emptyMap() : allocations;
    }

    @Override
    public String getProjectId() {
        return projectId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public Long getConsumerGeneration() {
        return consumerGeneration;
    }

    @Override
    public String getConsumerType() {
        return consumerType;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Allocation implements ProviderAllocation, Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("resources")
        private Map<String, Long> resources;
        @JsonProperty("generation")
        private Long generation;

        @Override
        public Map<String, Long> getResources() {
            return resources == null ? Collections.emptyMap() : resources;
        }

        @Override
        public Long getGeneration() {
            return generation;
        }
    }
}
```

`openstack/placement/v1/domain/PlacementResourceProviderAllocations.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.ResourceProviderAllocations;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementResourceProviderAllocations implements ResourceProviderAllocations {

    private static final long serialVersionUID = 1L;

    @JsonProperty("resource_provider_generation")
    private long resourceProviderGeneration;
    @JsonProperty("allocations")
    private Map<String, Allocation> allocations;

    @Override
    public long getResourceProviderGeneration() {
        return resourceProviderGeneration;
    }

    @Override
    public Map<String, ? extends ConsumerAllocation> getAllocations() {
        return allocations == null ? Collections.emptyMap() : allocations;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Allocation implements ConsumerAllocation, Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("resources")
        private Map<String, Long> resources;
        @JsonProperty("consumer_generation")
        private Long consumerGeneration;

        @Override
        public Map<String, Long> getResources() {
            return resources == null ? Collections.emptyMap() : resources;
        }

        @Override
        public Long getConsumerGeneration() {
            return consumerGeneration;
        }
    }
}
```

`openstack/placement/v1/internal/AllocationBodies.java`:
```java
package org.openstack4j.openstack.placement.v1.internal;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.placement.v1.AllocationRequest;
import org.openstack4j.model.placement.v1.PlacementMicroVersions;
import org.openstack4j.openstack.internal.MicroVersion;

/** Serialises {@link AllocationRequest} the way Placement 1.28+ expects, applying the consumer type rules of 1.38. */
final class AllocationBodies {

    private AllocationBodies() {
    }

    static void write(ObjectNode target, AllocationRequest request, MicroVersion version) {
        boolean typeSupported = version.compareTo(PlacementMicroVersions.V1_38) >= 0;
        if (typeSupported && request.getConsumerType() == null)
            throw new IllegalArgumentException(
                    "A consumer type is required for allocations from placement " + PlacementMicroVersions.V1_38
                            + " (negotiated " + version + "); set AllocationRequest.consumerType, for example \"INSTANCE\"");
        if (!typeSupported && request.getConsumerType() != null)
            throw new PlacementMicroVersionException("Consumer types require placement microversion "
                    + PlacementMicroVersions.V1_38 + ", but the negotiated version is " + version);

        ObjectNode allocations = target.putObject("allocations");
        for (Map.Entry<String, Map<String, Long>> provider : request.getAllocations().entrySet()) {
            ObjectNode resources = allocations.putObject(provider.getKey()).putObject("resources");
            provider.getValue().forEach(resources::put);
        }
        target.put("project_id", request.getProjectId());
        target.put("user_id", request.getUserId());
        if (request.getConsumerGeneration() == null) target.putNull("consumer_generation");
        else target.put("consumer_generation", request.getConsumerGeneration());
        if (request.getConsumerType() != null) target.put("consumer_type", request.getConsumerType());
    }
}
```

`api/placement/v1/AllocationService.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.AllocationRequest;
import org.openstack4j.model.placement.v1.ConsumerAllocations;
import org.openstack4j.model.placement.v1.ResourceProviderAllocations;

/**
 * Allocations ({@code /allocations}). Writing allocations by hand changes what the scheduler believes is in use;
 * normally Nova owns them.
 */
public interface AllocationService extends RestService {

    /** @return the consumer's allocations; empty (never {@code null}) for an unknown consumer */
    ConsumerAllocations get(String consumerUuid);

    /** Replaces the consumer's allocations. The request's consumer generation must match the server's. */
    void set(String consumerUuid, AllocationRequest request);

    /** Replaces the allocations of several consumers atomically ({@code POST /allocations}). */
    void setMany(Map<String, AllocationRequest> requestsByConsumer);

    ActionResponse delete(String consumerUuid);

    ResourceProviderAllocations listForProvider(String providerUuid);
}
```

`openstack/placement/v1/internal/AllocationServiceImpl.java`:
```java
package org.openstack4j.openstack.placement.v1.internal;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openstack4j.api.placement.v1.AllocationService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.AllocationRequest;
import org.openstack4j.model.placement.v1.ConsumerAllocations;
import org.openstack4j.model.placement.v1.ResourceProviderAllocations;
import org.openstack4j.openstack.placement.v1.domain.PlacementConsumerAllocations;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceProviderAllocations;

public class AllocationServiceImpl extends BasePlacementV1Service implements AllocationService {

    @Override
    public ConsumerAllocations get(String consumerUuid) {
        Objects.requireNonNull(consumerUuid, "consumerUuid");
        return executeOrThrow(placement(HttpMethod.GET, PlacementConsumerAllocations.class, uri("/allocations/%s", consumerUuid)));
    }

    @Override
    public void set(String consumerUuid, AllocationRequest request) {
        Objects.requireNonNull(consumerUuid, "consumerUuid");
        Objects.requireNonNull(request, "request");
        ObjectNode body = ObjectMapperSingleton.getContext(Object.class).createObjectNode();
        AllocationBodies.write(body, request, microVersion());
        executeOrThrow(placement(HttpMethod.PUT, Void.class, uri("/allocations/%s", consumerUuid)).json(body.toString()));
    }

    @Override
    public void setMany(Map<String, AllocationRequest> requestsByConsumer) {
        Objects.requireNonNull(requestsByConsumer, "requestsByConsumer");
        ObjectNode body = ObjectMapperSingleton.getContext(Object.class).createObjectNode();
        for (Map.Entry<String, AllocationRequest> entry : requestsByConsumer.entrySet())
            AllocationBodies.write(body.putObject(entry.getKey()), entry.getValue(), microVersion());
        executeOrThrow(placement(HttpMethod.POST, Void.class, "/allocations").json(body.toString()));
    }

    @Override
    public ActionResponse delete(String consumerUuid) {
        Objects.requireNonNull(consumerUuid, "consumerUuid");
        return executeAction(placement(HttpMethod.DELETE, ActionResponse.class, uri("/allocations/%s", consumerUuid)));
    }

    @Override
    public ResourceProviderAllocations listForProvider(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeOrThrow(placement(HttpMethod.GET, PlacementResourceProviderAllocations.class, uri("/resource_providers/%s/allocations", providerUuid)));
    }
}
```
`microVersion()` 은 요청 전에 호출되므로 consumer type 검사는 서버 요청 없이 끝난다(루트 `GET /` 는 이미 캐시됐거나 이때 한 번 일어난다). accessor `allocations()` 와 binding 추가.

- [ ] **Step 5: 테스트 통과 확인** — `-Dtest=AllocationTests`, Expected `Tests run: 11, Failures: 0, Errors: 0`
- [ ] **Step 6: 전체 빌드, 커밋, PR** — `feat(placement): add allocation API (v1)`

---

### Task 9: Allocation candidates

**Files:**
- Create (core): `model/placement/v1/AllocationCandidatesQuery.java`, `model/placement/v1/AllocationCandidates.java`, `api/placement/v1/AllocationCandidateService.java`, `openstack/placement/v1/domain/PlacementAllocationCandidates.java`, `openstack/placement/v1/internal/AllocationCandidateServiceImpl.java`
- Modify (core): accessor `allocationCandidates()` + binding
- Create (test): `core-test/src/main/resources/placement/v1/candidates.json`, `AllocationCandidateTests.java`

**Interfaces:**
- Produces: `AllocationCandidatesQuery.builder()` (`resources(rc, amount)`, `required(String...)`, `memberOf(String...)`, `inTree(rp)`, `group(String suffix, Consumer<RequestGroup>)`, `groupPolicy(GroupPolicy)`, `limit(int)`, `rootRequired(String...)`, `sameSubtree(String...)`, `build()`), `AllocationCandidates` (`getAllocationRequests()` → `List<? extends Candidate>` with `Map<String, Map<String, Long>> getAllocations()`, `Map<String, List<String>> getMappings()`; `getProviderSummaries()` → `Map<String, ? extends ProviderSummary>` with `Map<String, ? extends CapacityUsed> getResources()`, `List<String> getTraits()`, `getParentProviderUuid()`, `getRootProviderUuid()`).

- [ ] **Step 1: fixture 와 테스트**

```bash
git switch main && git pull && git switch -c task/b9-allocation-candidates
```

`core-test/src/main/resources/placement/v1/candidates.json`:
```json
{
  "allocation_requests": [
    {
      "allocations": {"3626308f-38dd-4da8-8f0f-6697b05d8f6c": {"resources": {"VCPU": 1, "MEMORY_MB": 512}}},
      "mappings": {"": ["3626308f-38dd-4da8-8f0f-6697b05d8f6c"]}
    }
  ],
  "provider_summaries": {
    "3626308f-38dd-4da8-8f0f-6697b05d8f6c": {
      "resources": {
        "VCPU": {"capacity": 48, "used": 1},
        "MEMORY_MB": {"capacity": 15072, "used": 1024},
        "DISK_GB": {"capacity": 467, "used": 0}
      },
      "traits": ["HW_CPU_X86_AVX2", "COMPUTE_NET_ATTACH_INTERFACE"],
      "parent_provider_uuid": null,
      "root_provider_uuid": "3626308f-38dd-4da8-8f0f-6697b05d8f6c"
    }
  }
}
```

`AllocationCandidateTests.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.Arrays;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.placement.v1.AllocationCandidates;
import org.openstack4j.model.placement.v1.AllocationCandidatesQuery;
import org.openstack4j.model.placement.v1.AllocationCandidatesQuery.GroupPolicy;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/AllocationCandidates")
public class AllocationCandidateTests extends AbstractPlacementTest {

    public void simpleQueryAndResponse() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/candidates.json");

        AllocationCandidates candidates = osv3().placement().allocationCandidates().list(AllocationCandidatesQuery.builder()
                .resources("VCPU", 1).resources("MEMORY_MB", 512).required("HW_CPU_X86_AVX2", "!CUSTOM_SLOW").limit(2).build());
        HttpUrl url = takeVersionAndRequest().getRequestUrl();

        Assert.assertEquals(url.encodedPath(), "/allocation_candidates");
        Assert.assertEquals(url.queryParameter("resources"), "VCPU:1,MEMORY_MB:512");
        Assert.assertEquals(url.queryParameter("required"), "HW_CPU_X86_AVX2,!CUSTOM_SLOW");
        Assert.assertEquals(url.queryParameter("limit"), "2");
        Assert.assertNull(url.queryParameter("group_policy"));
        Assert.assertEquals(candidates.getAllocationRequests().size(), 1);
        Assert.assertEquals(candidates.getAllocationRequests().get(0).getAllocations().get(RP).get("MEMORY_MB"), Long.valueOf(512));
        Assert.assertEquals(candidates.getAllocationRequests().get(0).getMappings().get(""), Arrays.asList(RP));
        Assert.assertEquals(candidates.getProviderSummaries().get(RP).getResources().get("VCPU").getCapacity(), 48L);
        Assert.assertEquals(candidates.getProviderSummaries().get(RP).getResources().get("VCPU").getUsed(), 1L);
        Assert.assertEquals(candidates.getProviderSummaries().get(RP).getTraits().size(), 2);
        Assert.assertNull(candidates.getProviderSummaries().get(RP).getParentProviderUuid());
    }

    public void granularGroupsGetSuffixedParameters() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/candidates.json");

        osv3().placement().allocationCandidates().list(AllocationCandidatesQuery.builder()
                .resources("VCPU", 2)
                .group("_GPU", g -> g.resources("VGPU", 1).required("CUSTOM_NVIDIA").memberOf("agg-gpu"))
                .group("_NET", g -> g.resources("NET_BW_EGR_KILOBIT_PER_SEC", 1000).inTree(RP))
                .groupPolicy(GroupPolicy.ISOLATE)
                .rootRequired("COMPUTE_STATUS_ENABLED")
                .sameSubtree("_GPU", "_NET")
                .build());
        HttpUrl url = takeVersionAndRequest().getRequestUrl();

        Assert.assertEquals(url.queryParameter("resources"), "VCPU:2");
        Assert.assertEquals(url.queryParameter("resources_GPU"), "VGPU:1");
        Assert.assertEquals(url.queryParameter("required_GPU"), "CUSTOM_NVIDIA");
        Assert.assertEquals(url.queryParameter("member_of_GPU"), "agg-gpu");
        Assert.assertEquals(url.queryParameter("in_tree_NET"), RP);
        Assert.assertEquals(url.queryParameter("group_policy"), "isolate");
        Assert.assertEquals(url.queryParameter("root_required"), "COMPUTE_STATUS_ENABLED");
        Assert.assertEquals(url.queryParameter("same_subtree"), "_GPU,_NET");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*group_policy.*")
    public void twoGroupsNeedAGroupPolicy() {
        AllocationCandidatesQuery.builder()
                .group("1", g -> g.resources("VCPU", 1))
                .group("2", g -> g.resources("MEMORY_MB", 1))
                .build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void atLeastOneResourceIsRequired() {
        AllocationCandidatesQuery.builder().required("HW_CPU_X86_AVX2").build();
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*same_subtree.*1\\.36.*")
    public void sameSubtreeRequires136() throws Exception {
        respondWithVersions("1.35");
        try {
            osv3().placement().allocationCandidates().list(AllocationCandidatesQuery.builder()
                    .group("1", g -> g.resources("VCPU", 1)).sameSubtree("1").build());
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.33.*")
    public void nonNumericSuffixRequires133() throws Exception {
        respondWithVersions("1.32");
        try {
            osv3().placement().allocationCandidates().list(AllocationCandidatesQuery.builder()
                    .group("_GPU", g -> g.resources("VGPU", 1)).build());
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    public void numericSuffixWorksBefore133() throws Exception {
        respondWithVersions("1.32");
        respondWith("/placement/v1/candidates.json");

        osv3().placement().allocationCandidates().list(AllocationCandidatesQuery.builder()
                .group("1", g -> g.resources("VGPU", 1)).build());

        Assert.assertEquals(takeVersionAndRequest().getRequestUrl().queryParameter("resources1"), "VGPU:1");
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

- [ ] **Step 3: 쿼리 빌더와 모델**

`model/placement/v1/AllocationCandidatesQuery.java`:
```java
package org.openstack4j.model.placement.v1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Query of {@code GET /allocation_candidates}. The unsuffixed group is set directly on the builder; granular
 * groups (placement 1.25+) are added with {@link Builder#group(String, Consumer)}.
 */
public final class AllocationCandidatesQuery {

    public enum GroupPolicy {
        NONE, ISOLATE;

        public String parameterValue() {
            return name().toLowerCase();
        }
    }

    /** One request group: resources plus optional trait, aggregate and tree constraints. */
    public static final class RequestGroup {
        private final Map<String, Long> resources = new LinkedHashMap<>();
        private final List<String> required = new ArrayList<>();
        private final List<String> memberOf = new ArrayList<>();
        private String inTree;

        public RequestGroup resources(String resourceClass, long amount) {
            resources.put(resourceClass, amount);
            return this;
        }

        /** {@code !TRAIT} forbids, {@code in:A,B} requires any (placement 1.39). */
        public RequestGroup required(String... traitExpressions) {
            Collections.addAll(required, traitExpressions);
            return this;
        }

        /** Each call adds one {@code member_of} parameter; {@code !uuid} forbids (placement 1.32). */
        public RequestGroup memberOf(String... aggregateExpressions) {
            Collections.addAll(memberOf, aggregateExpressions);
            return this;
        }

        /** Restrict the group to the tree of this provider (placement 1.31). */
        public RequestGroup inTree(String providerUuid) {
            this.inTree = providerUuid;
            return this;
        }

        public Map<String, Long> getResources() {
            return Collections.unmodifiableMap(resources);
        }

        public List<String> getRequired() {
            return Collections.unmodifiableList(required);
        }

        public List<String> getMemberOf() {
            return Collections.unmodifiableList(memberOf);
        }

        public String getInTree() {
            return inTree;
        }

        public String resourcesParameter() {
            return resources.isEmpty() ? null
                    : resources.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(","));
        }

        public String requiredParameter() {
            return required.isEmpty() ? null : String.join(",", required);
        }
    }

    private final Map<String, RequestGroup> groups;
    private final GroupPolicy groupPolicy;
    private final Integer limit;
    private final List<String> rootRequired;
    private final List<String> sameSubtree;

    private AllocationCandidatesQuery(Builder b) {
        this.groups = Collections.unmodifiableMap(new LinkedHashMap<>(b.groups));
        this.groupPolicy = b.groupPolicy;
        this.limit = b.limit;
        this.rootRequired = Collections.unmodifiableList(new ArrayList<>(b.rootRequired));
        this.sameSubtree = Collections.unmodifiableList(new ArrayList<>(b.sameSubtree));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @return request groups keyed by suffix; the unsuffixed group has key {@code ""} */
    public Map<String, RequestGroup> getGroups() {
        return groups;
    }

    public GroupPolicy getGroupPolicy() {
        return groupPolicy;
    }

    public Integer getLimit() {
        return limit;
    }

    public List<String> getRootRequired() {
        return rootRequired;
    }

    public List<String> getSameSubtree() {
        return sameSubtree;
    }

    public static final class Builder {
        private final Map<String, RequestGroup> groups = new LinkedHashMap<>();
        private GroupPolicy groupPolicy;
        private Integer limit;
        private final List<String> rootRequired = new ArrayList<>();
        private final List<String> sameSubtree = new ArrayList<>();

        private RequestGroup unsuffixed() {
            return groups.computeIfAbsent("", k -> new RequestGroup());
        }

        public Builder resources(String resourceClass, long amount) {
            unsuffixed().resources(resourceClass, amount);
            return this;
        }

        public Builder required(String... traitExpressions) {
            unsuffixed().required(traitExpressions);
            return this;
        }

        public Builder memberOf(String... aggregateExpressions) {
            unsuffixed().memberOf(aggregateExpressions);
            return this;
        }

        public Builder inTree(String providerUuid) {
            unsuffixed().inTree(providerUuid);
            return this;
        }

        /**
         * Adds a granular request group. Suffixes that are not plain numbers need placement 1.33.
         */
        public Builder group(String suffix, Consumer<RequestGroup> configure) {
            if (suffix == null || suffix.isEmpty())
                throw new IllegalArgumentException("a granular group needs a non-empty suffix");
            RequestGroup group = groups.computeIfAbsent(suffix, k -> new RequestGroup());
            configure.accept(group);
            return this;
        }

        /** Required when more than one granular group is present. */
        public Builder groupPolicy(GroupPolicy groupPolicy) {
            this.groupPolicy = groupPolicy;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        /** Traits the root provider must (or, with {@code !}, must not) have (placement 1.35). */
        public Builder rootRequired(String... traitExpressions) {
            Collections.addAll(rootRequired, traitExpressions);
            return this;
        }

        /** Group suffixes whose providers must share a subtree (placement 1.36). */
        public Builder sameSubtree(String... suffixes) {
            Collections.addAll(sameSubtree, suffixes);
            return this;
        }

        public AllocationCandidatesQuery build() {
            boolean anyResources = groups.values().stream().anyMatch(g -> !g.resources.isEmpty());
            if (!anyResources)
                throw new IllegalArgumentException("an allocation candidates query needs at least one resources entry");
            long granular = groups.keySet().stream().filter(k -> !k.isEmpty()).count();
            if (granular > 1 && groupPolicy == null)
                throw new IllegalArgumentException("group_policy is required when more than one granular group is requested");
            return new AllocationCandidatesQuery(this);
        }
    }
}
```

`model/placement/v1/AllocationCandidates.java`:
```java
package org.openstack4j.model.placement.v1;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** Response of {@code GET /allocation_candidates}. */
public interface AllocationCandidates extends ModelEntity {

    List<? extends Candidate> getAllocationRequests();

    /** @return summaries keyed by provider UUID */
    Map<String, ? extends ProviderSummary> getProviderSummaries();

    interface Candidate {
        /** @return provider UUID → (resource class → amount), ready to be sent as an allocation */
        Map<String, Map<String, Long>> getAllocations();

        /** @return request group suffix → provider UUIDs (placement 1.34), otherwise empty */
        Map<String, List<String>> getMappings();
    }

    interface ProviderSummary {
        Map<String, ? extends CapacityUsed> getResources();

        List<String> getTraits();

        String getParentProviderUuid();

        String getRootProviderUuid();
    }

    interface CapacityUsed {
        long getCapacity();

        long getUsed();
    }
}
```

- [ ] **Step 4: Jackson 모델과 구현**

`openstack/placement/v1/domain/PlacementAllocationCandidates.java`:
```java
package org.openstack4j.openstack.placement.v1.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.AllocationCandidates;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementAllocationCandidates implements AllocationCandidates {

    private static final long serialVersionUID = 1L;

    @JsonProperty("allocation_requests")
    private List<Request> allocationRequests;
    @JsonProperty("provider_summaries")
    private Map<String, Summary> providerSummaries;

    @Override
    public List<? extends Candidate> getAllocationRequests() {
        return allocationRequests == null ? Collections.emptyList() : allocationRequests;
    }

    @Override
    public Map<String, ? extends ProviderSummary> getProviderSummaries() {
        return providerSummaries == null ? Collections.emptyMap() : providerSummaries;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Request implements Candidate, Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("allocations")
        private Map<String, Resources> allocations;
        @JsonProperty("mappings")
        private Map<String, List<String>> mappings;

        @Override
        public Map<String, Map<String, Long>> getAllocations() {
            Map<String, Map<String, Long>> result = new LinkedHashMap<>();
            if (allocations != null)
                allocations.forEach((rp, r) -> result.put(rp, r.resources == null ? Collections.emptyMap() : r.resources));
            return result;
        }

        @Override
        public Map<String, List<String>> getMappings() {
            return mappings == null ? Collections.emptyMap() : mappings;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resources implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("resources")
        Map<String, Long> resources;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Summary implements ProviderSummary, Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("resources")
        private Map<String, Capacity> resources;
        @JsonProperty("traits")
        private List<String> traits;
        @JsonProperty("parent_provider_uuid")
        private String parentProviderUuid;
        @JsonProperty("root_provider_uuid")
        private String rootProviderUuid;

        @Override
        public Map<String, ? extends CapacityUsed> getResources() {
            return resources == null ? Collections.emptyMap() : resources;
        }

        @Override
        public List<String> getTraits() {
            return traits == null ? Collections.emptyList() : traits;
        }

        @Override
        public String getParentProviderUuid() {
            return parentProviderUuid;
        }

        @Override
        public String getRootProviderUuid() {
            return rootProviderUuid;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Capacity implements CapacityUsed, Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("capacity")
        private long capacity;
        @JsonProperty("used")
        private long used;

        @Override
        public long getCapacity() {
            return capacity;
        }

        @Override
        public long getUsed() {
            return used;
        }
    }
}
```

`api/placement/v1/AllocationCandidateService.java`:
```java
package org.openstack4j.api.placement.v1;

import org.openstack4j.common.RestService;
import org.openstack4j.model.placement.v1.AllocationCandidates;
import org.openstack4j.model.placement.v1.AllocationCandidatesQuery;

/** Scheduling candidates ({@code GET /allocation_candidates}). */
public interface AllocationCandidateService extends RestService {

    AllocationCandidates list(AllocationCandidatesQuery query);
}
```

`openstack/placement/v1/internal/AllocationCandidateServiceImpl.java`:
```java
package org.openstack4j.openstack.placement.v1.internal;

import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.placement.v1.AllocationCandidateService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.placement.v1.AllocationCandidates;
import org.openstack4j.model.placement.v1.AllocationCandidatesQuery;
import org.openstack4j.model.placement.v1.AllocationCandidatesQuery.RequestGroup;
import org.openstack4j.model.placement.v1.PlacementMicroVersions;
import org.openstack4j.openstack.placement.v1.domain.PlacementAllocationCandidates;

public class AllocationCandidateServiceImpl extends BasePlacementV1Service implements AllocationCandidateService {

    @Override
    public AllocationCandidates list(AllocationCandidatesQuery query) {
        Objects.requireNonNull(query, "query");
        checkVersions(query);
        Invocation<PlacementAllocationCandidates> invocation = placement(HttpMethod.GET, PlacementAllocationCandidates.class, "/allocation_candidates");
        for (Map.Entry<String, RequestGroup> entry : query.getGroups().entrySet()) {
            String suffix = entry.getKey();
            RequestGroup group = entry.getValue();
            invocation.param("resources" + suffix, group.resourcesParameter());
            invocation.param("required" + suffix, group.requiredParameter());
            invocation.param("in_tree" + suffix, group.getInTree());
            for (String aggregate : group.getMemberOf())
                invocation.param("member_of" + suffix, aggregate);
        }
        if (query.getGroupPolicy() != null) invocation.param("group_policy", query.getGroupPolicy().parameterValue());
        invocation.param("limit", query.getLimit());
        if (!query.getRootRequired().isEmpty()) invocation.param("root_required", String.join(",", query.getRootRequired()));
        if (!query.getSameSubtree().isEmpty()) invocation.param("same_subtree", String.join(",", query.getSameSubtree()));
        return executeOrThrow(invocation);
    }

    private void checkVersions(AllocationCandidatesQuery query) {
        for (Map.Entry<String, RequestGroup> entry : query.getGroups().entrySet()) {
            String suffix = entry.getKey();
            RequestGroup group = entry.getValue();
            if (!suffix.isEmpty() && !suffix.matches("\\d+"))
                requireMicroVersion("non-numeric request group suffixes such as '" + suffix + "'", PlacementMicroVersions.V1_33);
            if (group.getInTree() != null)
                requireMicroVersion("in_tree in allocation candidates", PlacementMicroVersions.V1_31);
            if (group.getMemberOf().stream().anyMatch(m -> m.startsWith("!")))
                requireMicroVersion("forbidden aggregates (member_of=!...)", PlacementMicroVersions.V1_32);
            if (group.getRequired().stream().anyMatch(t -> t.startsWith("in:")))
                requireMicroVersion("the trait 'in:' syntax", PlacementMicroVersions.V1_39);
        }
        if (!query.getRootRequired().isEmpty())
            requireMicroVersion("root_required", PlacementMicroVersions.V1_35);
        if (!query.getSameSubtree().isEmpty())
            requireMicroVersion("same_subtree", PlacementMicroVersions.V1_36);
    }
}
```
accessor `allocationCandidates()` 와 binding 추가.

- [ ] **Step 5: 테스트 통과 확인** — `-Dtest=AllocationCandidateTests`, Expected `Tests run: 7, Failures: 0, Errors: 0`
- [ ] **Step 6: 전체 빌드, 커밋, PR** — `feat(placement): add allocation candidates API (v1)`

---

### Task 10: Reshaper

**Files:**
- Create (core): `model/placement/v1/ReshapeRequest.java`, `api/placement/v1/ReshaperService.java`, `openstack/placement/v1/internal/ReshaperServiceImpl.java`
- Modify (core): accessor `reshaper()` + binding
- Create (test): `ReshaperTests.java`

**Interfaces:**
- Consumes: Task 3 `Inventory`/`PlacementInventory.from`, Task 8 `AllocationRequest`/`AllocationBodies.write`.
- Produces: `ReshapeRequest.builder().inventories(rp, generation, Map<String, Inventory>).allocation(consumer, AllocationRequest).build()`, `ReshaperService.reshape(ReshapeRequest)`.

- [ ] **Step 1: 테스트**

```bash
git switch main && git pull && git switch -c task/b10-reshaper
```

`ReshaperTests.java`:
```java
package org.openstack4j.api.placement.v1;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.placement.v1.AllocationRequest;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.ReshapeRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/Reshaper")
public class ReshaperTests extends AbstractPlacementTest {

    private static final String CHILD = "7b0c4d61-1d6b-4a3e-9d3b-8c1f2e5a9b11";
    private static final String CONSUMER = "96a38bed-26b5-410b-8cef-1913a2e0e0b8";

    private static ReshapeRequest request() {
        return ReshapeRequest.builder()
                .inventories(RP, 557L, Collections.singletonMap("VCPU", Inventory.builder().total(12).build()))
                .inventories(CHILD, 3L, Collections.singletonMap("VGPU", Inventory.builder().total(2).build()))
                .allocation(CONSUMER, AllocationRequest.builder().projectId("p").userId("u").consumerGeneration(1L).consumerType("INSTANCE")
                        .allocation(RP, "VCPU", 1).allocation(CHILD, "VGPU", 1).build())
                .build();
    }

    public void reshapePostsInventoriesAndAllocations() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        osv3().placement().reshaper().reshape(request());
        RecordedRequest post = takeVersionAndRequest();
        JsonNode body = body(post);

        Assert.assertEquals(post.getMethod(), "POST");
        Assert.assertEquals(post.getPath(), "/reshaper");
        Assert.assertEquals(body.get("inventories").get(RP).get("resource_provider_generation").asLong(), 557L);
        Assert.assertEquals(body.get("inventories").get(CHILD).get("inventories").get("VGPU").get("total").asLong(), 2L);
        Assert.assertEquals(body.get("allocations").get(CONSUMER).get("allocations").get(CHILD).get("resources").get("VGPU").asLong(), 1L);
        Assert.assertEquals(body.get("allocations").get(CONSUMER).get("consumer_generation").asLong(), 1L);
        Assert.assertEquals(body.get("allocations").get(CONSUMER).get("consumer_type").asText(), "INSTANCE");
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.30.*")
    public void reshapeRequires130() throws Exception {
        respondWithVersions("1.29");
        try {
            osv3().placement().reshaper().reshape(request());
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

- [ ] **Step 3: 구현**

`model/placement/v1/ReshapeRequest.java`:
```java
package org.openstack4j.model.placement.v1;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** Body of {@code POST /reshaper}: new inventories for several providers and the allocations that move with them. */
public final class ReshapeRequest implements ModelEntity {

    private static final long serialVersionUID = 1L;

    public static final class ProviderInventories implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final long generation;
        private final Map<String, Inventory> inventories;

        ProviderInventories(long generation, Map<String, ? extends Inventory> inventories) {
            this.generation = generation;
            this.inventories = Collections.unmodifiableMap(new LinkedHashMap<>(inventories));
        }

        public long getGeneration() {
            return generation;
        }

        public Map<String, Inventory> getInventories() {
            return inventories;
        }
    }

    private final Map<String, ProviderInventories> inventories;
    private final Map<String, AllocationRequest> allocations;

    private ReshapeRequest(Builder b) {
        this.inventories = Collections.unmodifiableMap(new LinkedHashMap<>(b.inventories));
        this.allocations = Collections.unmodifiableMap(new LinkedHashMap<>(b.allocations));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, ProviderInventories> getInventories() {
        return inventories;
    }

    public Map<String, AllocationRequest> getAllocations() {
        return allocations;
    }

    public static final class Builder {
        private final Map<String, ProviderInventories> inventories = new LinkedHashMap<>();
        private final Map<String, AllocationRequest> allocations = new LinkedHashMap<>();

        /** The complete new inventories of a provider, with its current generation. */
        public Builder inventories(String providerUuid, long generation, Map<String, ? extends Inventory> providerInventories) {
            inventories.put(providerUuid, new ProviderInventories(generation, providerInventories));
            return this;
        }

        public Builder allocation(String consumerUuid, AllocationRequest request) {
            allocations.put(consumerUuid, request);
            return this;
        }

        public ReshapeRequest build() {
            if (inventories.isEmpty())
                throw new IllegalArgumentException("a reshape needs inventories for at least one provider");
            return new ReshapeRequest(this);
        }
    }
}
```

`api/placement/v1/ReshaperService.java`:
```java
package org.openstack4j.api.placement.v1;

import org.openstack4j.common.RestService;
import org.openstack4j.model.placement.v1.ReshapeRequest;

/** Atomic inventory and allocation migration ({@code POST /reshaper}, placement 1.30). */
public interface ReshaperService extends RestService {

    void reshape(ReshapeRequest request);
}
```

`openstack/placement/v1/internal/ReshaperServiceImpl.java`:
```java
package org.openstack4j.openstack.placement.v1.internal;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openstack4j.api.placement.v1.ReshaperService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.placement.v1.AllocationRequest;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.PlacementMicroVersions;
import org.openstack4j.model.placement.v1.ReshapeRequest;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.placement.v1.domain.PlacementInventory;

public class ReshaperServiceImpl extends BasePlacementV1Service implements ReshaperService {

    private static final ObjectMapper MAPPER = ObjectMapperSingleton.getContext(Object.class);

    @Override
    public void reshape(ReshapeRequest request) {
        Objects.requireNonNull(request, "request");
        requireMicroVersion("reshaper", PlacementMicroVersions.V1_30);
        MicroVersion version = microVersion();
        ObjectNode body = MAPPER.createObjectNode();
        ObjectNode inventories = body.putObject("inventories");
        for (Map.Entry<String, ReshapeRequest.ProviderInventories> provider : request.getInventories().entrySet()) {
            ObjectNode node = inventories.putObject(provider.getKey());
            node.put("resource_provider_generation", provider.getValue().getGeneration());
            ObjectNode byClass = node.putObject("inventories");
            for (Map.Entry<String, Inventory> inventory : provider.getValue().getInventories().entrySet())
                byClass.set(requireName("resource class", inventory.getKey()), MAPPER.valueToTree(PlacementInventory.from(inventory.getValue())));
        }
        ObjectNode allocations = body.putObject("allocations");
        for (Map.Entry<String, AllocationRequest> consumer : request.getAllocations().entrySet())
            AllocationBodies.write(allocations.putObject(consumer.getKey()), consumer.getValue(), version);
        executeOrThrow(placement(HttpMethod.POST, Void.class, "/reshaper").json(body.toString()));
    }
}
```
accessor `reshaper()` 와 binding 추가.

- [ ] **Step 4: 테스트 통과 확인** — `-Dtest=ReshaperTests`, Expected `Tests run: 2, Failures: 0, Errors: 0`
- [ ] **Step 5: 전체 빌드, 커밋, PR** — `feat(placement): add reshaper API (v1)`

---

### Task 11: 실환경 통합 테스트와 문서

**Files:**
- Create (test): `core-test/src/main/java/org/openstack4j/api/placement/v1/PlacementLiveTests.java`
- Modify: `README.md` (Placement 절), `CHANGELOG.md` (4.1.0 항목)

**Interfaces:**
- Consumes: Task 1~10 의 모든 서비스.

- [ ] **Step 1: 실환경 테스트 작성**

```bash
git switch main && git pull && git switch -c task/b11-live-tests-docs
```

`PlacementLiveTests.java` — `AbstractTest` 를 상속하지 않는다(MockWebServer 가 필요 없다).
```java
package org.openstack4j.api.placement.v1;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.placement.PlacementService;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.placement.v1.AllocationCandidates;
import org.openstack4j.model.placement.v1.AllocationCandidatesQuery;
import org.openstack4j.model.placement.v1.AllocationRequest;
import org.openstack4j.model.placement.v1.ConsumerAllocations;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.PlacementVersion;
import org.openstack4j.model.placement.v1.ResourceCapacity;
import org.openstack4j.model.placement.v1.ResourceProvider;
import org.openstack4j.model.placement.v1.ResourceProviderCreate;
import org.openstack4j.model.placement.v1.ResourceProviderInventories;
import org.openstack4j.model.placement.v1.ResourceProviderListOptions;
import org.openstack4j.openstack.OSFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Runs against a real OpenStack when the standard OS_* environment variables are set; skipped otherwise.
 * Creates a throw-away provider, resource class, trait and consumer and deletes them in {@code finally}.
 */
@Test(suiteName = "Placement/Live", groups = "placement-live")
public class PlacementLiveTests {

    private PlacementService placement;
    private String projectId;
    private String userId;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live Placement tests");
        String domain = env("OS_USER_DOMAIN_NAME", "Default");
        OSClientV3 os = OSFactory.builderV3()
                .endpoint(url.replaceAll("/+$", "").endsWith("/v3") ? url : url.replaceAll("/+$", "") + "/v3")
                .credentials(env("OS_USERNAME", null), env("OS_PASSWORD", null), Identifier.byName(domain))
                .scopeToProject(Identifier.byName(env("OS_PROJECT_NAME", null)), Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default")))
                .authenticate();
        placement = os.placement();
        projectId = os.getToken().getProject().getId();
        userId = os.getToken().getUser().getId();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live Placement tests");
        }
        return value;
    }

    public void negotiatesAVersion() {
        PlacementVersion version = placement.versions().get();
        Assert.assertNotNull(version.getMicroVersion(), "server too old: " + version.getServerMaxVersion());
        Assert.assertFalse(version.isPinned());
    }

    public void readsExistingProvidersWithoutChangingThem() {
        for (ResourceProvider rp : placement.providers().list()) {
            Assert.assertNotNull(placement.providers().get(rp.getUuid()));
            placement.inventories().list(rp.getUuid());
            placement.usages().forProvider(rp.getUuid());
            placement.traits().listForProvider(rp.getUuid());
            placement.aggregates().listForProvider(rp.getUuid());
            placement.allocations().listForProvider(rp.getUuid());
            Map<String, ResourceCapacity> capacity = placement.usages().capacity(rp.getUuid());
            for (ResourceCapacity c : capacity.values())
                Assert.assertEquals(c.getFree(), c.getCapacity() - c.getUsed());
        }
        placement.usages().forProject(projectId, null, null);
        Assert.assertTrue(placement.resourceClasses().exists("VCPU"));
        Assert.assertFalse(placement.traits().list().isEmpty());
    }

    public void fullWriteRoundTripOnThrowAwayResources() {
        String tag = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String rcName = "CUSTOM_OS4J_IT_" + tag;
        String traitName = "CUSTOM_OS4J_IT_" + tag;
        String aggregate = UUID.randomUUID().toString();
        String consumer = UUID.randomUUID().toString();
        String rp = null;
        try {
            placement.resourceClasses().create(rcName);
            placement.traits().create(traitName);
            Assert.assertTrue(placement.resourceClasses().exists(rcName));
            Assert.assertTrue(placement.traits().exists(traitName));

            ResourceProvider created = placement.providers().create(ResourceProviderCreate.builder().name("os4j-it-" + tag).build());
            rp = created.getUuid();
            Assert.assertEquals(placement.providers().get(rp).getName(), "os4j-it-" + tag);

            ResourceProviderInventories inv = placement.inventories().replace(rp, created.getGeneration(),
                    Collections.singletonMap(rcName, Inventory.builder().total(10).reserved(2).allocationRatio(1.0f).maxUnit(10).build()));
            Assert.assertEquals(inv.getInventories().get(rcName).getTotal(), 10L);
            long gen = inv.getResourceProviderGeneration();

            gen = placement.traits().replaceForProvider(rp, gen, Collections.singletonList(traitName)).getResourceProviderGeneration();
            gen = placement.aggregates().replaceForProvider(rp, gen, Collections.singletonList(aggregate)).getResourceProviderGeneration();

            Assert.assertEquals(placement.providers().list(ResourceProviderListOptions.create().memberOf(aggregate).required(traitName)).size(), 1);

            AllocationCandidates candidates = placement.allocationCandidates().list(AllocationCandidatesQuery.builder()
                    .resources(rcName, 3).required(traitName).memberOf(aggregate).build());
            Assert.assertEquals(candidates.getAllocationRequests().size(), 1);

            String consumerType = placement.versions().get().getMicroVersion().compareTo("1.38") >= 0 ? "OS4J_IT" : null;
            placement.allocations().set(consumer, AllocationRequest.builder().projectId(projectId).userId(userId)
                    .consumerType(consumerType).allocation(rp, rcName, 3).build());
            ConsumerAllocations allocations = placement.allocations().get(consumer);
            Assert.assertEquals(allocations.getAllocations().get(rp).getResources().get(rcName), Long.valueOf(3));
            Assert.assertEquals(placement.usages().capacity(rp).get(rcName).getFree(), 5L); // (10 - 2) * 1.0 - 3

            placement.allocations().set(consumer, AllocationRequest.builder().projectId(projectId).userId(userId)
                    .consumerType(consumerType).consumerGeneration(allocations.getConsumerGeneration()).allocation(rp, rcName, 4).build());
            Assert.assertEquals(placement.usages().forProvider(rp).getUsages().get(rcName), Long.valueOf(4));
        } finally {
            placement.allocations().delete(consumer);
            if (rp != null) {
                placement.inventories().deleteAll(rp);
                placement.traits().deleteForProvider(rp);
                placement.providers().delete(rp);
            }
            placement.traits().delete(traitName);
            placement.resourceClasses().delete(rcName);
        }
        Assert.assertNull(placement.providers().get(rp));
        Assert.assertFalse(placement.resourceClasses().exists(rcName));
        Assert.assertFalse(placement.traits().exists(traitName));
    }
}
```
`getMicroVersion().compareTo("1.38")` 는 문자열 비교이므로 `"1.4"` 같은 값에서 틀릴 수 있다. 대신 `new org.openstack4j.openstack.internal.MicroVersion(placement.versions().get().getMicroVersion()).compareTo(PlacementMicroVersions.V1_38) >= 0` 로 쓴다.

- [ ] **Step 2: 환경 변수 없이 실행 → 전부 Skipped 확인**

Run: 공통 테스트 명령에 `-Dtest=PlacementLiveTests`
Expected: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 3`

- [ ] **Step 3: 개발용 OpenStack 에서 실행**

```bash
OS_AUTH_URL=http://192.168.140.12:5000/v3 OS_USERNAME=admin OS_PASSWORD=<비밀번호> OS_PROJECT_NAME=admin \
  ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest=PlacementLiveTests -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL'
```
Expected: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`. 실행 후 잔여 자원 확인:
```bash
# 토큰 발급 후
curl -s -H "X-Auth-Token: $T" -H 'OpenStack-API-Version: placement 1.39' 'http://192.168.140.18:8780/resource_providers?name=os4j-it' ; curl -s -H "X-Auth-Token: $T" 'http://192.168.140.18:8780/traits?name=startswith:CUSTOM_OS4J_IT'
```
Expected: 두 응답 모두 빈 목록. 실패했다면 `finally` 의 정리 순서(allocation → inventories → provider traits → provider → trait → resource class)를 로그로 확인해 고친다.

- [ ] **Step 4: README 와 CHANGELOG**

`README.md` 의 `## 사용 예` 절 뒤에 추가:
````markdown
## Placement

4.1.0 부터 Placement API 전체(microversion 1.28~1.39)를 지원합니다. 서버의 microversion 범위를 자동으로 협상하고, 원하면 고정할 수 있습니다.

```java
PlacementService placement = os.placement();
placement.versions().get();                      // 서버 범위와 협상된 버전
placement.useMicroVersion("1.36");               // 선택: 버전 고정

// 용량 조회
Map<String, ResourceCapacity> capacity = placement.usages().capacity(rpUuid);

// inventory 수정 (generation 충돌 시 재시도)
Placement.retryOnConcurrentUpdate(3, () -> {
    ResourceProviderInventories inv = placement.inventories().list(rpUuid);
    return placement.inventories().update(rpUuid, inv.getResourceProviderGeneration(), "VCPU",
            Inventory.builder().total(64).allocationRatio(4.0f).build());
});

// 스케줄링 후보
placement.allocationCandidates().list(AllocationCandidatesQuery.builder()
        .resources("VCPU", 2).resources("MEMORY_MB", 2048).required("HW_CPU_X86_AVX2").limit(10).build());
```

기존 `placement().resourceProviders()` 는 그대로 동작합니다. 1.28 보다 오래된 서버에서는 새 API 가 `PlacementMicroVersionException` 을 던집니다.
````

`CHANGELOG.md` 맨 위에 추가:
```markdown
## 4.1.0

- Placement API 전체 지원 (`os.placement().providers()/inventories()/resourceClasses()/traits()/aggregates()/allocations()/allocationCandidates()/usages()/reshaper()/versions()`), microversion 1.28~1.39 자동 협상과 `useMicroVersion` 고정
- Placement 오류를 `PlacementException`/`PlacementConcurrentUpdateException` 으로 매핑, `Placement.retryOnConcurrentUpdate` 도우미
- `usages().capacity()` 로 provider 별 자원 용량·사용량·여유량 계산
- 기존 `placement().resourceProviders()` 는 변경 없음

```

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B -q verify
git add -A
git commit -m "test(placement): add live integration tests and document the Placement API

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```
PR 본문에 Step 3 의 실행 결과(3/3 통과, 잔여 자원 없음)를 적는다.

---

### Task 12: 전체 리뷰와 4.1.0 릴리스

- [ ] **Step 1: 리뷰 패키지와 리뷰어**

`git merge-base`: Task 1 PR 의 머지 직전 main 커밋(= P1 머지 커밋). `review-package` 로 diff 를 만들고 가장 강한 모델의 리뷰어에게 spec, plan, Review Focus, ledger 의 `Ruling:` 과 함께 넘긴다. 특히 확인할 것: microversion 캐시 키(세션 identity), `AllocationBodies` 의 1.38 규칙, 404 → null 과 `exists` 의 `ActionResponse` 경로, 모든 쓰기 메서드가 `executeOrThrow` 를 쓰는지(`execute()` 로 빠진 곳이 있으면 Placement 오류가 일반 `ResponseException` 으로 새어 나간다), fixture 와 실제 응답 형식의 차이, 공개 API 의 Javadoc 누락.

- [ ] **Step 2: Critical/Important 수정 (RED→GREEN), Minor 는 ledger 로**

- [ ] **Step 3: 🛑 `v4.1.0` 배포 (사용자 확인 필수)**

```bash
git switch main && git pull
./mvnw -B -q verify && .github/scripts/verify-artifacts.sh
git tag -a v4.1.0 -m "OpenStack4j 4.1.0: full Placement API"
git push origin v4.1.0
gh run watch -R seogineer/openstack4j $(gh run list -R seogineer/openstack4j -w Release -L 1 --json databaseId -q '.[0].databaseId')
```
Expected: Release workflow 성공(validated 까지 대기). 이후 repo1 확인(수십 분):
```bash
curl -s -o /dev/null -w '%{http_code}\n' https://repo1.maven.org/maven2/io/github/seogineer/openstack4j-core/4.1.0/openstack4j-core-4.1.0.pom
```
Expected: `200`. `gh release view v4.1.0 -R seogineer/openstack4j` 가 prerelease 가 아닌지 확인.

- [ ] **Step 4: Central 의 4.1.0 으로 실환경 확인**

`~/openstack4j-check/pom.xml` 의 버전을 `4.1.0` 으로 바꾸고 `rm -f cp.txt && ./run.sh` (openrc 환경 변수 필요). Expected: 인증·Nova·Placement 조회 성공. 추가로 `Check.java` 에 `os.placement().versions().get().getMicroVersion()` 출력을 한 줄 넣어 `1.39` 가 나오는지 본다.

- [ ] **Step 5: 다음 개발 버전**

```bash
git switch -c chore/bump-4.2.0-snapshot
./mvnw -B -q versions:set -DnewVersion=4.2.0-SNAPSHOT -DgenerateBackupPoms=false -DprocessAllModules=true
sed -i 's|<openstack4j.version>4.1.0-SNAPSHOT</openstack4j.version>|<openstack4j.version>4.2.0-SNAPSHOT</openstack4j.version>|' examples/spring-boot-smoke/pom.xml
./mvnw -B -q verify
git add -A && git commit -m "chore: bump version to 4.2.0-SNAPSHOT

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```
공통 PR 절차.
