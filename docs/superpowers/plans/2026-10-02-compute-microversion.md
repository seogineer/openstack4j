# Nova(compute) microversion 프레임워크와 누락 API (C) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 기존 `os.compute()` 에 opt-in microversion 협상(2.1~2.104)을 추가하고, 그 위에서 2.1 이후의 응답 필드·요청 옵션·누락 엔드포인트를 채워 4.2.0 으로 배포한다.

**Architecture:** `BaseOpenStackService` 에 요청 장식 훅(`decorate`)을 추가하고, 공용 `openstack.internal.microversion` 유틸(세션·endpoint 별 상태 저장)을 둔다. `BaseComputeServices` 가 훅을 구현해, microversion 이 켜져 있을 때만 `OpenStack-API-Version`/`X-OpenStack-Nova-API-Version` 헤더를 붙이고, 클래스·메서드별 상한(ceiling)으로 Nova 가 제거한 레거시 API 를 보호하며, 새 기능은 하한(floor)을 검사한다. 꺼져 있으면 기존 동작과 바이트 단위로 같다.

**Tech Stack:** Java 17, Jackson 2.x, TestNG 7, OkHttp MockWebServer 4.12, Maven Wrapper.

**Spec:** `docs/superpowers/specs/2026-10-02-compute-microversion-design.md`

## Global Constraints

- 기존 메서드의 시그니처·반환 타입·**microversion 이 꺼졌을 때의 동작**은 바꾸지 않는다. `@Deprecated` 를 새로 붙이지 않는다. 기존 compute 테스트(core-test `api/compute/**`)는 **수정 없이** 통과해야 한다.
- 라이브러리 최고 compute microversion **2.104**, 최소 **2.1**. 개발용 OpenStack(epoxy) Nova 최대 2.100.
- 헤더(켜졌을 때만): `OpenStack-API-Version: compute <v>`, `X-OpenStack-Nova-API-Version: <v>`. 요청 버전 = min(적용 버전, 클래스 상한, 메서드 상한).
- 하한 미달·꺼짐 상태에서 새 기능 호출 → **서버 요청 없이** `org.openstack4j.api.exceptions.MicroVersionException`.
- 공용 예외 `MicroVersionException extends OS4JException`; `PlacementMicroVersionException` 의 부모만 이것으로 바꾼다.
- 410 API 와 2.6/2.44 에서 제거된 엔드포인트는 새로 추가하지 않는다.
- 회사 코드(`~/IdeaProjects/openstackit-java`)는 참고·복사하지 않는다.
- 각 Task = 브랜치 + PR, CI(JDK 17/21/25, 아티팩트 검사, 스모크) 통과 후 squash 머지(사전 승인). tag push(배포)는 매번 사용자 확인.
- 커밋 끝 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`, PR 본문 끝 `🤖 Generated with [Claude Code](https://claude.com/claude-code)`.
- 빌드 `./mvnw`. core-test 클래스 실행: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='<Class>' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test`.
- PR 도우미: `.superpowers/pr.sh "<본문>"` (push → PR → CI 대기 → squash 머지, git-ignored).
- compute 구현 코드의 `V(n)` 은 `import static org.openstack4j.openstack.compute.internal.ComputeMicroVersions.V;`, 버전 문자열 파싱은 `MicroVersions.parse`(Task 1)다. plan 의 코드 조각에는 import 를 생략했으니 IDE/컴파일러가 요구하는 대로 추가한다.
- 새 테스트는 모두 `org.openstack4j.api.compute.microversion` 패키지에 둔다. `all.xml` 이 `org.openstack4j.*` 를 스캔하므로 별도 등록이 필요 없다.
- 요청 본문에 명시적 `null` 이 필요하면 `Map` 이 아니라 `JsonBody`(Task 5)를 쓴다. 전역 mapper 가 `NON_NULL` 이라 `Map` 의 `null` 값은 빠진다.

## Review Focus

1. **microversion 을 켜지 않은 사용자에게 아무 변화가 없어야 한다** — 헤더 0개, 추가 요청 0개(루트 조회 없음). → Task 2 `noHeadersAndNoDiscoveryWhenDisabled`.
2. **2.100 이상을 켠 상태의 레거시 호출**(프록시 API, os-hosts, 공개키 없는 키페어 생성, personality·네트워크 미지정 서버 생성, 기존 diagnostics/VNC 콘솔/enable·disable 서비스, 2.101 의 볼륨 연결, 2.102 의 플레이버 생성) — 각자 상한 버전으로 나가 계속 동작해야 한다. → Task 2 `legacyMethodsAreCapped*`.
3. **2.47 이후 서버의 `flavor` 에 id 가 없음** — `getFlavor()` 가 `get(null)` 을 호출해 NPE/404 를 내면 안 된다. → Task 3 `flavorEmbeddedFormDoesNotTriggerLookup`.
4. **createSnapshot 의 이미지 id** — 2.45 이상은 `Location` 헤더가 없고 본문 `image_id` 로 온다. 두 경우 모두 id 를 돌려줘야 한다. → Task 2 `createSnapshotReadsImageIdFromBodyOn245`.
5. **endpoint 에 경로 접두사·tenant id 가 있는 배포**(`https://cloud/compute/v2.1/<tenant>`)의 버전 조회 — 루트 `GET /` 을 올바른 URL 로 보내야 한다. → Task 2 `discoveryStripsVersionAndTenantFromEndpoint`(단위: 루트 계산 함수).

---

## 실행 전 준비

- [ ] **P1: plan PR 머지** — 이 plan 을 `docs/compute-microversion-plan` 브랜치에 커밋해 `.superpowers/pr.sh` 로 머지한다.

### 공통 절차
- 각 Task 시작: `git switch main && git pull && git switch -c task/c<N>-<이름>`
- 끝: 전체 빌드 `./mvnw -B --no-transfer-progress install` 결과 `BUILD SUCCESS` 확인 → 커밋 → `.superpowers/pr.sh "<요약>"`.

### fixture 원본
개발용 epoxy(Nova 2.100)에서 캡처한 응답을 `core-test/src/main/resources/compute/microversion/` 에 둔다. IP·호스트명은 `10.0.0.x`/`compute-1` 로 일반화한다. 원본 캡처 위치: `/tmp/claude-1000/-home-seogineer-IdeaProjects/15c57ef7-653e-4e49-bba1-5d7ddc3e068a/scratchpad/nova/*.json`(사라졌으면 `scratchpad/nova/capture.sh` 와 같은 방식으로 다시 캡처).

---

### Task 1: 공용 microversion 유틸, 공용 예외, 요청 장식 훅

**Files:**
- Create: `core/src/main/java/org/openstack4j/api/exceptions/MicroVersionException.java`
- Create: `core/src/main/java/org/openstack4j/openstack/internal/microversion/MicroVersionState.java`, `MicroVersionStore.java`, `MicroVersions.java`
- Modify: `core/src/main/java/org/openstack4j/api/placement/v1/exceptions/PlacementMicroVersionException.java` (부모 변경)
- Modify: `core/src/main/java/org/openstack4j/openstack/internal/BaseOpenStackService.java` (`decorate` 훅)
- Test: `core/src/test/java/org/openstack4j/test/microversion/MicroVersionStoreTest.java`

**Interfaces:**
- Produces:
  - `MicroVersionException(String message)`
  - `MicroVersionState`: `getServerMin()`, `getServerMax()`, `getPinned()`, `setPinned(MicroVersion)`, `isEnabled()`, `setEnabled(boolean)`; 생성자 `(MicroVersion serverMin, MicroVersion serverMax)`
  - `MicroVersionStore`: `static MicroVersionState get(Object session, String key)`, `static MicroVersionState putIfAbsent(Object session, String key, MicroVersionState)`, `static void clearAll()`
  - `MicroVersions`: `static MicroVersion min(MicroVersion, MicroVersion)`, `max(...)`, `parse(String)` (형식 오류 → `MicroVersionException`)
  - `BaseOpenStackService`: `protected <R> Invocation<R> decorate(Invocation<R> invocation)` (기본: 그대로 반환), 모든 요청 생성 경로가 반환 직전에 호출

- [ ] **Step 1: 테스트 작성**

```bash
git switch main && git pull && git switch -c task/c1-microversion-util
mkdir -p core/src/test/java/org/openstack4j/test/microversion
```

`core/src/test/java/org/openstack4j/test/microversion/MicroVersionStoreTest.java`:
```java
package org.openstack4j.test.microversion;

import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionStore;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class MicroVersionStoreTest {

    @BeforeMethod
    public void clear() {
        MicroVersionStore.clearAll();
    }

    @Test
    public void stateIsKeptPerSessionIdentityAndKey() {
        Object sessionA = new Object();
        Object sessionB = new Object();
        MicroVersionState a = MicroVersionStore.putIfAbsent(sessionA, "compute|http://x", new MicroVersionState(new MicroVersion(2, 1), new MicroVersion(2, 100)));

        Assert.assertSame(MicroVersionStore.get(sessionA, "compute|http://x"), a);
        Assert.assertNull(MicroVersionStore.get(sessionA, "compute|http://y"));
        Assert.assertNull(MicroVersionStore.get(sessionB, "compute|http://x"));
    }

    @Test
    public void putIfAbsentKeepsTheFirstState() {
        Object session = new Object();
        MicroVersionState first = MicroVersionStore.putIfAbsent(session, "k", new MicroVersionState(new MicroVersion(2, 1), new MicroVersion(2, 50)));
        MicroVersionState second = MicroVersionStore.putIfAbsent(session, "k", new MicroVersionState(new MicroVersion(2, 1), new MicroVersion(2, 99)));

        Assert.assertSame(second, first);
        Assert.assertEquals(second.getServerMax().toString(), "2.50");
    }

    @Test
    public void newStateIsDisabledAndUnpinned() {
        MicroVersionState state = new MicroVersionState(new MicroVersion(2, 1), new MicroVersion(2, 100));
        Assert.assertFalse(state.isEnabled());
        Assert.assertNull(state.getPinned());
    }

    @Test
    public void minMaxAndParse() {
        Assert.assertEquals(MicroVersions.min(new MicroVersion(2, 104), new MicroVersion(2, 100)).toString(), "2.100");
        Assert.assertEquals(MicroVersions.max(new MicroVersion(2, 1), new MicroVersion(2, 9)).toString(), "2.9");
        Assert.assertEquals(MicroVersions.parse("2.47").toString(), "2.47");
    }

    @Test(expectedExceptions = MicroVersionException.class)
    public void parseRejectsGarbage() {
        MicroVersions.parse("latest");
    }

    @Test
    public void placementExceptionIsAMicroVersionException() {
        Assert.assertTrue(new PlacementMicroVersionException("x") instanceof MicroVersionException);
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q test -pl core -Dtest=MicroVersionStoreTest 2>&1 | grep -E 'does not exist|cannot find symbol' | head -2`
Expected: `package org.openstack4j.openstack.internal.microversion does not exist`

- [ ] **Step 3: 구현**

`api/exceptions/MicroVersionException.java`:
```java
package org.openstack4j.api.exceptions;

/**
 * The requested microversion or a feature that needs a newer microversion is not available. Raised before any request
 * is sent.
 */
public class MicroVersionException extends OS4JException {

    private static final long serialVersionUID = 1L;

    public MicroVersionException(String message) {
        super(message);
    }
}
```

`PlacementMicroVersionException.java` — `import org.openstack4j.api.exceptions.OS4JException;` 를 `import org.openstack4j.api.exceptions.MicroVersionException;` 로, `extends OS4JException` 을 `extends MicroVersionException` 으로 바꾼다(그 외 변경 없음).

`openstack/internal/microversion/MicroVersionState.java`:
```java
package org.openstack4j.openstack.internal.microversion;

import org.openstack4j.openstack.internal.MicroVersion;

/** Server microversion range of one service endpoint in one session, plus the session's choice. */
public final class MicroVersionState {

    private final MicroVersion serverMin;
    private final MicroVersion serverMax;
    private volatile MicroVersion pinned;
    private volatile boolean enabled;

    public MicroVersionState(MicroVersion serverMin, MicroVersion serverMax) {
        this.serverMin = serverMin;
        this.serverMax = serverMax;
    }

    public MicroVersion getServerMin() {
        return serverMin;
    }

    public MicroVersion getServerMax() {
        return serverMax;
    }

    public MicroVersion getPinned() {
        return pinned;
    }

    public void setPinned(MicroVersion pinned) {
        this.pinned = pinned;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
```

`openstack/internal/microversion/MicroVersionStore.java`:
```java
package org.openstack4j.openstack.internal.microversion;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Microversion state keyed by client session identity and a service key (for example {@code "compute|<endpoint>"}).
 * Weakly keyed so a discarded session releases its state.
 */
public final class MicroVersionStore {

    private static final Map<Object, Map<String, MicroVersionState>> STATES = new WeakHashMap<>();

    private MicroVersionStore() {
    }

    public static synchronized MicroVersionState get(Object session, String key) {
        Map<String, MicroVersionState> byKey = STATES.get(session);
        return byKey == null ? null : byKey.get(key);
    }

    public static synchronized MicroVersionState putIfAbsent(Object session, String key, MicroVersionState state) {
        return STATES.computeIfAbsent(session, s -> new HashMap<>()).merge(key, state, (existing, ignored) -> existing);
    }

    /** Forgets all state. Intended for tests. */
    public static synchronized void clearAll() {
        STATES.clear();
    }
}
```

`openstack/internal/microversion/MicroVersions.java`:
```java
package org.openstack4j.openstack.internal.microversion;

import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.openstack.internal.MicroVersion;

public final class MicroVersions {

    private MicroVersions() {
    }

    public static MicroVersion min(MicroVersion a, MicroVersion b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static MicroVersion max(MicroVersion a, MicroVersion b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static MicroVersion parse(String value) {
        try {
            return new MicroVersion(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new MicroVersionException("Invalid microversion '" + value + "': expected 'X.Y'");
        }
    }
}
```

`BaseOpenStackService.java` — `builder(Class<R>, String, HttpMethod)` 의 두 `return new Invocation<>(...)...;` 를 각각 `return decorate(new Invocation<>(...)...);` 로 감싸고, 클래스에 메서드 추가:
```java
    /**
     * Hook for subclasses to adjust every request this service builds (for example to add microversion headers).
     * The default returns the invocation unchanged.
     */
    protected <R> Invocation<R> decorate(Invocation<R> invocation) {
        return invocation;
    }
```

- [ ] **Step 4: 통과 확인**

Run: `./mvnw -B test -pl core -Dtest=MicroVersionStoreTest 2>&1 | grep -E 'Tests run:' | tail -1`
Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat: add shared microversion state store, MicroVersionException and request decorate hook

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
.superpowers/pr.sh "공용 microversion 유틸(MicroVersionStore/State/MicroVersions), 공용 MicroVersionException(PlacementMicroVersionException 의 부모), BaseOpenStackService.decorate 훅."
```

---

### Task 2: compute microversion 적용 — 협상·고정, 헤더, 상한

**Files:**
- Create (core, `core/src/main/java/org/openstack4j/` 아래): `model/compute/ComputeVersion.java`, `api/compute/ComputeMicroVersionService.java`, `openstack/compute/domain/NovaVersions.java`, `openstack/compute/domain/NovaComputeVersion.java`, `openstack/compute/internal/ComputeMicroVersions.java`, `openstack/compute/internal/ComputeVersionDiscovery.java`, `openstack/compute/internal/ComputeMicroVersionServiceImpl.java`
- Modify (core): `openstack/compute/internal/BaseComputeServices.java`, `api/compute/ComputeService.java`, `openstack/compute/internal/ComputeServiceImpl.java`, `openstack/provider/DefaultAPIProvider.java`, 상한 대상 구현 클래스(`ComputeImageServiceImpl`, `ComputeFloatingIPServiceImpl`, `ComputeSecurityGroupServiceImpl`, `ext/FloatingIPDNSDomainServiceImpl`, `ext/FloatingIPDNSEntryServiceImpl`, `HostServiceImpl`, `ServerServiceImpl`, `ServicesServiceImpl`, `ext/HypervisorServiceImpl`, `KeypairServiceImpl`, `ServerGroupServiceImpl`, `ServerTagServiceImpl`)
- Create (test): `core-test/src/main/java/org/openstack4j/api/compute/microversion/AbstractComputeMicroVersionTest.java`, `ComputeMicroVersionTests.java`, `LegacyCeilingTests.java`; `core/src/test/java/org/openstack4j/test/microversion/ComputeRootUrlTest.java`

**Interfaces:**
- Consumes: Task 1 전부.
- Produces:
  - `ComputeService.microVersions()` → `ComputeMicroVersionService`: `ComputeVersion negotiate()`, `ComputeVersion use(String version)`, `void clear()`, `ComputeVersion get()`
  - `ComputeVersion`: `getServerMinVersion()`, `getServerMaxVersion()`, `getMicroVersion()`(꺼짐 → null), `isPinned()`, `isEnabled()`
  - `ComputeMicroVersions`: `LATEST`(2.104), `MINIMUM`(2.1), 상수 `V(int minor)` → `MicroVersion`, `static MicroVersionState currentState()`, `static String rootUrl(String endpoint)`
  - `BaseComputeServices`: `protected MicroVersion classCeiling()`(기본 null), `protected MicroVersion effectiveMicroVersion(MicroVersion ceiling)`(꺼짐 → null), `protected <R> Invocation<R> capped(Invocation<R>, MicroVersion ceiling)`, `protected void requireMicroVersion(String feature, MicroVersion floor)`, `protected boolean isMicroVersionAtLeast(MicroVersion)`, `protected ActionResponse invokeAction(String serverId, ServerAction action, MicroVersion ceiling)`, `protected HttpResponse invokeActionWithResponse(String serverId, ServerAction action, MicroVersion ceiling)`
  - core-test `AbstractComputeMicroVersionTest`: `negotiate(String serverMax)`(루트 응답 enqueue + negotiate + 루트 요청 take), `assertVersionHeaders(RecordedRequest, String expected)`, `assertNoVersionHeaders(RecordedRequest)`, `assertNoMoreRequests()`, `body(RecordedRequest)`

- [ ] **Step 1: 루트 URL 계산 단위 테스트 (Review Focus 5)**

`core/src/test/java/org/openstack4j/test/microversion/ComputeRootUrlTest.java`:
```java
package org.openstack4j.test.microversion;

import org.openstack4j.openstack.compute.internal.ComputeMicroVersions;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ComputeRootUrlTest {

    @Test
    public void discoveryStripsVersionAndTenantFromEndpoint() {
        Assert.assertEquals(ComputeMicroVersions.rootUrl("http://10.0.0.1:8774/v2.1"), "http://10.0.0.1:8774");
        Assert.assertEquals(ComputeMicroVersions.rootUrl("http://10.0.0.1:8774/v2.1/"), "http://10.0.0.1:8774");
        Assert.assertEquals(ComputeMicroVersions.rootUrl("http://127.0.0.1:8774/v2/123ac695d4db400a9001b91bb3b8aa46"), "http://127.0.0.1:8774");
        Assert.assertEquals(ComputeMicroVersions.rootUrl("https://cloud.example.com/compute/v2.1/abc"), "https://cloud.example.com/compute");
        Assert.assertEquals(ComputeMicroVersions.rootUrl("https://cloud.example.com/compute"), "https://cloud.example.com/compute");
    }
}
```

- [ ] **Step 2: core-test 기반 클래스와 협상·상한 테스트 작성**

`core-test/src/main/java/org/openstack4j/api/compute/microversion/AbstractComputeMicroVersionTest.java`:
```java
package org.openstack4j.api.compute.microversion;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.openstack.internal.microversion.MicroVersionStore;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

public abstract class AbstractComputeMicroVersionTest extends AbstractTest {

    protected static final String SERVER = "96a38bed-26b5-410b-8cef-1913a2e0e0b8";

    @Override
    protected Service service() {
        return Service.COMPUTE;
    }

    @BeforeMethod
    public void forgetMicroVersions() {
        MicroVersionStore.clearAll();
    }

    /** Enqueues the Nova root document advertising {@code max}. */
    protected void respondWithNovaVersions(String max) {
        respondWith(200, "{\"versions\":[{\"id\":\"v2.0\",\"status\":\"DEPRECATED\",\"version\":\"\",\"min_version\":\"\"},"
                + "{\"id\":\"v2.1\",\"status\":\"CURRENT\",\"version\":\"" + max + "\",\"min_version\":\"2.1\"}]}");
    }

    /** Negotiates against a server advertising {@code max} and consumes the root request. */
    protected void negotiate(String max) throws InterruptedException {
        respondWithNovaVersions(max);
        osv3().compute().microVersions().negotiate();
        RecordedRequest root = takeRequest();
        Assert.assertEquals(root.getPath(), "/");
    }

    protected void assertVersionHeaders(RecordedRequest request, String expected) {
        Assert.assertEquals(request.getHeader("OpenStack-API-Version"), "compute " + expected);
        Assert.assertEquals(request.getHeader("X-OpenStack-Nova-API-Version"), expected);
    }

    protected void assertNoVersionHeaders(RecordedRequest request) {
        Assert.assertNull(request.getHeader("OpenStack-API-Version"));
        Assert.assertNull(request.getHeader("X-OpenStack-Nova-API-Version"));
    }

    protected void assertNoMoreRequests() throws InterruptedException {
        Assert.assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS), "unexpected extra request");
    }

    protected JsonNode body(RecordedRequest request) throws IOException {
        return new ObjectMapper().readTree(request.getBody().clone().readUtf8());
    }
}
```

`ComputeMicroVersionTests.java`:
```java
package org.openstack4j.api.compute.microversion;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.ComputeVersion;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/MicroVersion")
public class ComputeMicroVersionTests extends AbstractComputeMicroVersionTest {

    private static final String FLAVORS = "{\"flavors\": []}";

    public void noHeadersAndNoDiscoveryWhenDisabled() throws Exception {
        respondWith(200, FLAVORS);

        osv3().compute().flavors().list();
        RecordedRequest request = takeRequest();

        Assert.assertTrue(request.getPath().contains("/flavors"));
        assertNoVersionHeaders(request);
        assertNoMoreRequests();
        Assert.assertFalse(osv3().compute().microVersions().get().isEnabled());
        Assert.assertNull(osv3().compute().microVersions().get().getMicroVersion());
    }

    public void negotiateUsesMinOfLibraryAndServer() throws Exception {
        negotiate("2.100");
        respondWith(200, FLAVORS);

        osv3().compute().flavors().list();

        assertVersionHeaders(takeRequest(), "2.100");
        ComputeVersion version = osv3().compute().microVersions().get();
        Assert.assertEquals(version.getMicroVersion(), "2.100");
        Assert.assertEquals(version.getServerMaxVersion(), "2.100");
        Assert.assertTrue(version.isEnabled());
        Assert.assertFalse(version.isPinned());
    }

    public void newerServerIsCappedAtLibraryLatest() throws Exception {
        negotiate("2.120");
        Assert.assertEquals(osv3().compute().microVersions().get().getMicroVersion(), "2.104");
    }

    public void olderServerMaxIsUsedAsIs() throws Exception {
        negotiate("2.50");
        respondWith(200, FLAVORS);
        osv3().compute().flavors().list();
        assertVersionHeaders(takeRequest(), "2.50");
    }

    public void useThenClear() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.60");
        respondWith(200, FLAVORS);
        respondWith(200, FLAVORS);

        osv3().compute().flavors().list();
        assertVersionHeaders(takeRequest(), "2.60");
        osv3().compute().microVersions().clear();
        osv3().compute().flavors().list();
        assertNoVersionHeaders(takeRequest());
        Assert.assertFalse(osv3().compute().microVersions().get().isEnabled());
    }

    public void useWithoutNegotiateDiscoversOnce() throws Exception {
        respondWithNovaVersions("2.100");

        ComputeVersion version = osv3().compute().microVersions().use("2.53");

        Assert.assertEquals(takeRequest().getPath(), "/");
        Assert.assertEquals(version.getMicroVersion(), "2.53");
        Assert.assertTrue(version.isPinned());
        assertNoMoreRequests();
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.101.*")
    public void useAboveServerMaxIsRejected() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.101");
    }

    @Test(expectedExceptions = MicroVersionException.class)
    public void useWithGarbageIsRejected() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("latest");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*microversion.*")
    public void serverWithoutMicroVersionsCannotNegotiate() throws Exception {
        respondWith(200, "{\"versions\":[{\"id\":\"v2.0\",\"status\":\"CURRENT\",\"version\":\"\",\"min_version\":\"\"}]}");
        try {
            osv3().compute().microVersions().negotiate();
        } finally {
            takeRequest();
        }
    }
}
```

`LegacyCeilingTests.java`:
```java
package org.openstack4j.api.compute.microversion;

import java.util.Collections;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.VNCConsole;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/LegacyCeilings")
public class LegacyCeilingTests extends AbstractComputeMicroVersionTest {

    public void legacyMethodsAreCappedForProxies() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"floating_ips\": []}");
        respondWith(200, "{\"security_groups\": []}");
        respondWith(200, "{\"images\": []}");

        osv3().compute().floatingIps().list();
        osv3().compute().securityGroups().list();
        osv3().compute().images().list();

        assertVersionHeaders(takeRequest(), "2.35");
        assertVersionHeaders(takeRequest(), "2.35");
        assertVersionHeaders(takeRequest(), "2.35");
    }

    public void legacyMethodsAreCappedForHostsAndConsolesAndDiagnostics() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"hosts\": []}");
        respondWith(200, "{\"console\": {\"type\": \"novnc\", \"url\": \"http://x\"}}");
        respondWith(200, "{\"cpu0_time\": 1}");

        osv3().compute().host().list();
        osv3().compute().servers().getVNCConsole(SERVER, VNCConsole.Type.NOVNC);
        osv3().compute().servers().diagnostics(SERVER);

        assertVersionHeaders(takeRequest(), "2.42");
        assertVersionHeaders(takeRequest(), "2.5");
        assertVersionHeaders(takeRequest(), "2.47");
    }

    public void legacyKeypairWithoutPublicKeyIsCapped() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"keypair\": {\"name\": \"k\", \"public_key\": \"ssh-rsa A\", \"private_key\": \"P\", \"fingerprint\": \"f\"}}");
        respondWith(200, "{\"keypair\": {\"name\": \"k2\", \"public_key\": \"ssh-rsa B\", \"fingerprint\": \"f\"}}");

        osv3().compute().keypairs().create("k", null);
        osv3().compute().keypairs().create("k2", "ssh-rsa B");

        assertVersionHeaders(takeRequest(), "2.91");
        assertVersionHeaders(takeRequest(), "2.100");
    }

    public void legacyServicesEnableIsCappedAndForceDownKeepsLegacyHeaderWhenDisabled() throws Exception {
        respondWith(200, "{\"service\": {\"binary\": \"nova-compute\", \"host\": \"h\", \"forced_down\": true}}");
        osv3().compute().services().forceDownService("nova-compute", "h");
        RecordedRequest legacy = takeRequest();
        Assert.assertEquals(legacy.getHeader("x-openstack-nova-api-version"), "2.11");
        Assert.assertNull(legacy.getHeader("OpenStack-API-Version"));

        negotiate("2.100");
        respondWith(200, "{\"service\": {\"binary\": \"nova-compute\", \"host\": \"h\", \"status\": \"enabled\"}}");
        respondWith(200, "{\"service\": {\"binary\": \"nova-compute\", \"host\": \"h\", \"forced_down\": true}}");
        osv3().compute().services().enableService("nova-compute", "h");
        osv3().compute().services().forceDownService("nova-compute", "h");
        assertVersionHeaders(takeRequest(), "2.52");
        assertVersionHeaders(takeRequest(), "2.52");
    }

    public void legacyHypervisorStatisticsAndServerGroupCreateAreCapped() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"hypervisor_statistics\": {\"count\": 1}}");
        respondWith(200, "{\"server_group\": {\"id\": \"g\", \"name\": \"n\", \"policies\": [\"affinity\"]}}");

        osv3().compute().hypervisors().statistics();
        osv3().compute().serverGroups().create("n", "affinity");

        assertVersionHeaders(takeRequest(), "2.87");
        assertVersionHeaders(takeRequest(), "2.63");
    }

    public void bootWithoutNetworksOrWithPersonalityIsCapped() throws Exception {
        negotiate("2.100");
        respondWith(202, "{\"server\": {\"id\": \"s1\"}}");
        respondWith(202, "{\"server\": {\"id\": \"s2\"}}");
        respondWith(202, "{\"server\": {\"id\": \"s3\"}}");

        ServerCreate noNetworks = Builders.server().name("a").flavor("f").image("i").build();
        ServerCreate withPersonality = Builders.server().name("b").flavor("f").image("i").networks(Collections.singletonList("n1"))
                .addPersonality("/etc/motd", "hello").build();
        ServerCreate modern = Builders.server().name("c").flavor("f").image("i").networks(Collections.singletonList("n1")).build();
        osv3().compute().servers().boot(noNetworks);
        osv3().compute().servers().boot(withPersonality);
        osv3().compute().servers().boot(modern);

        assertVersionHeaders(takeRequest(), "2.36");
        assertVersionHeaders(takeRequest(), "2.56");
        assertVersionHeaders(takeRequest(), "2.100");
    }

    public void legacyAttachVolumeAndFlavorCreateAreCapped() throws Exception {
        negotiate("2.104");
        respondWith(200, "{\"volumeAttachment\": {\"device\": \"/dev/vdb\", \"id\": \"v1\", \"serverId\": \"s\", \"volumeId\": \"v1\"}}");
        respondWith(200, "{\"flavor\": {\"id\": \"f1\", \"name\": \"tiny\"}}");

        osv3().compute().servers().attachVolume(SERVER, "v1", "/dev/vdb");
        osv3().compute().flavors().create("tiny", 512, 1, 1, 0, 0, 1.0f, true);

        assertVersionHeaders(takeRequest(), "2.100");
        assertVersionHeaders(takeRequest(), "2.101");
    }

    public void createSnapshotReadsImageIdFromBodyOn245() throws Exception {
        negotiate("2.100");
        respondWith(202, "{\"image_id\": \"0e7761dd-ee98-41f0-ba35-05994e446431\"}");

        String imageId = osv3().compute().servers().createSnapshot(SERVER, "snap");

        assertVersionHeaders(takeRequest(), "2.100");
        Assert.assertEquals(imageId, "0e7761dd-ee98-41f0-ba35-05994e446431");
    }

    public void createSnapshotReadsLocationWhenDisabled() throws Exception {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("Location", "http://glance/v2/images/abc-123");
        respondWith(headers, 202);

        Assert.assertEquals(osv3().compute().servers().createSnapshot(SERVER, "snap"), "abc-123");
        assertNoVersionHeaders(takeRequest());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.26.*")
    public void serverTagsRequire226WhenEnabled() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.20");
        try {
            osv3().compute().serverTags().list(SERVER);
        } finally {
            assertNoMoreRequests();
        }
    }
}
```
`Builders.server()` 의 실제 메서드 이름(`networks`, `addPersonality`)이 다르면 `ServerCreateBuilder` 에 맞춰 고친다(기존 API 이므로 새로 추가하지 않는다).

- [ ] **Step 3: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q test -pl core -Dtest=ComputeRootUrlTest 2>&1 | grep -E 'cannot find symbol|does not exist' | head -2`
Expected: `ComputeMicroVersions` 없음.

- [ ] **Step 4: 모델·서비스·발견 구현**

`model/compute/ComputeVersion.java`:
```java
package org.openstack4j.model.compute;

import org.openstack4j.model.ModelEntity;

/** The compute microversion range of the server and the microversion this session sends. */
public interface ComputeVersion extends ModelEntity {

    /** @return the server's minimum microversion, or {@code null} before any discovery */
    String getServerMinVersion();

    String getServerMaxVersion();

    /** @return the microversion sent with requests, or {@code null} when microversions are off */
    String getMicroVersion();

    boolean isPinned();

    boolean isEnabled();
}
```

`api/compute/ComputeMicroVersionService.java`:
```java
package org.openstack4j.api.compute;

import org.openstack4j.common.RestService;
import org.openstack4j.model.compute.ComputeVersion;

/**
 * Opt-in compute microversions. Off by default: requests carry no microversion header (Nova treats them as 2.1).
 * Once turned on, methods whose API Nova removed in later microversions keep working because they are sent at the
 * highest microversion they support.
 */
public interface ComputeMicroVersionService extends RestService {

    /** Turns microversions on at min(library latest, server max). */
    ComputeVersion negotiate();

    /** Turns microversions on at a fixed version within the library and server range. */
    ComputeVersion use(String version);

    /** Turns microversions off again (no header). */
    void clear();

    ComputeVersion get();
}
```

`openstack/compute/domain/NovaVersions.java`:
```java
package org.openstack4j.openstack.compute.domain;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Nova root document: {@code {"versions": [{"id": "v2.1", "version": "2.100", "min_version": "2.1"}, ...]}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaVersions implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("versions")
    private List<Entry> versions;
    @JsonProperty("version")
    private Entry version;

    /** @return the v2.1 entry, or {@code null} when the server has no microversion-capable API */
    public Entry v21() {
        if (version != null && "v2.1".equals(version.id))
            return version;
        if (versions != null)
            for (Entry e : versions)
                if ("v2.1".equals(e.id))
                    return e;
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("id")
        public String id;
        @JsonProperty("version")
        public String version;
        @JsonProperty("min_version")
        public String minVersion;
    }
}
```

`openstack/compute/domain/NovaComputeVersion.java`:
```java
package org.openstack4j.openstack.compute.domain;

import org.openstack4j.model.compute.ComputeVersion;

public class NovaComputeVersion implements ComputeVersion {

    private static final long serialVersionUID = 1L;

    private final String serverMinVersion;
    private final String serverMaxVersion;
    private final String microVersion;
    private final boolean pinned;
    private final boolean enabled;

    public NovaComputeVersion(String serverMinVersion, String serverMaxVersion, String microVersion, boolean pinned, boolean enabled) {
        this.serverMinVersion = serverMinVersion;
        this.serverMaxVersion = serverMaxVersion;
        this.microVersion = microVersion;
        this.pinned = pinned;
        this.enabled = enabled;
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

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
```

`openstack/compute/internal/ComputeMicroVersions.java`:
```java
package org.openstack4j.openstack.compute.internal;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.OSClientSession;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionStore;

/** Compute microversion constants and session state lookup. */
public final class ComputeMicroVersions {

    public static final MicroVersion MINIMUM = new MicroVersion(2, 1);
    public static final MicroVersion LATEST = new MicroVersion(2, 104);

    private ComputeMicroVersions() {
    }

    /** @return compute microversion {@code 2.minor} */
    public static MicroVersion V(int minor) {
        return new MicroVersion(2, minor);
    }

    static String key(OSClientSession<?, ?> session) {
        return "compute|" + session.getEndpoint(ServiceType.COMPUTE);
    }

    /** @return this session's compute state, or {@code null} when microversions were never turned on */
    public static MicroVersionState currentState() {
        OSClientSession<?, ?> session = OSClientSession.getCurrent();
        return session == null ? null : MicroVersionStore.get(session, key(session));
    }

    /** Strips the {@code /v2[.1]} segment and anything after it (such as a tenant id) from a compute endpoint. */
    public static String rootUrl(String endpoint) {
        String trimmed = endpoint.replaceAll("/+$", "");
        return trimmed.replaceAll("/v2(\\.\\d+)?(/.*)?$", "");
    }
}
```

`openstack/compute/internal/ComputeVersionDiscovery.java`:
```java
package org.openstack4j.openstack.compute.internal;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.openstack.compute.domain.NovaVersions;
import org.openstack4j.openstack.internal.BaseOpenStackService;

/** Fetches the Nova root document. Extends BaseOpenStackService directly so no microversion header is added. */
final class ComputeVersionDiscovery extends BaseOpenStackService {

    ComputeVersionDiscovery() {
        super(ServiceType.COMPUTE, ComputeMicroVersions::rootUrl);
    }

    NovaVersions fetch() {
        return request(HttpMethod.GET, NovaVersions.class, "/").execute();
    }
}
```

`openstack/compute/internal/ComputeMicroVersionServiceImpl.java`:
```java
package org.openstack4j.openstack.compute.internal;

import org.openstack4j.api.compute.ComputeMicroVersionService;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.ComputeVersion;
import org.openstack4j.openstack.compute.domain.NovaComputeVersion;
import org.openstack4j.openstack.compute.domain.NovaVersions;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.OSClientSession;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionStore;
import org.openstack4j.openstack.internal.microversion.MicroVersions;

public class ComputeMicroVersionServiceImpl extends BaseComputeServices implements ComputeMicroVersionService {

    @Override
    public ComputeVersion negotiate() {
        MicroVersionState state = ensureState();
        state.setPinned(null);
        state.setEnabled(true);
        return get();
    }

    @Override
    public ComputeVersion use(String version) {
        MicroVersion requested = MicroVersions.parse(version);
        MicroVersionState state = ensureState();
        MicroVersion lowest = MicroVersions.max(ComputeMicroVersions.MINIMUM, state.getServerMin());
        MicroVersion highest = MicroVersions.min(ComputeMicroVersions.LATEST, state.getServerMax());
        if (requested.compareTo(lowest) < 0 || requested.compareTo(highest) > 0)
            throw new MicroVersionException(String.format(
                    "Compute microversion %s is outside the usable range %s - %s (library %s - %s, server %s - %s)",
                    requested, lowest, highest, ComputeMicroVersions.MINIMUM, ComputeMicroVersions.LATEST,
                    state.getServerMin(), state.getServerMax()));
        state.setPinned(requested);
        state.setEnabled(true);
        return get();
    }

    @Override
    public void clear() {
        MicroVersionState state = ComputeMicroVersions.currentState();
        if (state != null) {
            state.setEnabled(false);
            state.setPinned(null);
        }
    }

    @Override
    public ComputeVersion get() {
        MicroVersionState state = ComputeMicroVersions.currentState();
        if (state == null)
            return new NovaComputeVersion(null, null, null, false, false);
        MicroVersion effective = effectiveMicroVersion(null);
        return new NovaComputeVersion(state.getServerMin().toString(), state.getServerMax().toString(),
                effective == null ? null : effective.toString(), state.getPinned() != null, state.isEnabled());
    }

    private MicroVersionState ensureState() {
        MicroVersionState state = ComputeMicroVersions.currentState();
        if (state != null)
            return state;
        NovaVersions.Entry v21 = new ComputeVersionDiscovery().fetch().v21();
        if (v21 == null || v21.version == null || v21.version.isEmpty())
            throw new MicroVersionException("This compute endpoint does not support microversions (no v2.1 API with a version range)");
        MicroVersion min = v21.minVersion == null || v21.minVersion.isEmpty() ? ComputeMicroVersions.MINIMUM : MicroVersions.parse(v21.minVersion);
        OSClientSession<?, ?> session = OSClientSession.getCurrent();
        return MicroVersionStore.putIfAbsent(session, ComputeMicroVersions.key(session), new MicroVersionState(min, MicroVersions.parse(v21.version)));
    }
}
```

- [ ] **Step 5: BaseComputeServices — 헤더, 상한, 하한**

`BaseComputeServices.java` 전체를 다음으로 바꾼다(기존 `invokeAction`/`invokeActionWithResponse` 2-인자 메서드는 그대로 유지하고 3-인자 오버로드를 추가):
```java
package org.openstack4j.openstack.compute.internal;

import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.compute.domain.actions.ServerAction;
import org.openstack4j.openstack.compute.functions.ToActionResponseFunction;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersions;

/**
 * Base Compute Operations Implementation is responsible for insuring the proper endpoint is used for all extending
 * operation APIs. Adds compute microversion headers when the session turned microversions on.
 *
 * @author Jeremy Unruh
 */
public class BaseComputeServices extends BaseOpenStackService {

    static final String API_VERSION_HEADER = "OpenStack-API-Version";
    static final String NOVA_VERSION_HEADER = "X-OpenStack-Nova-API-Version";

    protected BaseComputeServices() {
        super(ServiceType.COMPUTE);
    }

    /** Highest microversion every API of this service supports, or {@code null} for no limit. */
    protected MicroVersion classCeiling() {
        return null;
    }

    @Override
    protected <R> Invocation<R> decorate(Invocation<R> invocation) {
        MicroVersion version = effectiveMicroVersion(null);
        if (version != null)
            setVersionHeaders(invocation, version);
        return invocation;
    }

    /** Sends this request at no more than {@code ceiling}. */
    protected <R> Invocation<R> capped(Invocation<R> invocation, MicroVersion ceiling) {
        MicroVersion version = effectiveMicroVersion(ceiling);
        if (version != null)
            setVersionHeaders(invocation, version);
        return invocation;
    }

    /** @return the microversion a request with {@code ceiling} would carry, or {@code null} when microversions are off */
    protected MicroVersion effectiveMicroVersion(MicroVersion ceiling) {
        MicroVersionState state = ComputeMicroVersions.currentState();
        if (state == null || !state.isEnabled())
            return null;
        MicroVersion version = state.getPinned() != null ? state.getPinned()
                : MicroVersions.min(ComputeMicroVersions.LATEST, state.getServerMax());
        MicroVersion classCeiling = classCeiling();
        if (classCeiling != null)
            version = MicroVersions.min(version, classCeiling);
        if (ceiling != null)
            version = MicroVersions.min(version, ceiling);
        return version;
    }

    protected boolean isMicroVersionAtLeast(MicroVersion version) {
        MicroVersion effective = effectiveMicroVersion(null);
        return effective != null && effective.compareTo(version) >= 0;
    }

    /** Fails before any request when {@code feature} needs a microversion the session does not send. */
    protected void requireMicroVersion(String feature, MicroVersion floor) {
        MicroVersion effective = effectiveMicroVersion(null);
        if (effective == null)
            throw new MicroVersionException(feature + " requires compute microversion " + floor
                    + "; turn microversions on with os.compute().microVersions().negotiate()");
        if (effective.compareTo(floor) < 0) {
            MicroVersionState state = ComputeMicroVersions.currentState();
            throw new MicroVersionException(String.format(
                    "%s requires compute microversion %s, but the session sends %s (server max %s)",
                    feature, floor, effective, state == null ? "?" : state.getServerMax()));
        }
    }

    protected ActionResponse invokeAction(String serverId, ServerAction action) {
        return ToActionResponseFunction.INSTANCE.apply(invokeActionWithResponse(serverId, action), action.getClass().getName());
    }

    protected ActionResponse invokeAction(String serverId, ServerAction action, MicroVersion ceiling) {
        return ToActionResponseFunction.INSTANCE.apply(invokeActionWithResponse(serverId, action, ceiling), action.getClass().getName());
    }

    protected HttpResponse invokeActionWithResponse(String serverId, ServerAction action) {
        return invokeActionWithResponse(serverId, action, null);
    }

    protected HttpResponse invokeActionWithResponse(String serverId, ServerAction action, MicroVersion ceiling) {
        return capped(post(Void.class, uri("/servers/%s/action", serverId)), ceiling)
                .entity(action)
                .executeWithResponse();
    }

    private static <R> void setVersionHeaders(Invocation<R> invocation, MicroVersion version) {
        invocation.header(API_VERSION_HEADER, "compute " + version);
        invocation.header(NOVA_VERSION_HEADER, version.toString());
    }
}
```
기존 import(`ServerAction`, `ToActionResponseFunction`, `HttpResponse`, `ActionResponse`)의 실제 패키지는 현재 파일을 따른다.

- [ ] **Step 6: accessor, binding, 레거시 상한**

- `ComputeService`: `ComputeMicroVersionService microVersions();` 추가(Javadoc: 위 인터페이스 설명 한 줄). `ComputeServiceImpl`: `return Apis.get(ComputeMicroVersionService.class);`. `DefaultAPIProvider`: `bind(ComputeMicroVersionService.class, ComputeMicroVersionServiceImpl.class);`
- 클래스 상한 — 각 클래스에 추가:
  ```java
  @Override
  protected MicroVersion classCeiling() {
      return ComputeMicroVersions.V(35);   // Nova removed this proxy API in 2.36
  }
  ```
  대상과 값: `ComputeImageServiceImpl` 35, `ComputeFloatingIPServiceImpl` 35, `ComputeSecurityGroupServiceImpl` 35, `ext/FloatingIPDNSDomainServiceImpl` 35, `ext/FloatingIPDNSEntryServiceImpl` 35, `HostServiceImpl` 42 (주석: "os-hosts removed in 2.43").
- 메서드 상한·분기:
  - `ServerServiceImpl.getVNCConsole`: `capped(post(...), V(5))`
  - `ServerServiceImpl.diagnostics`: `capped(get(HashMap.class, ...), V(47))` (주석: "2.48 returns a structured document; see diagnosticsStandard")
  - `ServerServiceImpl.liveMigrate(id, LiveMigrateOptions)`: `invokeAction(serverId, LiveMigrationAction.create(options), V(24))`
  - `ServerServiceImpl.evacuate(id, EvacuateOptions)`: `capped(post(AdminPass.class, ...), V(13))`
  - `ServerServiceImpl.rebuild(id, RebuildOptions)`: `invokeAction(serverId, RebuildAction.create(options), V(56))`
  - `ServerServiceImpl.boot`: 
    ```java
    MicroVersion ceiling = null;
    if (server.getPersonality() != null && !server.getPersonality().isEmpty())
        ceiling = ComputeMicroVersions.V(56);           // personality removed in 2.57
    if (server.getNetworks() == null || server.getNetworks().isEmpty())
        ceiling = ceiling == null ? ComputeMicroVersions.V(36) : MicroVersions.min(ceiling, ComputeMicroVersions.V(36)); // networks required from 2.37
    return capped(post(NovaServer.class, uri("/servers")), ceiling)
            .entity(WrapServerIfApplicableFunction.INSTANCE.apply(server))
            .execute();
    ```
    (Task 4 에서 `networks auto/none` 이 생기면 그 경우는 상한에서 제외한다.)
  - `ServerServiceImpl.invokeCreateSnapshotAction`: 본문을 `return imageIdFrom(invokeActionWithResponse(serverId, createSnapshotAction));` 로 바꾸고, Task 5 의 createBackup 도 쓰는 helper 를 추가한다:
    ```java
    /** Image id of a createImage/createBackup response: the Location header (before 2.45) or {"image_id"} (2.45+). */
    private static String imageIdFrom(HttpResponse response) {
        try {
            if (response.getStatus() != 202)
                return null;
            String location = response.header("location");
            if (location != null && location.contains("/")) {
                String[] s = location.split("/");
                return s[s.length - 1];
            }
            Map<?, ?> body = response.readEntity(HashMap.class);
            return body == null || body.get("image_id") == null ? null : String.valueOf(body.get("image_id"));
        } finally {
            try {
                response.close();
            } catch (IOException ignored) {
                // the connection is released either way
            }
        }
    }
    ```
    기존 코드의 `response.getEntity(Void.class)` 대신 `close()` 를 쓰는 이유: 본문을 이미 읽었을 수 있다. 202 가 아닌 응답(오류)은 기존처럼 `null` 을 돌려준다(예외 전파 동작이 기존 코드와 같은지 `ServerTests` 의 snapshot 테스트로 확인). `readEntity` 가 빈 본문에서 예외를 던지면 `try/catch` 로 `null` 처리한다.
  - `ServicesServiceImpl.enableService/disableService`: `capped(put(...), V(52))`.
  - `ServicesServiceImpl.forceDownService/forceUpService`:
    ```java
    Invocation<ExtService> invocation = put(ExtService.class, uri("/os-services/force-down"));
    if (effectiveMicroVersion(null) == null) {
        invocation.header("x-openstack-nova-api-version", "2.11");    // legacy behaviour
    } else {
        requireMicroVersion("forcing a service down", ComputeMicroVersions.V(11));
        capped(invocation, ComputeMicroVersions.V(52));
    }
    return invocation.entity(ServiceActions.forceDown(binary, host)).execute();
    ```
  - `ext/HypervisorServiceImpl.statistics`: `capped(get(...), V(87))`.
  - `KeypairServiceImpl.create(name, publicKey)`: `publicKey == null` 이면 `capped(..., V(91))`.
  - `ServerGroupServiceImpl.create(name, policy)`: `capped(..., V(63))`.
  - `ServerServiceImpl.attachVolume(serverId, volumeId, device)`: `capped(post(NovaVolumeAttachment.class, ...), V(100))` (주석: "2.101 answers 202 without a body; see attachVolumeAsync")
  - `FlavorServiceImpl.create(Flavor)` 과 `create(name, ram, ...)`: `capped(post(...), V(101))` (주석: "NovaFlavor always sends rxtx_factor, rejected from 2.102")
  - `ServerTagServiceImpl` 의 모든 공개 메서드 첫 줄: `if (effectiveMicroVersion(null) != null) requireMicroVersion("server tags", ComputeMicroVersions.V(26));`

- [ ] **Step 7: 통과 확인**

Run:
```bash
./mvnw -B test -pl core -Dtest='ComputeRootUrlTest,MicroVersionStoreTest' 2>&1 | grep -E 'Tests run:' | tail -1
./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ComputeMicroVersionTests,LegacyCeilingTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3
```
Expected: core `Tests run: 7`, core-test `Tests run: 19, Failures: 0`.

- [ ] **Step 8: 기존 compute 테스트 불변 확인 (Review Focus 1) 과 전체 빌드**

```bash
git diff --stat main -- core-test/src/main/java/org/openstack4j/api/compute | grep -v microversion   # 기존 테스트 파일 변경 0
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
```
Expected: 기존 compute 테스트 파일 변경 없음, `BUILD SUCCESS`.

- [ ] **Step 9: 커밋, PR**

```bash
git add -A && git commit -m "feat(compute): add opt-in microversion negotiation with per-method ceilings

Off by default (no header, no discovery). os.compute().microVersions()
negotiates min(2.104, server max) or pins a version. APIs Nova removed in
later microversions (proxies, os-hosts, legacy consoles/diagnostics,
services enable/disable, keypair generation, ...) are sent at the highest
microversion they support. createSnapshot reads the 2.45+ image_id body.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
.superpowers/pr.sh "compute microversion opt-in 프레임워크(협상·고정·해제, 헤더 2종), 레거시 메서드 상한, createSnapshot 2.45 대응, serverTags 2.26 검사(켜졌을 때)."
```

---

### Task 3: 서버 응답 모델 확장, 2.47 flavor 두 형식, 2.98 image properties, 2.89 attachment

**Files:**
- Modify: `core/src/main/java/org/openstack4j/model/compute/Server.java`, `Flavor.java`, `VolumeAttachment.java` (default getter 추가)
- Modify: `core/src/main/java/org/openstack4j/openstack/compute/domain/NovaServer.java`, `NovaFlavor.java`, `NovaVolumeAttachment.java`
- Create: `core-test/src/main/resources/compute/microversion/server_2_100.json`, `server_2_1.json`
- Create: `core-test/src/main/java/org/openstack4j/api/compute/microversion/ServerModelTests.java`

**Interfaces:**
- Consumes: Task 2 `AbstractComputeMicroVersionTest`(`negotiate`, `assertNoMoreRequests`, `SERVER`).
- Produces (모두 `default` 메서드, 기본값 `null` — 외부 구현체가 계속 컴파일되도록):
  - `Server`: `Flavor getFlavorSummary()`, `Map<String, String> getImageProperties()`, `String getHostname()`, `String getReservationId()`, `Integer getLaunchIndex()`, `String getKernelId()`, `String getRamdiskId()`, `String getRootDeviceName()`, `String getUserData()`, `Boolean getLocked()`, `String getHostStatus()`, `String getDescription()`, `List<String> getTags()`, `List<String> getTrustedImageCertificates()`, `List<String> getServerGroups()`, `String getLockedReason()`, `String getPinnedAvailabilityZone()`, `Map<String, Object> getSchedulerHints()`
  - `Flavor`: `String getOriginalName()`, `Map<String, String> getExtraSpecs()`, `String getDescription()`
  - `VolumeAttachment`: `String getAttachmentId()`, `String getBdmUuid()`, `String getTag()`, `Boolean getDeleteOnTermination()`

- [ ] **Step 1: fixture 작성**

```bash
git switch main && git pull && git switch -c task/c3-server-model
mkdir -p core-test/src/main/resources/compute/microversion
```

`core-test/src/main/resources/compute/microversion/server_2_100.json` (epoxy 2.100 실제 응답을 일반화):
```json
{
  "server": {
    "id": "96a38bed-26b5-410b-8cef-1913a2e0e0b8",
    "name": "vm-1",
    "status": "ACTIVE",
    "tenant_id": "2580a7b51d564c1d848ee27fda2db713",
    "user_id": "e365357fdf4d4a37a07fde3209cac4aa",
    "metadata": {},
    "hostId": "117e3ed6f2e7208a4dda442f363359608bca2b8bde5d079283e2ba42",
    "image": {
      "id": "777afa04-f596-42b1-86ac-7e5881b7d46c",
      "links": [{"rel": "bookmark", "href": "http://10.0.0.1:8774/images/777afa04-f596-42b1-86ac-7e5881b7d46c"}],
      "properties": {"os_type": "linux", "hw_disk_bus": "virtio", "min_ram": "0"}
    },
    "flavor": {
      "vcpus": 1, "ram": 1024, "disk": 0, "ephemeral": 2, "swap": 0,
      "original_name": "m1.tiny",
      "extra_specs": {"test1": "test"}
    },
    "created": "2026-09-22T08:02:43Z",
    "updated": "2026-09-22T08:02:55Z",
    "addresses": {"external-network": [{"version": 4, "addr": "10.0.0.17", "OS-EXT-IPS:type": "fixed", "OS-EXT-IPS-MAC:mac_addr": "fa:16:3e:ee:08:6f"}]},
    "accessIPv4": "",
    "accessIPv6": "",
    "links": [{"rel": "self", "href": "http://10.0.0.1:8774/v2.1/servers/96a38bed-26b5-410b-8cef-1913a2e0e0b8"}],
    "OS-DCF:diskConfig": "AUTO",
    "progress": 0,
    "OS-EXT-AZ:availability_zone": "nova",
    "pinned_availability_zone": "nova",
    "scheduler_hints": {"group": "4c3e2a2e-0000-4000-8000-000000000001"},
    "config_drive": "",
    "key_name": null,
    "OS-SRV-USG:launched_at": "2026-09-22T08:02:55.000000",
    "OS-SRV-USG:terminated_at": null,
    "security_groups": [{"name": "default"}],
    "OS-EXT-SRV-ATTR:host": "compute-1",
    "OS-EXT-SRV-ATTR:instance_name": "instance-00000094",
    "OS-EXT-SRV-ATTR:hypervisor_hostname": "compute-1",
    "OS-EXT-SRV-ATTR:reservation_id": "r-1ewf37jz",
    "OS-EXT-SRV-ATTR:launch_index": 0,
    "OS-EXT-SRV-ATTR:hostname": "vm-1",
    "OS-EXT-SRV-ATTR:kernel_id": "",
    "OS-EXT-SRV-ATTR:ramdisk_id": "",
    "OS-EXT-SRV-ATTR:root_device_name": "/dev/vda",
    "OS-EXT-SRV-ATTR:user_data": null,
    "OS-EXT-STS:task_state": null,
    "OS-EXT-STS:vm_state": "active",
    "OS-EXT-STS:power_state": 1,
    "os-extended-volumes:volumes_attached": [],
    "host_status": "UP",
    "locked": true,
    "locked_reason": "maintenance",
    "description": "web server",
    "tags": ["web", "prod"],
    "trusted_image_certificates": null,
    "server_groups": ["4c3e2a2e-0000-4000-8000-000000000001"]
  }
}
```

`core-test/src/main/resources/compute/microversion/server_2_1.json`:
```json
{
  "server": {
    "id": "96a38bed-26b5-410b-8cef-1913a2e0e0b8",
    "name": "vm-1",
    "status": "ACTIVE",
    "tenant_id": "2580a7b51d564c1d848ee27fda2db713",
    "user_id": "e365357fdf4d4a37a07fde3209cac4aa",
    "metadata": {},
    "image": {"id": "777afa04-f596-42b1-86ac-7e5881b7d46c", "links": []},
    "flavor": {"id": "m1.tiny", "links": [{"rel": "bookmark", "href": "http://10.0.0.1:8774/flavors/m1.tiny"}]},
    "created": "2026-09-22T08:02:43Z",
    "updated": "2026-09-22T08:02:55Z",
    "addresses": {},
    "links": [],
    "OS-EXT-STS:vm_state": "active"
  }
}
```

- [ ] **Step 2: 실패하는 테스트 작성 (Review Focus 3)**

`core-test/src/main/java/org/openstack4j/api/compute/microversion/ServerModelTests.java`:
```java
package org.openstack4j.api.compute.microversion;

import java.util.Arrays;

import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Server;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/ServerModel")
public class ServerModelTests extends AbstractComputeMicroVersionTest {

    public void readsMicroVersionFieldsOn2100() throws Exception {
        negotiate("2.100");
        respondWith("/compute/microversion/server_2_100.json");

        Server server = osv3().compute().servers().get(SERVER);
        takeRequest();

        Assert.assertEquals(server.getHostname(), "vm-1");
        Assert.assertEquals(server.getReservationId(), "r-1ewf37jz");
        Assert.assertEquals(server.getLaunchIndex(), Integer.valueOf(0));
        Assert.assertEquals(server.getRootDeviceName(), "/dev/vda");
        Assert.assertEquals(server.getLocked(), Boolean.TRUE);
        Assert.assertEquals(server.getLockedReason(), "maintenance");
        Assert.assertEquals(server.getHostStatus(), "UP");
        Assert.assertEquals(server.getDescription(), "web server");
        Assert.assertEquals(server.getTags(), Arrays.asList("web", "prod"));
        Assert.assertNull(server.getTrustedImageCertificates());
        Assert.assertEquals(server.getServerGroups(), Arrays.asList("4c3e2a2e-0000-4000-8000-000000000001"));
        Assert.assertEquals(server.getPinnedAvailabilityZone(), "nova");
        Assert.assertEquals(server.getSchedulerHints().get("group"), "4c3e2a2e-0000-4000-8000-000000000001");
        Assert.assertEquals(server.getImageProperties().get("os_type"), "linux");
        Assert.assertEquals(server.getImageId(), "777afa04-f596-42b1-86ac-7e5881b7d46c");
    }

    public void flavorEmbeddedFormDoesNotTriggerLookup() throws Exception {
        negotiate("2.100");
        respondWith("/compute/microversion/server_2_100.json");

        Server server = osv3().compute().servers().get(SERVER);
        takeRequest();
        Flavor summary = server.getFlavorSummary();
        Flavor flavor = server.getFlavor();

        Assert.assertNull(server.getFlavorId());
        Assert.assertSame(flavor, summary);
        Assert.assertEquals(summary.getOriginalName(), "m1.tiny");
        Assert.assertEquals(summary.getVcpus(), 1);
        Assert.assertEquals(summary.getRam(), 1024);
        Assert.assertEquals(summary.getEphemeral(), 2);
        Assert.assertEquals(summary.getExtraSpecs().get("test1"), "test");
        assertNoMoreRequests();
    }

    public void flavorReferenceFormStillLooksUpOn21() throws Exception {
        respondWith("/compute/microversion/server_2_1.json");
        respondWith(200, "{\"flavor\": {\"id\": \"m1.tiny\", \"name\": \"m1.tiny\", \"ram\": 512, \"vcpus\": 1, \"disk\": 1}}");

        Server server = osv3().compute().servers().get(SERVER);
        takeRequest();

        Assert.assertEquals(server.getFlavorId(), "m1.tiny");
        Assert.assertNull(server.getFlavorSummary().getName());
        Assert.assertEquals(server.getFlavor().getName(), "m1.tiny");
        Assert.assertTrue(takeRequest().getPath().endsWith("/flavors/m1.tiny"));
        Assert.assertNull(server.getHostname());
        Assert.assertNull(server.getTags());
        Assert.assertNull(server.getImageProperties());
    }

    public void volumeAttachmentReads289Fields() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"volumeAttachment\": {\"device\": \"/dev/vdb\", \"serverId\": \"" + SERVER + "\", \"volumeId\": \"v1\","
                + " \"attachment_id\": \"a1\", \"bdm_uuid\": \"b1\", \"tag\": \"data\", \"delete_on_termination\": true}}");

        var attachment = osv3().compute().servers().attachVolume(SERVER, "v1", "/dev/vdb");

        Assert.assertNull(attachment.getId());
        Assert.assertEquals(attachment.getAttachmentId(), "a1");
        Assert.assertEquals(attachment.getBdmUuid(), "b1");
        Assert.assertEquals(attachment.getTag(), "data");
        Assert.assertEquals(attachment.getDeleteOnTermination(), Boolean.TRUE);
    }
}
```
`respondWith(String resource)` 는 `AbstractTest` 의 기존 helper(클래스패스 JSON 을 200 으로 enqueue)다. 이름이 다르면 그 helper 를 쓴다.

- [ ] **Step 3: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -E 'cannot find symbol' | head -3`
Expected: `getHostname`, `getFlavorSummary` 등 symbol 없음.

- [ ] **Step 4: 모델 구현**

`Server.java` 에 추가(Javadoc 은 버전 표기 한 줄씩):
```java
    /** @return the flavor exactly as embedded in the server (2.47+: original_name, vcpus, ram, ...; before: id and links), without any lookup */
    default Flavor getFlavorSummary() { return null; }
    /** @return image properties embedded in the server (2.98+) */
    default Map<String, String> getImageProperties() { return null; }
    /** @return OS-EXT-SRV-ATTR:hostname (2.3+, all users from 2.90) */
    default String getHostname() { return null; }
    /** @return OS-EXT-SRV-ATTR:reservation_id (2.3+) */
    default String getReservationId() { return null; }
    /** @return OS-EXT-SRV-ATTR:launch_index (2.3+) */
    default Integer getLaunchIndex() { return null; }
    /** @return OS-EXT-SRV-ATTR:kernel_id (2.3+) */
    default String getKernelId() { return null; }
    /** @return OS-EXT-SRV-ATTR:ramdisk_id (2.3+) */
    default String getRamdiskId() { return null; }
    /** @return OS-EXT-SRV-ATTR:root_device_name (2.3+) */
    default String getRootDeviceName() { return null; }
    /** @return OS-EXT-SRV-ATTR:user_data, base64 (2.3+) */
    default String getUserData() { return null; }
    /** @return whether the server is locked (2.9+) */
    default Boolean getLocked() { return null; }
    /** @return host_status (2.16+) */
    default String getHostStatus() { return null; }
    /** @return description (2.19+) */
    default String getDescription() { return null; }
    /** @return tags (2.26+) */
    default List<String> getTags() { return null; }
    /** @return trusted image certificate ids (2.63+) */
    default List<String> getTrustedImageCertificates() { return null; }
    /** @return ids of the server groups the server belongs to (2.71+) */
    default List<String> getServerGroups() { return null; }
    /** @return locked_reason (2.73+) */
    default String getLockedReason() { return null; }
    /** @return pinned_availability_zone (2.96+) */
    default String getPinnedAvailabilityZone() { return null; }
    /** @return scheduler hints given at creation (2.100+) */
    default Map<String, Object> getSchedulerHints() { return null; }
```
(`Server` 인터페이스에 이미 같은 이름의 메서드가 있으면 — 예: `getUserData` — 새로 추가하지 않고 그 메서드에 NovaServer 필드를 연결한다. 이 경우 ledger 에 Ruling 으로 남긴다.)

`Flavor.java`:
```java
    /** @return original_name of a flavor embedded in a server (2.47+) */
    default String getOriginalName() { return null; }
    /** @return extra specs embedded in the flavor (2.47 server flavor, 2.61+ flavor API) */
    default Map<String, String> getExtraSpecs() { return null; }
    /** @return description (2.55+) */
    default String getDescription() { return null; }
```

`VolumeAttachment.java`:
```java
    /** @return attachment_id (2.89+) */
    default String getAttachmentId() { return null; }
    /** @return bdm_uuid (2.89+) */
    default String getBdmUuid() { return null; }
    /** @return tag (2.70+) */
    default String getTag() { return null; }
    /** @return delete_on_termination (2.79+) */
    default Boolean getDeleteOnTermination() { return null; }
```

`NovaFlavor.java` — 필드 추가와 `ephemeral` 별칭:
```java
    @JsonProperty("OS-FLV-EXT-DATA:ephemeral")
    @JsonAlias("ephemeral")
    private int ephemeral;
    ...
    @JsonProperty("original_name")
    private String originalName;
    @JsonProperty("extra_specs")
    private Map<String, String> extraSpecs;
    private String description;

    @Override
    public String getOriginalName() {
        return originalName;
    }

    @Override
    public Map<String, String> getExtraSpecs() {
        return extraSpecs;
    }

    @Override
    public String getDescription() {
        return description;
    }
```
(`NON_NULL` 직렬화라 기존 플레이버 생성 요청 본문은 변하지 않는다. `toString()` 에 `originalName` 추가.)

`NovaServer.java` — 필드와 getter:
```java
    @JsonProperty("OS-EXT-SRV-ATTR:hostname")
    private String hostname;
    @JsonProperty("OS-EXT-SRV-ATTR:reservation_id")
    private String reservationId;
    @JsonProperty("OS-EXT-SRV-ATTR:launch_index")
    private Integer launchIndex;
    @JsonProperty("OS-EXT-SRV-ATTR:kernel_id")
    private String kernelId;
    @JsonProperty("OS-EXT-SRV-ATTR:ramdisk_id")
    private String ramdiskId;
    @JsonProperty("OS-EXT-SRV-ATTR:root_device_name")
    private String rootDeviceName;
    @JsonProperty("OS-EXT-SRV-ATTR:user_data")
    private String userData;
    private Boolean locked;
    @JsonProperty("host_status")
    private String hostStatus;
    private String description;
    private List<String> tags;
    @JsonProperty("trusted_image_certificates")
    private List<String> trustedImageCertificates;
    @JsonProperty("server_groups")
    private List<String> serverGroups;
    @JsonProperty("locked_reason")
    private String lockedReason;
    @JsonProperty("pinned_availability_zone")
    private String pinnedAvailabilityZone;
    @JsonProperty("scheduler_hints")
    private Map<String, Object> schedulerHints;
```
각 필드의 `@Override` getter 를 추가한다(그대로 반환). 그 외:
```java
    @Override
    public Flavor getFlavor() {
        // 2.47+ embeds the flavor without an id; nothing to look up
        if (flavor != null && flavor.getName() == null && flavor.getId() != null)
            flavor = (NovaFlavor) Apis.getComputeServices().flavors().get(flavor.getId());
        return flavor;
    }

    @JsonIgnore
    @Override
    public Flavor getFlavorSummary() {
        return flavor;
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getImageProperties() {
        if (image instanceof Map) {
            Object properties = ((Map<String, Object>) image).get("properties");
            if (properties instanceof Map)
                return (Map<String, String>) properties;
        }
        return null;
    }
```
주의: `flavorReferenceFormStillLooksUpOn21` 는 `getFlavorSummary()` 를 `getFlavor()` 보다 먼저 호출하므로 요약은 지연 조회 전 값(`name == null`)이다.

`NovaVolumeAttachment.java`:
```java
    @JsonProperty("attachment_id")
    private String attachmentId;
    @JsonProperty("bdm_uuid")
    private String bdmUuid;
    private String tag;
    @JsonProperty("delete_on_termination")
    private Boolean deleteOnTermination;
```
과 각 `@Override` getter.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ServerModelTests,ServerTests,FlavorTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Failures: 0, Errors: 0` (ServerModelTests 4개 포함, 기존 ServerTests/FlavorTests 그대로 통과).

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(compute): read microversion server fields, 2.47 embedded flavor and 2.98 image properties

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
.superpowers/pr.sh "서버 응답 2.3~2.100 필드, 2.47 내장 flavor(지연 조회 안 함, getFlavorSummary), 2.98 image properties, 2.89 볼륨 attachment 필드."
```

---

### Task 4: 서버 생성 옵션과 목록 필터 + 버전 경계 검사

**Files:**
- Modify: `core/src/main/java/org/openstack4j/model/compute/ServerCreate.java`, `model/compute/builder/ServerCreateBuilder.java`, `model/compute/builder/BlockDeviceMappingBuilder.java`, `model/compute/NetworkCreate.java`
- Modify: `core/src/main/java/org/openstack4j/openstack/compute/domain/NovaServerCreate.java`, `NovaNetworkCreate.java`, `NovaBlockDeviceMappingCreate.java`
- Create: `core/src/main/java/org/openstack4j/model/compute/ServerListOptions.java`
- Modify: `api/compute/ServerService.java`, `openstack/compute/internal/ServerServiceImpl.java` (boot 경계 계산, `list(ServerListOptions)`)
- Create: `core-test/src/main/java/org/openstack4j/api/compute/microversion/ServerCreateOptionsTests.java`

**Interfaces:**
- Consumes: Task 2 `capped`, `requireMicroVersion`, `effectiveMicroVersion`, `ComputeMicroVersions.V`.
- Produces:
  - `ServerCreateBuilder`: `tags(List<String>)`(2.52), `trustedImageCertificates(List<String>)`(2.63), `hostname(String)`(2.90), `description(String)`(2.19), `autoAllocateNetwork()`/`noNetwork()`(2.37), `addTaggedNetwork(String networkId, String tag)`(2.42)
  - `BlockDeviceMappingBuilder`: `tag(String)`(2.42), `volumeType(String)`(2.67; 기존 필드 사용)
  - `ServerCreate` default getter: `getTags()`, `getTrustedImageCertificates()`, `getHostname()`, `getDescription()`, `getNetworksMode()` (`"auto"`/`"none"`/null)
  - `ServerListOptions` (fluent): `name`, `status`, `changesSince`, `changesBefore`(2.66), `flavor`, `image`, `host`, `allTenants`, `limit`, `marker`, `sortKey`, `sortDir`, `tags`/`tagsAny`/`notTags`/`notTagsAny`(2.26), `locked`(2.73), `availabilityZone`, `configDrive`, `keyName`, `createdAt`, `launchedAt`, `terminatedAt`, `powerState`, `taskState`, `vmState`, `progress`, `userId`; `Map<String, String> toQueryParams()`, `String getRequiredMicroVersion()`
  - `ServerService.list(ServerListOptions options)` → `GET /servers/detail`

경계 규칙(`ServerServiceImpl.boot`):
- 하한(floor) = 사용한 옵션의 최댓값: description 2.19, tags 2.52, networks auto/none 2.37, 네트워크·BDM tag 2.42, trusted certs 2.63, hostname 2.90. 이 옵션들은 **꺼진 상태에서도** 쓰면 `MicroVersionException`(새 기능).
- 기존 옵션 `host`/`hypervisorHostName`(2.74), BDM `volumeType`(2.67)은 **켜졌을 때만** 하한을 검사한다(꺼진 상태의 기존 동작 유지).
- 상한(ceiling): personality 있으면 2.56, networks 목록이 비고 mode 도 없으면 2.36.
- 하한 > 상한이면 요청 없이 `MicroVersionException("... cannot be combined ...")`.

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/c4-server-create-options
```

`ServerCreateOptionsTests.java`:
```java
package org.openstack4j.api.compute.microversion;

import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.ServerListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/ServerCreateOptions")
public class ServerCreateOptionsTests extends AbstractComputeMicroVersionTest {

    private static final String CREATED = "{\"server\": {\"id\": \"s1\"}}";

    public void newCreateOptionsAreSerialized() throws Exception {
        negotiate("2.100");
        respondWith(202, CREATED);

        ServerCreate create = Builders.server().name("a").flavor("f").image("i")
                .addTaggedNetwork("n1", "nic0")
                .tags(Arrays.asList("web", "prod"))
                .trustedImageCertificates(Collections.singletonList("cert-1"))
                .hostname("web-01")
                .description("frontend")
                .blockDevice(Builders.blockDeviceMapping().uuid("v1").bootIndex(0).tag("root").volumeType("ssd").build())
                .build();
        osv3().compute().servers().boot(create);

        RecordedRequest request = takeRequest();
        assertVersionHeaders(request, "2.100");
        JsonNode server = body(request).get("server");
        Assert.assertEquals(server.get("tags").get(1).asText(), "prod");
        Assert.assertEquals(server.get("trusted_image_certificates").get(0).asText(), "cert-1");
        Assert.assertEquals(server.get("hostname").asText(), "web-01");
        Assert.assertEquals(server.get("description").asText(), "frontend");
        Assert.assertEquals(server.get("networks").get(0).get("uuid").asText(), "n1");
        Assert.assertEquals(server.get("networks").get(0).get("tag").asText(), "nic0");
        Assert.assertEquals(server.get("block_device_mapping_v2").get(0).get("tag").asText(), "root");
        Assert.assertEquals(server.get("block_device_mapping_v2").get(0).get("volume_type").asText(), "ssd");
    }

    public void autoAndNoneNetworksAreStringsAndNotCapped() throws Exception {
        negotiate("2.100");
        respondWith(202, CREATED);
        respondWith(202, CREATED);

        osv3().compute().servers().boot(Builders.server().name("a").flavor("f").image("i").autoAllocateNetwork().build());
        osv3().compute().servers().boot(Builders.server().name("b").flavor("f").image("i").noNetwork().build());

        RecordedRequest auto = takeRequest();
        assertVersionHeaders(auto, "2.100");
        Assert.assertEquals(body(auto).get("server").get("networks").asText(), "auto");
        Assert.assertEquals(body(takeRequest()).get("server").get("networks").asText(), "none");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.90.*")
    public void hostnameNeeds290() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.89");
        try {
            osv3().compute().servers().boot(Builders.server().name("a").flavor("f").image("i").networks(Collections.singletonList("n1")).hostname("x").build());
        } finally {
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = MicroVersionException.class)
    public void newOptionWhileDisabledIsRejected() throws Exception {
        try {
            osv3().compute().servers().boot(Builders.server().name("a").flavor("f").image("i").tags(Collections.singletonList("t")).build());
        } finally {
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*combined.*")
    public void personalityWithHostnameIsRejected() throws Exception {
        negotiate("2.100");
        try {
            osv3().compute().servers().boot(Builders.server().name("a").flavor("f").image("i").networks(Collections.singletonList("n1"))
                    .addPersonality("/etc/motd", "hi").hostname("x").build());
        } finally {
            assertNoMoreRequests();
        }
    }

    public void legacyHostOptionUncheckedWhenDisabled() throws Exception {
        respondWith(202, CREATED);
        osv3().compute().servers().boot(Builders.server().name("a").flavor("f").image("i").host("compute-1").build());
        assertNoVersionHeaders(takeRequest());
    }

    public void listOptionsBecomeQueryAndCheckFloor() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"servers\": []}");

        osv3().compute().servers().list(ServerListOptions.create().locked(true).tags("web", "prod").changesBefore("2026-10-01T00:00:00Z").limit(5).availabilityZone("nova"));

        RecordedRequest request = takeRequest();
        assertVersionHeaders(request, "2.100");
        String path = request.getPath();
        Assert.assertTrue(path.contains("/servers/detail?"), path);
        Assert.assertTrue(path.contains("locked=true"), path);
        Assert.assertTrue(path.contains("tags=web%2Cprod"), path);
        Assert.assertTrue(path.contains("changes-before=2026-10-01T00%3A00%3A00Z"), path);
        Assert.assertTrue(path.contains("limit=5"), path);
        Assert.assertTrue(path.contains("availability_zone=nova"), path);
        Assert.assertEquals(ServerListOptions.create().locked(true).getRequiredMicroVersion(), "2.73");
        Assert.assertNull(ServerListOptions.create().name("x").getRequiredMicroVersion());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.66.*")
    public void listFloorIsChecked() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.60");
        try {
            osv3().compute().servers().list(ServerListOptions.create().changesBefore("2026-10-01T00:00:00Z"));
        } finally {
            assertNoMoreRequests();
        }
    }
}
```
(쿼리 인코딩 형식은 connector 마다 같다 — `Invocation.param` 이 URL 인코딩한다. 다르게 나오면 테스트를 실제 인코딩에 맞추고 Ruling 으로 남긴다.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -E 'cannot find symbol' | head -3`
Expected: `addTaggedNetwork`, `ServerListOptions` 등 없음.

- [ ] **Step 3: 요청 모델 구현**

`ServerCreate.java` 에 추가:
```java
    /** @return tags to set at creation (2.52+) */
    default List<String> getTags() { return null; }
    /** @return trusted image certificate ids (2.63+) */
    default List<String> getTrustedImageCertificates() { return null; }
    /** @return hostname (2.90+) */
    default String getHostname() { return null; }
    /** @return description (2.19+) */
    default String getDescription() { return null; }
    /** @return {@code "auto"} or {@code "none"} when networks were chosen that way (2.37+), otherwise {@code null} */
    default String getNetworksMode() { return null; }
```

`NetworkCreate.java`: `default String getTag() { return null; }`

`NovaServerCreate.java` — 필드·getter·any-getter:
```java
    private List<String> tags;
    @JsonProperty("trusted_image_certificates")
    private List<String> trustedImageCertificates;
    private String hostname;
    private String description;
    @JsonIgnore
    private String networksMode;

    @Override
    public List<String> getTags() { return tags; }

    @Override
    public List<String> getTrustedImageCertificates() { return trustedImageCertificates; }

    @Override
    public String getHostname() { return hostname; }

    @Override
    public String getDescription() { return description; }

    @JsonIgnore
    @Override
    public String getNetworksMode() { return networksMode; }

    /** Sends {@code "networks": "auto" | "none"} (2.37+); the networks list is null in that case. */
    @JsonAnyGetter
    Map<String, Object> networksModeProperty() {
        return networksMode == null ? Collections.emptyMap() : Collections.singletonMap("networks", networksMode);
    }

    public void addTaggedNetwork(String id, String tag) {
        initNetworks();
        networks.add(new NovaNetworkCreate(id, null, null, tag));
    }
```
`ServerCreateConcreteBuilder` 에:
```java
        @Override
        public ServerCreateBuilder tags(List<String> tags) { m.tags = tags; return this; }

        @Override
        public ServerCreateBuilder trustedImageCertificates(List<String> certificateIds) { m.trustedImageCertificates = certificateIds; return this; }

        @Override
        public ServerCreateBuilder hostname(String hostname) { m.hostname = hostname; return this; }

        @Override
        public ServerCreateBuilder description(String description) { m.description = description; return this; }

        @Override
        public ServerCreateBuilder autoAllocateNetwork() { m.networks = null; m.networksMode = "auto"; return this; }

        @Override
        public ServerCreateBuilder noNetwork() { m.networks = null; m.networksMode = "none"; return this; }

        @Override
        public ServerCreateBuilder addTaggedNetwork(String networkId, String tag) { m.networksMode = null; m.addTaggedNetwork(networkId, tag); return this; }
```
`ServerCreateBuilder` 인터페이스에 같은 7개 메서드 선언(Javadoc 에 하한 버전).

`NovaNetworkCreate.java`: 필드 `private String tag;`, 4-인자 생성자 `(String id, String fixedIp, String port, String tag)`(기존 3-인자 생성자는 이것을 호출), `@Override public String getTag()`. 기존 `@JsonProperty` 규칙(`uuid`, `fixed_ip`, `port`)은 그대로.

`NovaBlockDeviceMappingCreate.java`: `public String tag;` 필드(직렬화 이름 `tag`), 빌더에 `tag(String)` 추가. 기존 `volumeType` 필드의 JSON 이름이 `volume_type` 인지 확인하고, 빌더에 `volumeType(String)` 이 없으면 추가한다. `BlockDeviceMappingBuilder` 인터페이스에 `tag(String)`(와 없으면 `volumeType(String)`) 선언.

`model/compute/ServerListOptions.java`:
```java
package org.openstack4j.model.compute;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filters for {@code GET /servers/detail}. Filters that need a compute microversion are checked before the request.
 */
public class ServerListOptions {

    private final Map<String, String> params = new LinkedHashMap<>();
    private int requiredMinor = 0;

    public static ServerListOptions create() {
        return new ServerListOptions();
    }

    private ServerListOptions put(String key, Object value, int minor) {
        params.put(key, String.valueOf(value));
        requiredMinor = Math.max(requiredMinor, minor);
        return this;
    }

    public ServerListOptions name(String regex) { return put("name", regex, 0); }
    public ServerListOptions status(String status) { return put("status", status, 0); }
    public ServerListOptions changesSince(String isoTime) { return put("changes-since", isoTime, 0); }
    /** 2.66+ */
    public ServerListOptions changesBefore(String isoTime) { return put("changes-before", isoTime, 66); }
    public ServerListOptions flavor(String flavorId) { return put("flavor", flavorId, 0); }
    public ServerListOptions image(String imageId) { return put("image", imageId, 0); }
    public ServerListOptions host(String host) { return put("host", host, 0); }
    public ServerListOptions allTenants(boolean allTenants) { return put("all_tenants", allTenants, 0); }
    public ServerListOptions limit(int limit) { return put("limit", limit, 0); }
    public ServerListOptions marker(String serverId) { return put("marker", serverId, 0); }
    public ServerListOptions sortKey(String key) { return put("sort_key", key, 0); }
    public ServerListOptions sortDir(String dir) { return put("sort_dir", dir, 0); }
    /** Servers with all of these tags (2.26+). */
    public ServerListOptions tags(String... tags) { return put("tags", String.join(",", tags), 26); }
    /** Servers with any of these tags (2.26+). */
    public ServerListOptions tagsAny(String... tags) { return put("tags-any", String.join(",", tags), 26); }
    /** 2.26+ */
    public ServerListOptions notTags(String... tags) { return put("not-tags", String.join(",", tags), 26); }
    /** 2.26+ */
    public ServerListOptions notTagsAny(String... tags) { return put("not-tags-any", String.join(",", tags), 26); }
    /** 2.73+ */
    public ServerListOptions locked(boolean locked) { return put("locked", locked, 73); }
    // the following were admin-only before 2.83 and open to all users from 2.83, so no floor is enforced
    public ServerListOptions availabilityZone(String zone) { return put("availability_zone", zone, 0); }
    public ServerListOptions configDrive(boolean configDrive) { return put("config_drive", configDrive, 0); }
    public ServerListOptions keyName(String keyName) { return put("key_name", keyName, 0); }
    public ServerListOptions createdAt(String isoTime) { return put("created_at", isoTime, 0); }
    public ServerListOptions launchedAt(String isoTime) { return put("launched_at", isoTime, 0); }
    public ServerListOptions terminatedAt(String isoTime) { return put("terminated_at", isoTime, 0); }
    public ServerListOptions powerState(int powerState) { return put("power_state", powerState, 0); }
    public ServerListOptions taskState(String taskState) { return put("task_state", taskState, 0); }
    public ServerListOptions vmState(String vmState) { return put("vm_state", vmState, 0); }
    public ServerListOptions progress(int progress) { return put("progress", progress, 0); }
    public ServerListOptions userId(String userId) { return put("user_id", userId, 0); }

    public Map<String, String> toQueryParams() {
        return new LinkedHashMap<>(params);
    }

    /** @return the lowest compute microversion these filters need, such as {@code "2.73"}, or {@code null} */
    public String getRequiredMicroVersion() {
        return requiredMinor == 0 ? null : "2." + requiredMinor;
    }
}
```

- [ ] **Step 4: 서비스 구현**

`ServerService.java`:
```java
    /**
     * Lists servers with details using typed filters. Filters that need a newer compute microversion fail before the
     * request when the session does not send it.
     */
    List<? extends Server> list(ServerListOptions options);
```

`ServerServiceImpl.java` — Task 2 의 boot 상한 코드를 아래로 **대체**:
```java
    @Override
    public Server boot(ServerCreate server) {
        Objects.requireNonNull(server);
        return capped(post(NovaServer.class, uri("/servers")), bootCeiling(server))
                .entity(WrapServerIfApplicableFunction.INSTANCE.apply(server))
                .execute();
    }

    /** Checks the floors of the options used and returns the highest microversion the request can be sent at. */
    private MicroVersion bootCeiling(ServerCreate server) {
        MicroVersion floor = null;
        String floorFeature = null;
        Map<String, MicroVersion> used = new LinkedHashMap<>();
        if (server.getDescription() != null) used.put("description", V(19));
        if (server.getNetworksMode() != null) used.put("networks \"" + server.getNetworksMode() + "\"", V(37));
        if (server.getNetworks() != null && server.getNetworks().stream().anyMatch(n -> n.getTag() != null)) used.put("network tag", V(42));
        if (server.getBlockDeviceMapping() != null && server.getBlockDeviceMapping().stream().anyMatch(b -> b.getTag() != null)) used.put("block device tag", V(42));
        if (server.getTags() != null) used.put("tags", V(52));
        if (server.getTrustedImageCertificates() != null) used.put("trusted image certificates", V(63));
        if (server.getHostname() != null) used.put("hostname", V(90));
        if (effectiveMicroVersion(null) != null) {
            if (server.getHost() != null || server.getHypervisorHostName() != null) used.put("host / hypervisor_hostname", V(74));
            if (server.getBlockDeviceMapping() != null && server.getBlockDeviceMapping().stream().anyMatch(b -> b.getVolumeType() != null)) used.put("block device volume_type", V(67));
        }
        for (Map.Entry<String, MicroVersion> e : used.entrySet()) {
            requireMicroVersion("Server create option " + e.getKey(), e.getValue());
            if (floor == null || e.getValue().compareTo(floor) > 0) {
                floor = e.getValue();
                floorFeature = e.getKey();
            }
        }
        MicroVersion ceiling = null;
        String ceilingReason = null;
        if (server.getPersonality() != null && !server.getPersonality().isEmpty()) {
            ceiling = V(56);
            ceilingReason = "personality (removed in 2.57)";
        }
        if (server.getNetworksMode() == null && (server.getNetworks() == null || server.getNetworks().isEmpty())
                && (ceiling == null || V(36).compareTo(ceiling) < 0)) {
            ceiling = V(36);
            ceilingReason = "no networks (required from 2.37; use autoAllocateNetwork() or noNetwork())";
        }
        if (floor != null && ceiling != null && floor.compareTo(ceiling) > 0)
            throw new MicroVersionException("Server create option " + floorFeature + " needs " + floor + " and cannot be combined with "
                    + ceilingReason + ", which needs " + ceiling + " or lower");
        return ceiling;
    }

    @Override
    public List<? extends Server> list(ServerListOptions options) {
        Objects.requireNonNull(options);
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Server list filters " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
        Invocation<Servers> req = get(Servers.class, uri("/servers/detail"));
        options.toQueryParams().forEach(req::param);
        return req.execute().getList();
    }
```
(`static import ComputeMicroVersions.V`. `BlockDeviceMappingCreate` 에 `getTag()`/`getVolumeType()` 가 없으면 `default` 로 추가하고 `NovaBlockDeviceMappingCreate` 에서 구현한다. `ServerCreate.getBlockDeviceMapping()` 의 실제 이름을 확인해 맞춘다.)

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ServerCreateOptionsTests,LegacyCeilingTests,ServerTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Failures: 0, Errors: 0` (ServerCreateOptionsTests 8개).

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(compute): add microversion server create options and typed server list filters

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
.superpowers/pr.sh "서버 생성 옵션(tags, trusted certs, hostname, description, networks auto/none, 네트워크·BDM tag, volume_type)과 하한·상한 검사, ServerListOptions(2.26/2.66/2.73/2.83 필터)."
```

---

### Task 5: 서버 액션 보강

**Files:**
- Create (`core/src/main/java/org/openstack4j/model/compute/actions/`): `LiveMigrateRequest.java`, `EvacuateRequest.java`, `RebuildRequest.java`, `UnshelveRequest.java`, `RescueRequest.java`
- Create: `core/src/main/java/org/openstack4j/openstack/compute/domain/JsonBody.java` (명시적 `null` 을 보존하는 요청 본문)
- Modify: `api/compute/ServerService.java`, `openstack/compute/internal/ServerServiceImpl.java`
- Create: `core-test/src/main/java/org/openstack4j/api/compute/microversion/ServerActionTests.java`

**Interfaces:**
- Consumes: Task 2 `capped`, `requireMicroVersion`, `effectiveMicroVersion`, `imageIdFrom`, `V`.
- Produces (`ServerService`):
  - `ActionResponse lock(String serverId, String reason)` — 2.73
  - `ActionResponse migrateServer(String serverId, String host)` — 2.56
  - `ActionResponse liveMigrate(String serverId, LiveMigrateRequest request)`
  - `ActionResponse evacuate(String serverId, EvacuateRequest request)`
  - `ActionResponse rebuild(String serverId, RebuildRequest request)`
  - `ActionResponse unshelve(String serverId, UnshelveRequest request)` — 2.77
  - `ActionResponse rescue(String serverId, RescueRequest request)`
  - `String createBackup(String serverId, BackupOptions options)` — 이미지 id(Location 또는 2.45+ 본문)
- `JsonBody.of(String root, Map<String, ?> fields)`, `JsonBody.of(Map<String, ?> fields)` → `{"<root>": {...}}`, `null` 값은 JSON `null` 로 보존(Task 6, 9, 10 도 사용)
- 새 요청은 `JsonBody` 로 보낸다(`ServerServiceImpl.invokeMapAction`). 기존 `ServerAction` 클래스는 건드리지 않는다.
- 배경: 전역 mapper 가 `NON_NULL` 이라 `Map` 의 `null` 값은 직렬화에서 빠진다(Jackson 2.22 로 확인: `{"rebuild":{"k":null,"v":1}}` → `{"rebuild":{"v":1}}`). `ObjectNode` 는 `null` 을 유지한다. 또 `Invocation.entity` 는 `ModelEntity` 만 받는다.

버전 규칙:
| 요청 | 규칙 |
|---|---|
| LiveMigrateRequest | `blockMigrationAuto()` 하한 2.25; `force(true)` 하한 2.30·상한 2.67; `diskOverCommit(..)` 상한 2.24; block migration 을 지정하지 않으면 적용 버전 ≥2.25 → `"auto"`, 아니면 `false` |
| EvacuateRequest | `onSharedStorage(..)` 상한 2.13; `force(true)` 하한 2.29·상한 2.67; 응답 본문(2.13 이하 adminPass)은 읽지 않음 |
| RebuildRequest | description 2.19, keyName 2.54(`null` 로 지우기는 `removeKeyName()`), userData 2.57, trustedImageCertificates 2.63, hostname 2.90 |
| UnshelveRequest | availabilityZone 2.77; host 2.91; `unpinAvailabilityZone()` 2.91(`"availability_zone": null`) |
| RescueRequest | adminPass, rescueImageRef — 하한 없음(BFV 구조 복구는 서버가 2.87 이상일 때 자동) |

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/c5-server-actions
```

`ServerActionTests.java`:
```java
package org.openstack4j.api.compute.microversion;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.actions.BackupOptions;
import org.openstack4j.model.compute.actions.EvacuateRequest;
import org.openstack4j.model.compute.actions.LiveMigrateRequest;
import org.openstack4j.model.compute.actions.RebuildRequest;
import org.openstack4j.model.compute.actions.RescueRequest;
import org.openstack4j.model.compute.actions.UnshelveRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/ServerActions")
public class ServerActionTests extends AbstractComputeMicroVersionTest {

    private JsonNode sentAction(String expectedVersion) throws Exception {
        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/servers/" + SERVER + "/action"), request.getPath());
        if (expectedVersion != null)
            assertVersionHeaders(request, expectedVersion);
        return body(request);
    }

    public void lockWithReason() throws Exception {
        negotiate("2.100");
        respondWith(202);
        Assert.assertTrue(osv3().compute().servers().lock(SERVER, "maintenance").isSuccess());
        Assert.assertEquals(sentAction("2.100").get("lock").get("locked_reason").asText(), "maintenance");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.73.*")
    public void lockWithReasonNeeds273() throws Exception {
        try {
            osv3().compute().servers().lock(SERVER, "x");
        } finally {
            assertNoMoreRequests();
        }
    }

    public void migrateToHost() throws Exception {
        negotiate("2.100");
        respondWith(202);
        osv3().compute().servers().migrateServer(SERVER, "compute-2");
        Assert.assertEquals(sentAction("2.100").get("migrate").get("host").asText(), "compute-2");
    }

    public void liveMigrateDefaultsToAutoAndForceIsCapped() throws Exception {
        negotiate("2.100");
        respondWith(202);
        respondWith(202);

        osv3().compute().servers().liveMigrate(SERVER, LiveMigrateRequest.create().host("compute-2"));
        JsonNode auto = sentAction("2.100").get("os-migrateLive");
        Assert.assertEquals(auto.get("block_migration").asText(), "auto");
        Assert.assertEquals(auto.get("host").asText(), "compute-2");
        Assert.assertFalse(auto.has("disk_over_commit"));

        osv3().compute().servers().liveMigrate(SERVER, LiveMigrateRequest.create().host("compute-2").force(true));
        Assert.assertTrue(sentAction("2.67").get("os-migrateLive").get("force").asBoolean());
    }

    public void liveMigrateDiskOverCommitIsCappedAt224() throws Exception {
        negotiate("2.100");
        respondWith(202);
        osv3().compute().servers().liveMigrate(SERVER, LiveMigrateRequest.create().blockMigration(false).diskOverCommit(false));
        JsonNode body = sentAction("2.24").get("os-migrateLive");
        Assert.assertFalse(body.get("block_migration").asBoolean());
        Assert.assertFalse(body.get("disk_over_commit").asBoolean());
        Assert.assertTrue(body.get("host").isNull());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*combined.*")
    public void liveMigrateAutoWithDiskOverCommitIsRejected() throws Exception {
        negotiate("2.100");
        try {
            osv3().compute().servers().liveMigrate(SERVER, LiveMigrateRequest.create().blockMigrationAuto().diskOverCommit(true));
        } finally {
            assertNoMoreRequests();
        }
    }

    public void evacuateWithoutSharedStorage() throws Exception {
        negotiate("2.100");
        respondWith(200, "{}");
        osv3().compute().servers().evacuate(SERVER, EvacuateRequest.create().host("compute-2").adminPass("pw"));
        JsonNode body = sentAction("2.100").get("evacuate");
        Assert.assertEquals(body.get("host").asText(), "compute-2");
        Assert.assertEquals(body.get("adminPass").asText(), "pw");
        Assert.assertFalse(body.has("onSharedStorage"));
    }

    public void rebuildWithNewFields() throws Exception {
        negotiate("2.100");
        respondWith(202, "{\"server\": {\"id\": \"" + SERVER + "\"}}");
        osv3().compute().servers().rebuild(SERVER, RebuildRequest.create("img-2").name("vm-2").keyName("k1").userData("dXNlcg==")
                .hostname("vm-2").description("rebuilt").trustedImageCertificates(Collections.singletonList("c1")).preserveEphemeral(true));
        JsonNode body = sentAction("2.100").get("rebuild");
        Assert.assertEquals(body.get("imageRef").asText(), "img-2");
        Assert.assertEquals(body.get("key_name").asText(), "k1");
        Assert.assertEquals(body.get("user_data").asText(), "dXNlcg==");
        Assert.assertEquals(body.get("hostname").asText(), "vm-2");
        Assert.assertEquals(body.get("description").asText(), "rebuilt");
        Assert.assertEquals(body.get("trusted_image_certificates").get(0).asText(), "c1");
        Assert.assertTrue(body.get("preserve_ephemeral").asBoolean());
    }

    public void rebuildRemoveKeyNameSendsNull() throws Exception {
        negotiate("2.100");
        respondWith(202, "{\"server\": {\"id\": \"" + SERVER + "\"}}");
        osv3().compute().servers().rebuild(SERVER, RebuildRequest.create("img-2").removeKeyName());
        JsonNode body = sentAction("2.100").get("rebuild");
        Assert.assertTrue(body.has("key_name"));
        Assert.assertTrue(body.get("key_name").isNull());
    }

    public void unshelveToZoneAndHostAndUnpin() throws Exception {
        negotiate("2.100");
        respondWith(202);
        respondWith(202);
        osv3().compute().servers().unshelve(SERVER, UnshelveRequest.create().availabilityZone("az2").host("compute-3"));
        JsonNode first = sentAction("2.100").get("unshelve");
        Assert.assertEquals(first.get("availability_zone").asText(), "az2");
        Assert.assertEquals(first.get("host").asText(), "compute-3");

        osv3().compute().servers().unshelve(SERVER, UnshelveRequest.create().unpinAvailabilityZone());
        JsonNode second = sentAction("2.100").get("unshelve");
        Assert.assertTrue(second.has("availability_zone"));
        Assert.assertTrue(second.get("availability_zone").isNull());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.91.*")
    public void unshelveHostNeeds291() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.80");
        try {
            osv3().compute().servers().unshelve(SERVER, UnshelveRequest.create().host("compute-3"));
        } finally {
            assertNoMoreRequests();
        }
    }

    public void rescueWithImage() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"adminPass\": \"pw\"}");
        osv3().compute().servers().rescue(SERVER, RescueRequest.create().rescueImageRef("img-r").adminPass("pw"));
        JsonNode body = sentAction("2.100").get("rescue");
        Assert.assertEquals(body.get("rescue_image_ref").asText(), "img-r");
        Assert.assertEquals(body.get("adminPass").asText(), "pw");
    }

    public void createBackupReturnsImageIdFromBody() throws Exception {
        negotiate("2.100");
        respondWith(202, "{\"image_id\": \"bk-1\"}");
        String id = osv3().compute().servers().createBackup(SERVER, BackupOptions.create("daily", "daily", 2));
        Assert.assertEquals(id, "bk-1");
        Assert.assertEquals(sentAction("2.100").get("createBackup").get("rotation").asInt(), 2);
    }
}
```
(`respondWith(int status)` 는 본문 없는 응답을 enqueue 하는 기존 helper. 없으면 `respondWith(status, "")` 를 쓴다. `BackupOptions.create(name, type, rotation)` 의 실제 시그니처에 맞춘다.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -E 'cannot find symbol' | head -3`
Expected: `LiveMigrateRequest` 등 없음.

- [ ] **Step 3: 요청 클래스 구현**

`LiveMigrateRequest.java`:
```java
package org.openstack4j.model.compute.actions;

/** Live migration request for microversion-aware sessions ({@code os-migrateLive}). */
public class LiveMigrateRequest {

    private String host;
    private Boolean blockMigration;
    private boolean blockMigrationAuto;
    private Boolean diskOverCommit;
    private Boolean force;

    public static LiveMigrateRequest create() {
        return new LiveMigrateRequest();
    }

    /** Destination host; {@code null} lets the scheduler choose. */
    public LiveMigrateRequest host(String host) { this.host = host; return this; }

    public LiveMigrateRequest blockMigration(boolean blockMigration) { this.blockMigration = blockMigration; this.blockMigrationAuto = false; return this; }

    /** {@code "block_migration": "auto"} (2.25+). */
    public LiveMigrateRequest blockMigrationAuto() { this.blockMigration = null; this.blockMigrationAuto = true; return this; }

    /** Removed in 2.25; the request is sent at 2.24 or lower. */
    public LiveMigrateRequest diskOverCommit(boolean diskOverCommit) { this.diskOverCommit = diskOverCommit; return this; }

    /** Skip the scheduler check of {@code host} (2.30 - 2.67). */
    public LiveMigrateRequest force(boolean force) { this.force = force; return this; }

    public String getHost() { return host; }
    public Boolean getBlockMigration() { return blockMigration; }
    public boolean isBlockMigrationAuto() { return blockMigrationAuto; }
    public Boolean getDiskOverCommit() { return diskOverCommit; }
    public Boolean getForce() { return force; }
}
```

`EvacuateRequest.java`:
```java
package org.openstack4j.model.compute.actions;

/** Evacuate request for microversion-aware sessions. */
public class EvacuateRequest {

    private String host;
    private String adminPass;
    private Boolean onSharedStorage;
    private Boolean force;

    public static EvacuateRequest create() {
        return new EvacuateRequest();
    }

    public EvacuateRequest host(String host) { this.host = host; return this; }
    public EvacuateRequest adminPass(String adminPass) { this.adminPass = adminPass; return this; }
    /** Removed in 2.14; the request is sent at 2.13 or lower. */
    public EvacuateRequest onSharedStorage(boolean onSharedStorage) { this.onSharedStorage = onSharedStorage; return this; }
    /** 2.29 - 2.67 */
    public EvacuateRequest force(boolean force) { this.force = force; return this; }

    public String getHost() { return host; }
    public String getAdminPass() { return adminPass; }
    public Boolean getOnSharedStorage() { return onSharedStorage; }
    public Boolean getForce() { return force; }
}
```

`RebuildRequest.java`:
```java
package org.openstack4j.model.compute.actions;

import java.util.List;
import java.util.Map;

/** Rebuild request for microversion-aware sessions. Personality is not supported here (removed in 2.57). */
public class RebuildRequest {

    private final String imageRef;
    private String name;
    private String adminPass;
    private Map<String, String> metadata;
    private Boolean preserveEphemeral;
    private String accessIPv4;
    private String accessIPv6;
    private String description;
    private String keyName;
    private boolean removeKeyName;
    private String userData;
    private List<String> trustedImageCertificates;
    private String hostname;

    private RebuildRequest(String imageRef) {
        this.imageRef = imageRef;
    }

    public static RebuildRequest create(String imageRef) {
        return new RebuildRequest(imageRef);
    }

    public RebuildRequest name(String name) { this.name = name; return this; }
    public RebuildRequest adminPass(String adminPass) { this.adminPass = adminPass; return this; }
    public RebuildRequest metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }
    public RebuildRequest preserveEphemeral(boolean preserveEphemeral) { this.preserveEphemeral = preserveEphemeral; return this; }
    public RebuildRequest accessIPv4(String accessIPv4) { this.accessIPv4 = accessIPv4; return this; }
    public RebuildRequest accessIPv6(String accessIPv6) { this.accessIPv6 = accessIPv6; return this; }
    /** 2.19+ */
    public RebuildRequest description(String description) { this.description = description; return this; }
    /** 2.54+ */
    public RebuildRequest keyName(String keyName) { this.keyName = keyName; this.removeKeyName = false; return this; }
    /** Removes the key pair ({@code "key_name": null}, 2.54+). */
    public RebuildRequest removeKeyName() { this.keyName = null; this.removeKeyName = true; return this; }
    /** Base64 user data (2.57+). */
    public RebuildRequest userData(String userData) { this.userData = userData; return this; }
    /** 2.63+ */
    public RebuildRequest trustedImageCertificates(List<String> ids) { this.trustedImageCertificates = ids; return this; }
    /** 2.90+ */
    public RebuildRequest hostname(String hostname) { this.hostname = hostname; return this; }

    public String getImageRef() { return imageRef; }
    public String getName() { return name; }
    public String getAdminPass() { return adminPass; }
    public Map<String, String> getMetadata() { return metadata; }
    public Boolean getPreserveEphemeral() { return preserveEphemeral; }
    public String getAccessIPv4() { return accessIPv4; }
    public String getAccessIPv6() { return accessIPv6; }
    public String getDescription() { return description; }
    public String getKeyName() { return keyName; }
    public boolean isRemoveKeyName() { return removeKeyName; }
    public String getUserData() { return userData; }
    public List<String> getTrustedImageCertificates() { return trustedImageCertificates; }
    public String getHostname() { return hostname; }
}
```

`UnshelveRequest.java`:
```java
package org.openstack4j.model.compute.actions;

/** Unshelve request with a destination (2.77+). */
public class UnshelveRequest {

    private String availabilityZone;
    private boolean unpinAvailabilityZone;
    private String host;

    public static UnshelveRequest create() {
        return new UnshelveRequest();
    }

    /** 2.77+ */
    public UnshelveRequest availabilityZone(String availabilityZone) { this.availabilityZone = availabilityZone; this.unpinAvailabilityZone = false; return this; }
    /** {@code "availability_zone": null} (2.91+). */
    public UnshelveRequest unpinAvailabilityZone() { this.availabilityZone = null; this.unpinAvailabilityZone = true; return this; }
    /** 2.91+ */
    public UnshelveRequest host(String host) { this.host = host; return this; }

    public String getAvailabilityZone() { return availabilityZone; }
    public boolean isUnpinAvailabilityZone() { return unpinAvailabilityZone; }
    public String getHost() { return host; }
}
```

`RescueRequest.java`:
```java
package org.openstack4j.model.compute.actions;

public class RescueRequest {

    private String adminPass;
    private String rescueImageRef;

    public static RescueRequest create() {
        return new RescueRequest();
    }

    public RescueRequest adminPass(String adminPass) { this.adminPass = adminPass; return this; }
    public RescueRequest rescueImageRef(String imageRef) { this.rescueImageRef = imageRef; return this; }

    public String getAdminPass() { return adminPass; }
    public String getRescueImageRef() { return rescueImageRef; }
}
```

`openstack/compute/domain/JsonBody.java`:
```java
package org.openstack4j.openstack.compute.domain;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openstack4j.model.ModelEntity;

/**
 * A request body built from a map that keeps explicit {@code null} values ({@code "key_name": null}), which the
 * client's NON_NULL object mapper would otherwise drop.
 */
public final class JsonBody implements ModelEntity {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper PLAIN = new ObjectMapper();

    private final ObjectNode node;

    private JsonBody(ObjectNode node) {
        this.node = node;
    }

    /** @return {@code {"<root>": {fields}}} */
    public static JsonBody of(String root, Map<String, ?> fields) {
        ObjectNode body = PLAIN.createObjectNode();
        body.set(root, toNode(fields));
        return new JsonBody(body);
    }

    /** @return {@code {fields}} without a root element */
    public static JsonBody of(Map<String, ?> fields) {
        return new JsonBody(toNode(fields));
    }

    private static ObjectNode toNode(Map<String, ?> fields) {
        ObjectNode node = PLAIN.createObjectNode();
        fields.forEach((k, v) -> {
            if (v == null)
                node.putNull(k);
            else
                node.set(k, PLAIN.valueToTree(v));
        });
        return node;
    }

    @JsonValue
    public ObjectNode toJson() {
        return node;
    }
}
```
(중첩 값은 기본 설정 mapper 로 트리가 되므로 중첩 `Map` 안의 `null` 도 유지된다. 중첩에 POJO 를 넣지 않는다 — 모든 호출부는 `Map`/`List`/문자열/숫자만 넣는다.)

- [ ] **Step 4: 서비스 구현**

`ServerService.java` 에 Interfaces 의 8개 메서드를 Javadoc(하한·상한) 과 함께 선언한다.

`ServerServiceImpl.java`:
```java
    private ActionResponse invokeMapAction(String serverId, String action, Map<String, ?> body, MicroVersion ceiling) {
        HttpResponse response = capped(post(Void.class, uri("/servers/%s/action", serverId)), ceiling)
                .entity(JsonBody.of(action, body))
                .executeWithResponse();
        return ToActionResponseFunction.INSTANCE.apply(response, action);
    }

    private static MicroVersion lower(MicroVersion a, MicroVersion b) {
        return a == null ? b : b == null ? a : MicroVersions.min(a, b);
    }

    @Override
    public ActionResponse lock(String serverId, String reason) {
        Objects.requireNonNull(serverId);
        requireMicroVersion("Lock with a reason", V(73));
        return invokeMapAction(serverId, "lock", Collections.singletonMap("locked_reason", reason), null);
    }

    @Override
    public ActionResponse migrateServer(String serverId, String host) {
        Objects.requireNonNull(serverId);
        requireMicroVersion("Cold migration to a host", V(56));
        return invokeMapAction(serverId, "migrate", Collections.singletonMap("host", host), null);
    }

    @Override
    public ActionResponse liveMigrate(String serverId, LiveMigrateRequest request) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(request);
        MicroVersion ceiling = null;
        if (request.isBlockMigrationAuto())
            requireMicroVersion("block_migration \"auto\"", V(25));
        if (Boolean.TRUE.equals(request.getForce())) {
            requireMicroVersion("Forced live migration", V(30));
            ceiling = V(67);
        }
        if (request.getDiskOverCommit() != null) {
            if (request.isBlockMigrationAuto() || Boolean.TRUE.equals(request.getForce()))
                throw new MicroVersionException("disk_over_commit (2.24 or lower) cannot be combined with block_migration \"auto\" or force");
            ceiling = lower(ceiling, V(24));
        }
        MicroVersion effective = effectiveMicroVersion(ceiling);
        boolean modern = effective != null && effective.compareTo(V(25)) >= 0;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("host", request.getHost());
        if (request.isBlockMigrationAuto())
            body.put("block_migration", "auto");
        else if (request.getBlockMigration() != null)
            body.put("block_migration", request.getBlockMigration());
        else
            body.put("block_migration", modern ? "auto" : false);
        if (!modern)
            body.put("disk_over_commit", request.getDiskOverCommit() != null ? request.getDiskOverCommit() : false);
        if (request.getForce() != null)
            body.put("force", request.getForce());
        return invokeMapAction(serverId, "os-migrateLive", body, ceiling);
    }

    @Override
    public ActionResponse evacuate(String serverId, EvacuateRequest request) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(request);
        MicroVersion ceiling = null;
        if (Boolean.TRUE.equals(request.getForce())) {
            requireMicroVersion("Forced evacuate", V(29));
            ceiling = V(67);
        }
        if (request.getOnSharedStorage() != null) {
            if (request.getForce() != null)
                throw new MicroVersionException("onSharedStorage (2.13 or lower) cannot be combined with force (2.29+)");
            ceiling = lower(ceiling, V(13));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.getHost() != null) body.put("host", request.getHost());
        if (request.getAdminPass() != null) body.put("adminPass", request.getAdminPass());
        if (request.getOnSharedStorage() != null) body.put("onSharedStorage", request.getOnSharedStorage());
        if (request.getForce() != null) body.put("force", request.getForce());
        return invokeMapAction(serverId, "evacuate", body, ceiling);
    }

    @Override
    public ActionResponse rebuild(String serverId, RebuildRequest request) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("imageRef", request.getImageRef());
        if (request.getName() != null) body.put("name", request.getName());
        if (request.getAdminPass() != null) body.put("adminPass", request.getAdminPass());
        if (request.getMetadata() != null) body.put("metadata", request.getMetadata());
        if (request.getPreserveEphemeral() != null) body.put("preserve_ephemeral", request.getPreserveEphemeral());
        if (request.getAccessIPv4() != null) body.put("accessIPv4", request.getAccessIPv4());
        if (request.getAccessIPv6() != null) body.put("accessIPv6", request.getAccessIPv6());
        if (request.getDescription() != null) {
            requireMicroVersion("Rebuild description", V(19));
            body.put("description", request.getDescription());
        }
        if (request.getKeyName() != null || request.isRemoveKeyName()) {
            requireMicroVersion("Rebuild key_name", V(54));
            body.put("key_name", request.getKeyName());
        }
        if (request.getUserData() != null) {
            requireMicroVersion("Rebuild user_data", V(57));
            body.put("user_data", request.getUserData());
        }
        if (request.getTrustedImageCertificates() != null) {
            requireMicroVersion("Rebuild trusted_image_certificates", V(63));
            body.put("trusted_image_certificates", request.getTrustedImageCertificates());
        }
        if (request.getHostname() != null) {
            requireMicroVersion("Rebuild hostname", V(90));
            body.put("hostname", request.getHostname());
        }
        return invokeMapAction(serverId, "rebuild", body, null);
    }

    @Override
    public ActionResponse unshelve(String serverId, UnshelveRequest request) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(request);
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.getAvailabilityZone() != null) {
            requireMicroVersion("Unshelve to an availability zone", V(77));
            body.put("availability_zone", request.getAvailabilityZone());
        }
        if (request.isUnpinAvailabilityZone()) {
            requireMicroVersion("Unshelve unpinning the availability zone", V(91));
            body.put("availability_zone", null);
        }
        if (request.getHost() != null) {
            requireMicroVersion("Unshelve to a host", V(91));
            body.put("host", request.getHost());
        }
        if (body.isEmpty())
            requireMicroVersion("Unshelve with a request body", V(77));
        return invokeMapAction(serverId, "unshelve", body, null);
    }

    @Override
    public ActionResponse rescue(String serverId, RescueRequest request) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(request);
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.getAdminPass() != null) body.put("adminPass", request.getAdminPass());
        if (request.getRescueImageRef() != null) body.put("rescue_image_ref", request.getRescueImageRef());
        return invokeMapAction(serverId, "rescue", body, null);
    }

    @Override
    public String createBackup(String serverId, BackupOptions options) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(options);
        return imageIdFrom(invokeActionWithResponse(serverId, BackupAction.create(options)));
    }
```
`null` 보존은 `JsonBody` 가 맡는다(`rebuildRemoveKeyNameSendsNull`, `unshelveToZoneAndHostAndUnpin`, `liveMigrateDiskOverCommitIsCappedAt224` 의 `"host": null` 이 확인). `ToActionResponseFunction.apply(HttpResponse, String)` 는 기존 메서드다.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ServerActionTests,ServerTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Failures: 0, Errors: 0` (ServerActionTests 13개).

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(compute): add microversion-aware server actions

lock with reason, migrate to host, live migration with auto block
migration, evacuate, rebuild with key_name/user_data/hostname, unshelve
to zone/host, rescue with image and createBackup returning the image id.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
.superpowers/pr.sh "서버 액션 보강: lock(2.73), migrate host(2.56), live-migrate(2.25/2.30/2.68), evacuate(2.14/2.29/2.68), rebuild(2.19/2.54/2.57/2.63/2.90), unshelve(2.77/2.91), rescue, createBackup(2.45)."
```

---

### Task 6: topology, ips, remote-consoles, diagnostics 표준, 비동기 볼륨 연결, pinned AZ 수정

**Files:**
- Create (`core/src/main/java/org/openstack4j/model/compute/`): `ServerTopology.java`, `RemoteConsole.java`, `ServerDiagnosticsStandard.java`
- Create (`core/src/main/java/org/openstack4j/openstack/compute/domain/`): `NovaServerTopology.java`, `NovaRemoteConsole.java`, `NovaServerDiagnosticsStandard.java`, `NovaNetworkIps.java`
- Modify: `api/compute/ServerService.java`, `openstack/compute/internal/ServerServiceImpl.java`
- Create: `core-test/src/main/resources/compute/microversion/diagnostics_2_48.json`, `topology.json`
- Create: `core-test/src/main/java/org/openstack4j/api/compute/microversion/ServerSubResourceTests.java`

**Interfaces:**
- Consumes: Task 2 `requireMicroVersion`, `V`; Task 5 `JsonBody.of`.
- Produces (`ServerService`):
  - `ServerTopology topology(String serverId)` — 2.78, `GET /servers/{id}/topology`
  - `Addresses ips(String serverId)`, `List<? extends Address> ips(String serverId, String networkLabel)` — `GET /servers/{id}/ips[/{label}]`
  - `RemoteConsole remoteConsole(String serverId, String protocol, String type)` — 2.6(mks 2.8, spice-direct 2.99), `POST /servers/{id}/remote-consoles`
  - `ServerDiagnosticsStandard diagnosticsStandard(String serverId)` — 2.48, `GET /servers/{id}/diagnostics`
  - `ActionResponse attachVolumeAsync(String serverId, String volumeId, String device)` — 2.101, `device` nullable
  - `Server updatePinnedAvailabilityZone(String serverId, String availabilityZone)` — 2.104, `null` 은 고정 해제
- 모델:
  - `ServerTopology`: `List<? extends Node> getNodes()`, `Integer getPagesizeKb()`; `Node`: `Map<String, Integer> getCpuPinning()`, `Integer getHostNode()`, `Integer getMemoryMb()`, `List<List<Integer>> getSiblings()`, `List<Integer> getVcpuSet()`
  - `RemoteConsole`: `getProtocol()`, `getType()`, `getUrl()`
  - `ServerDiagnosticsStandard`: `getState()`, `getDriver()`, `getHypervisor()`, `getHypervisorOs()`, `Long getUptime()`, `Boolean getConfigDrive()`, `Integer getNumCpus()`, `Integer getNumNics()`, `Integer getNumDisks()`, `List<? extends CpuDetail> getCpuDetails()`, `List<? extends DiskDetail> getDiskDetails()`, `List<? extends NicDetail> getNicDetails()`, `MemoryDetails getMemoryDetails()`; 하위: `CpuDetail`(`Integer getId()`, `Long getTime()`, `Integer getUtilisation()`), `DiskDetail`(`Long getReadBytes()`, `Long getReadRequests()`, `Long getWriteBytes()`, `Long getWriteRequests()`, `Long getErrorsCount()`), `NicDetail`(`String getMacAddress()`, `Long getRxOctets()`, `Long getRxErrors()`, `Long getRxDrop()`, `Long getRxPackets()`, `Long getRxRate()`, `Long getTxOctets()`, `Long getTxErrors()`, `Long getTxDrop()`, `Long getTxPackets()`, `Long getTxRate()`), `MemoryDetails`(`Long getMaximum()`, `Long getUsed()`)

- [ ] **Step 1: fixture 작성**

```bash
git switch main && git pull && git switch -c task/c6-server-subresources
```

`diagnostics_2_48.json` (epoxy 실제 응답 일반화):
```json
{
  "state": "running",
  "driver": "libvirt",
  "hypervisor": "kvm",
  "hypervisor_os": "linux",
  "uptime": 858833,
  "config_drive": false,
  "num_cpus": 1,
  "num_nics": 1,
  "num_disks": 1,
  "disk_details": [{"read_bytes": 8446494208, "read_requests": 97087, "write_bytes": 7967033344, "write_requests": 38855, "errors_count": -1}],
  "cpu_details": [{"id": 0, "time": 10897320000000, "utilisation": null}],
  "nic_details": [{"mac_address": "fa:16:3e:ee:08:6f", "rx_octets": 8769411449, "rx_errors": 0, "rx_drop": 0, "rx_packets": 6227321, "rx_rate": null,
                   "tx_octets": 50291014, "tx_errors": 0, "tx_drop": 0, "tx_packets": 465870, "tx_rate": null}],
  "memory_details": {"maximum": 1048576, "used": 1048576}
}
```

`topology.json` (api-ref 예시):
```json
{
  "nodes": [
    {"cpu_pinning": {"0": 0, "1": 5}, "host_node": 0, "memory_mb": 1024, "siblings": [[0, 1]], "vcpu_set": [0, 1]},
    {"cpu_pinning": {"2": 1, "3": 8}, "host_node": 1, "memory_mb": 2048, "siblings": [[2, 3]], "vcpu_set": [2, 3]}
  ],
  "pagesize_kb": 4
}
```

- [ ] **Step 2: 실패하는 테스트 작성**

`ServerSubResourceTests.java`:
```java
package org.openstack4j.api.compute.microversion;

import java.util.Arrays;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Addresses;
import org.openstack4j.model.compute.RemoteConsole;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerDiagnosticsStandard;
import org.openstack4j.model.compute.ServerTopology;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

@Test(suiteName = "Compute/ServerSubResources")
public class ServerSubResourceTests extends AbstractComputeMicroVersionTest {

    public void topology() throws Exception {
        negotiate("2.100");
        respondWith("/compute/microversion/topology.json");

        ServerTopology topology = osv3().compute().servers().topology(SERVER);

        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/servers/" + SERVER + "/topology"));
        assertVersionHeaders(request, "2.100");
        Assert.assertEquals(topology.getPagesizeKb(), Integer.valueOf(4));
        Assert.assertEquals(topology.getNodes().size(), 2);
        Assert.assertEquals(topology.getNodes().get(1).getMemoryMb(), Integer.valueOf(2048));
        Assert.assertEquals(topology.getNodes().get(0).getCpuPinning().get("1"), Integer.valueOf(5));
        Assert.assertEquals(topology.getNodes().get(0).getSiblings().get(0), Arrays.asList(0, 1));
        Assert.assertEquals(topology.getNodes().get(1).getVcpuSet(), Arrays.asList(2, 3));
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.78.*")
    public void topologyNeeds278() throws Exception {
        try {
            osv3().compute().servers().topology(SERVER);
        } finally {
            assertNoMoreRequests();
        }
    }

    public void ipsAllAndByNetwork() throws Exception {
        respondWith(200, "{\"addresses\": {\"private\": [{\"version\": 4, \"addr\": \"10.0.0.5\"}], \"public\": [{\"version\": 6, \"addr\": \"2001:db8::5\"}]}}");
        respondWith(200, "{\"private\": [{\"version\": 4, \"addr\": \"10.0.0.5\"}]}");

        Addresses all = osv3().compute().servers().ips(SERVER);
        List<? extends Address> priv = osv3().compute().servers().ips(SERVER, "private");

        Assert.assertTrue(takeRequest().getPath().endsWith("/servers/" + SERVER + "/ips"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/servers/" + SERVER + "/ips/private"));
        Assert.assertEquals(all.getAddresses("public").get(0).getAddr(), "2001:db8::5");
        Assert.assertEquals(priv.size(), 1);
        Assert.assertEquals(priv.get(0).getAddr(), "10.0.0.5");
    }

    public void remoteConsole() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"remote_console\": {\"protocol\": \"vnc\", \"type\": \"novnc\", \"url\": \"http://10.0.0.1:6080/vnc_auto.html?token=t\"}}");

        RemoteConsole console = osv3().compute().servers().remoteConsole(SERVER, "vnc", "novnc");

        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/servers/" + SERVER + "/remote-consoles"));
        Assert.assertEquals(body(request).get("remote_console").get("protocol").asText(), "vnc");
        Assert.assertEquals(body(request).get("remote_console").get("type").asText(), "novnc");
        Assert.assertEquals(console.getUrl(), "http://10.0.0.1:6080/vnc_auto.html?token=t");
        Assert.assertEquals(console.getType(), "novnc");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.99.*")
    public void spiceDirectNeeds299() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.98");
        try {
            osv3().compute().servers().remoteConsole(SERVER, "spice", "spice-direct");
        } finally {
            assertNoMoreRequests();
        }
    }

    public void diagnosticsStandard() throws Exception {
        negotiate("2.100");
        respondWith("/compute/microversion/diagnostics_2_48.json");

        ServerDiagnosticsStandard d = osv3().compute().servers().diagnosticsStandard(SERVER);

        assertVersionHeaders(takeRequest(), "2.100");
        Assert.assertEquals(d.getDriver(), "libvirt");
        Assert.assertEquals(d.getUptime(), Long.valueOf(858833));
        Assert.assertEquals(d.getConfigDrive(), Boolean.FALSE);
        Assert.assertEquals(d.getCpuDetails().get(0).getTime(), Long.valueOf(10897320000000L));
        Assert.assertNull(d.getCpuDetails().get(0).getUtilisation());
        Assert.assertEquals(d.getDiskDetails().get(0).getErrorsCount(), Long.valueOf(-1));
        Assert.assertEquals(d.getNicDetails().get(0).getMacAddress(), "fa:16:3e:ee:08:6f");
        Assert.assertEquals(d.getMemoryDetails().getMaximum(), Long.valueOf(1048576));
    }

    public void attachVolumeAsync() throws Exception {
        negotiate("2.104");
        respondWith(202);

        Assert.assertTrue(osv3().compute().servers().attachVolumeAsync(SERVER, "v1", null).isSuccess());

        RecordedRequest request = takeRequest();
        assertVersionHeaders(request, "2.104");
        Assert.assertTrue(request.getPath().endsWith("/servers/" + SERVER + "/os-volume_attachments"));
        Assert.assertEquals(body(request).get("volumeAttachment").get("volumeId").asText(), "v1");
        Assert.assertFalse(body(request).get("volumeAttachment").has("device"));
    }

    public void unpinAvailabilityZoneSendsNull() throws Exception {
        negotiate("2.104");
        respondWith("/compute/microversion/server_2_100.json");

        Server server = osv3().compute().servers().updatePinnedAvailabilityZone(SERVER, null);

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertTrue(body(request).get("server").has("pinned_availability_zone"));
        Assert.assertTrue(body(request).get("server").get("pinned_availability_zone").isNull());
        Assert.assertEquals(server.getId(), SERVER);
    }
}
```

- [ ] **Step 3: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -E 'cannot find symbol' | head -3`
Expected: `ServerTopology` 등 없음.

- [ ] **Step 4: 모델 구현**

`model/compute/ServerTopology.java`:
```java
package org.openstack4j.model.compute;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** NUMA topology of a server (2.78+). Admin-only fields are {@code null} for other users. */
public interface ServerTopology extends ModelEntity {

    List<? extends Node> getNodes();

    Integer getPagesizeKb();

    interface Node extends ModelEntity {
        Map<String, Integer> getCpuPinning();
        Integer getHostNode();
        Integer getMemoryMb();
        List<List<Integer>> getSiblings();
        List<Integer> getVcpuSet();
    }
}
```

`openstack/compute/domain/NovaServerTopology.java`:
```java
package org.openstack4j.openstack.compute.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.compute.ServerTopology;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaServerTopology implements ServerTopology {

    private static final long serialVersionUID = 1L;

    @JsonProperty("nodes")
    private List<NovaNode> nodes;
    @JsonProperty("pagesize_kb")
    private Integer pagesizeKb;

    @Override
    public List<NovaNode> getNodes() { return nodes; }

    @Override
    public Integer getPagesizeKb() { return pagesizeKb; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NovaNode implements Node {
        private static final long serialVersionUID = 1L;
        @JsonProperty("cpu_pinning")
        private Map<String, Integer> cpuPinning;
        @JsonProperty("host_node")
        private Integer hostNode;
        @JsonProperty("memory_mb")
        private Integer memoryMb;
        @JsonProperty("siblings")
        private List<List<Integer>> siblings;
        @JsonProperty("vcpu_set")
        private List<Integer> vcpuSet;

        @Override public Map<String, Integer> getCpuPinning() { return cpuPinning; }
        @Override public Integer getHostNode() { return hostNode; }
        @Override public Integer getMemoryMb() { return memoryMb; }
        @Override public List<List<Integer>> getSiblings() { return siblings; }
        @Override public List<Integer> getVcpuSet() { return vcpuSet; }
    }
}
```

`model/compute/RemoteConsole.java`:
```java
package org.openstack4j.model.compute;

import org.openstack4j.model.ModelEntity;

/** A remote console URL (2.6+). */
public interface RemoteConsole extends ModelEntity {
    String getProtocol();
    String getType();
    String getUrl();
}
```

`openstack/compute/domain/NovaRemoteConsole.java`:
```java
package org.openstack4j.openstack.compute.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.compute.RemoteConsole;

@JsonRootName("remote_console")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaRemoteConsole implements RemoteConsole {

    private static final long serialVersionUID = 1L;

    private String protocol;
    private String type;
    private String url;

    public NovaRemoteConsole() {
    }

    public NovaRemoteConsole(String protocol, String type) {
        this.protocol = protocol;
        this.type = type;
    }

    @Override public String getProtocol() { return protocol; }
    @Override public String getType() { return type; }
    @Override public String getUrl() { return url; }
}
```
(요청과 응답 모두 이 클래스를 쓴다. `url` 은 요청에서 `null` 이라 `NON_NULL` 로 빠진다.)

`model/compute/ServerDiagnosticsStandard.java`:
```java
package org.openstack4j.model.compute;

import java.util.List;

import org.openstack4j.model.ModelEntity;

/** Standardised server diagnostics (2.48+). */
public interface ServerDiagnosticsStandard extends ModelEntity {

    String getState();
    String getDriver();
    String getHypervisor();
    String getHypervisorOs();
    Long getUptime();
    Boolean getConfigDrive();
    Integer getNumCpus();
    Integer getNumNics();
    Integer getNumDisks();
    List<? extends CpuDetail> getCpuDetails();
    List<? extends DiskDetail> getDiskDetails();
    List<? extends NicDetail> getNicDetails();
    MemoryDetails getMemoryDetails();

    interface CpuDetail extends ModelEntity {
        Integer getId();
        Long getTime();
        Integer getUtilisation();
    }

    interface DiskDetail extends ModelEntity {
        Long getReadBytes();
        Long getReadRequests();
        Long getWriteBytes();
        Long getWriteRequests();
        Long getErrorsCount();
    }

    interface NicDetail extends ModelEntity {
        String getMacAddress();
        Long getRxOctets();
        Long getRxErrors();
        Long getRxDrop();
        Long getRxPackets();
        Long getRxRate();
        Long getTxOctets();
        Long getTxErrors();
        Long getTxDrop();
        Long getTxPackets();
        Long getTxRate();
    }

    interface MemoryDetails extends ModelEntity {
        Long getMaximum();
        Long getUsed();
    }
}
```

`openstack/compute/domain/NovaServerDiagnosticsStandard.java`:
```java
package org.openstack4j.openstack.compute.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.compute.ServerDiagnosticsStandard;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaServerDiagnosticsStandard implements ServerDiagnosticsStandard {

    private static final long serialVersionUID = 1L;

    private String state;
    private String driver;
    private String hypervisor;
    @JsonProperty("hypervisor_os")
    private String hypervisorOs;
    private Long uptime;
    @JsonProperty("config_drive")
    private Boolean configDrive;
    @JsonProperty("num_cpus")
    private Integer numCpus;
    @JsonProperty("num_nics")
    private Integer numNics;
    @JsonProperty("num_disks")
    private Integer numDisks;
    @JsonProperty("cpu_details")
    private List<Cpu> cpuDetails;
    @JsonProperty("disk_details")
    private List<Disk> diskDetails;
    @JsonProperty("nic_details")
    private List<Nic> nicDetails;
    @JsonProperty("memory_details")
    private Memory memoryDetails;

    @Override public String getState() { return state; }
    @Override public String getDriver() { return driver; }
    @Override public String getHypervisor() { return hypervisor; }
    @Override public String getHypervisorOs() { return hypervisorOs; }
    @Override public Long getUptime() { return uptime; }
    @Override public Boolean getConfigDrive() { return configDrive; }
    @Override public Integer getNumCpus() { return numCpus; }
    @Override public Integer getNumNics() { return numNics; }
    @Override public Integer getNumDisks() { return numDisks; }
    @Override public List<Cpu> getCpuDetails() { return cpuDetails; }
    @Override public List<Disk> getDiskDetails() { return diskDetails; }
    @Override public List<Nic> getNicDetails() { return nicDetails; }
    @Override public Memory getMemoryDetails() { return memoryDetails; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cpu implements CpuDetail {
        private static final long serialVersionUID = 1L;
        private Integer id;
        private Long time;
        private Integer utilisation;
        @Override public Integer getId() { return id; }
        @Override public Long getTime() { return time; }
        @Override public Integer getUtilisation() { return utilisation; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Disk implements DiskDetail {
        private static final long serialVersionUID = 1L;
        @JsonProperty("read_bytes") private Long readBytes;
        @JsonProperty("read_requests") private Long readRequests;
        @JsonProperty("write_bytes") private Long writeBytes;
        @JsonProperty("write_requests") private Long writeRequests;
        @JsonProperty("errors_count") private Long errorsCount;
        @Override public Long getReadBytes() { return readBytes; }
        @Override public Long getReadRequests() { return readRequests; }
        @Override public Long getWriteBytes() { return writeBytes; }
        @Override public Long getWriteRequests() { return writeRequests; }
        @Override public Long getErrorsCount() { return errorsCount; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Nic implements NicDetail {
        private static final long serialVersionUID = 1L;
        @JsonProperty("mac_address") private String macAddress;
        @JsonProperty("rx_octets") private Long rxOctets;
        @JsonProperty("rx_errors") private Long rxErrors;
        @JsonProperty("rx_drop") private Long rxDrop;
        @JsonProperty("rx_packets") private Long rxPackets;
        @JsonProperty("rx_rate") private Long rxRate;
        @JsonProperty("tx_octets") private Long txOctets;
        @JsonProperty("tx_errors") private Long txErrors;
        @JsonProperty("tx_drop") private Long txDrop;
        @JsonProperty("tx_packets") private Long txPackets;
        @JsonProperty("tx_rate") private Long txRate;
        @Override public String getMacAddress() { return macAddress; }
        @Override public Long getRxOctets() { return rxOctets; }
        @Override public Long getRxErrors() { return rxErrors; }
        @Override public Long getRxDrop() { return rxDrop; }
        @Override public Long getRxPackets() { return rxPackets; }
        @Override public Long getRxRate() { return rxRate; }
        @Override public Long getTxOctets() { return txOctets; }
        @Override public Long getTxErrors() { return txErrors; }
        @Override public Long getTxDrop() { return txDrop; }
        @Override public Long getTxPackets() { return txPackets; }
        @Override public Long getTxRate() { return txRate; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Memory implements MemoryDetails {
        private static final long serialVersionUID = 1L;
        private Long maximum;
        private Long used;
        @Override public Long getMaximum() { return maximum; }
        @Override public Long getUsed() { return used; }
    }
}
```

`openstack/compute/domain/NovaNetworkIps.java`:
```java
package org.openstack4j.openstack.compute.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.openstack4j.openstack.compute.domain.NovaAddresses.NovaAddress;

/** {@code GET /servers/{id}/ips/{label}}: {@code {"<label>": [addresses]}}. */
public class NovaNetworkIps implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, List<NovaAddress>> byLabel = new HashMap<>();

    @JsonAnySetter
    void put(String label, List<NovaAddress> addresses) {
        byLabel.put(label, addresses);
    }

    public List<NovaAddress> get(String label) {
        List<NovaAddress> list = byLabel.get(label);
        return list == null ? Collections.emptyList() : list;
    }
}
```

- [ ] **Step 5: 서비스 구현**

`ServerService.java` 에 Interfaces 의 7개 메서드 선언(Javadoc 에 하한·엔드포인트).

`ServerServiceImpl.java`:
```java
    @Override
    public ServerTopology topology(String serverId) {
        Objects.requireNonNull(serverId);
        requireMicroVersion("Server topology", V(78));
        return get(NovaServerTopology.class, uri("/servers/%s/topology", serverId)).execute();
    }

    @Override
    public Addresses ips(String serverId) {
        Objects.requireNonNull(serverId);
        return get(NovaAddresses.class, uri("/servers/%s/ips", serverId)).execute();
    }

    @Override
    public List<? extends Address> ips(String serverId, String networkLabel) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(networkLabel);
        NovaNetworkIps ips = get(NovaNetworkIps.class, uri("/servers/%s/ips/%s", serverId, networkLabel)).execute();
        return ips == null ? Collections.emptyList() : ips.get(networkLabel);
    }

    @Override
    public RemoteConsole remoteConsole(String serverId, String protocol, String type) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(protocol);
        Objects.requireNonNull(type);
        requireMicroVersion("Remote consoles", V(6));
        if ("mks".equals(protocol))
            requireMicroVersion("MKS remote console", V(8));
        if ("spice-direct".equals(type))
            requireMicroVersion("spice-direct remote console", V(99));
        return post(NovaRemoteConsole.class, uri("/servers/%s/remote-consoles", serverId))
                .entity(new NovaRemoteConsole(protocol, type))
                .execute();
    }

    @Override
    public ServerDiagnosticsStandard diagnosticsStandard(String serverId) {
        Objects.requireNonNull(serverId);
        requireMicroVersion("Standard server diagnostics", V(48));
        return get(NovaServerDiagnosticsStandard.class, uri("/servers/%s/diagnostics", serverId)).execute();
    }

    @Override
    public ActionResponse attachVolumeAsync(String serverId, String volumeId, String device) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(volumeId);
        requireMicroVersion("Asynchronous volume attach", V(101));
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("volumeId", volumeId);
        if (device != null)
            attachment.put("device", device);
        return ToActionResponseFunction.INSTANCE.apply(
                post(Void.class, uri("/servers/%s/os-volume_attachments", serverId))
                        .entity(JsonBody.of("volumeAttachment", attachment))
                        .executeWithResponse());
    }

    @Override
    public Server updatePinnedAvailabilityZone(String serverId, String availabilityZone) {
        Objects.requireNonNull(serverId);
        requireMicroVersion("Updating pinned_availability_zone", V(104));
        Map<String, Object> server = new HashMap<>();
        server.put("pinned_availability_zone", availabilityZone);
        return put(NovaServer.class, uri("/servers/%s", serverId))
                .entity(JsonBody.of("server", server))
                .execute();
    }
```
(`NovaAddresses` 는 루트 이름이 없고 `addresses` 프로퍼티를 가진다. `NovaNetworkIps` 역직렬화는 루트 없는 mapper 를 쓴다. `JsonBody` 는 Task 5 에서 만든 것을 쓴다.)

- [ ] **Step 6: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ServerSubResourceTests,ServerTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Failures: 0, Errors: 0` (ServerSubResourceTests 8개).

- [ ] **Step 7: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(compute): add server topology, ips, remote consoles, standard diagnostics, async volume attach and AZ pinning

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
.superpowers/pr.sh "servers(): topology(2.78), ips, remoteConsole(2.6/2.8/2.99), diagnosticsStandard(2.48), attachVolumeAsync(2.101), updatePinnedAvailabilityZone(2.104)."
```

---

### Task 7: 서버 마이그레이션과 os-migrations 필터

**Files:**
- Create: `core/src/main/java/org/openstack4j/model/compute/ServerMigration.java`, `core/src/main/java/org/openstack4j/openstack/compute/domain/NovaServerMigration.java`
- Create: `core/src/main/java/org/openstack4j/model/compute/ext/MigrationListOptions.java`
- Modify: `api/compute/ServerService.java`, `openstack/compute/internal/ServerServiceImpl.java`, `api/compute/ext/MigrationService.java`, `openstack/compute/internal/ext/MigrationServiceImpl.java`
- Create: `core-test/src/main/java/org/openstack4j/api/compute/microversion/MigrationMicroVersionTests.java`

**Interfaces:**
- Consumes: Task 2 `requireMicroVersion`, `V`; Task 5 `JsonBody`.
- Produces:
  - `ServerService`: `List<? extends ServerMigration> migrations(String serverId)`(2.23), `ServerMigration migration(String serverId, String migrationId)`(2.23), `ActionResponse forceCompleteMigration(String serverId, String migrationId)`(2.22), `ActionResponse abortMigration(String serverId, String migrationId)`(2.24)
  - `ServerMigration`: `String getId()`, `String getUuid()`(2.59), `String getServerUuid()`, `String getStatus()`, `String getSourceCompute()`, `String getSourceNode()`, `String getDestCompute()`, `String getDestNode()`, `String getDestHost()`, `Long getMemoryTotalBytes()`, `Long getMemoryProcessedBytes()`, `Long getMemoryRemainingBytes()`, `Long getDiskTotalBytes()`, `Long getDiskProcessedBytes()`, `Long getDiskRemainingBytes()`, `Date getCreatedAt()`, `Date getUpdatedAt()`, `String getUserId()`(2.80), `String getProjectId()`(2.80)
  - `MigrationService.list(MigrationListOptions options)`; `MigrationListOptions`: `host`, `status`, `instanceUuid`, `sourceCompute`, `migrationType`(2.23), `hidden`, `changesSince`(2.59), `limit`(2.59), `marker`(2.59), `changesBefore`(2.66), `userId`(2.80), `projectId`(2.80); `toQueryParams()`, `getRequiredMicroVersion()`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/c7-migrations
```

`MigrationMicroVersionTests.java`:
```java
package org.openstack4j.api.compute.microversion;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.ServerMigration;
import org.openstack4j.model.compute.ext.MigrationListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/MigrationMicroVersion")
public class MigrationMicroVersionTests extends AbstractComputeMicroVersionTest {

    private static final String MIGRATION = "{\"id\": 4, \"uuid\": \"12341d4b-346a-40d0-83c6-5f4f6892b650\", \"server_uuid\": \"" + SERVER + "\","
            + " \"status\": \"running\", \"source_compute\": \"compute1\", \"source_node\": \"node1\", \"dest_compute\": \"compute2\","
            + " \"dest_node\": \"node2\", \"dest_host\": \"1.2.3.4\", \"memory_total_bytes\": 123456, \"memory_processed_bytes\": 12345,"
            + " \"memory_remaining_bytes\": 111111, \"disk_total_bytes\": 234567, \"disk_processed_bytes\": 23456,"
            + " \"disk_remaining_bytes\": 211111, \"created_at\": \"2016-01-29T13:42:02.000000\", \"updated_at\": \"2016-01-29T13:42:02.000000\","
            + " \"user_id\": \"8dbaa0f0-ab95-4ffe-8cb4-9c89d2ac9d24\", \"project_id\": \"5f705771-3aa9-4f4c-8660-0d9522ffdbea\"}";

    public void listAndGetServerMigrations() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"migrations\": [" + MIGRATION + "]}");
        respondWith(200, "{\"migration\": " + MIGRATION + "}");

        List<? extends ServerMigration> list = osv3().compute().servers().migrations(SERVER);
        ServerMigration one = osv3().compute().servers().migration(SERVER, "4");

        Assert.assertTrue(takeRequest().getPath().endsWith("/servers/" + SERVER + "/migrations"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/servers/" + SERVER + "/migrations/4"));
        Assert.assertEquals(list.get(0).getId(), "4");
        Assert.assertEquals(list.get(0).getUuid(), "12341d4b-346a-40d0-83c6-5f4f6892b650");
        Assert.assertEquals(one.getMemoryRemainingBytes(), Long.valueOf(111111));
        Assert.assertEquals(one.getProjectId(), "5f705771-3aa9-4f4c-8660-0d9522ffdbea");
        Assert.assertNotNull(one.getCreatedAt());
    }

    public void forceCompleteAndAbort() throws Exception {
        negotiate("2.100");
        respondWith(202);
        respondWith(202);

        Assert.assertTrue(osv3().compute().servers().forceCompleteMigration(SERVER, "4").isSuccess());
        Assert.assertTrue(osv3().compute().servers().abortMigration(SERVER, "4").isSuccess());

        RecordedRequest force = takeRequest();
        Assert.assertTrue(force.getPath().endsWith("/servers/" + SERVER + "/migrations/4/action"));
        Assert.assertTrue(body(force).has("force_complete"));
        Assert.assertTrue(body(force).get("force_complete").isNull());
        RecordedRequest abort = takeRequest();
        Assert.assertEquals(abort.getMethod(), "DELETE");
        Assert.assertTrue(abort.getPath().endsWith("/servers/" + SERVER + "/migrations/4"));
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.24.*")
    public void abortNeeds224() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.23");
        try {
            osv3().compute().servers().abortMigration(SERVER, "4");
        } finally {
            assertNoMoreRequests();
        }
    }

    public void osMigrationsWithOptions() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"migrations\": []}");

        osv3().compute().migrations().list(MigrationListOptions.create().host("compute1").limit(10).marker("m-1").userId("u1").projectId("p1").migrationType("live-migration"));

        String path = takeRequest().getPath();
        Assert.assertTrue(path.contains("/os-migrations?"), path);
        for (String part : new String[] {"host=compute1", "limit=10", "marker=m-1", "user_id=u1", "project_id=p1", "migration_type=live-migration"})
            Assert.assertTrue(path.contains(part), path + " lacks " + part);
        Assert.assertEquals(MigrationListOptions.create().userId("u").getRequiredMicroVersion(), "2.80");
        Assert.assertEquals(MigrationListOptions.create().limit(1).getRequiredMicroVersion(), "2.59");
        Assert.assertNull(MigrationListOptions.create().host("h").getRequiredMicroVersion());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.59.*")
    public void osMigrationsPagingNeeds259() throws Exception {
        try {
            osv3().compute().migrations().list(MigrationListOptions.create().limit(1));
        } finally {
            assertNoMoreRequests();
        }
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -E 'cannot find symbol' | head -3`
Expected: `ServerMigration`, `MigrationListOptions` 없음.

- [ ] **Step 3: 모델 구현**

`model/compute/ServerMigration.java`:
```java
package org.openstack4j.model.compute;

import java.util.Date;

import org.openstack4j.model.ModelEntity;

/** An in-progress live migration of a server ({@code /servers/{id}/migrations}, 2.23+). */
public interface ServerMigration extends ModelEntity {
    String getId();
    /** 2.59+ */
    String getUuid();
    String getServerUuid();
    String getStatus();
    String getSourceCompute();
    String getSourceNode();
    String getDestCompute();
    String getDestNode();
    String getDestHost();
    Long getMemoryTotalBytes();
    Long getMemoryProcessedBytes();
    Long getMemoryRemainingBytes();
    Long getDiskTotalBytes();
    Long getDiskProcessedBytes();
    Long getDiskRemainingBytes();
    Date getCreatedAt();
    Date getUpdatedAt();
    /** 2.80+ */
    String getUserId();
    /** 2.80+ */
    String getProjectId();
}
```

`openstack/compute/domain/NovaServerMigration.java`:
```java
package org.openstack4j.openstack.compute.domain;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.compute.ServerMigration;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("migration")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaServerMigration implements ServerMigration {

    private static final long serialVersionUID = 1L;

    private String id;
    private String uuid;
    @JsonProperty("server_uuid") private String serverUuid;
    private String status;
    @JsonProperty("source_compute") private String sourceCompute;
    @JsonProperty("source_node") private String sourceNode;
    @JsonProperty("dest_compute") private String destCompute;
    @JsonProperty("dest_node") private String destNode;
    @JsonProperty("dest_host") private String destHost;
    @JsonProperty("memory_total_bytes") private Long memoryTotalBytes;
    @JsonProperty("memory_processed_bytes") private Long memoryProcessedBytes;
    @JsonProperty("memory_remaining_bytes") private Long memoryRemainingBytes;
    @JsonProperty("disk_total_bytes") private Long diskTotalBytes;
    @JsonProperty("disk_processed_bytes") private Long diskProcessedBytes;
    @JsonProperty("disk_remaining_bytes") private Long diskRemainingBytes;
    @JsonProperty("created_at") private Date createdAt;
    @JsonProperty("updated_at") private Date updatedAt;
    @JsonProperty("user_id") private String userId;
    @JsonProperty("project_id") private String projectId;

    @Override public String getId() { return id; }
    @Override public String getUuid() { return uuid; }
    @Override public String getServerUuid() { return serverUuid; }
    @Override public String getStatus() { return status; }
    @Override public String getSourceCompute() { return sourceCompute; }
    @Override public String getSourceNode() { return sourceNode; }
    @Override public String getDestCompute() { return destCompute; }
    @Override public String getDestNode() { return destNode; }
    @Override public String getDestHost() { return destHost; }
    @Override public Long getMemoryTotalBytes() { return memoryTotalBytes; }
    @Override public Long getMemoryProcessedBytes() { return memoryProcessedBytes; }
    @Override public Long getMemoryRemainingBytes() { return memoryRemainingBytes; }
    @Override public Long getDiskTotalBytes() { return diskTotalBytes; }
    @Override public Long getDiskProcessedBytes() { return diskProcessedBytes; }
    @Override public Long getDiskRemainingBytes() { return diskRemainingBytes; }
    @Override public Date getCreatedAt() { return createdAt; }
    @Override public Date getUpdatedAt() { return updatedAt; }
    @Override public String getUserId() { return userId; }
    @Override public String getProjectId() { return projectId; }

    public static class NovaServerMigrations extends ListResult<NovaServerMigration> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("migrations")
        private List<NovaServerMigration> migrations;

        @Override
        protected List<NovaServerMigration> value() {
            return migrations;
        }
    }
}
```
(`id` 는 숫자로 오지만 Jackson 이 `String` 으로 강제 변환한다. 날짜 `2016-01-29T13:42:02.000000` 형식은 기존 compute 모델(`ExtMigration.createdAt`)과 같은 방식으로 읽힌다 — 실패하면 `ExtMigration` 의 `@JsonFormat` 을 그대로 붙인다.)

`model/compute/ext/MigrationListOptions.java`:
```java
package org.openstack4j.model.compute.ext;

import java.util.LinkedHashMap;
import java.util.Map;

/** Filters for {@code GET /os-migrations}. */
public class MigrationListOptions {

    private final Map<String, String> params = new LinkedHashMap<>();
    private int requiredMinor = 0;

    public static MigrationListOptions create() {
        return new MigrationListOptions();
    }

    private MigrationListOptions put(String key, Object value, int minor) {
        params.put(key, String.valueOf(value));
        requiredMinor = Math.max(requiredMinor, minor);
        return this;
    }

    public MigrationListOptions host(String host) { return put("host", host, 0); }
    public MigrationListOptions status(String status) { return put("status", status, 0); }
    public MigrationListOptions instanceUuid(String serverId) { return put("instance_uuid", serverId, 0); }
    public MigrationListOptions sourceCompute(String host) { return put("source_compute", host, 0); }
    /** {@code evacuation}, {@code live-migration}, {@code migration} or {@code resize} (2.23+). */
    public MigrationListOptions migrationType(String type) { return put("migration_type", type, 23); }
    public MigrationListOptions hidden(boolean hidden) { return put("hidden", hidden, 0); }
    /** 2.59+ */
    public MigrationListOptions changesSince(String isoTime) { return put("changes-since", isoTime, 59); }
    /** 2.59+ */
    public MigrationListOptions limit(int limit) { return put("limit", limit, 59); }
    /** Migration uuid (2.59+). */
    public MigrationListOptions marker(String migrationUuid) { return put("marker", migrationUuid, 59); }
    /** 2.66+ */
    public MigrationListOptions changesBefore(String isoTime) { return put("changes-before", isoTime, 66); }
    /** 2.80+ */
    public MigrationListOptions userId(String userId) { return put("user_id", userId, 80); }
    /** 2.80+ */
    public MigrationListOptions projectId(String projectId) { return put("project_id", projectId, 80); }

    public Map<String, String> toQueryParams() {
        return new LinkedHashMap<>(params);
    }

    public String getRequiredMicroVersion() {
        return requiredMinor == 0 ? null : "2." + requiredMinor;
    }
}
```

- [ ] **Step 4: 서비스 구현**

`ServerService.java` 에 4개 메서드, `MigrationService.java` 에 `List<? extends Migration> list(MigrationListOptions options);` 선언.

`ServerServiceImpl.java`:
```java
    @Override
    public List<? extends ServerMigration> migrations(String serverId) {
        Objects.requireNonNull(serverId);
        requireMicroVersion("Listing server migrations", V(23));
        return get(NovaServerMigration.NovaServerMigrations.class, uri("/servers/%s/migrations", serverId)).execute().getList();
    }

    @Override
    public ServerMigration migration(String serverId, String migrationId) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(migrationId);
        requireMicroVersion("Showing a server migration", V(23));
        return get(NovaServerMigration.class, uri("/servers/%s/migrations/%s", serverId, migrationId)).execute();
    }

    @Override
    public ActionResponse forceCompleteMigration(String serverId, String migrationId) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(migrationId);
        requireMicroVersion("Force-completing a live migration", V(22));
        return ToActionResponseFunction.INSTANCE.apply(
                post(Void.class, uri("/servers/%s/migrations/%s/action", serverId, migrationId))
                        .entity(JsonBody.of(Collections.singletonMap("force_complete", null)))
                        .executeWithResponse());
    }

    @Override
    public ActionResponse abortMigration(String serverId, String migrationId) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(migrationId);
        requireMicroVersion("Aborting a live migration", V(24));
        return ToActionResponseFunction.INSTANCE.apply(delete(Void.class, uri("/servers/%s/migrations/%s", serverId, migrationId)).executeWithResponse());
    }
```
(삭제는 기존 `delete(String serverId)` 와 같은 `ToActionResponseFunction` 방식이다.)

`MigrationServiceImpl.java`:
```java
    @Override
    public List<? extends Migration> list(MigrationListOptions options) {
        Objects.requireNonNull(options);
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Migration list filters " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
        return get(Migrations.class, uri("/os-migrations")).params(options.toQueryParams()).execute().getList();
    }
```

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='MigrationMicroVersionTests,MigrationTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Failures: 0, Errors: 0` (MigrationMicroVersionTests 5개, 기존 MigrationTests 통과).

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(compute): add server migrations and typed os-migrations filters

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
.superpowers/pr.sh "servers(): migrations/migration(2.23), forceCompleteMigration(2.22), abortMigration(2.24); migrations().list(MigrationListOptions)(2.23/2.59/2.66/2.80)."
```

---

### Task 8: 서버 shares (2.97)

**Files:**
- Create: `core/src/main/java/org/openstack4j/model/compute/ServerShare.java`, `core/src/main/java/org/openstack4j/openstack/compute/domain/NovaServerShare.java`
- Modify: `api/compute/ServerService.java`, `openstack/compute/internal/ServerServiceImpl.java`
- Create: `core-test/src/main/java/org/openstack4j/api/compute/microversion/ServerShareTests.java`

**Interfaces:**
- Consumes: Task 2 `requireMicroVersion`, `V`; Task 5 `JsonBody`.
- Produces:
  - `ServerService`: `List<? extends ServerShare> shares(String serverId)`, `ServerShare share(String serverId, String shareId)`, `ServerShare attachShare(String serverId, String shareId, String tag)`(`tag` nullable), `ActionResponse detachShare(String serverId, String shareId)` — 모두 2.97
  - `ServerShare`: `String getShareId()`, `String getStatus()`, `String getTag()`, `String getExportLocation()`(관리자), `String getUuid()`(관리자)

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/c8-server-shares
```

`ServerShareTests.java`:
```java
package org.openstack4j.api.compute.microversion;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.ServerShare;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/ServerShares")
public class ServerShareTests extends AbstractComputeMicroVersionTest {

    private static final String SHARE = "{\"share_id\": \"e8debdc0-447a-4376-a10a-4cd9122d7986\", \"status\": \"active\", \"tag\": \"e8debdc0-447a-4376-a10a-4cd9122d7986\","
            + " \"export_location\": \"10.0.0.50:/mnt/foo\", \"uuid\": \"68ba1762-fd6d-4221-8311-f3193dd93404\"}";

    public void listGetAttachDetach() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"shares\": [" + SHARE + "]}");
        respondWith(200, "{\"share\": " + SHARE + "}");
        respondWith(201, "{\"share\": {\"share_id\": \"s2\", \"status\": \"attaching\", \"tag\": \"data\"}}");
        respondWith(202);

        List<? extends ServerShare> shares = osv3().compute().servers().shares(SERVER);
        ServerShare one = osv3().compute().servers().share(SERVER, "e8debdc0-447a-4376-a10a-4cd9122d7986");
        ServerShare attached = osv3().compute().servers().attachShare(SERVER, "s2", "data");
        boolean detached = osv3().compute().servers().detachShare(SERVER, "s2").isSuccess();

        Assert.assertTrue(takeRequest().getPath().endsWith("/servers/" + SERVER + "/shares"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/servers/" + SERVER + "/shares/e8debdc0-447a-4376-a10a-4cd9122d7986"));
        RecordedRequest attach = takeRequest();
        Assert.assertEquals(attach.getMethod(), "POST");
        Assert.assertEquals(body(attach).get("share").get("share_id").asText(), "s2");
        Assert.assertEquals(body(attach).get("share").get("tag").asText(), "data");
        RecordedRequest detach = takeRequest();
        Assert.assertEquals(detach.getMethod(), "DELETE");
        Assert.assertTrue(detach.getPath().endsWith("/servers/" + SERVER + "/shares/s2"));

        Assert.assertEquals(shares.get(0).getExportLocation(), "10.0.0.50:/mnt/foo");
        Assert.assertEquals(one.getUuid(), "68ba1762-fd6d-4221-8311-f3193dd93404");
        Assert.assertEquals(attached.getStatus(), "attaching");
        Assert.assertTrue(detached);
    }

    public void attachWithoutTagOmitsIt() throws Exception {
        negotiate("2.100");
        respondWith(201, "{\"share\": {\"share_id\": \"s2\", \"status\": \"attaching\", \"tag\": \"s2\"}}");
        osv3().compute().servers().attachShare(SERVER, "s2", null);
        Assert.assertFalse(body(takeRequest()).get("share").has("tag"));
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.97.*")
    public void sharesNeed297() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.96");
        try {
            osv3().compute().servers().shares(SERVER);
        } finally {
            assertNoMoreRequests();
        }
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -E 'cannot find symbol' | head -3`
Expected: `ServerShare` 없음.

- [ ] **Step 3: 구현**

`model/compute/ServerShare.java`:
```java
package org.openstack4j.model.compute;

import org.openstack4j.model.ModelEntity;

/** A Manila share attached to a server (2.97+). */
public interface ServerShare extends ModelEntity {
    String getShareId();
    String getStatus();
    String getTag();
    /** Admin only. */
    String getExportLocation();
    /** Admin only. */
    String getUuid();
}
```

`openstack/compute/domain/NovaServerShare.java`:
```java
package org.openstack4j.openstack.compute.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.compute.ServerShare;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("share")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaServerShare implements ServerShare {

    private static final long serialVersionUID = 1L;

    @JsonProperty("share_id") private String shareId;
    private String status;
    private String tag;
    @JsonProperty("export_location") private String exportLocation;
    private String uuid;

    @Override public String getShareId() { return shareId; }
    @Override public String getStatus() { return status; }
    @Override public String getTag() { return tag; }
    @Override public String getExportLocation() { return exportLocation; }
    @Override public String getUuid() { return uuid; }

    public static class NovaServerShares extends ListResult<NovaServerShare> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("shares")
        private List<NovaServerShare> shares;

        @Override
        protected List<NovaServerShare> value() {
            return shares;
        }
    }
}
```

`ServerService.java` 에 4개 메서드 선언, `ServerServiceImpl.java`:
```java
    @Override
    public List<? extends ServerShare> shares(String serverId) {
        Objects.requireNonNull(serverId);
        requireMicroVersion("Server shares", V(97));
        return get(NovaServerShare.NovaServerShares.class, uri("/servers/%s/shares", serverId)).execute().getList();
    }

    @Override
    public ServerShare share(String serverId, String shareId) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(shareId);
        requireMicroVersion("Server shares", V(97));
        return get(NovaServerShare.class, uri("/servers/%s/shares/%s", serverId, shareId)).execute();
    }

    @Override
    public ServerShare attachShare(String serverId, String shareId, String tag) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(shareId);
        requireMicroVersion("Server shares", V(97));
        Map<String, Object> share = new LinkedHashMap<>();
        share.put("share_id", shareId);
        if (tag != null)
            share.put("tag", tag);
        return post(NovaServerShare.class, uri("/servers/%s/shares", serverId)).entity(JsonBody.of("share", share)).execute();
    }

    @Override
    public ActionResponse detachShare(String serverId, String shareId) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(shareId);
        requireMicroVersion("Server shares", V(97));
        return ToActionResponseFunction.INSTANCE.apply(delete(Void.class, uri("/servers/%s/shares/%s", serverId, shareId)).executeWithResponse());
    }
```

- [ ] **Step 4: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ServerShareTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(compute): add server shares (2.97)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
.superpowers/pr.sh "servers(): shares/share/attachShare/detachShare (2.97)."
```

---

### Task 9: hypervisors·services·aggregates·quota-sets·keypairs·server-groups 보강

**Files:**
- Create (`core/src/main/java/org/openstack4j/model/compute/ext/`): `HypervisorListOptions.java`, `ServiceUpdate.java`
- Create: `core/src/main/java/org/openstack4j/model/compute/KeypairListOptions.java`
- Modify: `api/compute/ext/HypervisorService.java`, `api/compute/ext/ServicesService.java`, `api/compute/HostAggregateService.java`, `api/compute/QuotaSetService.java`, `api/compute/KeypairService.java`, `api/compute/ServerGroupService.java` 와 각 구현 클래스
- Modify: `model/compute/Keypair.java`, `openstack/compute/domain/NovaKeypair.java` (`type`), `model/compute/ServerGroup.java`, `openstack/compute/domain/NovaServerGroup.java` (`policy`, `rules`, `project_id`, `user_id`)
- Create: `core-test/src/main/java/org/openstack4j/api/compute/microversion/ComputeServiceExtensionsTests.java`

**Interfaces:**
- Consumes: Task 2 `capped`, `requireMicroVersion`, `effectiveMicroVersion`, `V`; Task 5 `JsonBody`.
- Produces:
  - `HypervisorService.list(HypervisorListOptions)` → `GET /os-hypervisors/detail`; options: `hypervisorHostnamePattern`(2.53), `withServers`(2.53), `limit`(2.33), `marker`(2.33)
  - `ServicesService`: `Service update(String serviceId, ServiceUpdate update)`(2.53, `PUT /os-services/{id}`), `ActionResponse delete(String serviceId)`(`DELETE /os-services/{id}`), `ExtService disableWithReason(String binary, String host, String reason)`(상한 2.52, `PUT /os-services/disable-log-reason`); `ServiceUpdate`: `enable()`, `disable(String reason)`, `forcedDown(boolean)`
  - `HostAggregateService.cacheImages(String aggregateId, List<String> imageIds)`(2.81, `POST /os-aggregates/{id}/images`) → `ActionResponse`
  - `QuotaSetService.defaults(String tenantId)` → `QuotaSet` (`GET /os-quota-sets/{id}/defaults`)
  - `KeypairService`: `list(KeypairListOptions)`(`userId` 2.10, `limit`/`marker` 2.35), `create(String name, String publicKey, String type)`(2.2, publicKey 가 null 이면 기존처럼 상한 2.91)
  - `Keypair.getType()`(default null), `ServerGroup.getPolicy()`, `getRules()`, `getProjectId()`, `getUserId()`(default null)
  - `ServerGroupService.create(String name, String policy, Map<String, Object> rules)`(2.64, `rules` nullable)

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/c9-service-extensions
```

`ComputeServiceExtensionsTests.java`:
```java
package org.openstack4j.api.compute.microversion;

import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.compute.KeypairListOptions;
import org.openstack4j.model.compute.QuotaSet;
import org.openstack4j.model.compute.ServerGroup;
import org.openstack4j.model.compute.ext.HypervisorListOptions;
import org.openstack4j.model.compute.ext.Service;
import org.openstack4j.model.compute.ext.ServiceUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/ServiceExtensions")
public class ComputeServiceExtensionsTests extends AbstractComputeMicroVersionTest {

    private static final String SERVICE_ID = "e81d66a4-ddd3-4aba-8a84-171d1cb4d339";

    public void hypervisorListOptions() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"hypervisors\": [{\"id\": \"c48f6247-abe4-4a24-824e-ea39e108874f\", \"hypervisor_hostname\": \"compute-1\", \"state\": \"up\", \"status\": \"enabled\"}]}");

        osv3().compute().hypervisors().list(HypervisorListOptions.create().hypervisorHostnamePattern("compute").withServers(true).limit(5));

        String path = takeRequest().getPath();
        Assert.assertTrue(path.contains("/os-hypervisors/detail?"), path);
        Assert.assertTrue(path.contains("hypervisor_hostname_pattern=compute"), path);
        Assert.assertTrue(path.contains("with_servers=true"), path);
        Assert.assertTrue(path.contains("limit=5"), path);
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.53.*")
    public void hypervisorPatternNeeds253() throws Exception {
        try {
            osv3().compute().hypervisors().list(HypervisorListOptions.create().hypervisorHostnamePattern("c"));
        } finally {
            assertNoMoreRequests();
        }
    }

    public void serviceUpdateDeleteAndDisableWithReason() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"service\": {\"id\": \"" + SERVICE_ID + "\", \"binary\": \"nova-compute\", \"host\": \"compute-1\", \"status\": \"disabled\", \"disabled_reason\": \"maint\", \"forced_down\": false}}");
        respondWith(204);
        respondWith(200, "{\"service\": {\"binary\": \"nova-compute\", \"host\": \"compute-1\", \"status\": \"disabled\", \"disabled_reason\": \"maint\"}}");

        Service updated = osv3().compute().services().update(SERVICE_ID, ServiceUpdate.create().disable("maint"));
        boolean deleted = osv3().compute().services().delete(SERVICE_ID).isSuccess();
        osv3().compute().services().disableWithReason("nova-compute", "compute-1", "maint");

        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PUT");
        Assert.assertTrue(update.getPath().endsWith("/os-services/" + SERVICE_ID));
        Assert.assertEquals(body(update).get("status").asText(), "disabled");
        Assert.assertEquals(body(update).get("disabled_reason").asText(), "maint");
        Assert.assertFalse(body(update).has("forced_down"));
        RecordedRequest delete = takeRequest();
        Assert.assertEquals(delete.getMethod(), "DELETE");
        RecordedRequest reason = takeRequest();
        Assert.assertTrue(reason.getPath().endsWith("/os-services/disable-log-reason"));
        assertVersionHeaders(reason, "2.52");
        Assert.assertEquals(body(reason).get("disabled_reason").asText(), "maint");
        Assert.assertEquals(updated.getDisabledReason(), "maint");
        Assert.assertTrue(deleted);
    }

    public void aggregateCacheImagesAndQuotaDefaults() throws Exception {
        negotiate("2.100");
        respondWith(202);
        respondWith(200, "{\"quota_set\": {\"id\": \"p1\", \"cores\": 20, \"instances\": 10, \"ram\": 51200}}");

        Assert.assertTrue(osv3().compute().hostAggregates().cacheImages("1", Arrays.asList("img-1", "img-2")).isSuccess());
        QuotaSet defaults = osv3().compute().quotaSets().defaults("p1");

        RecordedRequest cache = takeRequest();
        Assert.assertTrue(cache.getPath().endsWith("/os-aggregates/1/images"));
        JsonNode list = body(cache).get("cache");
        Assert.assertEquals(list.get(1).get("id").asText(), "img-2");
        Assert.assertTrue(takeRequest().getPath().endsWith("/os-quota-sets/p1/defaults"));
        Assert.assertEquals(defaults.getCores(), 20);
    }

    public void keypairListOptionsAndTypedCreate() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"keypairs\": [{\"keypair\": {\"name\": \"k1\", \"type\": \"ssh\", \"public_key\": \"ssh-rsa A\", \"fingerprint\": \"f\"}}]}");
        respondWith(201, "{\"keypair\": {\"name\": \"x\", \"type\": \"x509\", \"public_key\": \"-----BEGIN CERTIFICATE-----\", \"fingerprint\": \"f\", \"user_id\": \"u1\"}}");

        osv3().compute().keypairs().list(KeypairListOptions.create().userId("u1").limit(10).marker("k0"));
        Keypair created = osv3().compute().keypairs().create("x", "-----BEGIN CERTIFICATE-----", "x509");

        String path = takeRequest().getPath();
        Assert.assertTrue(path.contains("user_id=u1") && path.contains("limit=10") && path.contains("marker=k0"), path);
        RecordedRequest create = takeRequest();
        Assert.assertEquals(body(create).get("keypair").get("type").asText(), "x509");
        assertVersionHeaders(create, "2.100");
        Assert.assertEquals(created.getType(), "x509");
    }

    public void serverGroupWithPolicyAndRules() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"server_group\": {\"id\": \"g1\", \"name\": \"web\", \"policy\": \"anti-affinity\", \"rules\": {\"max_server_per_host\": 3},"
                + " \"members\": [], \"project_id\": \"p1\", \"user_id\": \"u1\"}}");

        ServerGroup group = osv3().compute().serverGroups().create("web", "anti-affinity", Collections.singletonMap("max_server_per_host", 3));

        JsonNode sent = body(takeRequest()).get("server_group");
        Assert.assertEquals(sent.get("policy").asText(), "anti-affinity");
        Assert.assertEquals(sent.get("rules").get("max_server_per_host").asInt(), 3);
        Assert.assertFalse(sent.has("policies"));
        Assert.assertEquals(group.getPolicy(), "anti-affinity");
        Assert.assertEquals(((Number) group.getRules().get("max_server_per_host")).intValue(), 3);
        Assert.assertEquals(group.getProjectId(), "p1");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.64.*")
    public void serverGroupPolicyNeeds264() throws Exception {
        try {
            osv3().compute().serverGroups().create("web", "affinity", null);
        } finally {
            assertNoMoreRequests();
        }
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -E 'cannot find symbol' | head -3`
Expected: `HypervisorListOptions` 등 없음.

- [ ] **Step 3: 옵션·요청 클래스 구현**

`model/compute/ext/HypervisorListOptions.java`:
```java
package org.openstack4j.model.compute.ext;

import java.util.LinkedHashMap;
import java.util.Map;

/** Filters for {@code GET /os-hypervisors/detail}. */
public class HypervisorListOptions {

    private final Map<String, String> params = new LinkedHashMap<>();
    private int requiredMinor = 0;

    public static HypervisorListOptions create() {
        return new HypervisorListOptions();
    }

    private HypervisorListOptions put(String key, Object value, int minor) {
        params.put(key, String.valueOf(value));
        requiredMinor = Math.max(requiredMinor, minor);
        return this;
    }

    /** 2.53+ */
    public HypervisorListOptions hypervisorHostnamePattern(String pattern) { return put("hypervisor_hostname_pattern", pattern, 53); }
    /** 2.53+ */
    public HypervisorListOptions withServers(boolean withServers) { return put("with_servers", withServers, 53); }
    /** 2.33+ */
    public HypervisorListOptions limit(int limit) { return put("limit", limit, 33); }
    /** Hypervisor id (2.33+; a UUID from 2.53). */
    public HypervisorListOptions marker(String hypervisorId) { return put("marker", hypervisorId, 33); }

    public Map<String, String> toQueryParams() { return new LinkedHashMap<>(params); }

    public String getRequiredMicroVersion() { return requiredMinor == 0 ? null : "2." + requiredMinor; }
}
```

`model/compute/KeypairListOptions.java`:
```java
package org.openstack4j.model.compute;

import java.util.LinkedHashMap;
import java.util.Map;

/** Filters for {@code GET /os-keypairs}. */
public class KeypairListOptions {

    private final Map<String, String> params = new LinkedHashMap<>();
    private int requiredMinor = 0;

    public static KeypairListOptions create() {
        return new KeypairListOptions();
    }

    private KeypairListOptions put(String key, Object value, int minor) {
        params.put(key, String.valueOf(value));
        requiredMinor = Math.max(requiredMinor, minor);
        return this;
    }

    /** Admin: list another user's key pairs (2.10+). */
    public KeypairListOptions userId(String userId) { return put("user_id", userId, 10); }
    /** 2.35+ */
    public KeypairListOptions limit(int limit) { return put("limit", limit, 35); }
    /** Key pair name (2.35+). */
    public KeypairListOptions marker(String keypairName) { return put("marker", keypairName, 35); }

    public Map<String, String> toQueryParams() { return new LinkedHashMap<>(params); }

    public String getRequiredMicroVersion() { return requiredMinor == 0 ? null : "2." + requiredMinor; }
}
```

`model/compute/ext/ServiceUpdate.java`:
```java
package org.openstack4j.model.compute.ext;

import java.util.LinkedHashMap;
import java.util.Map;

/** Body of {@code PUT /os-services/{service_id}} (2.53+). Only the fields set are sent. */
public class ServiceUpdate {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    public static ServiceUpdate create() {
        return new ServiceUpdate();
    }

    public ServiceUpdate enable() {
        fields.put("status", "enabled");
        fields.remove("disabled_reason");
        return this;
    }

    /** @param reason optional reason, {@code null} for none */
    public ServiceUpdate disable(String reason) {
        fields.put("status", "disabled");
        if (reason != null)
            fields.put("disabled_reason", reason);
        return this;
    }

    public ServiceUpdate forcedDown(boolean forcedDown) {
        fields.put("forced_down", forcedDown);
        return this;
    }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(fields);
    }
}
```

- [ ] **Step 4: 모델 필드 추가**

`Keypair.java`: `/** @return ssh or x509 (2.2+) */ default String getType() { return null; }`
`NovaKeypair.java`: 필드 `private String type;`, `@Override public String getType()`, 팩토리 추가:
```java
    public static NovaKeypair create(String name, String publicKey, String type) {
        NovaKeypair kp = create(name, publicKey);
        kp.type = type;
        return kp;
    }
```

`ServerGroup.java`:
```java
    /** @return the single policy (2.64+) */
    default String getPolicy() { return null; }
    /** @return policy rules such as max_server_per_host (2.64+) */
    default Map<String, Object> getRules() { return null; }
    /** @return project_id (2.13+) */
    default String getProjectId() { return null; }
    /** @return user_id (2.13+) */
    default String getUserId() { return null; }
```
`NovaServerGroup.java`: 필드 `private String policy; private Map<String, Object> rules; @JsonProperty("project_id") private String projectId; @JsonProperty("user_id") private String userId;` 와 getter, 팩토리:
```java
    public static NovaServerGroup create(String name, String policy, Map<String, Object> rules) {
        NovaServerGroup ns = new NovaServerGroup();
        ns.name = name;
        ns.policy = policy;
        ns.rules = rules;
        return ns;
    }
```
(`policies` 는 `null` 이라 `NON_NULL` 로 빠진다 — `serverGroupWithPolicyAndRules` 가 확인.)

- [ ] **Step 5: 서비스 구현**

인터페이스에 Interfaces 의 메서드를 선언하고 구현:

`HypervisorServiceImpl`:
```java
    @Override
    public List<? extends Hypervisor> list(HypervisorListOptions options) {
        Objects.requireNonNull(options);
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Hypervisor list filters " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
        return get(Hypervisors.class, "/os-hypervisors/detail").params(options.toQueryParams()).execute().getList();
    }
```

`ServicesServiceImpl`:
```java
    @Override
    public Service update(String serviceId, ServiceUpdate update) {
        Objects.requireNonNull(serviceId);
        Objects.requireNonNull(update);
        requireMicroVersion("Updating a service by id", V(53));
        return put(ExtService.class, uri("/os-services/%s", serviceId)).entity(JsonBody.of(update.toMap())).execute();
    }

    @Override
    public ActionResponse delete(String serviceId) {
        Objects.requireNonNull(serviceId);
        return ToActionResponseFunction.INSTANCE.apply(delete(Void.class, uri("/os-services/%s", serviceId)).executeWithResponse());
    }

    @Override
    public ExtService disableWithReason(String binary, String host, String reason) {
        Objects.requireNonNull(binary);
        Objects.requireNonNull(host);
        Objects.requireNonNull(reason);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("binary", binary);
        body.put("host", host);
        body.put("disabled_reason", reason);
        return capped(put(ExtService.class, uri("/os-services/disable-log-reason")), V(52)).entity(JsonBody.of(body)).execute();
    }
```
(`PUT /os-services/{id}` 응답은 `{"service": {...}}` — `ExtService` 가 `@JsonRootName("service")` 라 그대로 읽힌다. `ExtService.getDisabledReason()` 이 `Service` 인터페이스에 있는지 확인한다.)

`HostAggregateServiceImpl`:
```java
    @Override
    public ActionResponse cacheImages(String aggregateId, List<String> imageIds) {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(imageIds);
        requireMicroVersion("Aggregate image caching", V(81));
        List<Map<String, String>> cache = imageIds.stream().map(id -> Collections.singletonMap("id", id)).collect(Collectors.toList());
        return ToActionResponseFunction.INSTANCE.apply(
                post(Void.class, uri("/os-aggregates/%s/images", aggregateId))
                        .entity(JsonBody.of(Collections.singletonMap("cache", cache)))
                        .executeWithResponse());
    }
```

`QuotaSetServiceImpl`:
```java
    @Override
    public QuotaSet defaults(String tenantId) {
        Objects.requireNonNull(tenantId);
        return get(NovaQuotaSet.class, uri("/os-quota-sets/%s/defaults", tenantId)).execute();
    }
```

`KeypairServiceImpl`:
```java
    @Override
    public List<? extends Keypair> list(KeypairListOptions options) {
        Objects.requireNonNull(options);
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Key pair list filters " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
        return get(Keypairs.class, uri("/os-keypairs")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public Keypair create(String name, @Nullable String publicKey, String type) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        requireMicroVersion("Key pair type", V(2));
        Invocation<NovaKeypair> req = post(NovaKeypair.class, uri("/os-keypairs"));
        if (publicKey == null)
            capped(req, V(91));    // key generation removed in 2.92
        return req.entity(NovaKeypair.create(name, publicKey, type)).execute();
    }
```

`ServerGroupServiceImpl`:
```java
    @Override
    public ServerGroup create(String name, String policy, Map<String, Object> rules) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(policy);
        requireMicroVersion("Server group policy and rules", V(64));
        return post(NovaServerGroup.class, uri("/os-server-groups")).entity(NovaServerGroup.create(name, policy, rules)).execute();
    }
```

- [ ] **Step 6: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ComputeServiceExtensionsTests,ServiceTests,HostAggregateTests,QuotaSetTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Failures: 0, Errors: 0` (ComputeServiceExtensionsTests 7개).

- [ ] **Step 7: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(compute): extend hypervisors, services, aggregates, quota sets, keypairs and server groups

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
.superpowers/pr.sh "hypervisors list 옵션(2.33/2.53), services update/delete(2.53)·disableWithReason, aggregates cacheImages(2.81), quota defaults, keypairs list 옵션(2.10/2.35)·type(2.2), serverGroups policy/rules(2.64)."
```

---

### Task 10: 새 서비스 4개 — external events, assisted volume snapshots, console auth tokens, usage audit logs

**Files:**
- Create (`core/src/main/java/org/openstack4j/api/compute/`): `ServerExternalEventService.java`, `AssistedVolumeSnapshotService.java`, `ConsoleAuthTokenService.java`, `InstanceUsageAuditLogService.java`
- Create (`core/src/main/java/org/openstack4j/model/compute/`): `ExternalEvent.java`, `ExternalEventCreate.java`, `AssistedVolumeSnapshot.java`, `ConsoleConnectionInfo.java`, `InstanceUsageAuditLog.java`
- Create (`core/src/main/java/org/openstack4j/openstack/compute/domain/`): `NovaExternalEvent.java`, `NovaAssistedVolumeSnapshot.java`, `NovaConsoleConnectionInfo.java`, `NovaInstanceUsageAuditLog.java`
- Create (`core/src/main/java/org/openstack4j/openstack/compute/internal/`): `ServerExternalEventServiceImpl.java`, `AssistedVolumeSnapshotServiceImpl.java`, `ConsoleAuthTokenServiceImpl.java`, `InstanceUsageAuditLogServiceImpl.java`
- Modify: `api/compute/ComputeService.java`, `openstack/compute/internal/ComputeServiceImpl.java`, `openstack/provider/DefaultAPIProvider.java`
- Create: `core-test/src/main/java/org/openstack4j/api/compute/microversion/NewComputeServicesTests.java`

**Interfaces:**
- Consumes: Task 2 `requireMicroVersion`, `V`; Task 5 `JsonBody`.
- Produces:
  - `ComputeService`: `serverExternalEvents()`, `assistedVolumeSnapshots()`, `consoleAuthTokens()`, `instanceUsageAuditLogs()`
  - `ServerExternalEventService.create(List<ExternalEventCreate> events)` → `List<? extends ExternalEvent>`; 이벤트 이름별 하한: `volume-extended` 2.51, `power-update` 2.76, `accelerator-request-bound` 2.82
  - `ExternalEventCreate.of(String name, String serverUuid)` + `status(String)`, `tag(String)`; getter `getName()`, `getServerUuid()`, `getStatus()`, `getTag()`
  - `ExternalEvent`: `getName()`, `getServerUuid()`, `getStatus()`, `getTag()`, `Integer getCode()`
  - `AssistedVolumeSnapshotService`: `AssistedVolumeSnapshot create(String volumeId, Map<String, Object> createInfo)`, `ActionResponse delete(String snapshotId, Map<String, Object> deleteInfo)`; `AssistedVolumeSnapshot`: `getId()`, `getVolumeId()`
  - `ConsoleAuthTokenService.get(String token)` → `ConsoleConnectionInfo`: `getInstanceUuid()`, `getHost()`, `Integer getPort()`, `Integer getTlsPort()`(2.99), `getInternalAccessPath()`
  - `InstanceUsageAuditLogService`: `InstanceUsageAuditLog list()`, `InstanceUsageAuditLog get(String before)`; `InstanceUsageAuditLog`: `List<String> getHostsNotRun()`, `Map<String, Object> getLog()`, `Integer getNumHosts()`, `Integer getNumHostsDone()`, `Integer getNumHostsNotRun()`, `Integer getNumHostsRunning()`, `String getOverallStatus()`, `String getPeriodBeginning()`, `String getPeriodEnding()`, `Integer getTotalErrors()`, `Integer getTotalInstances()`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/c10-new-compute-services
```

`NewComputeServicesTests.java`:
```java
package org.openstack4j.api.compute.microversion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.AssistedVolumeSnapshot;
import org.openstack4j.model.compute.ConsoleConnectionInfo;
import org.openstack4j.model.compute.ExternalEvent;
import org.openstack4j.model.compute.ExternalEventCreate;
import org.openstack4j.model.compute.InstanceUsageAuditLog;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/NewServices")
public class NewComputeServicesTests extends AbstractComputeMicroVersionTest {

    public void externalEvents() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"events\": [{\"name\": \"network-changed\", \"server_uuid\": \"" + SERVER + "\", \"status\": \"completed\", \"code\": 200, \"tag\": \"port-1\"}]}");

        List<? extends ExternalEvent> events = osv3().compute().serverExternalEvents()
                .create(Collections.singletonList(ExternalEventCreate.of("network-changed", SERVER).tag("port-1")));

        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/os-server-external-events"));
        JsonNode sent = body(request).get("events").get(0);
        Assert.assertEquals(sent.get("name").asText(), "network-changed");
        Assert.assertEquals(sent.get("server_uuid").asText(), SERVER);
        Assert.assertEquals(sent.get("tag").asText(), "port-1");
        Assert.assertFalse(sent.has("status"));
        Assert.assertEquals(events.get(0).getCode(), Integer.valueOf(200));
        Assert.assertEquals(events.get(0).getStatus(), "completed");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.76.*")
    public void powerUpdateEventNeeds276() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.75");
        try {
            osv3().compute().serverExternalEvents().create(Collections.singletonList(ExternalEventCreate.of("power-update", SERVER).tag("POWER_ON")));
        } finally {
            assertNoMoreRequests();
        }
    }

    public void assistedVolumeSnapshots() throws Exception {
        respondWith(200, "{\"snapshot\": {\"id\": \"421752a6-acf6-4b2d-bc7a-119f9148cd8c\", \"volumeId\": \"521752a6-acf6-4b2d-bc7a-119f9148cd8c\"}}");
        respondWith(204);

        AssistedVolumeSnapshot snapshot = osv3().compute().assistedVolumeSnapshots().create("521752a6-acf6-4b2d-bc7a-119f9148cd8c",
                java.util.Map.of("snapshot_id", "421752a6-acf6-4b2d-bc7a-119f9148cd8c", "type", "qcow2", "new_file", "new_file_name"));
        boolean deleted = osv3().compute().assistedVolumeSnapshots().delete("421752a6-acf6-4b2d-bc7a-119f9148cd8c",
                java.util.Map.of("volume_id", "521752a6-acf6-4b2d-bc7a-119f9148cd8c", "type", "qcow2")).isSuccess();

        RecordedRequest create = takeRequest();
        Assert.assertEquals(body(create).get("snapshot").get("volume_id").asText(), "521752a6-acf6-4b2d-bc7a-119f9148cd8c");
        Assert.assertEquals(body(create).get("snapshot").get("create_info").get("type").asText(), "qcow2");
        RecordedRequest delete = takeRequest();
        Assert.assertEquals(delete.getMethod(), "DELETE");
        Assert.assertTrue(delete.getPath().contains("/os-assisted-volume-snapshots/421752a6-acf6-4b2d-bc7a-119f9148cd8c?delete_info="), delete.getPath());
        String deleteInfo = java.net.URLDecoder.decode(delete.getRequestUrl().queryParameter("delete_info"), "UTF-8");
        Assert.assertTrue(deleteInfo.contains("\"type\":\"qcow2\""), deleteInfo);
        Assert.assertEquals(snapshot.getVolumeId(), "521752a6-acf6-4b2d-bc7a-119f9148cd8c");
        Assert.assertTrue(deleted);
    }

    public void consoleAuthToken() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"console\": {\"instance_uuid\": \"" + SERVER + "\", \"host\": \"localhost\", \"port\": 5900, \"tls_port\": 5901, \"internal_access_path\": null}}");

        ConsoleConnectionInfo info = osv3().compute().consoleAuthTokens().get("b60bcfc3-5fd4-4d21-986c-e83379107819");

        Assert.assertTrue(takeRequest().getPath().endsWith("/os-console-auth-tokens/b60bcfc3-5fd4-4d21-986c-e83379107819"));
        Assert.assertEquals(info.getInstanceUuid(), SERVER);
        Assert.assertEquals(info.getPort(), Integer.valueOf(5900));
        Assert.assertEquals(info.getTlsPort(), Integer.valueOf(5901));
        Assert.assertNull(info.getInternalAccessPath());
    }

    public void instanceUsageAuditLogs() throws Exception {
        respondWith(200, "{\"instance_usage_audit_logs\": {\"period_beginning\": \"2026-09-01 00:00:00\", \"period_ending\": \"2026-10-01 00:00:00\","
                + " \"num_hosts\": 1, \"num_hosts_done\": 1, \"num_hosts_running\": 0, \"num_hosts_not_run\": 0, \"hosts_not_run\": [],"
                + " \"total_instances\": 3, \"total_errors\": 0, \"overall_status\": \"ALL hosts done. 0 errors.\", \"log\": {\"compute-1\": {\"state\": \"DONE\"}}}}");
        respondWith(200, "{\"instance_usage_audit_log\": {\"period_beginning\": \"2026-08-01 00:00:00\", \"period_ending\": \"2026-09-01 00:00:00\","
                + " \"num_hosts\": 0, \"hosts_not_run\": [\"compute-2\"], \"total_instances\": 0, \"total_errors\": 0, \"overall_status\": \"x\", \"log\": {}}}");

        InstanceUsageAuditLog current = osv3().compute().instanceUsageAuditLogs().list();
        InstanceUsageAuditLog before = osv3().compute().instanceUsageAuditLogs().get("2026-09-01 00:00:00");

        Assert.assertTrue(takeRequest().getPath().endsWith("/os-instance_usage_audit_log"));
        Assert.assertTrue(takeRequest().getPath().contains("/os-instance_usage_audit_log/2026-09-01"));
        Assert.assertEquals(current.getTotalInstances(), Integer.valueOf(3));
        Assert.assertEquals(current.getOverallStatus(), "ALL hosts done. 0 errors.");
        Assert.assertTrue(current.getLog().containsKey("compute-1"));
        Assert.assertEquals(before.getHostsNotRun(), Arrays.asList("compute-2"));
        Assert.assertEquals(before.getPeriodBeginning(), "2026-08-01 00:00:00");
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -E 'cannot find symbol' | head -3`
Expected: `serverExternalEvents` 등 없음.

- [ ] **Step 3: 모델 구현**

`model/compute/ExternalEventCreate.java`:
```java
package org.openstack4j.model.compute;

/** One event for {@code POST /os-server-external-events} (normally sent by Neutron, Cinder or Cyborg; admin only). */
public class ExternalEventCreate {

    private final String name;
    private final String serverUuid;
    private String status;
    private String tag;

    private ExternalEventCreate(String name, String serverUuid) {
        this.name = name;
        this.serverUuid = serverUuid;
    }

    /**
     * @param name network-changed, network-vif-plugged, network-vif-unplugged, network-vif-deleted,
     *             volume-extended (2.51+), power-update (2.76+) or accelerator-request-bound (2.82+)
     */
    public static ExternalEventCreate of(String name, String serverUuid) {
        return new ExternalEventCreate(name, serverUuid);
    }

    /** failed, completed (default) or in-progress */
    public ExternalEventCreate status(String status) { this.status = status; return this; }

    public ExternalEventCreate tag(String tag) { this.tag = tag; return this; }

    public String getName() { return name; }
    public String getServerUuid() { return serverUuid; }
    public String getStatus() { return status; }
    public String getTag() { return tag; }
}
```

`model/compute/ExternalEvent.java`:
```java
package org.openstack4j.model.compute;

import org.openstack4j.model.ModelEntity;

public interface ExternalEvent extends ModelEntity {
    String getName();
    String getServerUuid();
    String getStatus();
    String getTag();
    /** Per-event result code: 200, 400, 404 or 422. */
    Integer getCode();
}
```

`openstack/compute/domain/NovaExternalEvent.java`:
```java
package org.openstack4j.openstack.compute.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.compute.ExternalEvent;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaExternalEvent implements ExternalEvent {

    private static final long serialVersionUID = 1L;

    private String name;
    @JsonProperty("server_uuid") private String serverUuid;
    private String status;
    private String tag;
    private Integer code;

    @Override public String getName() { return name; }
    @Override public String getServerUuid() { return serverUuid; }
    @Override public String getStatus() { return status; }
    @Override public String getTag() { return tag; }
    @Override public Integer getCode() { return code; }

    public static class NovaExternalEvents extends ListResult<NovaExternalEvent> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("events")
        private List<NovaExternalEvent> events;

        @Override
        protected List<NovaExternalEvent> value() {
            return events;
        }
    }
}
```

`model/compute/AssistedVolumeSnapshot.java`:
```java
package org.openstack4j.model.compute;

import org.openstack4j.model.ModelEntity;

/** Result of an assisted volume snapshot (used by Cinder volume drivers; admin only). */
public interface AssistedVolumeSnapshot extends ModelEntity {
    String getId();
    String getVolumeId();
}
```

`openstack/compute/domain/NovaAssistedVolumeSnapshot.java`:
```java
package org.openstack4j.openstack.compute.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.compute.AssistedVolumeSnapshot;

@JsonRootName("snapshot")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaAssistedVolumeSnapshot implements AssistedVolumeSnapshot {

    private static final long serialVersionUID = 1L;

    private String id;
    private String volumeId;

    @Override public String getId() { return id; }
    @Override public String getVolumeId() { return volumeId; }
}
```

`model/compute/ConsoleConnectionInfo.java`:
```java
package org.openstack4j.model.compute;

import org.openstack4j.model.ModelEntity;

/** Connection details behind a console token (admin only; all console types from 2.31). */
public interface ConsoleConnectionInfo extends ModelEntity {
    String getInstanceUuid();
    String getHost();
    Integer getPort();
    /** 2.99+ */
    Integer getTlsPort();
    String getInternalAccessPath();
}
```

`openstack/compute/domain/NovaConsoleConnectionInfo.java`:
```java
package org.openstack4j.openstack.compute.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.compute.ConsoleConnectionInfo;

@JsonRootName("console")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaConsoleConnectionInfo implements ConsoleConnectionInfo {

    private static final long serialVersionUID = 1L;

    @JsonProperty("instance_uuid") private String instanceUuid;
    private String host;
    private Integer port;
    @JsonProperty("tls_port") private Integer tlsPort;
    @JsonProperty("internal_access_path") private String internalAccessPath;

    @Override public String getInstanceUuid() { return instanceUuid; }
    @Override public String getHost() { return host; }
    @Override public Integer getPort() { return port; }
    @Override public Integer getTlsPort() { return tlsPort; }
    @Override public String getInternalAccessPath() { return internalAccessPath; }
}
```

`model/compute/InstanceUsageAuditLog.java`:
```java
package org.openstack4j.model.compute;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** Status of the instance usage audit task for one period (admin only). */
public interface InstanceUsageAuditLog extends ModelEntity {
    List<String> getHostsNotRun();
    Map<String, Object> getLog();
    Integer getNumHosts();
    Integer getNumHostsDone();
    Integer getNumHostsNotRun();
    Integer getNumHostsRunning();
    String getOverallStatus();
    String getPeriodBeginning();
    String getPeriodEnding();
    Integer getTotalErrors();
    Integer getTotalInstances();
}
```

`openstack/compute/domain/NovaInstanceUsageAuditLog.java`:
```java
package org.openstack4j.openstack.compute.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.compute.InstanceUsageAuditLog;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaInstanceUsageAuditLog implements InstanceUsageAuditLog {

    private static final long serialVersionUID = 1L;

    @JsonProperty("hosts_not_run") private List<String> hostsNotRun;
    private Map<String, Object> log;
    @JsonProperty("num_hosts") private Integer numHosts;
    @JsonProperty("num_hosts_done") private Integer numHostsDone;
    @JsonProperty("num_hosts_not_run") private Integer numHostsNotRun;
    @JsonProperty("num_hosts_running") private Integer numHostsRunning;
    @JsonProperty("overall_status") private String overallStatus;
    @JsonProperty("period_beginning") private String periodBeginning;
    @JsonProperty("period_ending") private String periodEnding;
    @JsonProperty("total_errors") private Integer totalErrors;
    @JsonProperty("total_instances") private Integer totalInstances;

    @Override public List<String> getHostsNotRun() { return hostsNotRun; }
    @Override public Map<String, Object> getLog() { return log; }
    @Override public Integer getNumHosts() { return numHosts; }
    @Override public Integer getNumHostsDone() { return numHostsDone; }
    @Override public Integer getNumHostsNotRun() { return numHostsNotRun; }
    @Override public Integer getNumHostsRunning() { return numHostsRunning; }
    @Override public String getOverallStatus() { return overallStatus; }
    @Override public String getPeriodBeginning() { return periodBeginning; }
    @Override public String getPeriodEnding() { return periodEnding; }
    @Override public Integer getTotalErrors() { return totalErrors; }
    @Override public Integer getTotalInstances() { return totalInstances; }

    /** {@code GET /os-instance_usage_audit_log} */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Current implements ModelEntity {
        private static final long serialVersionUID = 1L;
        @JsonProperty("instance_usage_audit_logs")
        public NovaInstanceUsageAuditLog log;
    }

    /** {@code GET /os-instance_usage_audit_log/{before}} */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Before implements ModelEntity {
        private static final long serialVersionUID = 1L;
        @JsonProperty("instance_usage_audit_log")
        public NovaInstanceUsageAuditLog log;
    }
}
```

- [ ] **Step 4: 서비스 구현**

인터페이스 4개(`extends RestService`, Javadoc 에 엔드포인트·관리자 전용 표시)와 구현 4개(`extends BaseComputeServices`):

`ServerExternalEventServiceImpl`:
```java
public class ServerExternalEventServiceImpl extends BaseComputeServices implements ServerExternalEventService {

    private static final Map<String, MicroVersion> EVENT_FLOORS = Map.of(
            "volume-extended", ComputeMicroVersions.V(51),
            "power-update", ComputeMicroVersions.V(76),
            "accelerator-request-bound", ComputeMicroVersions.V(82));

    @Override
    public List<? extends ExternalEvent> create(List<ExternalEventCreate> events) {
        Objects.requireNonNull(events);
        List<Map<String, Object>> body = new ArrayList<>();
        for (ExternalEventCreate e : events) {
            MicroVersion floor = EVENT_FLOORS.get(e.getName());
            if (floor != null)
                requireMicroVersion("External event " + e.getName(), floor);
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("name", e.getName());
            event.put("server_uuid", e.getServerUuid());
            if (e.getStatus() != null) event.put("status", e.getStatus());
            if (e.getTag() != null) event.put("tag", e.getTag());
            body.add(event);
        }
        return post(NovaExternalEvent.NovaExternalEvents.class, uri("/os-server-external-events"))
                .entity(JsonBody.of(Collections.singletonMap("events", body)))
                .execute().getList();
    }
}
```

`AssistedVolumeSnapshotServiceImpl`:
```java
public class AssistedVolumeSnapshotServiceImpl extends BaseComputeServices implements AssistedVolumeSnapshotService {

    private static final ObjectMapper PLAIN = new ObjectMapper();

    @Override
    public AssistedVolumeSnapshot create(String volumeId, Map<String, Object> createInfo) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(createInfo);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("volume_id", volumeId);
        snapshot.put("create_info", createInfo);
        return post(NovaAssistedVolumeSnapshot.class, uri("/os-assisted-volume-snapshots"))
                .entity(JsonBody.of("snapshot", snapshot))
                .execute();
    }

    @Override
    public ActionResponse delete(String snapshotId, Map<String, Object> deleteInfo) {
        Objects.requireNonNull(snapshotId);
        Objects.requireNonNull(deleteInfo);
        String json;
        try {
            json = PLAIN.writeValueAsString(deleteInfo);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("delete_info is not serializable", e);
        }
        return ToActionResponseFunction.INSTANCE.apply(
                delete(Void.class, uri("/os-assisted-volume-snapshots/%s", snapshotId)).param("delete_info", json).executeWithResponse());
    }
}
```

`ConsoleAuthTokenServiceImpl`:
```java
public class ConsoleAuthTokenServiceImpl extends BaseComputeServices implements ConsoleAuthTokenService {

    @Override
    public ConsoleConnectionInfo get(String token) {
        Objects.requireNonNull(token);
        return get(NovaConsoleConnectionInfo.class, uri("/os-console-auth-tokens/%s", token)).execute();
    }
}
```

`InstanceUsageAuditLogServiceImpl`:
```java
public class InstanceUsageAuditLogServiceImpl extends BaseComputeServices implements InstanceUsageAuditLogService {

    @Override
    public InstanceUsageAuditLog list() {
        NovaInstanceUsageAuditLog.Current current = get(NovaInstanceUsageAuditLog.Current.class, uri("/os-instance_usage_audit_log")).execute();
        return current == null ? null : current.log;
    }

    @Override
    public InstanceUsageAuditLog get(String before) {
        Objects.requireNonNull(before);
        NovaInstanceUsageAuditLog.Before log = get(NovaInstanceUsageAuditLog.Before.class, uri("/os-instance_usage_audit_log/%s", before)).execute();
        return log == null ? null : log.log;
    }
}
```
(`uri(...)` 가 `before` 의 공백·콜론을 인코딩하지 않으면 `URLEncoder.encode(before, UTF_8).replace("+", "%20")` 로 인코딩해 넘긴다 — 테스트는 경로에 `2026-09-01` 이 포함되는지만 본다.)

`ComputeService`/`ComputeServiceImpl` 에 accessor 4개(`Apis.get(...)`), `DefaultAPIProvider` 에 바인딩 4개.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='NewComputeServicesTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(compute): add server external events, assisted volume snapshots, console auth tokens and usage audit logs

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
.superpowers/pr.sh "새 compute 서비스 4개: serverExternalEvents(2.51/2.76/2.82 이벤트 하한), assistedVolumeSnapshots, consoleAuthTokens, instanceUsageAuditLogs."
```

---

### Task 11: 집계·인스턴스 액션·마이그레이션·하이퍼바이저 모델 필드 추가

(플레이버 필드는 Task 3, 키페어 `type`·서버 그룹 `policy`/`rules` 는 Task 9 에서 이미 추가했다. 서비스 `forced_down` 은 기존 `ExtService.forcedDown` 이 읽는다.)

**Files:**
- Modify: `model/compute/HostAggregate.java`, `openstack/compute/domain/NovaHostAggregate.java` (`uuid`, 2.41)
- Modify: `model/compute/InstanceAction.java`, `openstack/compute/domain/NovaInstanceAction.java` (`updated_at` 2.58, `events`)
- Modify: `model/compute/ServerActionEvent.java`, `openstack/compute/internal/NovaServerActionEvent.java` (`host`·`hostId` 2.62, `details` 2.84)
- Modify: `model/compute/ext/Migration.java`, `openstack/compute/domain/ext/ExtMigration.java` (`migration_type` 2.23, `uuid` 2.59, `user_id`·`project_id` 2.80)
- Modify: `model/compute/ext/Hypervisor.java`, `openstack/compute/domain/ext/ExtHypervisor.java` (`servers` 2.53 with_servers, `uptime` 2.88)
- Create: `core-test/src/main/java/org/openstack4j/api/compute/microversion/ModelFieldTests.java`

**Interfaces:**
- Consumes: Task 7 `MigrationListOptions`, Task 9 `HypervisorListOptions` (테스트 호출).
- Produces (모두 `default`, 기본 `null`):
  - `HostAggregate.getUuid()`
  - `InstanceAction.getUpdatedAt()`(Date), `InstanceAction.getEvents()`(`List<? extends ServerActionEvent>`)
  - `ServerActionEvent.getHost()`, `getHostId()`, `getDetails()`
  - `Migration.getMigrationType()`, `getUuid()`, `getUserId()`, `getProjectId()`
  - `Hypervisor.getServers()`(`List<? extends HypervisorServer>`; `HypervisorServer`: `getUuid()`, `getName()`), `Hypervisor.getUptime()`(String)

결정: 2.88 에서 사라진 하이퍼바이저 통계 필드(`vcpus`, `memory_mb`, ...)는 기존 getter 가 `int` 라 `0` 으로 남는다(반환 타입을 바꾸면 호환이 깨진다). Javadoc 에 "2.88 이상에서는 0, Placement 를 쓰라"고 적는다.

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/c11-model-fields
```

`ModelFieldTests.java`:
```java
package org.openstack4j.api.compute.microversion;

import java.util.List;

import org.openstack4j.model.compute.HostAggregate;
import org.openstack4j.model.compute.InstanceAction;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.compute.ext.HypervisorListOptions;
import org.openstack4j.model.compute.ext.Migration;
import org.openstack4j.model.compute.ext.MigrationListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/ModelFields")
public class ModelFieldTests extends AbstractComputeMicroVersionTest {

    public void aggregateUuid() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"aggregate\": {\"id\": 1, \"uuid\": \"fd0a5b12-7e8d-469d-bfd5-64a6823e7407\", \"name\": \"agg\", \"hosts\": [], \"metadata\": {}, \"deleted\": false}}");
        HostAggregate aggregate = osv3().compute().hostAggregates().get("1");
        takeRequest();
        Assert.assertEquals(aggregate.getUuid(), "fd0a5b12-7e8d-469d-bfd5-64a6823e7407");
    }

    public void instanceActionUpdatedAtAndEventDetails() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"instanceAction\": {\"action\": \"create\", \"instance_uuid\": \"" + SERVER + "\", \"request_id\": \"req-1\","
                + " \"start_time\": \"2026-09-22T08:02:43.000000\", \"updated_at\": \"2026-09-22T08:02:56.000000\", \"user_id\": \"u\", \"project_id\": \"p\","
                + " \"events\": [{\"event\": \"compute__do_build_and_run_instance\", \"start_time\": \"2026-09-22T08:02:44.000000\","
                + " \"finish_time\": \"2026-09-22T08:02:55.000000\", \"result\": \"Success\", \"host\": \"compute-1\", \"hostId\": \"abc\", \"details\": null}]}}");

        InstanceAction action = osv3().compute().servers().instanceActions().get(SERVER, "req-1");
        takeRequest();

        Assert.assertNotNull(action.getUpdatedAt());
        Assert.assertEquals(action.getEvents().size(), 1);
        Assert.assertEquals(action.getEvents().get(0).getHost(), "compute-1");
        Assert.assertEquals(action.getEvents().get(0).getHostId(), "abc");
        Assert.assertNull(action.getEvents().get(0).getDetails());
    }

    public void migrationFields() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"migrations\": [{\"id\": 1, \"uuid\": \"42341d4b-346a-40d0-83c6-5f4f6892b650\", \"migration_type\": \"live-migration\","
                + " \"status\": \"completed\", \"instance_uuid\": \"" + SERVER + "\", \"user_id\": \"u1\", \"project_id\": \"p1\","
                + " \"created_at\": \"2026-09-22T08:02:43.000000\", \"updated_at\": \"2026-09-22T08:02:56.000000\"}]}");

        List<? extends Migration> migrations = osv3().compute().migrations().list(MigrationListOptions.create().userId("u1"));
        takeRequest();

        Assert.assertEquals(migrations.get(0).getMigrationType(), "live-migration");
        Assert.assertEquals(migrations.get(0).getUuid(), "42341d4b-346a-40d0-83c6-5f4f6892b650");
        Assert.assertEquals(migrations.get(0).getProjectId(), "p1");
    }

    public void hypervisorServersAndUptime() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"hypervisors\": [{\"id\": \"c48f6247-abe4-4a24-824e-ea39e108874f\", \"hypervisor_hostname\": \"compute-1\", \"state\": \"up\","
                + " \"status\": \"enabled\", \"uptime\": \" 08:32:11 up 93 days\", \"servers\": [{\"uuid\": \"" + SERVER + "\", \"name\": \"instance-00000001\"}],"
                + " \"service\": {\"host\": \"compute-1\", \"id\": \"e81d66a4-ddd3-4aba-8a84-171d1cb4d339\", \"disabled_reason\": null}}]}");

        List<? extends Hypervisor> hypervisors = osv3().compute().hypervisors().list(HypervisorListOptions.create().withServers(true));
        takeRequest();

        Assert.assertEquals(hypervisors.get(0).getServers().get(0).getUuid(), SERVER);
        Assert.assertEquals(hypervisors.get(0).getUptime(), " 08:32:11 up 93 days");
        Assert.assertEquals(hypervisors.get(0).getVirtualCPU(), 0);
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -E 'cannot find symbol' | head -3`
Expected: `getUuid`, `getEvents` 등 없음.

- [ ] **Step 3: 구현**

인터페이스(`default`):
```java
// HostAggregate
    /** @return uuid (2.41+) */
    default String getUuid() { return null; }

// InstanceAction
    /** @return updated_at (2.58+) */
    default Date getUpdatedAt() { return null; }
    /** @return events, present when the action is shown by request id */
    default List<? extends ServerActionEvent> getEvents() { return null; }

// ServerActionEvent
    /** @return host name, if policy allows (2.62+) */
    default String getHost() { return null; }
    /** @return obfuscated host id (2.62+) */
    default String getHostId() { return null; }
    /** @return failure details (2.84+) */
    default String getDetails() { return null; }

// Migration
    /** @return evacuation, live-migration, migration or resize (2.23+) */
    default String getMigrationType() { return null; }
    /** @return uuid (2.59+) */
    default String getUuid() { return null; }
    /** @return user_id (2.80+) */
    default String getUserId() { return null; }
    /** @return project_id (2.80+) */
    default String getProjectId() { return null; }

// Hypervisor
    /** @return servers on the hypervisor when listed with with_servers (2.53+) */
    default List<? extends HypervisorServer> getServers() { return null; }
    /** @return uptime text (2.88+ in hypervisor detail) */
    default String getUptime() { return null; }

    interface HypervisorServer extends ModelEntity {
        String getUuid();
        String getName();
    }
```

구현 필드(각 `@Override` getter 포함):
- `NovaHostAggregate`: `public String uuid;`
- `NovaInstanceAction`: `@JsonProperty("updated_at") private Date updatedAt;`, `private List<NovaServerActionEvent> events;` (`NovaServerActionEvent` 는 `openstack.compute.internal` 패키지에 있다 — import)
- `NovaServerActionEvent`: `private String host;`, `@JsonProperty("hostId") private String hostId;`, `private String details;`
- `ExtMigration`: `@JsonProperty("migration_type") private String migrationType;`, `private String uuid;`, `@JsonProperty("user_id") private String userId;`, `@JsonProperty("project_id") private String projectId;`
- `ExtHypervisor`: `private List<Server> servers;`, `private String uptime;`, 중첩 클래스
  ```java
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Server implements HypervisorServer {
      private static final long serialVersionUID = 1L;
      private String uuid;
      private String name;
      @Override public String getUuid() { return uuid; }
      @Override public String getName() { return name; }
  }
  ```
  그리고 `getVirtualCPU()` 등 2.88 에서 사라진 필드 getter 의 Javadoc 에 `<p>Always 0 from compute microversion 2.88; use Placement inventories instead.</p>` 추가.

`NovaInstanceAction`/`ExtMigration` 의 날짜 형식(`2026-09-22T08:02:56.000000`)은 같은 클래스의 기존 날짜 필드(`startTime`, `createdAt`)와 같은 설정을 따른다 — 기존 필드에 `@JsonFormat` 이 있으면 똑같이 붙인다.

- [ ] **Step 4: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ModelFieldTests,HostAggregateTests,MigrationTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Failures: 0, Errors: 0` (ModelFieldTests 4개).

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(compute): read microversion fields of aggregates, instance actions, migrations and hypervisors

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
.superpowers/pr.sh "모델 필드: aggregate uuid(2.41), instance action updated_at(2.58)·events host/hostId(2.62)·details(2.84), migration type/uuid/user/project(2.23/2.59/2.80), hypervisor servers(2.53)·uptime(2.88)."
```

---

### Task 12: 실환경 통합 테스트, README, MIGRATION, CHANGELOG

**Files:**
- Create: `core-test/src/main/java/org/openstack4j/api/compute/microversion/ComputeLiveTests.java` (B 의 `core-test/.../placement/v1/PlacementLiveTests.java` 와 같은 위치·방식)
- Modify: `README.md` (compute microversion 절), `MIGRATION.md`, `CHANGELOG.md`

**Interfaces:**
- Consumes: Task 2~11 의 공개 API 전부.
- Produces: 없음(문서·테스트).

환경 변수(Placement 실환경 테스트 B 와 같은 이름): `OS_AUTH_URL`, `OS_USERNAME`, `OS_PASSWORD`, `OS_PROJECT_NAME`, `OS_USER_DOMAIN_NAME`, `OS_PROJECT_DOMAIN_NAME`. 없으면 클래스 전체 `SkipException`. 기존 서버는 읽기만 하고, 테스트가 만든 자원(키페어, 임시 서버)은 `finally` 에서 삭제한다.

- [ ] **Step 1: 실환경 테스트 작성**

```bash
git switch main && git pull && git switch -c task/c12-live-tests-docs
sed -n 30,70p core-test/src/main/java/org/openstack4j/api/placement/v1/PlacementLiveTests.java   # 인증·skip 패턴 확인
```

`ComputeLiveTests.java` (인증은 `PlacementLiveTests` 와 같은 방식: `/v3` 보정, 누락 변수는 `SkipException`, `OS_*_DOMAIN_NAME` 기본값 `Default`):
```java
package org.openstack4j.api.compute.microversion;

import java.util.List;
import java.util.UUID;

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.compute.RemoteConsole;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ComputeVersion;
import org.openstack4j.model.image.v2.Image;
import org.openstack4j.openstack.OSFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Runs only when OS_AUTH_URL and friends are set; checks the compute microversion support against a real Nova. */
@Test(suiteName = "Compute/Live", groups = "compute-live")
public class ComputeLiveTests {

    private OSClientV3 os;

    @BeforeClass
    public void authenticate() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null)
            throw new SkipException("OS_AUTH_URL not set");
        os = OSFactory.builderV3()
                .endpoint(url)
                .credentials(System.getenv("OS_USERNAME"), System.getenv("OS_PASSWORD"), Identifier.byName(System.getenv("OS_USER_DOMAIN_NAME")))
                .scopeToProject(Identifier.byName(System.getenv("OS_PROJECT_NAME")), Identifier.byName(System.getenv("OS_PROJECT_DOMAIN_NAME")))
                .authenticate();
        ComputeVersion version = os.compute().microVersions().negotiate();
        Assert.assertTrue(version.isEnabled());
        System.out.println("compute microversion " + version.getMicroVersion() + " (server max " + version.getServerMaxVersion() + ")");
    }

    @Test
    public void listsServersWithEmbeddedFlavor() {
        List<? extends Server> servers = os.compute().servers().list();
        if (servers.isEmpty())
            throw new SkipException("no servers");
        Server server = os.compute().servers().get(servers.get(0).getId());
        Assert.assertNotNull(server.getFlavorSummary().getOriginalName());
        Assert.assertNotNull(server.getTags());
        Assert.assertNotNull(os.compute().servers().ips(server.getId()));
        os.compute().servers().migrations(server.getId());
        os.compute().servers().instanceActions().list(server.getId());
    }

    @Test
    public void legacyCallsStillWorkAfterNegotiation() {
        os.compute().flavors().list();
        os.compute().hypervisors().list();
        os.compute().services().list();
        os.compute().keypairs().list();
        os.compute().serverGroups().list();
        os.compute().hostAggregates().list();
    }

    @Test
    public void keypairWithoutPublicKeyUsesCeiling() {
        String name = "os4j-live-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            Keypair keypair = os.compute().keypairs().create(name, null);
            Assert.assertNotNull(keypair.getPrivateKey());
        } finally {
            os.compute().keypairs().delete(name);
        }
    }

    @Test
    public void temporaryServerLifecycle() throws Exception {
        Flavor flavor = os.compute().flavors().list().stream()
                .min((a, b) -> Integer.compare(a.getRam(), b.getRam())).orElseThrow(() -> new SkipException("no flavor"));
        Image image = os.imagesV2().list().stream().filter(i -> "active".equalsIgnoreCase(String.valueOf(i.getStatus())))
                .findFirst().orElseThrow(() -> new SkipException("no active image"));
        String name = "os4j-live-" + UUID.randomUUID().toString().substring(0, 8);
        Server server = null;
        try {
            try {
                server = os.compute().servers().boot(os.compute().servers().serverBuilder()
                        .name(name).flavor(flavor.getId()).image(image.getId()).autoAllocateNetwork()
                        .tags(java.util.Collections.singletonList("os4j")).build());
            } catch (RuntimeException e) {
                throw new SkipException("cannot create a server here: " + e.getMessage());
            }
            server = os.compute().servers().waitForServerStatus(server.getId(), Server.Status.ACTIVE, 5, java.util.concurrent.TimeUnit.MINUTES);
            if (server.getStatus() != Server.Status.ACTIVE)
                throw new SkipException("server did not become ACTIVE: " + server.getStatus());
            Assert.assertEquals(server.getTags(), java.util.Collections.singletonList("os4j"));
            Assert.assertTrue(os.compute().servers().lock(server.getId(), "os4j live test").isSuccess());
            Assert.assertEquals(os.compute().servers().get(server.getId()).getLockedReason(), "os4j live test");
            os.compute().servers().action(server.getId(), org.openstack4j.model.compute.Action.UNLOCK);
            os.compute().servers().topology(server.getId());
            RemoteConsole console = os.compute().servers().remoteConsole(server.getId(), "vnc", "novnc");
            Assert.assertNotNull(console.getUrl());
            Assert.assertNotNull(os.compute().servers().diagnosticsStandard(server.getId()).getDriver());
        } finally {
            if (server != null)
                os.compute().servers().delete(server.getId());
        }
    }
}
```
(`autoAllocateNetwork()` 가 실패하는 배포(네트워크 자동 할당 미구성)면 boot 의 `catch` 가 Skip 한다. `os.imagesV2()` 의 실제 accessor 이름을 확인한다.)

- [ ] **Step 2: 개발용 OpenStack 에서 실행**

```bash
export OS_AUTH_URL=http://192.168.140.12:5000/v3 OS_USERNAME=admin OS_PROJECT_NAME=admin OS_USER_DOMAIN_NAME=Default OS_PROJECT_DOMAIN_NAME=Default
read -s "OS_PASSWORD?OpenStack password: " && export OS_PASSWORD    # zsh. 비밀번호는 저장하지 않는다
./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ComputeLiveTests -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'compute microversion|Tests run:|FAIL|Skipped' | tail -5
```
Expected: `compute microversion 2.100 (server max 2.100)`, `Failures: 0, Errors: 0`. 이후 `openstack server list --name os4j-live` 와 `openstack keypair list | grep os4j-live` 가 비어 있음을 확인(임시 자원 없음).

비밀번호는 사용자가 입력해야 한다. 실행 단계에서는 사용자에게 위 명령을 프롬프트에서 `!` 접두사로 실행해 달라고 요청하고 결과를 받는다. 비밀번호는 어디에도 저장하지 않는다.

- [ ] **Step 3: README compute 절**

`README.md` 의 Placement 절 뒤에 추가:
````markdown
### Compute microversions

Compute requests carry no microversion header by default, so Nova treats them as 2.1 and existing code keeps
working unchanged. Turn microversions on per client to use newer fields and APIs:

```java
ComputeVersion v = os.compute().microVersions().negotiate();   // min(2.104, server max)
os.compute().microVersions().use("2.79");                      // or pin a version
os.compute().microVersions().clear();                          // back to no header
```

- Methods whose API Nova removed in a later microversion (proxy APIs such as `floatingIps()`, `securityGroups()`,
  `images()`, `host()`, the legacy `getVNCConsole`, `diagnostics`, key pair generation, ...) are sent at the highest
  microversion they support, so they keep working after `negotiate()`.
- New features check their minimum microversion before sending and throw `MicroVersionException` when the session
  does not send it (or microversions are off).
- The choice is kept per client session and compute endpoint.
````

- [ ] **Step 4: MIGRATION, CHANGELOG**

`MIGRATION.md` 에 "4.1 → 4.2" 절:
```markdown
## 4.1 → 4.2

No breaking changes. Compute microversions are opt-in (`os.compute().microVersions().negotiate()`); without it,
requests are identical to 4.1.

- `PlacementMicroVersionException` now extends the new `org.openstack4j.api.exceptions.MicroVersionException`
  (still an `OS4JException`); existing `catch` blocks keep working.
- Model interfaces such as `Server`, `Flavor`, `Keypair` and `ServerGroup` gained `default` getters. Classes that
  implement these interfaces outside the library keep compiling.
- After `negotiate()`, a server's `getFlavorId()` is `null` (Nova 2.47+ embeds the flavor); use `getFlavorSummary()`.
- After `negotiate()`, hypervisor statistics fields removed in 2.88 read as `0`.
```

`CHANGELOG.md` 의 `## [Unreleased]` 아래에 Task 1~11 의 추가 사항을 `### Added`(microversion 프레임워크, 서버 필드·옵션·액션·하위 리소스, 새 서비스 4개, 서비스별 보강)와 `### Changed`(`PlacementMicroVersionException` 부모) 로 정리한다(각 항목 한 줄, 하한 버전 표기).

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "test(compute): add live microversion tests; docs: compute microversions in README, MIGRATION and CHANGELOG

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
.superpowers/pr.sh "ComputeLiveTests(환경 변수 있을 때만), README compute microversion 절, MIGRATION 4.1→4.2, CHANGELOG."
```

---

### Task 13: 전체 리뷰 → 4.2.0 릴리스 → 4.3.0-SNAPSHOT

**Files:**
- Modify: 모든 `pom.xml` 의 버전(`4.2.0-SNAPSHOT` → `4.2.0` → `4.3.0-SNAPSHOT`), `CHANGELOG.md`

**Interfaces:**
- Consumes: Task 1~12 결과.
- Produces: Maven Central `io.github.seogineer:openstack4j:4.2.0` 과 모듈 아티팩트.

- [ ] **Step 1: 전체 브랜치 리뷰**

실행 skill 의 최종 리뷰를 수행한다(가장 성능 좋은 모델의 reviewer, 이 plan 의 Review Focus 5개 포함). Critical/Important 는 TDD 로 수정해 PR 로 머지한다.

- [ ] **Step 2: 세 connector 전체 테스트와 완료 기준 점검**

```bash
git switch main && git pull
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git diff --stat v4.1.0 -- core-test/src/main/java/org/openstack4j/api/compute | grep -v microversion   # 기존 compute 테스트 변경 0
```
Expected: `BUILD SUCCESS`, 기존 compute 테스트 파일 변경 없음(완료 기준 1).

- [ ] **Step 3: 릴리스 버전 PR**

```bash
git switch -c release/4.2.0
./mvnw -B -q versions:set -DnewVersion=4.2.0 -DgenerateBackupPoms=false
sed -i 's/^## \[Unreleased\]/## [Unreleased]\n\n## [4.2.0] - '"$(date +%F)"'/' CHANGELOG.md
git add -A && git commit -m "release: 4.2.0

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
.superpowers/pr.sh "4.2.0 릴리스 버전."
```

- [ ] **Step 4: 사용자 확인 후 tag push**

**사용자에게 "v4.2.0 tag 를 push 해 Maven Central 에 배포할까요?" 를 묻고, 승인 받은 뒤에만** 실행:
```bash
git switch main && git pull
git tag -a v4.2.0 -m "4.2.0" && git push origin v4.2.0
gh run watch -R seogineer/openstack4j "$(gh run list -R seogineer/openstack4j --workflow release.yml -L 1 --json databaseId -q '.[0].databaseId')" --exit-status
curl -s -o /dev/null -w '%{http_code}\n' https://repo1.maven.org/maven2/io/github/seogineer/openstack4j/4.2.0/openstack4j-4.2.0.pom
```
Expected: release 워크플로 성공, repo1 이 `200`(반영까지 수십 분 걸릴 수 있다 — 404 면 `central.sonatype.com` 검색으로 상태 확인 후 기다린다).

- [ ] **Step 5: 다음 개발 버전**

```bash
git switch -c chore/4.3.0-snapshot
./mvnw -B -q versions:set -DnewVersion=4.3.0-SNAPSHOT -DgenerateBackupPoms=false
git add -A && git commit -m "chore: start 4.3.0-SNAPSHOT

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
.superpowers/pr.sh "4.3.0-SNAPSHOT 개발 시작."
```

- [ ] **Step 6: Central 아티팩트로 실환경 확인**

`~/openstack4j-check/run.sh` 의 버전을 4.2.0 으로 바꿔 개발용 OpenStack 에 대해 실행하고(compute microversion 협상 결과 출력 추가), 결과를 사용자에게 보고한다.
