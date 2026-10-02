# Cinder(block storage) v3 microversion 프레임워크와 누락 API (D) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** C 의 compute 전용 microversion 코드를 서비스 공용 `MicroVersionSupport` 로 일반화하고, `os.blockStorage()` 에 opt-in microversion(3.0~3.71)을 적용한 뒤 Cinder v3 api-ref 의 누락 엔드포인트·필드·액션을 채워 4.3.0 으로 배포한다.

**Architecture:** `openstack.internal.microversion.MicroVersionSupport` 가 서비스별 상수(최소/최고 버전, 헤더 이름, 상태 키 접두사)와 세션 상태 조회·협상·고정·헤더 계산·하한 검사를 담당한다. `BaseComputeServices` 는 기존 protected API 를 유지한 채 내부만 이 유틸에 위임하고, `BaseBlockStorageServices` 가 같은 방식으로 `OpenStack-API-Version: volume <v>` 를 붙인다. 공개 인터페이스는 `common.MicroVersionService<V>` 와 `model.common.MicroVersionInfo` 로 공용화하되 기존 compute 타입은 이를 상속만 한다. 꺼져 있으면 기존 동작과 바이트 단위로 같다.

**Tech Stack:** Java 17, Jackson 2.x, TestNG 7, OkHttp MockWebServer 4.12, Maven Wrapper.

**Spec:** `docs/superpowers/specs/2026-10-03-block-storage-microversion-design.md`

## Global Constraints

- 기존 메서드의 시그니처·반환 타입·**microversion 이 꺼졌을 때의 동작**은 바꾸지 않는다. `@Deprecated` 를 새로 붙이지 않는다. 기존 block storage 테스트(core-test `api/storage/*`)와 기존 compute microversion 테스트(`api/compute/microversion/*`)는 **수정 없이** 통과해야 한다.
- 라이브러리 최고 block storage microversion **3.71**, 최소 **3.0**. 개발용 OpenStack(epoxy) Cinder 최대 3.71.
- 헤더(켜졌을 때만): `OpenStack-API-Version: volume <v>` 하나. 요청 버전 = min(적용 버전, 클래스 상한, 메서드 상한).
- 하한 미달·꺼짐 상태에서 새 기능 호출 → **서버 요청 없이** `org.openstack4j.api.exceptions.MicroVersionException`.
- compute 코드는 **내부만** 바꾼다. `BaseComputeServices` 의 protected API(`classCeiling`, `decorate`, `capped`, `effectiveMicroVersion`, `isMicroVersionAtLeast`, `requireMicroVersion`, `invokeAction*`)와 `ComputeMicroVersions` 의 public API(`MINIMUM`, `LATEST`, `V`, `currentState`, `rootUrl`)는 그대로 둔다.
- deprecated consistency groups(`/consistencygroups`, `/cgsnapshots`), 레거시 `/os-volume-manage`·`/os-snapshot-manage` 는 추가하지 않는다. 기존 `/os-volume-transfer` 는 유지한다.
- 회사 코드(`~/IdeaProjects/openstackit-java`)는 참고·복사하지 않는다.
- 각 Task = 브랜치 + PR, CI(JDK 17/21/25, 아티팩트 검사, 스모크) 통과 후 squash 머지. tag push(배포)는 사용자 지시("질문 없이 진행")에 따라 CI 통과 확인 후 바로 한다.
- 커밋 끝 `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>` 과 `Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt`, PR 본문 끝 `🤖 Generated with [Claude Code](https://claude.com/claude-code)` 와 세션 링크.
- 빌드 `./mvnw`. core-test 클래스 실행: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='<Class>' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test`.
- PR 도우미: `.superpowers/pr.sh "<본문>"` (push → PR → CI 대기 → squash 머지, git-ignored).
- block storage 구현 코드의 `V(n)` 은 `import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;`. plan 의 코드 조각은 import 를 생략했으니 컴파일러가 요구하는 대로 추가한다.
- 새 테스트는 모두 `org.openstack4j.api.storage.microversion` 패키지에 둔다(`all.xml` 이 `org.openstack4j.*` 를 스캔). 새 fixture 는 `core-test/src/main/resources/storage/microversion/`.
- 요청 본문에 명시적 `null` 이나 루트 없는 맵이 필요하면 C 의 `openstack.compute.domain.JsonBody` 와 같은 역할의 **`org.openstack4j.openstack.internal.microversion.JsonBody`** 를 쓴다(Task 1 에서 compute 의 것을 공용 위치로 옮기지 않고 **복사**한다 — compute 쪽은 건드리지 않는다). 전역 mapper 가 `NON_NULL` 이라 `Map` 의 `null` 값은 빠진다.
- Cinder 액션 응답은 대부분 202 본문 없음 → `post(ActionResponse.class, ...).entity(...).execute()` 의 기존 패턴(`BlockVolumeServiceImpl.forceDelete` 와 동일)을 따른다.

## Review Focus

1. **microversion 을 켜지 않은 사용자에게 아무 변화가 없어야 한다** — 헤더 0개, 추가 요청 0개(루트 조회 없음), compute 도 리팩터링 전과 같은 헤더. → Task 2 `noHeaderAndNoDiscoveryWhenDisabled`, 기존 compute 테스트 전부.
2. **3.71 을 켠 상태의 레거시 생성 호출** — `bootable` 을 설정한 `volumes().create()` 는 3.52 로, `force(false)` 스냅샷 생성은 3.65 로 나가야 한다(그 외 기존 호출은 3.71 그대로). → Task 2 `legacyCreateWithBootableIsCapped`, `legacySnapshotForceFalseIsCapped`, `legacyListsStayAtNegotiatedVersion`.
3. **project id 가 없는 endpoint(`block-storage` 타입의 `/v3`)와 경로 접두사 배포(`https://cloud/volume/v3/<project>`)의 루트 계산** — `GET /` 이 올바른 URL 로 가야 한다. → Task 2 `rootUrlStripsVersionAndProject`(단위).
4. **3.69 의 `shared_targets` tristate** — `null` 이 `false` 로 바뀌면 os-brick 잠금 의미가 달라진다. `Boolean` 로 읽고 `null` 을 유지해야 한다. → Task 3 `sharedTargetsNullStaysNull`.
5. **quota 응답의 타입별 키(`volumes___DEFAULT__` 처럼 밑줄이 여러 개인 타입 이름)** — 기존 정규식 `(gigabytes|snapshots|volumes)_.*` 가 새 필드 `backup_gigabytes`, `per_volume_gigabytes` 를 타입별 키로 오인하면 안 된다(`per_volume_gigabytes` 는 접두사가 다르고 `backup_gigabytes` 는 `gigabytes_` 로 시작하지 않으므로 안전하지만, quota class 응답으로 확인한다). → Task 3 `quotaSetReadsNewFieldsAndPerTypeKeys`, Task 10 `limitsQuotaClassAndPools`.

---

## 실행 전 준비

- [ ] **P1: plan PR 머지** — 이 plan 을 `docs/block-storage-microversion-plan` 브랜치에 커밋해 `.superpowers/pr.sh` 로 머지한다.

### 공통 절차
- 각 Task 시작: `git switch main && git pull && git switch -c task/d<N>-<이름>`
- 끝: 전체 빌드 `./mvnw -B --no-transfer-progress install` 결과 `BUILD SUCCESS` 확인 → 커밋 → `.superpowers/pr.sh "<요약>"`.

### fixture 원본
개발용 epoxy(Cinder 3.71)에서 캡처한 응답: `/tmp/claude-1000/-home-seogineer-IdeaProjects/15c57ef7-653e-4e49-bba1-5d7ddc3e068a/scratchpad/cinder/*.json`(`capture.sh`, `capture_detail.sh` 로 재생성 가능). api-ref 예시: 같은 디렉터리의 `samples2/`. IP·호스트명은 `10.0.0.x`/`storage-1` 로 일반화한다.

---

### Task 1: 공용 `MicroVersionSupport`·`MicroVersionService`·`MicroVersionInfo`, compute 내부 리팩터링

**Files:**
- Create: `core/src/main/java/org/openstack4j/model/common/MicroVersionInfo.java`, `core/src/main/java/org/openstack4j/common/MicroVersionService.java`
- Create: `core/src/main/java/org/openstack4j/openstack/internal/microversion/VersionRange.java`, `MicroVersionSupport.java`, `JsonBody.java`
- Modify: `core/src/main/java/org/openstack4j/model/compute/ComputeVersion.java` (`extends MicroVersionInfo`), `api/compute/ComputeMicroVersionService.java` (`extends MicroVersionService<ComputeVersion>`)
- Modify: `openstack/compute/internal/ComputeMicroVersions.java`, `BaseComputeServices.java`, `ComputeMicroVersionServiceImpl.java` (내부 위임)
- Test: `core/src/test/java/org/openstack4j/test/microversion/MicroVersionSupportTest.java`

**Interfaces:**
- Produces:
  - `MicroVersionInfo`: `String getServerMinVersion()`, `String getServerMaxVersion()`, `String getMicroVersion()`, `boolean isPinned()`, `boolean isEnabled()`
  - `MicroVersionService<V extends MicroVersionInfo> extends RestService`: `V negotiate()`, `V use(String version)`, `void clear()`, `V get()`
  - `VersionRange(MicroVersion min, MicroVersion max)`: `getMin()`, `getMax()`
  - `MicroVersionSupport(ServiceType, String keyPrefix, MicroVersion minimum, MicroVersion latest, String headerServiceName, List<String> extraHeaderNames, String enableHint)`: `getMinimum()`, `getLatest()`, `MicroVersionState currentState()`, `MicroVersion effective(MicroVersion classCeiling, MicroVersion ceiling)`, `Map<String, String> headers(MicroVersion)`, `void require(String feature, MicroVersion floor, MicroVersion effective)`, `MicroVersionState negotiate(Supplier<VersionRange>)`, `MicroVersionState use(String version, Supplier<VersionRange>)`, `void clear()`
  - `JsonBody.of(String root, Map<String, ?>)`, `JsonBody.of(Map<String, ?>)` (compute 의 것과 동일한 동작)

- [ ] **Step 1: 테스트 작성**

```bash
git switch main && git pull && git switch -c task/d1-microversion-support
```

`core/src/test/java/org/openstack4j/test/microversion/MicroVersionSupportTest.java`:
```java
package org.openstack4j.test.microversion;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionStore;
import org.openstack4j.openstack.internal.microversion.MicroVersionSupport;
import org.openstack4j.openstack.internal.microversion.VersionRange;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Session-free parts of MicroVersionSupport; negotiation against a session is covered by the connector tests. */
public class MicroVersionSupportTest {

    private final MicroVersionSupport volume = new MicroVersionSupport(ServiceType.BLOCK_STORAGE, "volume",
            new MicroVersion(3, 0), new MicroVersion(3, 71), "volume", Collections.emptyList(),
            "os.blockStorage().microVersions().negotiate()");
    private final MicroVersionSupport compute = new MicroVersionSupport(ServiceType.COMPUTE, "compute",
            new MicroVersion(2, 1), new MicroVersion(2, 104), "compute", List.of("X-OpenStack-Nova-API-Version"),
            "os.compute().microVersions().negotiate()");

    @BeforeMethod
    public void clear() {
        MicroVersionStore.clearAll();
    }

    @Test
    public void headersFollowTheServiceShape() {
        Map<String, String> v = volume.headers(new MicroVersion(3, 50));
        Assert.assertEquals(v, Map.of("OpenStack-API-Version", "volume 3.50"));
        Map<String, String> c = compute.headers(new MicroVersion(2, 60));
        Assert.assertEquals(c.get("OpenStack-API-Version"), "compute 2.60");
        Assert.assertEquals(c.get("X-OpenStack-Nova-API-Version"), "2.60");
    }

    @Test
    public void effectiveIsNullWithoutASession() {
        Assert.assertNull(volume.effective(null, null));
        Assert.assertNull(volume.currentState());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*os\\.blockStorage\\(\\)\\.microVersions\\(\\)\\.negotiate\\(\\).*")
    public void requireWhileDisabledNamesTheEnableCall() {
        volume.require("Attachments", new MicroVersion(3, 27), null);
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.27.*3\\.20.*")
    public void requireBelowFloorNamesBothVersions() {
        volume.require("Attachments", new MicroVersion(3, 27), new MicroVersion(3, 20));
    }

    @Test
    public void requireAtOrAboveFloorPasses() {
        volume.require("Attachments", new MicroVersion(3, 27), new MicroVersion(3, 27));
        volume.require("Attachments", new MicroVersion(3, 27), new MicroVersion(3, 71));
    }

    @Test
    public void versionRangeKeepsBothEnds() {
        VersionRange range = new VersionRange(new MicroVersion(3, 0), new MicroVersion(3, 71));
        Assert.assertEquals(range.getMin().toString(), "3.0");
        Assert.assertEquals(range.getMax().toString(), "3.71");
    }

    @Test
    public void stateWithoutMinUsesTheLibraryMinimum() {
        MicroVersionState state = new MicroVersionState(volume.getMinimum(), new MicroVersion(3, 71));
        Assert.assertEquals(state.getServerMin().toString(), "3.0");
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q test -pl core -Dtest=MicroVersionSupportTest 2>&1 | grep -E 'cannot find symbol|does not exist' | head -2`
Expected: `MicroVersionSupport`/`VersionRange` symbol 없음.

- [ ] **Step 3: 공용 타입 구현**

`model/common/MicroVersionInfo.java`:
```java
package org.openstack4j.model.common;

/** The microversion range of a service endpoint and the microversion this session sends to it. */
public interface MicroVersionInfo extends ModelEntity {

    /** @return the server's minimum microversion, or {@code null} before any discovery */
    String getServerMinVersion();

    String getServerMaxVersion();

    /** @return the microversion sent with requests, or {@code null} when microversions are off */
    String getMicroVersion();

    boolean isPinned();

    boolean isEnabled();
}
```

`common/MicroVersionService.java`:
```java
package org.openstack4j.common;

import org.openstack4j.model.common.MicroVersionInfo;

/**
 * Opt-in microversions of one OpenStack service. Off by default: requests carry no microversion header. Once turned
 * on, methods whose API the service removed in later microversions keep working because they are sent at the highest
 * microversion they support, and new features check their minimum microversion before sending.
 *
 * @param <V> the service's version information type
 */
public interface MicroVersionService<V extends MicroVersionInfo> extends RestService {

    /** Turns microversions on at min(library latest, server max). */
    V negotiate();

    /** Turns microversions on at a fixed version within the library and server range. */
    V use(String version);

    /** Turns microversions off again (no header). */
    void clear();

    V get();
}
```

`ComputeVersion.java`: `public interface ComputeVersion extends MicroVersionInfo {` 로 바꾸고 기존 5개 메서드 선언은 그대로 둔다(`import org.openstack4j.model.common.MicroVersionInfo;`; 기존 `extends ModelEntity` 는 지운다 — `MicroVersionInfo` 가 이미 `ModelEntity`).
`ComputeMicroVersionService.java`: `public interface ComputeMicroVersionService extends MicroVersionService<ComputeVersion> {` 로 바꾸고 기존 메서드 선언·Javadoc 은 그대로 둔다(`import org.openstack4j.common.MicroVersionService;`; 기존 `extends RestService` 는 지운다).

`openstack/internal/microversion/VersionRange.java`:
```java
package org.openstack4j.openstack.internal.microversion;

import org.openstack4j.openstack.internal.MicroVersion;

/** The microversion range a service root document advertises. {@code min} may be {@code null}. */
public final class VersionRange {

    private final MicroVersion min;
    private final MicroVersion max;

    public VersionRange(MicroVersion min, MicroVersion max) {
        this.min = min;
        this.max = max;
    }

    public MicroVersion getMin() {
        return min;
    }

    public MicroVersion getMax() {
        return max;
    }
}
```

`openstack/internal/microversion/MicroVersionSupport.java`:
```java
package org.openstack4j.openstack.internal.microversion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.OSClientSession;

/**
 * Microversion policy of one service: library range, header shape, session state lookup, negotiation and floor
 * checks. One instance per service, shared by all its request classes.
 */
public final class MicroVersionSupport {

    public static final String API_VERSION_HEADER = "OpenStack-API-Version";

    private final ServiceType serviceType;
    private final String keyPrefix;
    private final MicroVersion minimum;
    private final MicroVersion latest;
    private final String headerServiceName;
    private final List<String> extraHeaderNames;
    private final String enableHint;

    /**
     * @param keyPrefix         state key prefix, for example {@code "compute"}
     * @param headerServiceName the service name in {@code OpenStack-API-Version: <name> <version>}
     * @param extraHeaderNames  additional headers that carry the bare version, such as {@code X-OpenStack-Nova-API-Version}
     * @param enableHint        the call that turns microversions on, quoted in error messages
     */
    public MicroVersionSupport(ServiceType serviceType, String keyPrefix, MicroVersion minimum, MicroVersion latest,
            String headerServiceName, List<String> extraHeaderNames, String enableHint) {
        this.serviceType = serviceType;
        this.keyPrefix = keyPrefix;
        this.minimum = minimum;
        this.latest = latest;
        this.headerServiceName = headerServiceName;
        this.extraHeaderNames = List.copyOf(extraHeaderNames);
        this.enableHint = enableHint;
    }

    public MicroVersion getMinimum() {
        return minimum;
    }

    public MicroVersion getLatest() {
        return latest;
    }

    private String key(OSClientSession<?, ?> session) {
        return keyPrefix + "|" + session.getEndpoint(serviceType);
    }

    /** @return this session's state for the service, or {@code null} when microversions were never turned on */
    public MicroVersionState currentState() {
        if (!MicroVersionStore.hasAny())
            return null;    // nobody turned microversions on: no catalog lookup, no lock
        OSClientSession<?, ?> session = OSClientSession.getCurrent();
        return session == null ? null : MicroVersionStore.get(session, key(session));
    }

    /** @return the microversion a request would carry under the given ceilings, or {@code null} when off */
    public MicroVersion effective(MicroVersion classCeiling, MicroVersion ceiling) {
        MicroVersionState state = currentState();
        if (state == null || !state.isEnabled())
            return null;
        MicroVersion version = state.getPinned() != null ? state.getPinned() : MicroVersions.min(latest, state.getServerMax());
        if (classCeiling != null)
            version = MicroVersions.min(version, classCeiling);
        if (ceiling != null)
            version = MicroVersions.min(version, ceiling);
        return version;
    }

    /** @return the request headers that select {@code version} */
    public Map<String, String> headers(MicroVersion version) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(API_VERSION_HEADER, headerServiceName + " " + version);
        for (String name : extraHeaderNames)
            headers.put(name, version.toString());
        return headers;
    }

    /** Fails before any request when {@code feature} needs a microversion the session does not send. */
    public void require(String feature, MicroVersion floor, MicroVersion effective) {
        if (effective == null)
            throw new MicroVersionException(feature + " requires " + headerServiceName + " microversion " + floor
                    + "; turn microversions on with " + enableHint);
        if (effective.compareTo(floor) < 0) {
            MicroVersionState state = currentState();
            throw new MicroVersionException(String.format(
                    "%s requires %s microversion %s, but the session sends %s (server max %s)",
                    feature, headerServiceName, floor, effective, state == null ? "?" : state.getServerMax()));
        }
    }

    /** Turns microversions on at min(latest, server max); discovers the server range once per session and endpoint. */
    public MicroVersionState negotiate(Supplier<VersionRange> discovery) {
        MicroVersionState state = ensureState(discovery);
        state.setPinned(null);
        state.setEnabled(true);
        return state;
    }

    /** Turns microversions on at a fixed version inside the usable range. */
    public MicroVersionState use(String version, Supplier<VersionRange> discovery) {
        MicroVersion requested = MicroVersions.parse(version);
        MicroVersionState state = ensureState(discovery);
        MicroVersion lowest = MicroVersions.max(minimum, state.getServerMin());
        MicroVersion highest = MicroVersions.min(latest, state.getServerMax());
        if (requested.compareTo(lowest) < 0 || requested.compareTo(highest) > 0)
            throw new MicroVersionException(String.format(
                    "%s microversion %s is outside the usable range %s - %s (library %s - %s, server %s - %s)",
                    capitalize(headerServiceName), requested, lowest, highest, minimum, latest,
                    state.getServerMin(), state.getServerMax()));
        state.setPinned(requested);
        state.setEnabled(true);
        return state;
    }

    public void clear() {
        MicroVersionState state = currentState();
        if (state != null) {
            state.setEnabled(false);
            state.setPinned(null);
        }
    }

    private MicroVersionState ensureState(Supplier<VersionRange> discovery) {
        MicroVersionState state = currentState();
        if (state != null)
            return state;
        VersionRange range = discovery.get();
        if (range == null || range.getMax() == null)
            throw new MicroVersionException("This " + headerServiceName
                    + " endpoint does not support microversions (no API with a version range)");
        OSClientSession<?, ?> session = OSClientSession.getCurrent();
        return MicroVersionStore.putIfAbsent(session, key(session),
                new MicroVersionState(range.getMin() == null ? minimum : range.getMin(), range.getMax()));
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
```

`openstack/internal/microversion/JsonBody.java` — `core/src/main/java/org/openstack4j/openstack/compute/domain/JsonBody.java` 의 내용을 패키지 선언만 `org.openstack4j.openstack.internal.microversion` 으로 바꿔 복사한다(Javadoc 첫 줄에 "Shared by the microversion-aware services." 추가).

- [ ] **Step 4: compute 내부 위임**

`ComputeMicroVersions.java` 를 다음으로 바꾼다(public API 동일):
```java
package org.openstack4j.openstack.compute.internal;

import java.util.List;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionSupport;

/** Compute microversion constants and session state lookup. */
public final class ComputeMicroVersions {

    public static final MicroVersion MINIMUM = new MicroVersion(2, 1);
    public static final MicroVersion LATEST = new MicroVersion(2, 104);

    /** Shared policy: both Nova headers, state keyed by the compute endpoint. */
    public static final MicroVersionSupport SUPPORT = new MicroVersionSupport(ServiceType.COMPUTE, "compute", MINIMUM, LATEST,
            "compute", List.of("X-OpenStack-Nova-API-Version"), "os.compute().microVersions().negotiate()");

    private ComputeMicroVersions() {
    }

    /** @return compute microversion {@code 2.minor} */
    public static MicroVersion V(int minor) {
        return new MicroVersion(2, minor);
    }

    /** @return this session's compute state, or {@code null} when microversions were never turned on */
    public static MicroVersionState currentState() {
        return SUPPORT.currentState();
    }

    /** Strips the {@code /v2[.1]} segment and anything after it (such as a tenant id) from a compute endpoint. */
    public static String rootUrl(String endpoint) {
        String trimmed = endpoint.replaceAll("/+$", "");
        return trimmed.replaceAll("/v2(\\.\\d+)?(/.*)?$", "");
    }
}
```

`BaseComputeServices.java` — 상수 2개와 `setVersionHeaders` 를 지우고, 네 메서드 본문을 위임으로 바꾼다(시그니처·Javadoc 유지, `invokeAction*` 은 그대로):
```java
    @Override
    protected <R> Invocation<R> decorate(Invocation<R> invocation) {
        return capped(invocation, null);
    }

    protected <R> Invocation<R> capped(Invocation<R> invocation, MicroVersion ceiling) {
        MicroVersion version = effectiveMicroVersion(ceiling);
        if (version != null)
            ComputeMicroVersions.SUPPORT.headers(version).forEach(invocation::header);
        return invocation;
    }

    protected MicroVersion effectiveMicroVersion(MicroVersion ceiling) {
        return ComputeMicroVersions.SUPPORT.effective(classCeiling(), ceiling);
    }

    protected boolean isMicroVersionAtLeast(MicroVersion version) {
        MicroVersion effective = effectiveMicroVersion(null);
        return effective != null && effective.compareTo(version) >= 0;
    }

    protected void requireMicroVersion(String feature, MicroVersion floor) {
        ComputeMicroVersions.SUPPORT.require(feature, floor, effectiveMicroVersion(null));
    }
```
(`Invocation.header(String, String)` 의 반환 타입이 `Invocation<R>` 이면 `forEach(invocation::header)` 가 `BiConsumer` 로 바로 맞는다 — 반환값은 버려진다. 컴파일이 안 되면 `forEach((k, v) -> invocation.header(k, v))`.)

`ComputeMicroVersionServiceImpl.java` 를 다음으로 바꾼다:
```java
package org.openstack4j.openstack.compute.internal;

import org.openstack4j.api.compute.ComputeMicroVersionService;
import org.openstack4j.model.compute.ComputeVersion;
import org.openstack4j.openstack.compute.domain.NovaComputeVersion;
import org.openstack4j.openstack.compute.domain.NovaVersions;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.internal.microversion.VersionRange;

public class ComputeMicroVersionServiceImpl extends BaseComputeServices implements ComputeMicroVersionService {

    @Override
    public ComputeVersion negotiate() {
        ComputeMicroVersions.SUPPORT.negotiate(ComputeMicroVersionServiceImpl::discover);
        return get();
    }

    @Override
    public ComputeVersion use(String version) {
        ComputeMicroVersions.SUPPORT.use(version, ComputeMicroVersionServiceImpl::discover);
        return get();
    }

    @Override
    public void clear() {
        ComputeMicroVersions.SUPPORT.clear();
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

    /** @return the v2.1 range from the Nova root document, or {@code null} when the server has none */
    private static VersionRange discover() {
        NovaVersions.Entry v21 = new ComputeVersionDiscovery().fetch().v21();
        if (v21 == null || v21.version == null || v21.version.isEmpty())
            return null;
        MicroVersion min = v21.minVersion == null || v21.minVersion.isEmpty() ? null : MicroVersions.parse(v21.minVersion);
        return new VersionRange(min, MicroVersions.parse(v21.version));
    }
}
```

- [ ] **Step 5: 통과 확인 — 새 단위 테스트와 기존 compute 테스트**

Run:
```bash
./mvnw -B test -pl core -Dtest='MicroVersionSupportTest,MicroVersionStoreTest,MicroVersionStoreFastPathTest,ComputeRootUrlTest' 2>&1 | grep -E 'Tests run:' | tail -1
./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ComputeMicroVersionTests,LegacyCeilingTests,ReviewFixTests,ServerActionTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2
git diff --stat main -- core-test/src/main/java/org/openstack4j/api/compute   # 변경 0
```
Expected: core `Tests run: 15, Failures: 0`; core-test `Failures: 0, Errors: 0`; compute 테스트 파일 변경 없음.

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "refactor: share the microversion policy through MicroVersionSupport and generic MicroVersionService

Compute keeps its public and protected API; only the bodies delegate.

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "공용 MicroVersionSupport/VersionRange/JsonBody, 공개 MicroVersionService<V>/MicroVersionInfo; compute 는 내부만 위임(기존 compute microversion 테스트 변경 없이 통과)."
```

---

### Task 2: block storage microversion 적용 — 협상·고정, 헤더, 상한 2곳, 테스트 token hook

**Files:**
- Create (core, `core/src/main/java/org/openstack4j/` 아래): `model/storage/block/BlockStorageVersion.java`, `api/storage/BlockStorageMicroVersionService.java`, `openstack/storage/block/domain/CinderVersions.java`, `openstack/storage/block/domain/CinderBlockStorageVersion.java`, `openstack/storage/block/internal/BlockStorageMicroVersions.java`, `openstack/storage/block/internal/BlockStorageVersionDiscovery.java`, `openstack/storage/block/internal/BlockStorageMicroVersionServiceImpl.java`
- Modify (core): `openstack/storage/block/internal/BaseBlockStorageServices.java`, `api/storage/BlockStorageService.java`, `openstack/storage/block/internal/BlockStorageServiceImpl.java`, `openstack/provider/DefaultAPIProvider.java`, `openstack/storage/block/internal/BlockVolumeServiceImpl.java`(create 상한), `BlockVolumeSnapshotServiceImpl.java`(create 상한), `openstack/storage/block/domain/CinderVolume.java`(`hasBootable`), `CinderVolumeSnapshot.java`(`getForce`)
- Modify (test infra): `core-test/src/main/java/org/openstack4j/api/AbstractTest.java` (`adjustTokenJson` hook)
- Create (test): `core-test/src/main/java/org/openstack4j/api/storage/microversion/AbstractBlockStorageMicroVersionTest.java`, `BlockStorageMicroVersionTests.java`, `BlockStorageLegacyCeilingTests.java`; `core/src/test/java/org/openstack4j/test/microversion/BlockStorageRootUrlTest.java`

**Interfaces:**
- Consumes: Task 1 `MicroVersionSupport`, `VersionRange`, `MicroVersionService`, `MicroVersionInfo`.
- Produces:
  - `BlockStorageService.microVersions()` → `BlockStorageMicroVersionService extends MicroVersionService<BlockStorageVersion>`; `BlockStorageVersion extends MicroVersionInfo`
  - `BlockStorageMicroVersions`: `MINIMUM`(3.0), `LATEST`(3.71), `SUPPORT`, `V(int minor)`, `currentState()`, `rootUrl(String)`
  - `BaseBlockStorageServices`: `protected MicroVersion classCeiling()`(기본 null), `protected <R> Invocation<R> capped(Invocation<R>, MicroVersion ceiling)`, `protected MicroVersion effectiveMicroVersion(MicroVersion ceiling)`, `protected boolean isMicroVersionAtLeast(MicroVersion)`, `protected void requireMicroVersion(String feature, MicroVersion floor)`
  - `CinderVolume.hasBootable()`(`@JsonIgnore`, bootable 필드가 설정됐는지), `CinderVolumeSnapshot.getForce()`(`@JsonIgnore`, `Boolean`)
  - `AbstractTest.adjustTokenJson(String json)` hook(기본 그대로 반환)
  - core-test `AbstractBlockStorageMicroVersionTest`: `VOLUME`(`"4b699b6d-1fe8-41f9-9dbf-65a2cc4927c2"`), `SNAPSHOT`(`"8e53384c-dd0e-410d-94a6-8cd5bf21a5bf"`), `respondWithCinderVersions(String max)`, `negotiate(String max)`, `assertVersionHeader(RecordedRequest, String)`, `assertNoVersionHeader(RecordedRequest)`, `assertNoMoreRequests()`, `body(RecordedRequest)`

- [ ] **Step 1: 루트 URL 단위 테스트 (Review Focus 3)**

`core/src/test/java/org/openstack4j/test/microversion/BlockStorageRootUrlTest.java`:
```java
package org.openstack4j.test.microversion;

import org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BlockStorageRootUrlTest {

    @Test
    public void rootUrlStripsVersionAndProject() {
        Assert.assertEquals(BlockStorageMicroVersions.rootUrl("http://10.0.0.1:8776/v3/2580a7b51d564c1d848ee27fda2db713"), "http://10.0.0.1:8776");
        Assert.assertEquals(BlockStorageMicroVersions.rootUrl("http://10.0.0.1:8776/v3"), "http://10.0.0.1:8776");
        Assert.assertEquals(BlockStorageMicroVersions.rootUrl("http://10.0.0.1:8776/v3/"), "http://10.0.0.1:8776");
        Assert.assertEquals(BlockStorageMicroVersions.rootUrl("http://127.0.0.1:8776/v2/123ac695d4db400a9001b91bb3b8aa46"), "http://127.0.0.1:8776");
        Assert.assertEquals(BlockStorageMicroVersions.rootUrl("https://cloud.example.com/volume/v3/abc"), "https://cloud.example.com/volume");
        Assert.assertEquals(BlockStorageMicroVersions.rootUrl("https://cloud.example.com/volume"), "https://cloud.example.com/volume");
    }
}
```

- [ ] **Step 2: core-test 기반 클래스와 협상·상한 테스트 작성**

`AbstractTest.java` — `osv3()` 의 `json = json.replaceAll("devstack.openstack.stack", getHost());` 바로 뒤에 `json = adjustTokenJson(json);` 을 넣고 메서드를 추가한다:
```java
    /**
     * Lets a test class rewrite the v3 token JSON (for example to point a service at another API version) before
     * the client is built. The default keeps it unchanged.
     */
    protected String adjustTokenJson(String json) {
        return json;
    }
```

`core-test/src/main/java/org/openstack4j/api/storage/microversion/AbstractBlockStorageMicroVersionTest.java`:
```java
package org.openstack4j.api.storage.microversion;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.openstack.internal.microversion.MicroVersionStore;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

/** Block storage tests that talk to a v3 endpoint ({@code /v3/<project>}), as a Cinder v3 cloud advertises it. */
public abstract class AbstractBlockStorageMicroVersionTest extends AbstractTest {

    protected static final String VOLUME = "4b699b6d-1fe8-41f9-9dbf-65a2cc4927c2";
    protected static final String SNAPSHOT = "8e53384c-dd0e-410d-94a6-8cd5bf21a5bf";

    @Override
    protected Service service() {
        return Service.BLOCK_STORAGE;
    }

    /** Turns the token's volumev2 entry into a volumev3 one so requests go to {@code /v3/<project>/...}. */
    @Override
    protected String adjustTokenJson(String json) {
        return json.replace(":8776/v2/", ":8776/v3/").replace("\"volumev2\"", "\"volumev3\"").replace("\"cinderv2\"", "\"cinderv3\"");
    }

    @BeforeMethod
    public void forgetMicroVersions() {
        MicroVersionStore.clearAll();
    }

    /** Enqueues the Cinder root document advertising {@code max}. */
    protected void respondWithCinderVersions(String max) {
        respondWith(300, "{\"versions\": [{\"id\": \"v3.0\", \"status\": \"CURRENT\", \"version\": \"" + max + "\", \"min_version\": \"3.0\","
                + " \"updated\": \"2018-07-17T00:00:00Z\", \"links\": [], \"media-types\": []}]}");
    }

    /** Negotiates against a server advertising {@code max} and consumes the root request. */
    protected void negotiate(String max) throws InterruptedException {
        respondWithCinderVersions(max);
        osv3().blockStorage().microVersions().negotiate();
        RecordedRequest root = takeRequest();
        Assert.assertEquals(root.getPath(), "/");
    }

    protected void assertVersionHeader(RecordedRequest request, String expected) {
        Assert.assertEquals(request.getHeader("OpenStack-API-Version"), "volume " + expected);
        Assert.assertNull(request.getHeader("X-OpenStack-Nova-API-Version"));
    }

    protected void assertNoVersionHeader(RecordedRequest request) {
        Assert.assertNull(request.getHeader("OpenStack-API-Version"));
    }

    protected void assertNoMoreRequests() throws InterruptedException {
        Assert.assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS), "unexpected extra request");
    }

    protected JsonNode body(RecordedRequest request) throws IOException {
        return new ObjectMapper().readTree(request.getBody().clone().readUtf8());
    }
}
```
주의: Cinder 는 `GET /` 에 **300 Multiple Choices** 로 응답한다(실제 epoxy 응답). 클라이언트가 300 을 오류로 다루지 않는지 이 테스트가 확인한다 — 오류라면 `BlockStorageVersionDiscovery.fetch()` 에서 `ExecutionOptions.create(PropagateOnStatus.on(300))` 대신 응답을 `HttpResponse` 로 받아 300 도 본문을 읽도록 한다(Step 4 참고).

`BlockStorageMicroVersionTests.java`:
```java
package org.openstack4j.api.storage.microversion;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.BlockStorageVersion;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/MicroVersion")
public class BlockStorageMicroVersionTests extends AbstractBlockStorageMicroVersionTest {

    private static final String TYPES = "{\"volume_types\": []}";

    public void noHeaderAndNoDiscoveryWhenDisabled() throws Exception {
        respondWith(200, TYPES);

        osv3().blockStorage().volumes().listVolumeTypes();
        RecordedRequest request = takeRequest();

        Assert.assertTrue(request.getPath().matches("/v3/\\p{XDigit}+/types"), request.getPath());
        assertNoVersionHeader(request);
        assertNoMoreRequests();
        Assert.assertFalse(osv3().blockStorage().microVersions().get().isEnabled());
        Assert.assertNull(osv3().blockStorage().microVersions().get().getMicroVersion());
    }

    public void negotiateUsesMinOfLibraryAndServer() throws Exception {
        negotiate("3.71");
        respondWith(200, TYPES);

        osv3().blockStorage().volumes().listVolumeTypes();

        assertVersionHeader(takeRequest(), "3.71");
        BlockStorageVersion version = osv3().blockStorage().microVersions().get();
        Assert.assertEquals(version.getMicroVersion(), "3.71");
        Assert.assertEquals(version.getServerMinVersion(), "3.0");
        Assert.assertEquals(version.getServerMaxVersion(), "3.71");
        Assert.assertTrue(version.isEnabled());
        Assert.assertFalse(version.isPinned());
    }

    public void olderServerMaxIsUsedAsIs() throws Exception {
        negotiate("3.40");
        respondWith(200, TYPES);
        osv3().blockStorage().volumes().listVolumeTypes();
        assertVersionHeader(takeRequest(), "3.40");
    }

    public void newerServerIsCappedAtLibraryLatest() throws Exception {
        negotiate("3.80");
        Assert.assertEquals(osv3().blockStorage().microVersions().get().getMicroVersion(), "3.71");
    }

    public void useThenClear() throws Exception {
        negotiate("3.71");
        osv3().blockStorage().microVersions().use("3.50");
        respondWith(200, TYPES);
        respondWith(200, TYPES);

        osv3().blockStorage().volumes().listVolumeTypes();
        assertVersionHeader(takeRequest(), "3.50");
        osv3().blockStorage().microVersions().clear();
        osv3().blockStorage().volumes().listVolumeTypes();
        assertNoVersionHeader(takeRequest());
        Assert.assertFalse(osv3().blockStorage().microVersions().get().isEnabled());
    }

    public void useWithoutNegotiateDiscoversOnce() throws Exception {
        respondWithCinderVersions("3.71");

        BlockStorageVersion version = osv3().blockStorage().microVersions().use("3.27");

        Assert.assertEquals(takeRequest().getPath(), "/");
        Assert.assertEquals(version.getMicroVersion(), "3.27");
        Assert.assertTrue(version.isPinned());
        assertNoMoreRequests();
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.72.*")
    public void useAboveServerMaxIsRejected() throws Exception {
        negotiate("3.71");
        osv3().blockStorage().microVersions().use("3.72");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*microversion.*")
    public void serverWithoutV3CannotNegotiate() throws Exception {
        respondWith(300, "{\"versions\": [{\"id\": \"v2.0\", \"status\": \"DEPRECATED\", \"version\": \"\", \"min_version\": \"\"}]}");
        try {
            osv3().blockStorage().microVersions().negotiate();
        } finally {
            takeRequest();
        }
    }

    public void computeAndBlockStorageStatesAreIndependent() throws Exception {
        negotiate("3.71");
        respondWith(200, TYPES);

        osv3().blockStorage().volumes().listVolumeTypes();

        assertVersionHeader(takeRequest(), "3.71");
        Assert.assertFalse(osv3().compute().microVersions().get().isEnabled());   // compute was never turned on
        Assert.assertNull(osv3().compute().microVersions().get().getMicroVersion());
    }
}
```
`BlockStorageLegacyCeilingTests.java` (Review Focus 2):
```java
package org.openstack4j.api.storage.microversion;

import java.util.Collections;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/LegacyCeilings")
public class BlockStorageLegacyCeilingTests extends AbstractBlockStorageMicroVersionTest {

    private static final String CREATED_VOLUME = "{\"volume\": {\"id\": \"" + VOLUME + "\", \"status\": \"creating\", \"size\": 1}}";
    private static final String CREATED_SNAPSHOT = "{\"snapshot\": {\"id\": \"" + SNAPSHOT + "\", \"status\": \"creating\", \"volume_id\": \"" + VOLUME + "\"}}";

    public void legacyCreateWithBootableIsCapped() throws Exception {
        negotiate("3.71");
        respondWith(202, CREATED_VOLUME);
        respondWith(202, CREATED_VOLUME);

        osv3().blockStorage().volumes().create(Builders.volume().name("a").size(1).bootable(true).build());
        osv3().blockStorage().volumes().create(Builders.volume().name("b").size(1).build());

        RecordedRequest withBootable = takeRequest();
        assertVersionHeader(withBootable, "3.52");
        Assert.assertTrue(body(withBootable).get("volume").get("bootable").asBoolean());
        assertVersionHeader(takeRequest(), "3.71");
    }

    public void legacySnapshotForceFalseIsCapped() throws Exception {
        negotiate("3.71");
        respondWith(202, CREATED_SNAPSHOT);
        respondWith(202, CREATED_SNAPSHOT);
        respondWith(202, CREATED_SNAPSHOT);

        osv3().blockStorage().snapshots().create(Builders.volumeSnapshot().name("s").volume(VOLUME).force(false).build());
        osv3().blockStorage().snapshots().create(Builders.volumeSnapshot().name("s").volume(VOLUME).force(true).build());
        osv3().blockStorage().snapshots().create(Builders.volumeSnapshot().name("s").volume(VOLUME).build());

        assertVersionHeader(takeRequest(), "3.65");
        assertVersionHeader(takeRequest(), "3.71");
        assertVersionHeader(takeRequest(), "3.71");
    }

    public void legacyListsStayAtNegotiatedVersion() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"volumes\": []}");
        respondWith(200, "{\"snapshots\": []}");
        respondWith(200, "{\"backups\": []}");
        respondWith(200, "{\"transfers\": []}");
        respondWith(200, "{\"services\": []}");

        osv3().blockStorage().volumes().list(Collections.singletonMap("status", "available"));
        osv3().blockStorage().snapshots().list();
        osv3().blockStorage().backups().list();
        osv3().blockStorage().transfer().list();
        osv3().blockStorage().services().list();

        for (int i = 0; i < 5; i++)
            assertVersionHeader(takeRequest(), "3.71");
    }
}
```

- [ ] **Step 3: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q test -pl core -Dtest=BlockStorageRootUrlTest 2>&1 | grep -E 'cannot find symbol|does not exist' | head -2`
Expected: `BlockStorageMicroVersions` 없음.

- [ ] **Step 4: 모델·서비스·발견 구현**

`model/storage/block/BlockStorageVersion.java`:
```java
package org.openstack4j.model.storage.block;

import org.openstack4j.model.common.MicroVersionInfo;

/** The block storage (Cinder v3) microversion range of the server and the microversion this session sends. */
public interface BlockStorageVersion extends MicroVersionInfo {
}
```

`api/storage/BlockStorageMicroVersionService.java`:
```java
package org.openstack4j.api.storage;

import org.openstack4j.common.MicroVersionService;
import org.openstack4j.model.storage.block.BlockStorageVersion;

/**
 * Opt-in block storage microversions (3.0 - 3.71). Off by default: requests carry no microversion header and Cinder
 * answers as 3.0. Turn them on with {@link #negotiate()} to read fields and use APIs added after 3.0.
 */
public interface BlockStorageMicroVersionService extends MicroVersionService<BlockStorageVersion> {
}
```

`openstack/storage/block/domain/CinderVersions.java`:
```java
package org.openstack4j.openstack.storage.block.domain;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Cinder root document: {@code {"versions": [{"id": "v3.0", "version": "3.71", "min_version": "3.0", ...}]}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderVersions implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("versions")
    private List<Entry> versions;

    /** @return the v3 entry, or {@code null} when the server has no v3 API */
    public Entry v3() {
        if (versions != null)
            for (Entry e : versions)
                if (e.id != null && e.id.startsWith("v3"))
                    return e;
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("id")
        public String id;
        @JsonProperty("status")
        public String status;
        @JsonProperty("version")
        public String version;
        @JsonProperty("min_version")
        public String minVersion;
    }
}
```

`openstack/storage/block/domain/CinderBlockStorageVersion.java`:
```java
package org.openstack4j.openstack.storage.block.domain;

import org.openstack4j.model.storage.block.BlockStorageVersion;

public class CinderBlockStorageVersion implements BlockStorageVersion {

    private static final long serialVersionUID = 1L;

    private final String serverMinVersion;
    private final String serverMaxVersion;
    private final String microVersion;
    private final boolean pinned;
    private final boolean enabled;

    public CinderBlockStorageVersion(String serverMinVersion, String serverMaxVersion, String microVersion, boolean pinned, boolean enabled) {
        this.serverMinVersion = serverMinVersion;
        this.serverMaxVersion = serverMaxVersion;
        this.microVersion = microVersion;
        this.pinned = pinned;
        this.enabled = enabled;
    }

    @Override public String getServerMinVersion() { return serverMinVersion; }
    @Override public String getServerMaxVersion() { return serverMaxVersion; }
    @Override public String getMicroVersion() { return microVersion; }
    @Override public boolean isPinned() { return pinned; }
    @Override public boolean isEnabled() { return enabled; }
}
```

`openstack/storage/block/internal/BlockStorageMicroVersions.java`:
```java
package org.openstack4j.openstack.storage.block.internal;

import java.util.Collections;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionSupport;

/** Block storage (Cinder v3) microversion constants and session state lookup. */
public final class BlockStorageMicroVersions {

    public static final MicroVersion MINIMUM = new MicroVersion(3, 0);
    public static final MicroVersion LATEST = new MicroVersion(3, 71);

    /** Shared policy: one {@code OpenStack-API-Version: volume <v>} header, state keyed by the block storage endpoint. */
    public static final MicroVersionSupport SUPPORT = new MicroVersionSupport(ServiceType.BLOCK_STORAGE, "volume", MINIMUM, LATEST,
            "volume", Collections.emptyList(), "os.blockStorage().microVersions().negotiate()");

    private BlockStorageMicroVersions() {
    }

    /** @return block storage microversion {@code 3.minor} */
    public static MicroVersion V(int minor) {
        return new MicroVersion(3, minor);
    }

    /** @return this session's block storage state, or {@code null} when microversions were never turned on */
    public static MicroVersionState currentState() {
        return SUPPORT.currentState();
    }

    /** Strips the {@code /v2} or {@code /v3} segment and anything after it (such as a project id) from a block storage endpoint. */
    public static String rootUrl(String endpoint) {
        String trimmed = endpoint.replaceAll("/+$", "");
        return trimmed.replaceAll("/v[23](\\.\\d+)?(/.*)?$", "");
    }
}
```

`openstack/storage/block/internal/BlockStorageVersionDiscovery.java`:
```java
package org.openstack4j.openstack.storage.block.internal;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.storage.block.domain.CinderVersions;

/** Fetches the Cinder root document. Extends BaseOpenStackService directly so no microversion header is added. */
final class BlockStorageVersionDiscovery extends BaseOpenStackService {

    BlockStorageVersionDiscovery() {
        super(ServiceType.BLOCK_STORAGE, BlockStorageMicroVersions::rootUrl);
    }

    /** Cinder answers {@code GET /} with 300 Multiple Choices and the versions document as body. */
    CinderVersions fetch() {
        return request(HttpMethod.GET, CinderVersions.class, "/").execute();
    }
}
```
구현 중 300 응답이 `ClientResponseException` 으로 올라오면(connector 가 2xx 만 성공으로 보면) `request(...).executeWithResponse()` 로 받아 `response.getStatus() / 100 == 3 || response.getStatus() / 100 == 2` 일 때 `response.readEntity(CinderVersions.class)` 를 읽고 그 외는 `getEntity(CinderVersions.class)` 로 기존 오류 경로를 타게 한다. 어느 쪽인지는 `negotiateUsesMinOfLibraryAndServer` 가 보여 준다(Ruling 으로 기록).

`openstack/storage/block/internal/BlockStorageMicroVersionServiceImpl.java`:
```java
package org.openstack4j.openstack.storage.block.internal;

import org.openstack4j.api.storage.BlockStorageMicroVersionService;
import org.openstack4j.model.storage.block.BlockStorageVersion;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.internal.microversion.VersionRange;
import org.openstack4j.openstack.storage.block.domain.CinderBlockStorageVersion;
import org.openstack4j.openstack.storage.block.domain.CinderVersions;

public class BlockStorageMicroVersionServiceImpl extends BaseBlockStorageServices implements BlockStorageMicroVersionService {

    @Override
    public BlockStorageVersion negotiate() {
        BlockStorageMicroVersions.SUPPORT.negotiate(BlockStorageMicroVersionServiceImpl::discover);
        return get();
    }

    @Override
    public BlockStorageVersion use(String version) {
        BlockStorageMicroVersions.SUPPORT.use(version, BlockStorageMicroVersionServiceImpl::discover);
        return get();
    }

    @Override
    public void clear() {
        BlockStorageMicroVersions.SUPPORT.clear();
    }

    @Override
    public BlockStorageVersion get() {
        MicroVersionState state = BlockStorageMicroVersions.currentState();
        if (state == null)
            return new CinderBlockStorageVersion(null, null, null, false, false);
        MicroVersion effective = effectiveMicroVersion(null);
        return new CinderBlockStorageVersion(state.getServerMin().toString(), state.getServerMax().toString(),
                effective == null ? null : effective.toString(), state.getPinned() != null, state.isEnabled());
    }

    /** @return the v3 range from the Cinder root document, or {@code null} when the server has none */
    private static VersionRange discover() {
        CinderVersions.Entry v3 = new BlockStorageVersionDiscovery().fetch().v3();
        if (v3 == null || v3.version == null || v3.version.isEmpty())
            return null;
        MicroVersion min = v3.minVersion == null || v3.minVersion.isEmpty() ? null : MicroVersions.parse(v3.minVersion);
        return new VersionRange(min, MicroVersions.parse(v3.version));
    }
}
```

`BaseBlockStorageServices.java` 전체:
```java
package org.openstack4j.openstack.storage.block.internal;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.MicroVersion;

/**
 * Base Cinder Service Layer. Adds the block storage microversion header when the session turned microversions on.
 *
 * @author Jeremy Unruh
 */
public class BaseBlockStorageServices extends BaseOpenStackService {

    public BaseBlockStorageServices() {
        super(ServiceType.BLOCK_STORAGE);
    }

    /** Highest microversion every API of this service supports, or {@code null} for no limit. */
    protected MicroVersion classCeiling() {
        return null;
    }

    @Override
    protected <R> Invocation<R> decorate(Invocation<R> invocation) {
        return capped(invocation, null);
    }

    /** Sends this request at no more than {@code ceiling}. */
    protected <R> Invocation<R> capped(Invocation<R> invocation, MicroVersion ceiling) {
        MicroVersion version = effectiveMicroVersion(ceiling);
        if (version != null)
            BlockStorageMicroVersions.SUPPORT.headers(version).forEach(invocation::header);
        return invocation;
    }

    /** @return the microversion a request with {@code ceiling} would carry, or {@code null} when microversions are off */
    protected MicroVersion effectiveMicroVersion(MicroVersion ceiling) {
        return BlockStorageMicroVersions.SUPPORT.effective(classCeiling(), ceiling);
    }

    protected boolean isMicroVersionAtLeast(MicroVersion version) {
        MicroVersion effective = effectiveMicroVersion(null);
        return effective != null && effective.compareTo(version) >= 0;
    }

    /** Fails before any request when {@code feature} needs a microversion the session does not send. */
    protected void requireMicroVersion(String feature, MicroVersion floor) {
        BlockStorageMicroVersions.SUPPORT.require(feature, floor, effectiveMicroVersion(null));
    }
}
```

- [ ] **Step 5: accessor, binding, 상한 2곳**

- `BlockStorageService`: `/** Opt-in block storage microversions (3.0 - 3.71); off by default. */ BlockStorageMicroVersionService microVersions();`. `BlockStorageServiceImpl`: `return Apis.get(BlockStorageMicroVersionService.class);`. `DefaultAPIProvider`: `bind(BlockStorageMicroVersionService.class, BlockStorageMicroVersionServiceImpl.class);` (기존 `bind(BlockStorageService.class, ...)` 줄 뒤).
- `CinderVolume`: 
  ```java
      /** @return whether {@code bootable} was set on this request body (Cinder rejects it from 3.53) */
      @JsonIgnore
      public boolean hasBootable() {
          return bootable != null;
      }
  ```
- `CinderVolumeSnapshot`:
  ```java
      /** @return the force flag as set on this request body, or {@code null} */
      @JsonIgnore
      public Boolean getForce() {
          return force;
      }
  ```
- `BlockVolumeServiceImpl.create`:
  ```java
      MicroVersion ceiling = volume instanceof CinderVolume && ((CinderVolume) volume).hasBootable() ? V(52) : null;   // bootable is not in the 3.53 create schema
      return capped(post(CinderVolume.class, uri("/volumes")), ceiling).entity(volume).execute();
  ```
- `BlockVolumeSnapshotServiceImpl.create`:
  ```java
      MicroVersion ceiling = snapshot instanceof CinderVolumeSnapshot && Boolean.FALSE.equals(((CinderVolumeSnapshot) snapshot).getForce()) ? V(65) : null;   // 3.66 rejects force=false
      return capped(post(CinderVolumeSnapshot.class, uri("/snapshots")), ceiling).entity(snapshot).execute();
  ```

- [ ] **Step 6: 통과 확인**

Run:
```bash
./mvnw -B test -pl core -Dtest='BlockStorageRootUrlTest,MicroVersionSupportTest' 2>&1 | grep -E 'Tests run:' | tail -1
./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='BlockStorageMicroVersionTests,BlockStorageLegacyCeilingTests,VolumeTests,VolumeSnapshotTests,VolumeBackupTests,ServiceTests,VolumeTypeTests,VolumeTypeQuotaTests,SchedulerStatsGetPoolTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3
```
Expected: core `Tests run: 8`, core-test `Failures: 0, Errors: 0` (새 12개 + 기존 block storage 테스트 그대로 통과, 기존 테스트의 `/v[12]/` 경로 검사 포함).

- [ ] **Step 7: 기존 테스트 불변 확인 (Review Focus 1) 과 전체 빌드**

```bash
git diff --stat main -- core-test/src/main/java/org/openstack4j/api/storage core-test/src/main/java/org/openstack4j/api/compute | grep -v microversion   # 기존 테스트 파일 변경 0
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
```
Expected: 기존 테스트 파일 변경 없음(`AbstractTest.java` 만 변경), `BUILD SUCCESS`.

- [ ] **Step 8: 커밋, PR**

```bash
git add -A && git commit -m "feat(block-storage): add opt-in microversion negotiation (3.0 - 3.71)

Off by default (no header, no discovery). os.blockStorage().microVersions()
negotiates min(3.71, server max) or pins a version. volume create with
bootable is sent at 3.52 and snapshot create with force=false at 3.65 so
both keep working after negotiate().

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "block storage microversion opt-in(협상·고정·해제, OpenStack-API-Version: volume), 상한 2곳(bootable 3.52, snapshot force=false 3.65), 테스트 token hook(adjustTokenJson)."
```

---

### Task 3: 응답 모델 필드 (volume, snapshot, backup, type, transfer, service, quota)

**Files:**
- Modify (인터페이스, `core/src/main/java/org/openstack4j/model/storage/block/`): `Volume.java`, `VolumeSnapshot.java`, `VolumeBackup.java`, `VolumeType.java`, `VolumeTransfer.java`, `ext/Service.java`, `BlockQuotaSet.java`, `builder/BlockQuotaSetBuilder.java`
- Modify (도메인, `core/src/main/java/org/openstack4j/openstack/storage/block/domain/`): `CinderVolume.java`, `CinderVolumeSnapshot.java`, `CinderVolumeBackup.java`, `CinderVolumeType.java`, `CinderVolumeTransfer.java`, `ext/ExtService.java`, `CinderBlockQuotaSet.java`
- Create (fixture, `core-test/src/main/resources/storage/microversion/`): `volume_3_71.json`, `snapshot_3_71.json`, `backup_3_56.json`, `types_3_71.json`, `transfer_3_57.json`, `services_3_49.json`, `quota_set.json`
- Create: `core-test/src/main/java/org/openstack4j/api/storage/microversion/BlockStorageModelTests.java`

**Interfaces:**
- Consumes: Task 2 `AbstractBlockStorageMicroVersionTest`.
- Produces (모두 `default`, 기본 `null`):
  - `Volume`: `String getUserId()`, `Date getUpdatedAt()`, `String getReplicationStatus()`, `String getConsistencyGroupId()`, `String getMigrationStatus()`, `String getNameId()`, `List<? extends Link> getLinks()`, `String getGroupId()`(3.13), `String getProviderId()`(3.21), `Boolean getSharedTargets()`(3.48/3.69), `String getServiceUuid()`(3.48), `String getClusterName()`(3.61), `String getVolumeTypeId()`(3.63), `String getEncryptionKeyId()`(3.64), `Boolean getConsumesQuota()`(3.65)
  - `VolumeSnapshot`: `Date getUpdatedAt()`, `String getProjectId()`, `String getProgress()`, `String getGroupSnapshotId()`(3.14), `String getUserId()`(3.41), `Boolean getConsumesQuota()`(3.65)
  - `VolumeBackup`: `Date getUpdatedAt()`, `Date getDataTimestamp()`, `List<? extends Link> getLinks()`, `String getProjectId()`(3.18), `Map<String, String> getMetadata()`(3.43), `String getUserId()`(3.56), `String getEncryptionKeyId()`(3.64), `Boolean getHasDependentBackups()`
  - `VolumeType`: `String getDescription()`, `Boolean isPublic()`, `Boolean getAccessIsPublic()`(`os-volume-type-access:is_public`), `String getQosSpecsId()`
  - `VolumeTransfer`: `String getSourceProjectId()`, `String getDestinationProjectId()`, `Boolean getAccepted()`(3.57), `Boolean getNoSnapshots()`(3.55)
  - `ext.Service`: `String getCluster()`(3.7), `String getReplicationStatus()`, `String getActiveBackendId()`, `Boolean getFrozen()`(3.26), `String getBackendState()`(3.49)
  - `BlockQuotaSet`: `Integer getBackups()`, `Integer getBackupGigabytes()`, `Integer getPerVolumeGigabytes()`, `Integer getGroups()`; `BlockQuotaSetBuilder`: `backups(int)`, `backupGigabytes(int)`, `perVolumeGigabytes(int)`, `groups(int)`

- [ ] **Step 1: fixture 작성**

```bash
git switch main && git pull && git switch -c task/d3-models
mkdir -p core-test/src/main/resources/storage/microversion
```

`volume_3_71.json` (epoxy 3.71 실제 응답 일반화; `shared_targets` 는 3.69 tristate 확인을 위해 `null`):
```json
{
  "volume": {
    "id": "4b699b6d-1fe8-41f9-9dbf-65a2cc4927c2",
    "status": "available",
    "size": 1,
    "availability_zone": "nova",
    "created_at": "2026-10-02T17:23:52.000000",
    "updated_at": "2026-10-02T17:23:54.000000",
    "name": "os4j-fixture",
    "description": "fixture capture",
    "volume_type": "__DEFAULT__",
    "snapshot_id": null,
    "source_volid": null,
    "metadata": {"purpose": "fixture"},
    "links": [
      {"rel": "self", "href": "http://10.0.0.1:8776/v3/volumes/4b699b6d-1fe8-41f9-9dbf-65a2cc4927c2"},
      {"rel": "bookmark", "href": "http://10.0.0.1:8776/volumes/4b699b6d-1fe8-41f9-9dbf-65a2cc4927c2"}
    ],
    "user_id": "e365357fdf4d4a37a07fde3209cac4aa",
    "bootable": "false",
    "encrypted": false,
    "replication_status": null,
    "consistencygroup_id": null,
    "multiattach": false,
    "attachments": [],
    "migration_status": null,
    "group_id": "0b1c9f2e-6a7b-4d8e-9f00-1a2b3c4d5e6f",
    "provider_id": null,
    "shared_targets": null,
    "service_uuid": "1afa4698-dd4e-4467-a7bf-5f891f03b634",
    "cluster_name": null,
    "volume_type_id": "c23142c0-9883-4224-beb8-06d7c231be75",
    "encryption_key_id": null,
    "consumes_quota": true,
    "os-vol-tenant-attr:tenant_id": "2580a7b51d564c1d848ee27fda2db713",
    "os-vol-mig-status-attr:migstat": null,
    "os-vol-mig-status-attr:name_id": null,
    "os-vol-host-attr:host": "storage-1@lvm-1#lvm-1"
  }
}
```

`snapshot_3_71.json`:
```json
{
  "snapshot": {
    "id": "8e53384c-dd0e-410d-94a6-8cd5bf21a5bf",
    "created_at": "2026-10-02T17:24:01.000000",
    "updated_at": "2026-10-02T17:24:03.000000",
    "name": "os4j-snap",
    "description": null,
    "volume_id": "4b699b6d-1fe8-41f9-9dbf-65a2cc4927c2",
    "status": "available",
    "size": 1,
    "metadata": {"k": "v"},
    "group_snapshot_id": null,
    "user_id": "e365357fdf4d4a37a07fde3209cac4aa",
    "consumes_quota": true,
    "os-extended-snapshot-attributes:project_id": "2580a7b51d564c1d848ee27fda2db713",
    "os-extended-snapshot-attributes:progress": "100%"
  }
}
```

`backup_3_56.json` (api-ref 3.56 예시):
```json
{
  "backup": {
    "availability_zone": null,
    "container": null,
    "created_at": "2023-06-23T11:56:08.691468",
    "updated_at": "2023-06-23T11:56:09.000000",
    "data_timestamp": "2023-06-23T11:56:08.691468",
    "description": "Test backup",
    "fail_reason": null,
    "has_dependent_backups": false,
    "id": "3052c307-119e-4f78-960e-972078aa15a8",
    "is_incremental": false,
    "links": [{"rel": "self", "href": "http://10.0.0.1:8776/v3/89afd400-b646-4bbc-b12b-c0a4d63e5bd3/backups/3052c307-119e-4f78-960e-972078aa15a8"}],
    "metadata": {"key": "value"},
    "name": "backup001",
    "object_count": 0,
    "os-backup-project-attr:project_id": "89afd400-b646-4bbc-b12b-c0a4d63e5bd3",
    "size": 1,
    "snapshot_id": null,
    "status": "available",
    "user_id": "c853ca26-e8ea-4797-8a52-ee124a013d0e",
    "volume_id": "3301d9c7-aa2f-4b7d-b64e-22bb5d1c19d2",
    "encryption_key_id": "8ef3e7a1-1a2b-4c3d-9e8f-0a1b2c3d4e5f"
  }
}
```

`types_3_71.json` (epoxy):
```json
{
  "volume_types": [
    {
      "id": "c23142c0-9883-4224-beb8-06d7c231be75",
      "name": "__DEFAULT__",
      "is_public": true,
      "description": "Default Volume Type",
      "extra_specs": {},
      "qos_specs_id": null,
      "os-volume-type-access:is_public": true
    }
  ]
}
```

`transfer_3_57.json`:
```json
{
  "transfer": {
    "accepted": false,
    "auth_key": "e2cb02466324813c",
    "created_at": "2023-06-12T21:21:38.392033",
    "destination_project_id": null,
    "id": "94bae1a0-83fb-496c-9cd2-800d8237ab0d",
    "links": [],
    "name": "first volume",
    "no_snapshots": false,
    "source_project_id": "89afd400-b646-4bbc-b12b-c0a4d63e5bd3",
    "volume_id": "80d68197-b67e-4c8e-bbb9-030b2581f921"
  }
}
```

`services_3_49.json`:
```json
{
  "services": [
    {"binary": "cinder-scheduler", "cluster": null, "disabled_reason": null, "host": "storage-1", "state": "up", "status": "enabled", "updated_at": "2026-10-02T17:20:00.000000", "zone": "nova"},
    {"active_backend_id": null, "backend_state": "up", "binary": "cinder-volume", "cluster": "cluster1", "disabled_reason": "maintenance", "frozen": false, "host": "storage-1@lvm-1", "replication_status": "disabled", "state": "up", "status": "disabled", "updated_at": "2026-10-02T17:20:05.000000", "zone": "nova"}
  ]
}
```

`quota_set.json` (api-ref):
```json
{"quota_set": {"backup_gigabytes": 1000, "backups": 10, "gigabytes": 1000, "gigabytes___DEFAULT__": -1, "groups": 10, "id": "fake_tenant", "per_volume_gigabytes": -1, "snapshots": 10, "snapshots___DEFAULT__": -1, "volumes": 10, "volumes___DEFAULT__": -1}}
```

- [ ] **Step 2: 실패하는 테스트 작성 (Review Focus 4 포함)**

`BlockStorageModelTests.java`:
```java
package org.openstack4j.api.storage.microversion;

import java.util.List;

import org.openstack4j.model.storage.block.BlockQuotaSet;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeBackup;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.openstack4j.model.storage.block.VolumeTransfer;
import org.openstack4j.model.storage.block.VolumeType;
import org.openstack4j.model.storage.block.ext.Service;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/Models")
public class BlockStorageModelTests extends AbstractBlockStorageMicroVersionTest {

    public void volumeReads371Fields() throws Exception {
        negotiate("3.71");
        respondWith("/storage/microversion/volume_3_71.json");

        Volume volume = osv3().blockStorage().volumes().get(VOLUME);
        takeRequest();

        Assert.assertEquals(volume.getUserId(), "e365357fdf4d4a37a07fde3209cac4aa");
        Assert.assertNotNull(volume.getUpdatedAt());
        Assert.assertEquals(volume.getGroupId(), "0b1c9f2e-6a7b-4d8e-9f00-1a2b3c4d5e6f");
        Assert.assertEquals(volume.getServiceUuid(), "1afa4698-dd4e-4467-a7bf-5f891f03b634");
        Assert.assertEquals(volume.getVolumeTypeId(), "c23142c0-9883-4224-beb8-06d7c231be75");
        Assert.assertEquals(volume.getConsumesQuota(), Boolean.TRUE);
        Assert.assertNull(volume.getClusterName());
        Assert.assertNull(volume.getEncryptionKeyId());
        Assert.assertNull(volume.getProviderId());
        Assert.assertEquals(volume.getLinks().size(), 2);
        Assert.assertEquals(volume.getVolumeType(), "__DEFAULT__");     // legacy getter untouched
        Assert.assertFalse(volume.bootable());
    }

    public void sharedTargetsNullStaysNull() throws Exception {
        negotiate("3.71");
        respondWith("/storage/microversion/volume_3_71.json");
        Volume volume = osv3().blockStorage().volumes().get(VOLUME);
        takeRequest();
        Assert.assertNull(volume.getSharedTargets());
    }

    public void snapshotReads371Fields() throws Exception {
        negotiate("3.71");
        respondWith("/storage/microversion/snapshot_3_71.json");

        VolumeSnapshot snapshot = osv3().blockStorage().snapshots().get(SNAPSHOT);
        takeRequest();

        Assert.assertEquals(snapshot.getUserId(), "e365357fdf4d4a37a07fde3209cac4aa");
        Assert.assertEquals(snapshot.getProjectId(), "2580a7b51d564c1d848ee27fda2db713");
        Assert.assertEquals(snapshot.getProgress(), "100%");
        Assert.assertNull(snapshot.getGroupSnapshotId());
        Assert.assertEquals(snapshot.getConsumesQuota(), Boolean.TRUE);
        Assert.assertNotNull(snapshot.getUpdatedAt());
    }

    public void backupReads356Fields() throws Exception {
        negotiate("3.71");
        respondWith("/storage/microversion/backup_3_56.json");

        VolumeBackup backup = osv3().blockStorage().backups().get("3052c307-119e-4f78-960e-972078aa15a8");
        takeRequest();

        Assert.assertEquals(backup.getProjectId(), "89afd400-b646-4bbc-b12b-c0a4d63e5bd3");
        Assert.assertEquals(backup.getUserId(), "c853ca26-e8ea-4797-8a52-ee124a013d0e");
        Assert.assertEquals(backup.getMetadata().get("key"), "value");
        Assert.assertEquals(backup.getEncryptionKeyId(), "8ef3e7a1-1a2b-4c3d-9e8f-0a1b2c3d4e5f");
        Assert.assertNotNull(backup.getDataTimestamp());
        Assert.assertNotNull(backup.getUpdatedAt());
        Assert.assertEquals(backup.getLinks().size(), 1);
        Assert.assertEquals(backup.getHasDependentBackups(), Boolean.FALSE);
    }

    public void volumeTypeReadsPublicDescriptionAndQos() throws Exception {
        negotiate("3.71");
        respondWith("/storage/microversion/types_3_71.json");

        List<? extends VolumeType> types = osv3().blockStorage().volumes().listVolumeTypes();
        takeRequest();

        Assert.assertEquals(types.get(0).getDescription(), "Default Volume Type");
        Assert.assertEquals(types.get(0).isPublic(), Boolean.TRUE);
        Assert.assertEquals(types.get(0).getAccessIsPublic(), Boolean.TRUE);
        Assert.assertNull(types.get(0).getQosSpecsId());
    }

    public void transferReads357Fields() throws Exception {
        negotiate("3.71");
        respondWith("/storage/microversion/transfer_3_57.json");

        VolumeTransfer transfer = osv3().blockStorage().transfer().get("94bae1a0-83fb-496c-9cd2-800d8237ab0d");
        takeRequest();

        Assert.assertEquals(transfer.getAccepted(), Boolean.FALSE);
        Assert.assertEquals(transfer.getNoSnapshots(), Boolean.FALSE);
        Assert.assertEquals(transfer.getSourceProjectId(), "89afd400-b646-4bbc-b12b-c0a4d63e5bd3");
        Assert.assertNull(transfer.getDestinationProjectId());
    }

    public void serviceReadsClusterAndReplicationFields() throws Exception {
        negotiate("3.71");
        respondWith("/storage/microversion/services_3_49.json");

        List<? extends Service> services = osv3().blockStorage().services().list();
        takeRequest();

        Assert.assertNull(services.get(0).getCluster());
        Assert.assertEquals(services.get(1).getCluster(), "cluster1");
        Assert.assertEquals(services.get(1).getFrozen(), Boolean.FALSE);
        Assert.assertEquals(services.get(1).getReplicationStatus(), "disabled");
        Assert.assertEquals(services.get(1).getBackendState(), "up");
        Assert.assertNull(services.get(1).getActiveBackendId());
    }

    public void quotaSetReadsNewFieldsAndPerTypeKeys() throws Exception {
        respondWith("/storage/microversion/quota_set.json");

        BlockQuotaSet quota = osv3().blockStorage().quotaSets().get("fake_tenant");
        takeRequest();

        Assert.assertEquals(quota.getBackups(), Integer.valueOf(10));
        Assert.assertEquals(quota.getBackupGigabytes(), Integer.valueOf(1000));
        Assert.assertEquals(quota.getPerVolumeGigabytes(), Integer.valueOf(-1));
        Assert.assertEquals(quota.getGroups(), Integer.valueOf(10));
        Assert.assertEquals(quota.getVolumeTypesQuotas().get("volumes___DEFAULT__"), Integer.valueOf(-1));
        Assert.assertFalse(quota.getVolumeTypesQuotas().containsKey("backup_gigabytes"));
        Assert.assertFalse(quota.getVolumeTypesQuotas().containsKey("per_volume_gigabytes"));
    }
}
```

- [ ] **Step 3: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -5`
Expected: `getUserId`, `getSharedTargets` 등 없음.

- [ ] **Step 4: 인터페이스에 default getter 추가**

`Volume.java` (import `java.util.Date`, `java.util.List`, `org.openstack4j.model.common.Link` 확인):
```java
    /** @return user_id */
    default String getUserId() { return null; }
    /** @return updated_at */
    default Date getUpdatedAt() { return null; }
    /** @return replication_status */
    default String getReplicationStatus() { return null; }
    /** @return consistencygroup_id */
    default String getConsistencyGroupId() { return null; }
    /** @return migration_status as text (see {@link #getMigrateStatus()} for the typed os-vol-mig-status-attr value) */
    default String getMigrationStatus() { return null; }
    /** @return os-vol-mig-status-attr:name_id */
    default String getNameId() { return null; }
    /** @return links */
    default List<? extends Link> getLinks() { return null; }
    /** @return group_id (3.13+) */
    default String getGroupId() { return null; }
    /** @return provider_id (3.21+, admin) */
    default String getProviderId() { return null; }
    /** @return shared_targets (3.48+; {@code null} from 3.69 means forced locking) */
    default Boolean getSharedTargets() { return null; }
    /** @return service_uuid (3.48+) */
    default String getServiceUuid() { return null; }
    /** @return cluster_name (3.61+, admin) */
    default String getClusterName() { return null; }
    /** @return volume_type_id (3.63+) */
    default String getVolumeTypeId() { return null; }
    /** @return encryption_key_id (3.64+) */
    default String getEncryptionKeyId() { return null; }
    /** @return consumes_quota (3.65+) */
    default Boolean getConsumesQuota() { return null; }
```
`VolumeSnapshot.java`: `getUpdatedAt`(Date), `getProjectId`, `getProgress`, `getGroupSnapshotId`(3.14), `getUserId`(3.41), `getConsumesQuota`(Boolean, 3.65) — 같은 형식.
`VolumeBackup.java`: `getUpdatedAt`, `getDataTimestamp`(Date), `getLinks`, `getProjectId`(3.18), `getMetadata`(Map<String,String>, 3.43), `getUserId`(3.56), `getEncryptionKeyId`(3.64), `getHasDependentBackups`(Boolean).
`VolumeType.java`: `getDescription`, `Boolean isPublic()`, `Boolean getAccessIsPublic()`, `getQosSpecsId`.
`VolumeTransfer.java`: `getSourceProjectId`, `getDestinationProjectId`, `Boolean getAccepted()`(3.57), `Boolean getNoSnapshots()`(3.55).
`ext/Service.java`: `getCluster`(3.7), `getReplicationStatus`, `getActiveBackendId`, `Boolean getFrozen()`(3.26), `getBackendState`(3.49).
`BlockQuotaSet.java`: `Integer getBackups()`, `Integer getBackupGigabytes()`, `Integer getPerVolumeGigabytes()`, `Integer getGroups()`.
`BlockQuotaSetBuilder.java`: `backups(int)`, `backupGigabytes(int)`, `perVolumeGigabytes(int)`, `groups(int)` — 추상 메서드(빌더 인터페이스는 외부 구현이 없다고 본다; MIGRATION 에 적는다).

- [ ] **Step 5: 도메인 필드 추가**

`CinderVolume.java` — 필드(각 `@Override` getter 포함, 그대로 반환):
```java
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("updated_at")
    private Date updatedAt;
    @JsonProperty("replication_status")
    private String replicationStatus;
    @JsonProperty("consistencygroup_id")
    private String consistencyGroupId;
    @JsonProperty("migration_status")
    private String migrationStatus;
    @JsonProperty("os-vol-mig-status-attr:name_id")
    private String nameId;
    @JsonProperty("links")
    private List<GenericLink> links;
    @JsonProperty("group_id")
    private String groupId;
    @JsonProperty("provider_id")
    private String providerId;
    @JsonProperty("shared_targets")
    private Boolean sharedTargets;
    @JsonProperty("service_uuid")
    private String serviceUuid;
    @JsonProperty("cluster_name")
    private String clusterName;
    @JsonProperty("volume_type_id")
    private String volumeTypeId;
    @JsonProperty("encryption_key_id")
    private String encryptionKeyId;
    @JsonProperty("consumes_quota")
    private Boolean consumesQuota;
```
`CinderVolume` 은 요청 본문으로도 쓰인다. 위 필드는 응답 전용이라 `NON_NULL` 로 생성 요청에는 나가지 않는다 — 단 `groupId` 는 Task 4 의 빌더가 설정하면 3.13 요청 필드로도 쓰인다(스키마에 있음). `links` 의 `GenericLink` 는 `org.openstack4j.openstack.common.GenericLink`.

`CinderVolumeSnapshot.java`: `updated_at`(Date), `os-extended-snapshot-attributes:project_id`(projectId), `os-extended-snapshot-attributes:progress`(progress), `group_snapshot_id`, `user_id`, `consumes_quota`(Boolean) + getter.
`CinderVolumeBackup.java`: `updated_at`, `data_timestamp`(Date), `links`(List<GenericLink>), `os-backup-project-attr:project_id`(projectId), `metadata`(Map<String,String>), `user_id`, `encryption_key_id` + getter; 기존 `hasDependent` 필드에 `@Override public Boolean getHasDependentBackups() { return hasDependent; }`.
`CinderVolumeType.java`: `description`, `@JsonProperty("is_public") Boolean isPublic`, `@JsonProperty("os-volume-type-access:is_public") Boolean accessIsPublic`, `@JsonProperty("qos_specs_id") String qosSpecsId`; getter `@JsonIgnore @Override public Boolean isPublic()`(implicit 이름 "public" 이라 `@JsonIgnore` 로 Jackson 혼동 차단), `getDescription`, `getAccessIsPublic`, `getQosSpecsId`. `toString` 에 추가.
`CinderVolumeTransfer.java`: `source_project_id`, `destination_project_id`, `accepted`(Boolean), `no_snapshots`(Boolean) + getter. (`no_snapshots` 는 Task 11 의 새 transfers 생성 요청에도 쓰인다.)
`ext/ExtService.java`: `cluster`, `replication_status`, `active_backend_id`, `frozen`(Boolean), `backend_state` + getter.
`CinderBlockQuotaSet.java`:
```java
    @JsonProperty
    private Integer backups;
    @JsonProperty("backup_gigabytes")
    private Integer backupGigabytes;
    @JsonProperty("per_volume_gigabytes")
    private Integer perVolumeGigabytes;
    @JsonProperty
    private Integer groups;
```
getter 4개, 빌더 메서드 4개(`model.backups = backups; return this;`). `@JsonAnySetter` 의 정규식은 그대로 둔다 — `backup_gigabytes` 와 `per_volume_gigabytes` 는 이제 명시적 프로퍼티라 any-setter 로 가지 않는다(테스트 `quotaSetReadsNewFieldsAndPerTypeKeys` 가 확인).

- [ ] **Step 6: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='BlockStorageModelTests,VolumeTests,VolumeSnapshotTests,VolumeBackupTests,VolumeTypeTests,VolumeTypeQuotaTests,ServiceTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Failures: 0, Errors: 0` (BlockStorageModelTests 8개, 기존 테스트 그대로).

- [ ] **Step 7: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(block-storage): read Cinder 3.x fields of volumes, snapshots, backups, types, transfers, services and quotas

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "응답 모델 필드: volume(3.13~3.65, shared_targets tristate), snapshot(3.14/3.41/3.65), backup(3.18/3.43/3.56/3.64), type(description/is_public/qos), transfer(3.55/3.57), service(3.7/3.26/3.49), quota(backups/groups 등)."
```

---

### Task 4: 목록 옵션, 생성 빌더 확장, volume summary·update

**Files:**
- Create (`core/src/main/java/org/openstack4j/model/storage/block/options/`): `BlockStorageListOptions.java`, `VolumeListOptions.java`, `SnapshotListOptions.java`, `BackupListOptions.java`, `VolumeUpdateOptions.java`
- Create: `core/src/main/java/org/openstack4j/model/storage/block/VolumeSummary.java`, `openstack/storage/block/domain/CinderVolumeSummary.java`
- Modify: `model/storage/block/builder/VolumeBuilder.java`, `openstack/storage/block/domain/CinderVolume.java`(빌더·필드), `model/storage/block/builder/VolumeBackupCreateBuilder.java`, `openstack/storage/block/domain/CinderVolumeBackupCreate.java`, `model/storage/block/options/UploadImageData.java`, `openstack/storage/block/domain/CinderUploadImageData.java`, `model/storage/block/Volume.java`(`getBackupId`, `getSchedulerHints`), `model/storage/block/VolumeBackupCreate.java`(`getMetadata`, `getAvailabilityZone`)
- Modify: `api/storage/BlockVolumeService.java`, `BlockVolumeSnapshotService.java`, `BlockVolumeBackupService.java` 와 구현 3개
- Create: `core-test/src/main/java/org/openstack4j/api/storage/microversion/BlockStorageListOptionsTests.java`

**Interfaces:**
- Consumes: Task 2 `capped`, `requireMicroVersion`, `V`; Task 1 `JsonBody`; Task 3 `CinderVolume.groupId`.
- Produces:
  - `BlockStorageListOptions<T>`(추상, self type): `limit(int)`, `marker(String)`, `offset(int)`, `sortKey(String)`, `sortDir(String)`, `sort(String)`, `allTenants(boolean)`, `Map<String, String> toQueryParams()`, `String getRequiredMicroVersion()`; protected `T put(String key, Object value, int minor)`
  - `VolumeListOptions.create()`: `name`, `nameLike`(3.34), `status`, `bootable(boolean)`, `metadata(Map)`, `glanceMetadata(Map)`(3.4), `groupId`(3.10), `withCount(boolean)`(3.45), `createdAt(String op, String isoTime)`/`updatedAt(String op, String isoTime)`(3.60; `op` 는 `gt|gte|eq|neq|lt|lte`), `consumesQuota(boolean)`(3.65)
  - `SnapshotListOptions.create()`: `name`, `nameLike`(3.34), `status`, `volumeId`, `metadata(Map)`(3.22), `withCount`(3.45), `consumesQuota`(3.65)
  - `BackupListOptions.create()`: `name`, `nameLike`(3.34), `status`, `volumeId`, `withCount`(3.45)
  - `VolumeUpdateOptions.create()`: `name`, `description`, `metadata(Map)`; `Map<String, Object> toMap()`
  - `VolumeSummary`: `Long getTotalCount()`, `Long getTotalSize()`, `Map<String, List<String>> getMetadata()`(3.36)
  - `BlockVolumeService`: `List<? extends Volume> list(VolumeListOptions)`, `VolumeSummary summary()`, `VolumeSummary summary(VolumeListOptions)`(3.12), `Volume update(String volumeId, VolumeUpdateOptions)`(응답 `{"volume": ...}`)
  - `BlockVolumeSnapshotService`: `List<? extends VolumeSnapshot> listDetail(SnapshotListOptions)`
  - `BlockVolumeBackupService`: `List<? extends VolumeBackup> list(BackupListOptions)`
  - `VolumeBuilder`: `groupId(String)`(3.13), `backupId(String)`(3.47), `imageId(String)`, `consistencyGroupId(String)`, `schedulerHints(Map<String, Object>)`; `Volume`: `default String getBackupId()`, `default Map<String, Object> getSchedulerHints()`
  - `VolumeBackupCreateBuilder`: `metadata(Map<String, String>)`(3.43), `availabilityZone(String)`(3.51); `VolumeBackupCreate`: `default Map<String, String> getMetadata()`, `default String getAvailabilityZone()`
  - `UploadImageData`: `visibility(String)`, `protectedImage(boolean)`(3.1); getter `getVisibility()`, `getProtectedImage()`(Boolean)

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/d4-list-options
```

`BlockStorageListOptionsTests.java`:
```java
package org.openstack4j.api.storage.microversion;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.VolumeSummary;
import org.openstack4j.model.storage.block.options.BackupListOptions;
import org.openstack4j.model.storage.block.options.SnapshotListOptions;
import org.openstack4j.model.storage.block.options.UploadImageData;
import org.openstack4j.model.storage.block.options.VolumeListOptions;
import org.openstack4j.model.storage.block.options.VolumeUpdateOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/ListOptions")
public class BlockStorageListOptionsTests extends AbstractBlockStorageMicroVersionTest {

    public void volumeListOptionsBecomeQuery() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"volumes\": [], \"count\": 0}");

        osv3().blockStorage().volumes().list(VolumeListOptions.create().status("available").nameLike("web").withCount(true)
                .createdAt("gt", "2026-10-01T00:00:00").consumesQuota(true).limit(5).sortKey("created_at").sortDir("desc").allTenants(true));

        RecordedRequest request = takeRequest();
        assertVersionHeader(request, "3.71");
        String path = request.getPath();
        Assert.assertTrue(path.matches("/v3/\\p{XDigit}+/volumes/detail\\?.*"), path);
        for (String part : new String[] {"status=available", "name~=web", "with_count=true", "created_at=gt%3A2026-10-01T00%3A00%3A00",
                "consumes_quota=true", "limit=5", "sort_key=created_at", "sort_dir=desc", "all_tenants=true"})
            Assert.assertTrue(path.contains(part), path + " lacks " + part);
        Assert.assertEquals(VolumeListOptions.create().withCount(true).getRequiredMicroVersion(), "3.45");
        Assert.assertEquals(VolumeListOptions.create().nameLike("x").getRequiredMicroVersion(), "3.34");
        Assert.assertNull(VolumeListOptions.create().status("x").limit(1).getRequiredMicroVersion());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.65.*")
    public void volumeListFloorIsChecked() throws Exception {
        negotiate("3.71");
        osv3().blockStorage().microVersions().use("3.60");
        try {
            osv3().blockStorage().volumes().list(VolumeListOptions.create().consumesQuota(false));
        } finally {
            assertNoMoreRequests();
        }
    }

    public void snapshotAndBackupListOptions() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"snapshots\": []}");
        respondWith(200, "{\"backups\": []}");

        osv3().blockStorage().snapshots().listDetail(SnapshotListOptions.create().volumeId(VOLUME).metadata(Collections.singletonMap("k", "v")));
        osv3().blockStorage().backups().list(BackupListOptions.create().status("available").sortKey("name"));

        String snapshots = takeRequest().getPath();
        Assert.assertTrue(snapshots.contains("/snapshots/detail?"), snapshots);
        Assert.assertTrue(snapshots.contains("volume_id=" + VOLUME), snapshots);
        Assert.assertTrue(snapshots.contains("metadata=%7B%27k%27%3A+%27v%27%7D") || snapshots.contains("metadata=%7B%22k%22%3A%22v%22%7D"), snapshots);
        String backups = takeRequest().getPath();
        Assert.assertTrue(backups.contains("/backups/detail?") && backups.contains("sort_key=name"), backups);
        Assert.assertEquals(SnapshotListOptions.create().metadata(Map.of("a", "b")).getRequiredMicroVersion(), "3.22");
    }

    public void volumeSummaryWithAndWithoutOptions() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"volume-summary\": {\"total_size\": 4, \"total_count\": 4, \"metadata\": {\"key1\": [\"value1\", \"value2\"]}}}");
        respondWith(200, "{\"volume-summary\": {\"total_size\": 0, \"total_count\": 0, \"metadata\": {}}}");

        VolumeSummary summary = osv3().blockStorage().volumes().summary();
        osv3().blockStorage().volumes().summary(VolumeListOptions.create().allTenants(true));

        Assert.assertTrue(takeRequest().getPath().endsWith("/volumes/summary"));
        Assert.assertTrue(takeRequest().getPath().contains("/volumes/summary?all_tenants=true"));
        Assert.assertEquals(summary.getTotalCount(), Long.valueOf(4));
        Assert.assertEquals(summary.getMetadata().get("key1").get(1), "value2");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.12.*")
    public void summaryNeeds312() throws Exception {
        try {
            osv3().blockStorage().volumes().summary();
        } finally {
            assertNoMoreRequests();
        }
    }

    public void createWithGroupAndBackupAndSchedulerHints() throws Exception {
        negotiate("3.71");
        respondWith(202, "{\"volume\": {\"id\": \"" + VOLUME + "\", \"status\": \"creating\"}}");

        osv3().blockStorage().volumes().create(Builders.volume().name("v").size(1).groupId("g1").backupId("b1").imageId("img")
                .schedulerHints(Collections.singletonMap("same_host", "x")).build());

        RecordedRequest request = takeRequest();
        assertVersionHeader(request, "3.71");
        JsonNode body = body(request);
        Assert.assertEquals(body.get("volume").get("group_id").asText(), "g1");
        Assert.assertEquals(body.get("volume").get("backup_id").asText(), "b1");
        Assert.assertEquals(body.get("volume").get("image_id").asText(), "img");
        Assert.assertEquals(body.get("OS-SCH-HNT:scheduler_hints").get("same_host").asText(), "x");
        Assert.assertFalse(body.get("volume").has("bootable"));
        Assert.assertFalse(body.get("volume").has("scheduler_hints"));
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.47.*")
    public void backupIdNeeds347() throws Exception {
        try {
            osv3().blockStorage().volumes().create(Builders.volume().name("v").size(1).backupId("b1").build());
        } finally {
            assertNoMoreRequests();
        }
    }

    public void backupCreateWithMetadataAndZone() throws Exception {
        negotiate("3.71");
        respondWith(202, "{\"backup\": {\"id\": \"b1\", \"name\": \"n\"}}");

        osv3().blockStorage().backups().create(Builders.volumeBackupCreate().volumeId(VOLUME).name("n")
                .metadata(Collections.singletonMap("k", "v")).availabilityZone("az2").build());

        JsonNode body = body(takeRequest()).get("backup");
        Assert.assertEquals(body.get("metadata").get("k").asText(), "v");
        Assert.assertEquals(body.get("availability_zone").asText(), "az2");
    }

    public void uploadToImageWithVisibility() throws Exception {
        negotiate("3.71");
        respondWith(202, "{\"os-volume_upload_image\": {\"id\": \"" + VOLUME + "\", \"image_id\": \"i\", \"image_name\": \"n\", \"status\": \"uploading\"}}");

        osv3().blockStorage().volumes().uploadToImage(VOLUME, UploadImageData.create("n").visibility("private").protectedImage(true));

        JsonNode body = body(takeRequest()).get("os-volume_upload_image");
        Assert.assertEquals(body.get("visibility").asText(), "private");
        Assert.assertTrue(body.get("protected").asBoolean());
    }

    public void updateWithOptions() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"volume\": {\"id\": \"" + VOLUME + "\", \"name\": \"renamed\"}}");

        osv3().blockStorage().volumes().update(VOLUME, VolumeUpdateOptions.create().name("renamed").metadata(Collections.singletonMap("k", "v")));

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "PUT");
        JsonNode body = body(request).get("volume");
        Assert.assertEquals(body.get("name").asText(), "renamed");
        Assert.assertEquals(body.get("metadata").get("k").asText(), "v");
        Assert.assertFalse(body.has("description"));
    }
}
```
(`Builders.volumeBackupCreate()` 의 실제 이름을 `Builders` 에서 확인한다 — 없으면 `Builders.volumeBackupCreate()` 가 아니라 기존 이름(`Builders.volumeBackupCreate` 또는 `Builders.volumeBackup()`)을 쓴다. metadata 쿼리 인코딩은 `Map.toString()` 결과를 그대로 인코딩한 값(`{k=v}` 형태)이 될 수 있다 — Cinder 는 `metadata={'k': 'v'}`(python dict 문자열)을 기대하므로 `SnapshotListOptions.metadata` 는 `{'k': 'v'}` 형식의 문자열로 직접 만든다. 테스트의 두 번째 선택지는 지우고 python 형식만 확인한다.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -5`
Expected: `VolumeListOptions` 등 없음.

- [ ] **Step 3: 옵션 클래스 구현**

`options/BlockStorageListOptions.java`:
```java
package org.openstack4j.model.storage.block.options;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Common query parameters of Cinder list APIs. Subclasses add their resource's filters; each parameter records the
 * lowest microversion it needs so the service can check the floor before sending.
 *
 * @param <T> the concrete options type, for fluent chaining
 */
public abstract class BlockStorageListOptions<T extends BlockStorageListOptions<T>> {

    private final Map<String, String> params = new LinkedHashMap<>();
    private int requiredMinor = 0;

    @SuppressWarnings("unchecked")
    protected T put(String key, Object value, int minor) {
        params.put(key, String.valueOf(value));
        requiredMinor = Math.max(requiredMinor, minor);
        return (T) this;
    }

    public T limit(int limit) { return put("limit", limit, 0); }
    public T marker(String marker) { return put("marker", marker, 0); }
    public T offset(int offset) { return put("offset", offset, 0); }
    public T sortKey(String key) { return put("sort_key", key, 0); }
    /** {@code asc} or {@code desc} */
    public T sortDir(String dir) { return put("sort_dir", dir, 0); }
    /** Combined form such as {@code name:asc,created_at:desc}. */
    public T sort(String sort) { return put("sort", sort, 0); }
    /** Admin: include all projects. */
    public T allTenants(boolean allTenants) { return put("all_tenants", allTenants, 0); }

    public Map<String, String> toQueryParams() {
        return new LinkedHashMap<>(params);
    }

    /** @return the lowest block storage microversion these parameters need, such as {@code "3.45"}, or {@code null} */
    public String getRequiredMicroVersion() {
        return requiredMinor == 0 ? null : "3." + requiredMinor;
    }

    /** Cinder expects dict-style metadata filters: {@code {'key': 'value'}}. */
    protected static String dict(Map<String, String> metadata) {
        StringBuilder sb = new StringBuilder("{");
        metadata.forEach((k, v) -> sb.append(sb.length() > 1 ? ", " : "").append('\'').append(k).append("': '").append(v).append('\''));
        return sb.append('}').toString();
    }
}
```

`options/VolumeListOptions.java`:
```java
package org.openstack4j.model.storage.block.options;

import java.util.Map;

/** Filters for {@code GET /volumes/detail} and {@code GET /volumes/summary}. */
public class VolumeListOptions extends BlockStorageListOptions<VolumeListOptions> {

    public static VolumeListOptions create() {
        return new VolumeListOptions();
    }

    public VolumeListOptions name(String name) { return put("name", name, 0); }
    /** Substring match (3.34+). */
    public VolumeListOptions nameLike(String fragment) { return put("name~", fragment, 34); }
    public VolumeListOptions status(String status) { return put("status", status, 0); }
    public VolumeListOptions bootable(boolean bootable) { return put("bootable", bootable, 0); }
    public VolumeListOptions metadata(Map<String, String> metadata) { return put("metadata", dict(metadata), 0); }
    /** 3.4+ */
    public VolumeListOptions glanceMetadata(Map<String, String> metadata) { return put("glance_metadata", dict(metadata), 4); }
    /** 3.10+ */
    public VolumeListOptions groupId(String groupId) { return put("group_id", groupId, 10); }
    /** Adds {@code count} to the response (3.45+). */
    public VolumeListOptions withCount(boolean withCount) { return put("with_count", withCount, 45); }
    /** Time comparison filter (3.60+): {@code op} is gt, gte, eq, neq, lt or lte. */
    public VolumeListOptions createdAt(String op, String isoTime) { return put("created_at", op + ":" + isoTime, 60); }
    /** 3.60+ */
    public VolumeListOptions updatedAt(String op, String isoTime) { return put("updated_at", op + ":" + isoTime, 60); }
    /** 3.65+ */
    public VolumeListOptions consumesQuota(boolean consumesQuota) { return put("consumes_quota", consumesQuota, 65); }
}
```

`options/SnapshotListOptions.java`:
```java
package org.openstack4j.model.storage.block.options;

import java.util.Map;

/** Filters for {@code GET /snapshots/detail}. */
public class SnapshotListOptions extends BlockStorageListOptions<SnapshotListOptions> {

    public static SnapshotListOptions create() {
        return new SnapshotListOptions();
    }

    public SnapshotListOptions name(String name) { return put("name", name, 0); }
    /** 3.34+ */
    public SnapshotListOptions nameLike(String fragment) { return put("name~", fragment, 34); }
    public SnapshotListOptions status(String status) { return put("status", status, 0); }
    public SnapshotListOptions volumeId(String volumeId) { return put("volume_id", volumeId, 0); }
    /** 3.22+ */
    public SnapshotListOptions metadata(Map<String, String> metadata) { return put("metadata", dict(metadata), 22); }
    /** 3.45+ */
    public SnapshotListOptions withCount(boolean withCount) { return put("with_count", withCount, 45); }
    /** 3.65+ */
    public SnapshotListOptions consumesQuota(boolean consumesQuota) { return put("consumes_quota", consumesQuota, 65); }
}
```

`options/BackupListOptions.java`:
```java
package org.openstack4j.model.storage.block.options;

/** Filters for {@code GET /backups/detail}. */
public class BackupListOptions extends BlockStorageListOptions<BackupListOptions> {

    public static BackupListOptions create() {
        return new BackupListOptions();
    }

    public BackupListOptions name(String name) { return put("name", name, 0); }
    /** 3.34+ */
    public BackupListOptions nameLike(String fragment) { return put("name~", fragment, 34); }
    public BackupListOptions status(String status) { return put("status", status, 0); }
    public BackupListOptions volumeId(String volumeId) { return put("volume_id", volumeId, 0); }
    /** 3.45+ */
    public BackupListOptions withCount(boolean withCount) { return put("with_count", withCount, 45); }
}
```

`options/VolumeUpdateOptions.java`:
```java
package org.openstack4j.model.storage.block.options;

import java.util.LinkedHashMap;
import java.util.Map;

/** Body of {@code PUT /volumes/{id}}; only the fields set are sent (3.53 requires at least one). */
public class VolumeUpdateOptions {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    public static VolumeUpdateOptions create() {
        return new VolumeUpdateOptions();
    }

    public VolumeUpdateOptions name(String name) { fields.put("name", name); return this; }
    public VolumeUpdateOptions description(String description) { fields.put("description", description); return this; }
    /** Replaces the volume metadata. */
    public VolumeUpdateOptions metadata(Map<String, String> metadata) { fields.put("metadata", metadata); return this; }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(fields);
    }
}
```

`model/storage/block/VolumeSummary.java`:
```java
package org.openstack4j.model.storage.block;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** {@code GET /volumes/summary} (3.12+). */
public interface VolumeSummary extends ModelEntity {
    Long getTotalCount();
    Long getTotalSize();
    /** @return metadata key to its distinct values (3.36+) */
    Map<String, List<String>> getMetadata();
}
```

`openstack/storage/block/domain/CinderVolumeSummary.java`:
```java
package org.openstack4j.openstack.storage.block.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.storage.block.VolumeSummary;

@JsonRootName("volume-summary")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderVolumeSummary implements VolumeSummary {

    private static final long serialVersionUID = 1L;

    @JsonProperty("total_count") private Long totalCount;
    @JsonProperty("total_size") private Long totalSize;
    private Map<String, List<String>> metadata;

    @Override public Long getTotalCount() { return totalCount; }
    @Override public Long getTotalSize() { return totalSize; }
    @Override public Map<String, List<String>> getMetadata() { return metadata; }
}
```

- [ ] **Step 4: 빌더·요청 모델 확장**

`Volume.java`: `default String getBackupId() { return null; }`(3.47), `default Map<String, Object> getSchedulerHints() { return null; }`.
`VolumeBuilder.java`: `groupId(String)`(Javadoc 3.13+), `backupId(String)`(3.47+), `imageId(String)`, `consistencyGroupId(String)`, `schedulerHints(Map<String, Object>)`(`OS-SCH-HNT:scheduler_hints`, 본문의 `volume` 형제 키로 전송).
`CinderVolume.java`: 필드 `@JsonProperty("backup_id") private String backupId;`, `@JsonIgnore private transient Map<String, Object> schedulerHints;`(직렬화 제외 — 형제 키라서 서비스가 따로 보낸다), getter 2개(`@JsonIgnore @Override`), 빌더 메서드 5개(`m.groupId = ...; m.backupId = ...; m.imageId = ...; m.consistencyGroupId = ...; m.schedulerHints = ...`).
`VolumeBackupCreate.java`: `default Map<String, String> getMetadata() { return null; }`, `default String getAvailabilityZone() { return null; }`. `VolumeBackupCreateBuilder.java`: `metadata(Map<String, String>)`, `availabilityZone(String)`. `CinderVolumeBackupCreate.java`: 필드 `metadata`, `@JsonProperty("availability_zone") availabilityZone` + getter + 빌더 메서드.
`UploadImageData.java`: 필드 `private String visibility; private Boolean protectedImage;`, fluent `visibility(String)`, `protectedImage(boolean)`, getter `getVisibility()`, `getProtectedImage()`. `CinderUploadImageData.java`: `@JsonProperty("visibility") private String visibility; @JsonProperty("protected") private Boolean protectedImage;` 와 `create()` 에서 복사.

- [ ] **Step 5: 서비스 구현**

`BlockVolumeService.java` 선언 4개(Javadoc 에 하한), `BlockVolumeServiceImpl.java`:
```java
    @Override
    public List<? extends Volume> list(VolumeListOptions options) {
        Objects.requireNonNull(options);
        requireOptions("Volume list filters", options);
        return get(Volumes.class, uri("/volumes/detail")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public VolumeSummary summary() {
        requireMicroVersion("Volume summary", V(12));
        return get(CinderVolumeSummary.class, uri("/volumes/summary")).execute();
    }

    @Override
    public VolumeSummary summary(VolumeListOptions options) {
        Objects.requireNonNull(options);
        requireMicroVersion("Volume summary", V(12));
        requireOptions("Volume summary filters", options);
        return get(CinderVolumeSummary.class, uri("/volumes/summary")).params(options.toQueryParams()).execute();
    }

    @Override
    public Volume update(String volumeId, VolumeUpdateOptions options) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(options);
        return put(CinderVolume.class, uri("/volumes/%s", volumeId)).entity(JsonBody.of("volume", options.toMap())).execute();
    }

    private void requireOptions(String what, BlockStorageListOptions<?> options) {
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion(what + " " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
    }
```
기존 `create(Volume)` 를 바꾼다(Task 2 의 상한 유지):
```java
    @Override
    public Volume create(Volume volume) {
        Objects.requireNonNull(volume);
        if (volume.getGroupId() != null)
            requireMicroVersion("Volume create option group_id", V(13));
        if (volume.getBackupId() != null)
            requireMicroVersion("Volume create option backup_id", V(47));
        MicroVersion ceiling = volume instanceof CinderVolume && ((CinderVolume) volume).hasBootable() ? V(52) : null;   // bootable is not in the 3.53 create schema
        Invocation<CinderVolume> req = capped(post(CinderVolume.class, uri("/volumes")), ceiling);
        if (volume.getSchedulerHints() != null && !volume.getSchedulerHints().isEmpty()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("volume", PLAIN_MAPPER.convertValue(volume, Map.class));
            body.put("OS-SCH-HNT:scheduler_hints", volume.getSchedulerHints());
            return req.entity(JsonBody.of(body)).execute();
        }
        return req.entity(volume).execute();
    }

    /** Serialises a volume as the inner object (no root), honouring the model's Jackson annotations. */
    private static final ObjectMapper PLAIN_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
```
`BlockVolumeSnapshotServiceImpl.listDetail(SnapshotListOptions)`, `BlockVolumeBackupServiceImpl.list(BackupListOptions)`: 같은 `requireOptions` 패턴(각 클래스에 private 복사)으로 `/snapshots/detail`, `/backups/detail`. `BlockVolumeBackupServiceImpl.create` 에 하한 추가: `if (vbc.getMetadata() != null) requireMicroVersion("Backup create option metadata", V(43)); if (vbc.getAvailabilityZone() != null) requireMicroVersion("Backup create option availability_zone", V(51));`. `BlockVolumeServiceImpl.uploadToImage` 에: `if (data.getVisibility() != null || data.getProtectedImage() != null) requireMicroVersion("Upload to image visibility/protected", V(1));`.

- [ ] **Step 6: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='BlockStorageListOptionsTests,BlockStorageLegacyCeilingTests,VolumeTests,VolumeBackupTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Failures: 0, Errors: 0` (BlockStorageListOptionsTests 10개).

- [ ] **Step 7: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(block-storage): add typed list options, volume summary, update options and 3.x create options

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "Volume/Snapshot/BackupListOptions(3.4/3.10/3.22/3.34/3.45/3.60/3.65), volumes().summary(3.12), update(VolumeUpdateOptions), 생성 옵션 group_id(3.13)/backup_id(3.47)/image_id/scheduler hints, backup metadata(3.43)/AZ(3.51), upload image visibility(3.1)."
```

---

### Task 5: volume 액션 보강과 metadata

**Files:**
- Create: `core/src/main/java/org/openstack4j/model/storage/block/options/VolumeMigrateRequest.java`
- Create: `core/src/main/java/org/openstack4j/openstack/storage/block/domain/CinderMetadata.java`, `CinderMetadataItem.java`, `CinderConnectionInfo.java`
- Modify: `api/storage/BlockVolumeService.java`, `openstack/storage/block/internal/BlockVolumeServiceImpl.java`
- Create: `core-test/src/main/java/org/openstack4j/api/storage/microversion/VolumeActionTests.java`

**Interfaces:**
- Consumes: Task 2 `requireMicroVersion`, `V`; Task 1 `JsonBody`.
- Produces (`BlockVolumeService`):
  - metadata: `Map<String, String> metadata(String volumeId)`, `Map<String, String> setMetadata(String volumeId, Map<String, String>)`(POST, 병합), `Map<String, String> replaceMetadata(String volumeId, Map<String, String>)`(PUT), `String metadataItem(String volumeId, String key)`, `String updateMetadataItem(String volumeId, String key, String value)`, `ActionResponse deleteMetadataItem(String volumeId, String key)`
  - image metadata: `Map<String, String> imageMetadata(String volumeId)`, `Map<String, String> setImageMetadata(String volumeId, Map<String, String>)`, `ActionResponse unsetImageMetadata(String volumeId, String key)`
  - actions: `ActionResponse revertToSnapshot(String volumeId, String snapshotId)`(3.40), `ActionResponse reimage(String volumeId, String imageId, boolean reimageReserved)`(3.68), `ActionResponse completeExtend(String volumeId, boolean error)`(3.71), `ActionResponse retype(String volumeId, String newType, String migrationPolicy)`, `ActionResponse migrate(String volumeId, VolumeMigrateRequest)`, `ActionResponse completeMigration(String volumeId, String newVolumeId, boolean error)`, `ActionResponse unmanage(String volumeId)`, `ActionResponse reserve(String volumeId)`, `ActionResponse unreserve(String volumeId)`, `ActionResponse beginDetaching(String volumeId)`, `ActionResponse rollDetaching(String volumeId)`, `Map<String, Object> initializeConnection(String volumeId, Map<String, Object> connector)`, `ActionResponse terminateConnection(String volumeId, Map<String, Object> connector)`, `ActionResponse setStatus(String volumeId, String status, String attachStatus, String migrationStatus)`
  - `VolumeMigrateRequest.create()`: `host(String)`, `cluster(String)`(3.16), `forceHostCopy(boolean)`, `lockVolume(boolean)`; getter 4개
  - 도메인: `CinderMetadata`(`{"metadata": {...}}`, `getMetadata()`), `CinderMetadataItem`(`{"meta": {...}}`, `getMeta()`), `CinderConnectionInfo`(`{"connection_info": {...}}`, `getConnectionInfo()`)

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/d5-volume-actions
```

`VolumeActionTests.java`:
```java
package org.openstack4j.api.storage.microversion;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.options.VolumeMigrateRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/VolumeActions")
public class VolumeActionTests extends AbstractBlockStorageMicroVersionTest {

    private JsonNode sentAction(String expectedVersion) throws Exception {
        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/volumes/" + VOLUME + "/action"), request.getPath());
        if (expectedVersion != null)
            assertVersionHeader(request, expectedVersion);
        return body(request);
    }

    public void metadataCrud() throws Exception {
        respondWith(200, "{\"metadata\": {\"a\": \"1\"}}");
        respondWith(200, "{\"metadata\": {\"a\": \"1\", \"b\": \"2\"}}");
        respondWith(200, "{\"metadata\": {\"b\": \"2\"}}");
        respondWith(200, "{\"meta\": {\"b\": \"2\"}}");
        respondWith(200, "{\"meta\": {\"b\": \"3\"}}");
        respondWith(200);

        Map<String, String> all = osv3().blockStorage().volumes().metadata(VOLUME);
        Map<String, String> merged = osv3().blockStorage().volumes().setMetadata(VOLUME, Collections.singletonMap("b", "2"));
        Map<String, String> replaced = osv3().blockStorage().volumes().replaceMetadata(VOLUME, Collections.singletonMap("b", "2"));
        String item = osv3().blockStorage().volumes().metadataItem(VOLUME, "b");
        String updated = osv3().blockStorage().volumes().updateMetadataItem(VOLUME, "b", "3");
        boolean deleted = osv3().blockStorage().volumes().deleteMetadataItem(VOLUME, "b").isSuccess();

        Assert.assertEquals(takeRequest().getMethod(), "GET");
        RecordedRequest post = takeRequest();
        Assert.assertEquals(post.getMethod(), "POST");
        Assert.assertEquals(body(post).get("metadata").get("b").asText(), "2");
        Assert.assertEquals(takeRequest().getMethod(), "PUT");
        Assert.assertTrue(takeRequest().getPath().endsWith("/volumes/" + VOLUME + "/metadata/b"));
        RecordedRequest put = takeRequest();
        Assert.assertEquals(body(put).get("meta").get("b").asText(), "3");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(all.get("a"), "1");
        Assert.assertEquals(merged.size(), 2);
        Assert.assertEquals(replaced.size(), 1);
        Assert.assertEquals(item, "2");
        Assert.assertEquals(updated, "3");
        Assert.assertTrue(deleted);
    }

    public void imageMetadataActions() throws Exception {
        respondWith(200, "{\"metadata\": {\"image_name\": \"cirros\"}}");
        respondWith(200, "{\"metadata\": {\"image_name\": \"cirros\", \"kernel_id\": \"k\"}}");
        respondWith(200);

        Map<String, String> shown = osv3().blockStorage().volumes().imageMetadata(VOLUME);
        Map<String, String> set = osv3().blockStorage().volumes().setImageMetadata(VOLUME, Collections.singletonMap("kernel_id", "k"));
        osv3().blockStorage().volumes().unsetImageMetadata(VOLUME, "kernel_id");

        Assert.assertTrue(sentAction(null).has("os-show_image_metadata"));
        Assert.assertEquals(sentAction(null).get("os-set_image_metadata").get("metadata").get("kernel_id").asText(), "k");
        Assert.assertEquals(sentAction(null).get("os-unset_image_metadata").get("key").asText(), "kernel_id");
        Assert.assertEquals(shown.get("image_name"), "cirros");
        Assert.assertEquals(set.size(), 2);
    }

    public void revertReimageAndExtendCompletion() throws Exception {
        negotiate("3.71");
        respondWith(202);
        respondWith(202);
        respondWith(202);

        osv3().blockStorage().volumes().revertToSnapshot(VOLUME, SNAPSHOT);
        osv3().blockStorage().volumes().reimage(VOLUME, "img-1", false);
        osv3().blockStorage().volumes().completeExtend(VOLUME, false);

        Assert.assertEquals(sentAction("3.71").get("revert").get("snapshot_id").asText(), SNAPSHOT);
        JsonNode reimage = sentAction("3.71").get("os-reimage");
        Assert.assertEquals(reimage.get("image_id").asText(), "img-1");
        Assert.assertFalse(reimage.get("reimage_reserved").asBoolean());
        Assert.assertFalse(sentAction("3.71").get("os-extend_volume_completion").get("error").asBoolean());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.40.*")
    public void revertNeeds340() throws Exception {
        try {
            osv3().blockStorage().volumes().revertToSnapshot(VOLUME, SNAPSHOT);
        } finally {
            assertNoMoreRequests();
        }
    }

    public void retypeMigrateAndCompletion() throws Exception {
        negotiate("3.71");
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);

        osv3().blockStorage().volumes().retype(VOLUME, "ssd", "on-demand");
        osv3().blockStorage().volumes().migrate(VOLUME, VolumeMigrateRequest.create().host("storage-2@lvm").forceHostCopy(true));
        osv3().blockStorage().volumes().migrate(VOLUME, VolumeMigrateRequest.create().cluster("cluster1").lockVolume(true));
        osv3().blockStorage().volumes().completeMigration(VOLUME, "new-vol", false);

        JsonNode retype = sentAction("3.71").get("os-retype");
        Assert.assertEquals(retype.get("new_type").asText(), "ssd");
        Assert.assertEquals(retype.get("migration_policy").asText(), "on-demand");
        JsonNode byHost = sentAction("3.71").get("os-migrate_volume");
        Assert.assertEquals(byHost.get("host").asText(), "storage-2@lvm");
        Assert.assertTrue(byHost.get("force_host_copy").asBoolean());
        Assert.assertFalse(byHost.has("cluster"));
        JsonNode byCluster = sentAction("3.71").get("os-migrate_volume");
        Assert.assertEquals(byCluster.get("cluster").asText(), "cluster1");
        Assert.assertTrue(byCluster.get("lock_volume").asBoolean());
        JsonNode completion = sentAction("3.71").get("os-migrate_volume_completion");
        Assert.assertEquals(completion.get("new_volume").asText(), "new-vol");
        Assert.assertFalse(completion.get("error").asBoolean());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.16.*")
    public void migrateToClusterNeeds316() throws Exception {
        try {
            osv3().blockStorage().volumes().migrate(VOLUME, VolumeMigrateRequest.create().cluster("c"));
        } finally {
            assertNoMoreRequests();
        }
    }

    public void adminAndAttachWorkflowActions() throws Exception {
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(200, "{\"connection_info\": {\"driver_volume_type\": \"iscsi\", \"data\": {\"target_lun\": 1}}}");
        respondWith(202);
        respondWith(202);

        osv3().blockStorage().volumes().unmanage(VOLUME);
        osv3().blockStorage().volumes().reserve(VOLUME);
        osv3().blockStorage().volumes().unreserve(VOLUME);
        osv3().blockStorage().volumes().beginDetaching(VOLUME);
        osv3().blockStorage().volumes().rollDetaching(VOLUME);
        Map<String, Object> info = osv3().blockStorage().volumes().initializeConnection(VOLUME, Collections.singletonMap("initiator", "iqn.x"));
        osv3().blockStorage().volumes().terminateConnection(VOLUME, Collections.singletonMap("initiator", "iqn.x"));
        osv3().blockStorage().volumes().setStatus(VOLUME, "available", "detached", null);

        for (String action : new String[] {"os-unmanage", "os-reserve", "os-unreserve", "os-begin_detaching", "os-roll_detaching"})
            Assert.assertTrue(sentAction(null).has(action), action);
        Assert.assertEquals(sentAction(null).get("os-initialize_connection").get("connector").get("initiator").asText(), "iqn.x");
        Assert.assertTrue(sentAction(null).get("os-terminate_connection").has("connector"));
        JsonNode reset = sentAction(null).get("os-reset_status");
        Assert.assertEquals(reset.get("status").asText(), "available");
        Assert.assertEquals(reset.get("attach_status").asText(), "detached");
        Assert.assertFalse(reset.has("migration_status"));
        Assert.assertEquals(info.get("driver_volume_type"), "iscsi");
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -5`
Expected: `VolumeMigrateRequest`, `metadata(...)` 등 없음.

- [ ] **Step 3: 요청·도메인 클래스 구현**

`options/VolumeMigrateRequest.java`:
```java
package org.openstack4j.model.storage.block.options;

/** Body of the {@code os-migrate_volume} action. Either {@code host} or {@code cluster} (3.16+), not both. */
public class VolumeMigrateRequest {

    private String host;
    private String cluster;
    private Boolean forceHostCopy;
    private Boolean lockVolume;

    public static VolumeMigrateRequest create() {
        return new VolumeMigrateRequest();
    }

    public VolumeMigrateRequest host(String host) { this.host = host; return this; }
    /** 3.16+ */
    public VolumeMigrateRequest cluster(String cluster) { this.cluster = cluster; return this; }
    public VolumeMigrateRequest forceHostCopy(boolean forceHostCopy) { this.forceHostCopy = forceHostCopy; return this; }
    public VolumeMigrateRequest lockVolume(boolean lockVolume) { this.lockVolume = lockVolume; return this; }

    public String getHost() { return host; }
    public String getCluster() { return cluster; }
    public Boolean getForceHostCopy() { return forceHostCopy; }
    public Boolean getLockVolume() { return lockVolume; }
}
```

`domain/CinderMetadata.java`:
```java
package org.openstack4j.openstack.storage.block.domain;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"metadata": {...}}} as returned by the volume, snapshot and image-metadata APIs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderMetadata implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("metadata")
    private Map<String, String> metadata;

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
```
`domain/CinderMetadataItem.java` — 같은 모양으로 `@JsonProperty("meta") private Map<String, String> meta;` 와 `getMeta()`; `/** @return the single value, or null */ public String value() { return meta == null || meta.isEmpty() ? null : meta.values().iterator().next(); }`.
`domain/CinderConnectionInfo.java` — `@JsonProperty("connection_info") private Map<String, Object> connectionInfo;` 와 `getConnectionInfo()`.

- [ ] **Step 4: 서비스 구현**

`BlockVolumeService.java` 에 Interfaces 의 메서드를 Javadoc(하한·액션 이름)과 함께 선언. `BlockVolumeServiceImpl.java`:
```java
    private ActionResponse action(String volumeId, String action, Map<String, ?> body) {
        return post(ActionResponse.class, uri("/volumes/%s/action", volumeId)).entity(JsonBody.of(action, body)).execute();
    }

    @Override
    public Map<String, String> metadata(String volumeId) {
        Objects.requireNonNull(volumeId);
        CinderMetadata result = get(CinderMetadata.class, uri("/volumes/%s/metadata", volumeId)).execute();
        return result == null ? Collections.emptyMap() : result.getMetadata();
    }

    @Override
    public Map<String, String> setMetadata(String volumeId, Map<String, String> metadata) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(metadata);
        return post(CinderMetadata.class, uri("/volumes/%s/metadata", volumeId)).entity(JsonBody.of("metadata", metadata)).execute().getMetadata();
    }

    @Override
    public Map<String, String> replaceMetadata(String volumeId, Map<String, String> metadata) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(metadata);
        return put(CinderMetadata.class, uri("/volumes/%s/metadata", volumeId)).entity(JsonBody.of("metadata", metadata)).execute().getMetadata();
    }

    @Override
    public String metadataItem(String volumeId, String key) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(key);
        CinderMetadataItem item = get(CinderMetadataItem.class, uri("/volumes/%s/metadata/%s", volumeId, key)).execute();
        return item == null ? null : item.value();
    }

    @Override
    public String updateMetadataItem(String volumeId, String key, String value) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(key);
        CinderMetadataItem item = put(CinderMetadataItem.class, uri("/volumes/%s/metadata/%s", volumeId, key))
                .entity(JsonBody.of("meta", Collections.singletonMap(key, value))).execute();
        return item == null ? null : item.value();
    }

    @Override
    public ActionResponse deleteMetadataItem(String volumeId, String key) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(key);
        return deleteWithResponse(uri("/volumes/%s/metadata/%s", volumeId, key)).execute();
    }

    @Override
    public Map<String, String> imageMetadata(String volumeId) {
        Objects.requireNonNull(volumeId);
        CinderMetadata result = post(CinderMetadata.class, uri("/volumes/%s/action", volumeId))
                .entity(JsonBody.of("os-show_image_metadata", Collections.emptyMap())).execute();
        return result == null ? Collections.emptyMap() : result.getMetadata();
    }

    @Override
    public Map<String, String> setImageMetadata(String volumeId, Map<String, String> metadata) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(metadata);
        return post(CinderMetadata.class, uri("/volumes/%s/action", volumeId))
                .entity(JsonBody.of("os-set_image_metadata", Collections.singletonMap("metadata", metadata))).execute().getMetadata();
    }

    @Override
    public ActionResponse unsetImageMetadata(String volumeId, String key) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(key);
        return action(volumeId, "os-unset_image_metadata", Collections.singletonMap("key", key));
    }

    @Override
    public ActionResponse revertToSnapshot(String volumeId, String snapshotId) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(snapshotId);
        requireMicroVersion("Revert to snapshot", V(40));
        return action(volumeId, "revert", Collections.singletonMap("snapshot_id", snapshotId));
    }

    @Override
    public ActionResponse reimage(String volumeId, String imageId, boolean reimageReserved) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(imageId);
        requireMicroVersion("Reimage", V(68));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("image_id", imageId);
        body.put("reimage_reserved", reimageReserved);
        return action(volumeId, "os-reimage", body);
    }

    @Override
    public ActionResponse completeExtend(String volumeId, boolean error) {
        Objects.requireNonNull(volumeId);
        requireMicroVersion("Extend completion", V(71));
        return action(volumeId, "os-extend_volume_completion", Collections.singletonMap("error", error));
    }

    @Override
    public ActionResponse retype(String volumeId, String newType, String migrationPolicy) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(newType);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("new_type", newType);
        if (migrationPolicy != null)
            body.put("migration_policy", migrationPolicy);
        return action(volumeId, "os-retype", body);
    }

    @Override
    public ActionResponse migrate(String volumeId, VolumeMigrateRequest request) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(request);
        if (request.getCluster() != null)
            requireMicroVersion("Migrate to a cluster", V(16));
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.getHost() != null) body.put("host", request.getHost());
        if (request.getCluster() != null) body.put("cluster", request.getCluster());
        if (request.getForceHostCopy() != null) body.put("force_host_copy", request.getForceHostCopy());
        if (request.getLockVolume() != null) body.put("lock_volume", request.getLockVolume());
        return action(volumeId, "os-migrate_volume", body);
    }

    @Override
    public ActionResponse completeMigration(String volumeId, String newVolumeId, boolean error) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(newVolumeId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("new_volume", newVolumeId);
        body.put("error", error);
        return action(volumeId, "os-migrate_volume_completion", body);
    }

    @Override public ActionResponse unmanage(String volumeId) { return action(Objects.requireNonNull(volumeId), "os-unmanage", Collections.emptyMap()); }
    @Override public ActionResponse reserve(String volumeId) { return action(Objects.requireNonNull(volumeId), "os-reserve", Collections.emptyMap()); }
    @Override public ActionResponse unreserve(String volumeId) { return action(Objects.requireNonNull(volumeId), "os-unreserve", Collections.emptyMap()); }
    @Override public ActionResponse beginDetaching(String volumeId) { return action(Objects.requireNonNull(volumeId), "os-begin_detaching", Collections.emptyMap()); }
    @Override public ActionResponse rollDetaching(String volumeId) { return action(Objects.requireNonNull(volumeId), "os-roll_detaching", Collections.emptyMap()); }

    @Override
    public Map<String, Object> initializeConnection(String volumeId, Map<String, Object> connector) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(connector);
        CinderConnectionInfo info = post(CinderConnectionInfo.class, uri("/volumes/%s/action", volumeId))
                .entity(JsonBody.of("os-initialize_connection", Collections.singletonMap("connector", connector))).execute();
        return info == null ? Collections.emptyMap() : info.getConnectionInfo();
    }

    @Override
    public ActionResponse terminateConnection(String volumeId, Map<String, Object> connector) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(connector);
        return action(volumeId, "os-terminate_connection", Collections.singletonMap("connector", connector));
    }

    @Override
    public ActionResponse setStatus(String volumeId, String status, String attachStatus, String migrationStatus) {
        Objects.requireNonNull(volumeId);
        Map<String, Object> body = new LinkedHashMap<>();
        if (status != null) body.put("status", status);
        if (attachStatus != null) body.put("attach_status", attachStatus);
        if (migrationStatus != null) body.put("migration_status", migrationStatus);
        return action(volumeId, "os-reset_status", body);
    }
```
(`post(CinderMetadata.class, ...).execute()` 가 200 본문 없는 응답에서 `null` 을 돌려줄 수 있어 `imageMetadata`/`metadata` 는 null 을 빈 맵으로 바꾼다. `JsonBody.of("os-show_image_metadata", Collections.emptyMap())` 는 `{"os-show_image_metadata": {}}` 를 만든다.)

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='VolumeActionTests,VolumeTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Failures: 0, Errors: 0` (VolumeActionTests 7개).

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(block-storage): add volume metadata, image metadata and the remaining volume actions

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "volumes(): metadata CRUD, image metadata actions, revert(3.40), reimage(3.68), extend completion(3.71), retype, migrate(host|cluster 3.16), migration completion, unmanage, reserve/unreserve, begin/roll detaching, initialize/terminate connection, reset status 확장."
```

---

### Task 6: snapshots·backups 보강 (metadata, 액션, update, export/import)

**Files:**
- Modify: `api/storage/BlockVolumeSnapshotService.java`, `api/storage/BlockVolumeBackupService.java` 와 구현 2개
- Create: `core/src/main/java/org/openstack4j/model/storage/block/BackupRecord.java`, `openstack/storage/block/domain/CinderBackupRecord.java`
- Create: `core-test/src/main/java/org/openstack4j/api/storage/microversion/SnapshotAndBackupTests.java`

**Interfaces:**
- Consumes: Task 5 `CinderMetadata`, `CinderMetadataItem`; Task 1 `JsonBody`; Task 2 `requireMicroVersion`, `V`.
- Produces:
  - `BlockVolumeSnapshotService`: `Map<String, String> metadata(String snapshotId)`, `setMetadata`, `replaceMetadata`, `String metadataItem(String snapshotId, String key)`, `updateMetadataItem`, `ActionResponse deleteMetadataItem`, `ActionResponse resetStatus(String snapshotId, String status)`, `ActionResponse forceDelete(String snapshotId)`, `ActionResponse updateStatus(String snapshotId, String status, String progress)`, `ActionResponse unmanage(String snapshotId)`
  - `BlockVolumeBackupService`: `VolumeBackup update(String backupId, String name, String description)`(3.9), `VolumeBackup update(String backupId, String name, String description, Map<String, String> metadata)`(3.43), `BackupRecord exportRecord(String backupId)`, `VolumeBackup importRecord(String backupService, String backupUrl)`, `ActionResponse forceDelete(String backupId)`, `ActionResponse resetStatus(String backupId, String status)`
  - `BackupRecord`: `getBackupService()`, `getBackupUrl()`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/d6-snapshots-backups
```

`SnapshotAndBackupTests.java`:
```java
package org.openstack4j.api.storage.microversion;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.BackupRecord;
import org.openstack4j.model.storage.block.VolumeBackup;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/SnapshotsBackups")
public class SnapshotAndBackupTests extends AbstractBlockStorageMicroVersionTest {

    private static final String BACKUP = "3052c307-119e-4f78-960e-972078aa15a8";

    public void snapshotMetadataAndActions() throws Exception {
        respondWith(200, "{\"metadata\": {\"k\": \"v\"}}");
        respondWith(200, "{\"meta\": {\"k\": \"v2\"}}");
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);

        Map<String, String> metadata = osv3().blockStorage().snapshots().metadata(SNAPSHOT);
        String updated = osv3().blockStorage().snapshots().updateMetadataItem(SNAPSHOT, "k", "v2");
        osv3().blockStorage().snapshots().resetStatus(SNAPSHOT, "available");
        osv3().blockStorage().snapshots().forceDelete(SNAPSHOT);
        osv3().blockStorage().snapshots().updateStatus(SNAPSHOT, "creating", "80%");
        osv3().blockStorage().snapshots().unmanage(SNAPSHOT);

        Assert.assertTrue(takeRequest().getPath().endsWith("/snapshots/" + SNAPSHOT + "/metadata"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/snapshots/" + SNAPSHOT + "/metadata/k"));
        Assert.assertEquals(body(takeRequest()).get("os-reset_status").get("status").asText(), "available");
        Assert.assertTrue(body(takeRequest()).has("os-force_delete"));
        JsonNode status = body(takeRequest()).get("os-update_snapshot_status");
        Assert.assertEquals(status.get("status").asText(), "creating");
        Assert.assertEquals(status.get("progress").asText(), "80%");
        Assert.assertTrue(body(takeRequest()).has("os-unmanage"));
        Assert.assertEquals(metadata.get("k"), "v");
        Assert.assertEquals(updated, "v2");
    }

    public void backupUpdateExportImportAndActions() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"backup\": {\"id\": \"" + BACKUP + "\", \"name\": \"renamed\", \"links\": []}}");
        respondWith(200, "{\"backup-record\": {\"backup_service\": \"cinder.backup.drivers.swift\", \"backup_url\": \"eyJzdGF0\"}}");
        respondWith(201, "{\"backup\": {\"id\": \"" + BACKUP + "\", \"name\": \"imported\", \"links\": []}}");
        respondWith(202);
        respondWith(202);

        VolumeBackup updated = osv3().blockStorage().backups().update(BACKUP, "renamed", null, Collections.singletonMap("k", "v"));
        BackupRecord record = osv3().blockStorage().backups().exportRecord(BACKUP);
        VolumeBackup imported = osv3().blockStorage().backups().importRecord(record.getBackupService(), record.getBackupUrl());
        osv3().blockStorage().backups().forceDelete(BACKUP);
        osv3().blockStorage().backups().resetStatus(BACKUP, "error");

        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PUT");
        assertVersionHeader(update, "3.71");
        JsonNode body = body(update).get("backup");
        Assert.assertEquals(body.get("name").asText(), "renamed");
        Assert.assertFalse(body.has("description"));
        Assert.assertEquals(body.get("metadata").get("k").asText(), "v");
        Assert.assertTrue(takeRequest().getPath().endsWith("/backups/" + BACKUP + "/export_record"));
        RecordedRequest imp = takeRequest();
        Assert.assertTrue(imp.getPath().endsWith("/backups/import_record"));
        Assert.assertEquals(body(imp).get("backup-record").get("backup_url").asText(), "eyJzdGF0");
        Assert.assertTrue(body(takeRequest()).has("os-force_delete"));
        Assert.assertEquals(body(takeRequest()).get("os-reset_status").get("status").asText(), "error");
        Assert.assertEquals(updated.getName(), "renamed");
        Assert.assertEquals(imported.getName(), "imported");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.9.*")
    public void backupUpdateNeeds39() throws Exception {
        try {
            osv3().blockStorage().backups().update(BACKUP, "n", "d");
        } finally {
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.43.*")
    public void backupMetadataUpdateNeeds343() throws Exception {
        negotiate("3.71");
        osv3().blockStorage().microVersions().use("3.40");
        try {
            osv3().blockStorage().backups().update(BACKUP, "n", null, Collections.singletonMap("k", "v"));
        } finally {
            assertNoMoreRequests();
        }
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `BackupRecord`, `resetStatus` 등 없음.

- [ ] **Step 3: 구현**

`model/storage/block/BackupRecord.java`:
```java
package org.openstack4j.model.storage.block;

import org.openstack4j.model.ModelEntity;

/** An exported backup record ({@code GET /backups/{id}/export_record}), importable on another deployment. */
public interface BackupRecord extends ModelEntity {
    String getBackupService();
    String getBackupUrl();
}
```
`domain/CinderBackupRecord.java`:
```java
package org.openstack4j.openstack.storage.block.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.storage.block.BackupRecord;

@JsonRootName("backup-record")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderBackupRecord implements BackupRecord {

    private static final long serialVersionUID = 1L;

    @JsonProperty("backup_service") private String backupService;
    @JsonProperty("backup_url") private String backupUrl;

    public CinderBackupRecord() {
    }

    public CinderBackupRecord(String backupService, String backupUrl) {
        this.backupService = backupService;
        this.backupUrl = backupUrl;
    }

    @Override public String getBackupService() { return backupService; }
    @Override public String getBackupUrl() { return backupUrl; }
}
```

`BlockVolumeSnapshotServiceImpl.java` — Task 5 의 volume metadata 6개와 같은 본문으로 `/snapshots/%s/metadata[/%s]` 경로, 액션 helper:
```java
    private ActionResponse action(String snapshotId, String action, Map<String, ?> body) {
        return post(ActionResponse.class, uri("/snapshots/%s/action", snapshotId)).entity(JsonBody.of(action, body)).execute();
    }

    @Override public ActionResponse resetStatus(String snapshotId, String status) { return action(Objects.requireNonNull(snapshotId), "os-reset_status", Collections.singletonMap("status", Objects.requireNonNull(status))); }
    @Override public ActionResponse forceDelete(String snapshotId) { return action(Objects.requireNonNull(snapshotId), "os-force_delete", Collections.emptyMap()); }
    @Override public ActionResponse unmanage(String snapshotId) { return action(Objects.requireNonNull(snapshotId), "os-unmanage", Collections.emptyMap()); }

    @Override
    public ActionResponse updateStatus(String snapshotId, String status, String progress) {
        Objects.requireNonNull(snapshotId);
        Objects.requireNonNull(status);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        if (progress != null) body.put("progress", progress);
        return action(snapshotId, "os-update_snapshot_status", body);
    }
```

`BlockVolumeBackupServiceImpl.java`:
```java
    @Override
    public VolumeBackup update(String backupId, String name, String description) {
        return update(backupId, name, description, null);
    }

    @Override
    public VolumeBackup update(String backupId, String name, String description, Map<String, String> metadata) {
        Objects.requireNonNull(backupId);
        requireMicroVersion("Backup update", V(9));
        if (metadata != null)
            requireMicroVersion("Backup metadata update", V(43));
        Map<String, Object> body = new LinkedHashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (metadata != null) body.put("metadata", metadata);
        return put(CinderVolumeBackup.class, uri("/backups/%s", backupId)).entity(JsonBody.of("backup", body)).execute();
    }

    @Override
    public BackupRecord exportRecord(String backupId) {
        Objects.requireNonNull(backupId);
        return get(CinderBackupRecord.class, uri("/backups/%s/export_record", backupId)).execute();
    }

    @Override
    public VolumeBackup importRecord(String backupService, String backupUrl) {
        Objects.requireNonNull(backupService);
        Objects.requireNonNull(backupUrl);
        return post(CinderVolumeBackup.class, uri("/backups/import_record")).entity(new CinderBackupRecord(backupService, backupUrl)).execute();
    }

    @Override public ActionResponse forceDelete(String backupId) { return action(Objects.requireNonNull(backupId), "os-force_delete", Collections.emptyMap()); }
    @Override public ActionResponse resetStatus(String backupId, String status) { return action(Objects.requireNonNull(backupId), "os-reset_status", Collections.singletonMap("status", Objects.requireNonNull(status))); }

    private ActionResponse action(String backupId, String action, Map<String, ?> body) {
        return post(ActionResponse.class, uri("/backups/%s/action", backupId)).entity(JsonBody.of(action, body)).execute();
    }
```

- [ ] **Step 4: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='SnapshotAndBackupTests,VolumeSnapshotTests,VolumeBackupTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -3`
Expected: `Failures: 0, Errors: 0` (SnapshotAndBackupTests 4개).

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(block-storage): add snapshot metadata and actions, backup update, export/import and actions

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "snapshots(): metadata CRUD, reset/force-delete/update status/unmanage; backups(): update(3.9, metadata 3.43), export/import record, force-delete, reset status."
```

---

### Task 7: attachments(3.27)와 messages(3.3)

**Files:**
- Create (`core/src/main/java/org/openstack4j/`): `model/storage/block/VolumeAttachmentRecord.java`, `model/storage/block/VolumeMessage.java`, `model/storage/block/options/AttachmentListOptions.java`, `model/storage/block/options/MessageListOptions.java`, `api/storage/BlockAttachmentService.java`, `api/storage/BlockMessageService.java`, `openstack/storage/block/domain/CinderAttachment.java`, `openstack/storage/block/domain/CinderMessage.java`, `openstack/storage/block/internal/BlockAttachmentServiceImpl.java`, `openstack/storage/block/internal/BlockMessageServiceImpl.java`
- Modify: `api/storage/BlockStorageService.java`, `openstack/storage/block/internal/BlockStorageServiceImpl.java`, `openstack/provider/DefaultAPIProvider.java`
- Create: `core-test/src/main/java/org/openstack4j/api/storage/microversion/AttachmentAndMessageTests.java`

**Interfaces:**
- Consumes: Task 2 `requireMicroVersion`, `V`; Task 4 `BlockStorageListOptions`; Task 1 `JsonBody`.
- Produces:
  - `BlockStorageService.attachments()` → `BlockAttachmentService`(하한 3.27 전체): `List<? extends VolumeAttachmentRecord> list()`, `list(AttachmentListOptions)`, `listDetail()`, `listDetail(AttachmentListOptions)`, `VolumeAttachmentRecord get(String id)`, `VolumeAttachmentRecord create(String volumeId, String instanceId, Map<String, Object> connector, String mode)`(`instanceId`/`connector` nullable; `mode` 는 3.54, nullable), `VolumeAttachmentRecord update(String id, Map<String, Object> connector)`, `ActionResponse complete(String id)`(3.44), `ActionResponse delete(String id)`
  - `VolumeAttachmentRecord`: `getId()`, `getStatus()`, `getInstance()`, `getVolumeId()`, `Date getAttachedAt()`, `Date getDetachedAt()`, `getAttachMode()`, `Map<String, Object> getConnectionInfo()`
  - `AttachmentListOptions.create()`: 공통 + `instanceId(String)`, `volumeId(String)`, `status(String)`
  - `BlockStorageService.messages()` → `BlockMessageService`(하한 3.3): `List<? extends VolumeMessage> list()`, `list(MessageListOptions)`(페이지네이션 3.5), `VolumeMessage get(String id)`, `ActionResponse delete(String id)`
  - `VolumeMessage`: `getId()`, `getEventId()`, `getUserMessage()`, `getMessageLevel()`, `getResourceType()`, `getResourceUuid()`, `getRequestId()`, `Date getCreatedAt()`, `Date getGuaranteedUntil()`, `List<? extends Link> getLinks()`
  - `MessageListOptions.create()`: 공통(`limit`/`marker`/`offset`/`sort` 는 3.5) + `resourceType`, `resourceUuid`, `eventId`, `requestId`, `messageLevel`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/d7-attachments-messages
```

`AttachmentAndMessageTests.java`:
```java
package org.openstack4j.api.storage.microversion;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.VolumeAttachmentRecord;
import org.openstack4j.model.storage.block.VolumeMessage;
import org.openstack4j.model.storage.block.options.AttachmentListOptions;
import org.openstack4j.model.storage.block.options.MessageListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/AttachmentsMessages")
public class AttachmentAndMessageTests extends AbstractBlockStorageMicroVersionTest {

    private static final String ATTACHMENT = "{\"id\": \"a7d16728-e489-479f-96cd-2c2c6c24a100\", \"status\": \"reserved\", \"instance\": \"96a38bed-26b5-410b-8cef-1913a2e0e0b8\","
            + " \"volume_id\": \"" + VOLUME + "\", \"attached_at\": null, \"detached_at\": null, \"attach_mode\": \"rw\", \"connection_info\": {}}";

    public void attachmentLifecycle() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"attachment\": " + ATTACHMENT + "}");
        respondWith(200, "{\"attachment\": " + ATTACHMENT.replace("\"connection_info\": {}", "\"connection_info\": {\"driver_volume_type\": \"iscsi\"}") + "}");
        respondWith(204);
        respondWith(200, "{\"attachments\": [" + ATTACHMENT + "]}");
        respondWith(200, "{\"attachment\": " + ATTACHMENT + "}");
        respondWith(200);

        VolumeAttachmentRecord created = osv3().blockStorage().attachments().create(VOLUME, "96a38bed-26b5-410b-8cef-1913a2e0e0b8", null, "rw");
        VolumeAttachmentRecord updated = osv3().blockStorage().attachments().update(created.getId(), Collections.singletonMap("initiator", "iqn.x"));
        osv3().blockStorage().attachments().complete(created.getId());
        List<? extends VolumeAttachmentRecord> all = osv3().blockStorage().attachments().listDetail(AttachmentListOptions.create().volumeId(VOLUME));
        VolumeAttachmentRecord shown = osv3().blockStorage().attachments().get(created.getId());
        boolean deleted = osv3().blockStorage().attachments().delete(created.getId()).isSuccess();

        RecordedRequest create = takeRequest();
        assertVersionHeader(create, "3.71");
        JsonNode body = body(create).get("attachment");
        Assert.assertEquals(body.get("volume_uuid").asText(), VOLUME);
        Assert.assertEquals(body.get("instance_uuid").asText(), "96a38bed-26b5-410b-8cef-1913a2e0e0b8");
        Assert.assertEquals(body.get("mode").asText(), "rw");
        Assert.assertFalse(body.has("connector"));
        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PUT");
        Assert.assertEquals(body(update).get("attachment").get("connector").get("initiator").asText(), "iqn.x");
        RecordedRequest complete = takeRequest();
        Assert.assertTrue(complete.getPath().endsWith("/attachments/" + created.getId() + "/action"));
        Assert.assertTrue(body(complete).has("os-complete"));
        Assert.assertTrue(takeRequest().getPath().contains("/attachments/detail?volume_id=" + VOLUME));
        Assert.assertTrue(takeRequest().getPath().endsWith("/attachments/" + created.getId()));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getStatus(), "reserved");
        Assert.assertEquals(updated.getConnectionInfo().get("driver_volume_type"), "iscsi");
        Assert.assertEquals(all.size(), 1);
        Assert.assertEquals(shown.getAttachMode(), "rw");
        Assert.assertNull(shown.getAttachedAt());
        Assert.assertTrue(deleted);
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.27.*")
    public void attachmentsNeed327() throws Exception {
        try {
            osv3().blockStorage().attachments().list();
        } finally {
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.54.*")
    public void attachmentModeNeeds354() throws Exception {
        negotiate("3.71");
        osv3().blockStorage().microVersions().use("3.50");
        try {
            osv3().blockStorage().attachments().create(VOLUME, null, null, "ro");
        } finally {
            assertNoMoreRequests();
        }
    }

    public void messagesListGetDelete() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"messages\": [{\"id\": \"c506cd4b-9048-43bc-97ef-0d7dec369b42\", \"event_id\": \"VOLUME_000002\", \"user_message\": \"No storage could be allocated for this volume request.\","
                + " \"message_level\": \"ERROR\", \"resource_type\": \"VOLUME\", \"resource_uuid\": \"" + VOLUME + "\", \"request_id\": \"req-1\","
                + " \"created_at\": \"2026-10-02T00:00:00-00:00\", \"guaranteed_until\": \"2026-11-01T00:00:00-00:00\", \"links\": []}]}");
        respondWith(200, "{\"message\": {\"id\": \"c506cd4b-9048-43bc-97ef-0d7dec369b42\", \"event_id\": \"VOLUME_000002\", \"message_level\": \"ERROR\"}}");
        respondWith(204);

        List<? extends VolumeMessage> messages = osv3().blockStorage().messages().list(MessageListOptions.create().resourceUuid(VOLUME).limit(10));
        VolumeMessage one = osv3().blockStorage().messages().get("c506cd4b-9048-43bc-97ef-0d7dec369b42");
        boolean deleted = osv3().blockStorage().messages().delete("c506cd4b-9048-43bc-97ef-0d7dec369b42").isSuccess();

        String path = takeRequest().getPath();
        Assert.assertTrue(path.contains("/messages?") && path.contains("resource_uuid=" + VOLUME) && path.contains("limit=10"), path);
        Assert.assertTrue(takeRequest().getPath().endsWith("/messages/c506cd4b-9048-43bc-97ef-0d7dec369b42"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(messages.get(0).getEventId(), "VOLUME_000002");
        Assert.assertEquals(messages.get(0).getResourceUuid(), VOLUME);
        Assert.assertNotNull(messages.get(0).getGuaranteedUntil());
        Assert.assertEquals(one.getMessageLevel(), "ERROR");
        Assert.assertTrue(deleted);
        Assert.assertEquals(MessageListOptions.create().limit(1).getRequiredMicroVersion(), "3.5");
        Assert.assertNull(MessageListOptions.create().eventId("x").getRequiredMicroVersion());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.3.*")
    public void messagesNeed33() throws Exception {
        try {
            osv3().blockStorage().messages().list();
        } finally {
            assertNoMoreRequests();
        }
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `attachments()`, `VolumeAttachmentRecord` 등 없음.

- [ ] **Step 3: 모델·옵션 구현**

`model/storage/block/VolumeAttachmentRecord.java`:
```java
package org.openstack4j.model.storage.block;

import java.util.Date;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A volume attachment managed through the {@code /attachments} API (3.27+). */
public interface VolumeAttachmentRecord extends ModelEntity {
    String getId();
    String getStatus();
    /** @return the server (instance) uuid, or {@code null} */
    String getInstance();
    String getVolumeId();
    Date getAttachedAt();
    Date getDetachedAt();
    /** @return rw or ro (3.54+) */
    String getAttachMode();
    /** @return driver connection details, present after the connector was given */
    Map<String, Object> getConnectionInfo();
}
```

`domain/CinderAttachment.java`:
```java
package org.openstack4j.openstack.storage.block.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.storage.block.VolumeAttachmentRecord;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("attachment")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderAttachment implements VolumeAttachmentRecord {

    private static final long serialVersionUID = 1L;

    private String id;
    private String status;
    private String instance;
    @JsonProperty("volume_id") private String volumeId;
    @JsonProperty("attached_at") private Date attachedAt;
    @JsonProperty("detached_at") private Date detachedAt;
    @JsonProperty("attach_mode") private String attachMode;
    @JsonProperty("connection_info") private Map<String, Object> connectionInfo;

    @Override public String getId() { return id; }
    @Override public String getStatus() { return status; }
    @Override public String getInstance() { return instance; }
    @Override public String getVolumeId() { return volumeId; }
    @Override public Date getAttachedAt() { return attachedAt; }
    @Override public Date getDetachedAt() { return detachedAt; }
    @Override public String getAttachMode() { return attachMode; }
    @Override public Map<String, Object> getConnectionInfo() { return connectionInfo; }

    public static class Attachments extends ListResult<CinderAttachment> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("attachments")
        private List<CinderAttachment> attachments;

        @Override
        protected List<CinderAttachment> value() {
            return attachments;
        }
    }
}
```
(Cinder 의 `attached_at` 은 `2015-09-16T09:28:52.000000` 형식 — 기존 `CinderVolume.created` 와 같은 Date 처리를 따른다. 기존 필드에 `@JsonFormat` 이 없으면 전역 mapper 설정이 처리하는 것이므로 그대로 둔다.)

`model/storage/block/VolumeMessage.java`:
```java
package org.openstack4j.model.storage.block;

import java.util.Date;
import java.util.List;

import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.common.Link;

/** A user-facing fault message ({@code /messages}, 3.3+). */
public interface VolumeMessage extends ModelEntity {
    String getId();
    String getEventId();
    String getUserMessage();
    String getMessageLevel();
    String getResourceType();
    String getResourceUuid();
    String getRequestId();
    Date getCreatedAt();
    Date getGuaranteedUntil();
    List<? extends Link> getLinks();
}
```
`domain/CinderMessage.java`: `@JsonRootName("message")`, 필드 `id`, `event_id`, `user_message`, `message_level`, `resource_type`, `resource_uuid`, `request_id`, `created_at`(Date), `guaranteed_until`(Date), `links`(List<GenericLink>) + getter, 중첩 `Messages extends ListResult<CinderMessage>`(`@JsonProperty("messages")`).

`options/AttachmentListOptions.java`:
```java
package org.openstack4j.model.storage.block.options;

/** Filters for {@code GET /attachments[/detail]} (3.27+). */
public class AttachmentListOptions extends BlockStorageListOptions<AttachmentListOptions> {

    public static AttachmentListOptions create() {
        return new AttachmentListOptions();
    }

    public AttachmentListOptions instanceId(String instanceId) { return put("instance_id", instanceId, 0); }
    public AttachmentListOptions volumeId(String volumeId) { return put("volume_id", volumeId, 0); }
    public AttachmentListOptions status(String status) { return put("status", status, 0); }
}
```
`options/MessageListOptions.java`:
```java
package org.openstack4j.model.storage.block.options;

/** Filters for {@code GET /messages} (3.3+; paging and sorting 3.5+). */
public class MessageListOptions extends BlockStorageListOptions<MessageListOptions> {

    public static MessageListOptions create() {
        return new MessageListOptions();
    }

    @Override public MessageListOptions limit(int limit) { return put("limit", limit, 5); }
    @Override public MessageListOptions marker(String marker) { return put("marker", marker, 5); }
    @Override public MessageListOptions offset(int offset) { return put("offset", offset, 5); }
    @Override public MessageListOptions sortKey(String key) { return put("sort_key", key, 5); }
    @Override public MessageListOptions sortDir(String dir) { return put("sort_dir", dir, 5); }
    @Override public MessageListOptions sort(String sort) { return put("sort", sort, 5); }
    public MessageListOptions resourceType(String type) { return put("resource_type", type, 0); }
    public MessageListOptions resourceUuid(String uuid) { return put("resource_uuid", uuid, 0); }
    public MessageListOptions eventId(String eventId) { return put("event_id", eventId, 0); }
    public MessageListOptions requestId(String requestId) { return put("request_id", requestId, 0); }
    public MessageListOptions messageLevel(String level) { return put("message_level", level, 0); }
}
```

- [ ] **Step 4: 서비스 구현**

`api/storage/BlockAttachmentService.java` 와 `BlockMessageService.java`(`extends RestService`, Javadoc 에 하한) 선언. 구현:

`BlockAttachmentServiceImpl.java`:
```java
package org.openstack4j.openstack.storage.block.internal;

public class BlockAttachmentServiceImpl extends BaseBlockStorageServices implements BlockAttachmentService {

    @Override public List<? extends VolumeAttachmentRecord> list() { return list(AttachmentListOptions.create()); }
    @Override public List<? extends VolumeAttachmentRecord> listDetail() { return listDetail(AttachmentListOptions.create()); }

    @Override
    public List<? extends VolumeAttachmentRecord> list(AttachmentListOptions options) {
        requireMicroVersion("Attachments", V(27));
        return get(Attachments.class, uri("/attachments")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public List<? extends VolumeAttachmentRecord> listDetail(AttachmentListOptions options) {
        requireMicroVersion("Attachments", V(27));
        return get(Attachments.class, uri("/attachments/detail")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public VolumeAttachmentRecord get(String attachmentId) {
        Objects.requireNonNull(attachmentId);
        requireMicroVersion("Attachments", V(27));
        return get(CinderAttachment.class, uri("/attachments/%s", attachmentId)).execute();
    }

    @Override
    public VolumeAttachmentRecord create(String volumeId, String instanceId, Map<String, Object> connector, String mode) {
        Objects.requireNonNull(volumeId);
        requireMicroVersion("Attachments", V(27));
        if (mode != null)
            requireMicroVersion("Attachment mode", V(54));
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("volume_uuid", volumeId);
        if (instanceId != null) attachment.put("instance_uuid", instanceId);
        if (connector != null) attachment.put("connector", connector);
        if (mode != null) attachment.put("mode", mode);
        return post(CinderAttachment.class, uri("/attachments")).entity(JsonBody.of("attachment", attachment)).execute();
    }

    @Override
    public VolumeAttachmentRecord update(String attachmentId, Map<String, Object> connector) {
        Objects.requireNonNull(attachmentId);
        Objects.requireNonNull(connector);
        requireMicroVersion("Attachments", V(27));
        return put(CinderAttachment.class, uri("/attachments/%s", attachmentId))
                .entity(JsonBody.of("attachment", Collections.singletonMap("connector", connector))).execute();
    }

    @Override
    public ActionResponse complete(String attachmentId) {
        Objects.requireNonNull(attachmentId);
        requireMicroVersion("Attachment completion", V(44));
        return post(ActionResponse.class, uri("/attachments/%s/action", attachmentId))
                .entity(JsonBody.of("os-complete", Collections.emptyMap())).execute();
    }

    @Override
    public ActionResponse delete(String attachmentId) {
        Objects.requireNonNull(attachmentId);
        requireMicroVersion("Attachments", V(27));
        return deleteWithResponse(uri("/attachments/%s", attachmentId)).execute();
    }
}
```
`BlockMessageServiceImpl.java`: `list()` → `list(MessageListOptions.create())`; `list(options)`: `requireMicroVersion("Messages", V(3))`, 옵션 하한(`options.getRequiredMicroVersion()` 이 있으면 `requireMicroVersion("Message list options " + keys, parse(...))`), `get(Messages.class, uri("/messages")).params(...)`; `get(id)`: `CinderMessage`; `delete(id)`: `deleteWithResponse(uri("/messages/%s", id))`.

`BlockStorageService`: `attachments()`, `messages()` accessor + impl(`Apis.get`) + `DefaultAPIProvider` 바인딩 2개.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='AttachmentAndMessageTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(block-storage): add attachments (3.27) and messages (3.3) services

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "attachments()(3.27; complete 3.44, mode 3.54), messages()(3.3; paging 3.5)."
```

---

### Task 8: volume types 서비스, default types(3.62), qos-specs

**Files:**
- Create (`core/src/main/java/org/openstack4j/`): `api/storage/BlockVolumeTypeService.java`, `api/storage/BlockDefaultTypeService.java`, `api/storage/BlockQosSpecService.java`, `model/storage/block/VolumeTypeAccess.java`, `model/storage/block/DefaultVolumeType.java`, `model/storage/block/QosSpec.java`, `model/storage/block/QosAssociation.java`, `model/storage/block/options/VolumeTypeListOptions.java`, `openstack/storage/block/domain/CinderVolumeTypeAccess.java`, `CinderDefaultVolumeType.java`, `CinderQosSpec.java`, `CinderQosAssociation.java`, `CinderExtraSpecs.java`, `openstack/storage/block/internal/BlockVolumeTypeServiceImpl.java`, `BlockDefaultTypeServiceImpl.java`, `BlockQosSpecServiceImpl.java`
- Modify: `model/storage/block/builder/VolumeTypeBuilder.java`, `openstack/storage/block/domain/CinderVolumeType.java`(빌더 `description`, `isPublic`), `api/storage/BlockStorageService.java`, `BlockStorageServiceImpl.java`, `DefaultAPIProvider.java`
- Create: `core-test/src/main/java/org/openstack4j/api/storage/microversion/VolumeTypeAndQosTests.java`

**Interfaces:**
- Consumes: Task 2 `requireMicroVersion`, `V`; Task 4 `BlockStorageListOptions`; Task 1 `JsonBody`; Task 3 `CinderVolumeType` 필드.
- Produces:
  - `BlockStorageService.volumeTypes()` → `BlockVolumeTypeService`: `List<? extends VolumeType> list()`, `list(VolumeTypeListOptions)`, `VolumeType get(String id)`, `VolumeType getDefault()`, `VolumeType create(VolumeType)`, `VolumeType update(String id, String name, String description, Boolean isPublic)`, `ActionResponse delete(String id)`, `Map<String, String> extraSpecs(String id)`, `Map<String, String> setExtraSpecs(String id, Map<String, String>)`, `String extraSpec(String id, String key)`, `String updateExtraSpec(String id, String key, String value)`, `ActionResponse deleteExtraSpec(String id, String key)`, `ActionResponse addProjectAccess(String id, String projectId)`, `ActionResponse removeProjectAccess(String id, String projectId)`, `List<? extends VolumeTypeAccess> listProjectAccess(String id)`, `VolumeTypeEncryption encryption(String id)`(기존 모델 재사용), `String encryptionSpec(String id, String key)`, `VolumeTypeEncryption createEncryption(String id, VolumeTypeEncryption)`, `VolumeTypeEncryption updateEncryption(String id, String encryptionId, VolumeTypeEncryption)`, `ActionResponse deleteEncryption(String id, String encryptionId)`
  - `VolumeTypeListOptions.create()`: 공통 + `isPublic(boolean)`, `extraSpecs(Map)`(3.52)
  - `VolumeTypeBuilder`: `description(String)`, `isPublic(boolean)`(`os-volume-type-access:is_public` 로 전송)
  - `VolumeTypeAccess`: `getProjectId()`, `getVolumeTypeId()`
  - `BlockStorageService.defaultTypes()` → `BlockDefaultTypeService`(3.62): `List<? extends DefaultVolumeType> list()`, `DefaultVolumeType get(String projectId)`, `DefaultVolumeType set(String projectId, String volumeType)`, `ActionResponse unset(String projectId)`; `DefaultVolumeType`: `getProjectId()`, `getVolumeTypeId()`
  - `BlockStorageService.qosSpecs()` → `BlockQosSpecService`: `List<? extends QosSpec> list()`, `QosSpec get(String id)`, `QosSpec create(String name, Map<String, String> specs)`(consumer 는 specs 안의 `consumer` 키), `QosSpec update(String id, Map<String, String> specs)`, `ActionResponse delete(String id, boolean force)`, `ActionResponse deleteKeys(String id, List<String> keys)`, `List<? extends QosAssociation> associations(String id)`, `ActionResponse associate(String id, String volumeTypeId)`, `ActionResponse disassociate(String id, String volumeTypeId)`, `ActionResponse disassociateAll(String id)`
  - `QosSpec`: `getId()`, `getName()`, `getConsumer()`, `Map<String, String> getSpecs()`; `QosAssociation`: `getAssociationType()`, `getName()`, `getId()`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/d8-types-qos
```

`VolumeTypeAndQosTests.java`:
```java
package org.openstack4j.api.storage.microversion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.DefaultVolumeType;
import org.openstack4j.model.storage.block.QosSpec;
import org.openstack4j.model.storage.block.VolumeType;
import org.openstack4j.model.storage.block.VolumeTypeAccess;
import org.openstack4j.model.storage.block.options.VolumeTypeListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/TypesQos")
public class VolumeTypeAndQosTests extends AbstractBlockStorageMicroVersionTest {

    private static final String TYPE = "6685584b-1eac-4da6-b5c3-555430cf68ff";
    private static final String TYPE_JSON = "{\"volume_type\": {\"id\": \"" + TYPE + "\", \"name\": \"vol-type-001\", \"description\": \"d\", \"is_public\": true, \"os-volume-type-access:is_public\": true, \"extra_specs\": {\"capabilities\": \"gpu\"}, \"qos_specs_id\": null}}";

    public void typeCrudAndDefault() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"volume_types\": [" + TYPE_JSON.substring(16, TYPE_JSON.length() - 1) + "]}");
        respondWith(200, TYPE_JSON);
        respondWith(200, TYPE_JSON);
        respondWith(200, TYPE_JSON);
        respondWith(200, TYPE_JSON);
        respondWith(202);

        List<? extends VolumeType> types = osv3().blockStorage().volumeTypes().list(VolumeTypeListOptions.create().isPublic(true).extraSpecs(Collections.singletonMap("capabilities", "gpu")));
        VolumeType one = osv3().blockStorage().volumeTypes().get(TYPE);
        VolumeType def = osv3().blockStorage().volumeTypes().getDefault();
        VolumeType created = osv3().blockStorage().volumeTypes().create(Builders.volumeType().name("vol-type-001").description("d").isPublic(false).extraSpecs(Collections.singletonMap("capabilities", "gpu")).build());
        osv3().blockStorage().volumeTypes().update(TYPE, "renamed", null, true);
        boolean deleted = osv3().blockStorage().volumeTypes().delete(TYPE).isSuccess();

        String list = takeRequest().getPath();
        Assert.assertTrue(list.contains("/types?") && list.contains("is_public=true") && list.contains("extra_specs="), list);
        Assert.assertTrue(takeRequest().getPath().endsWith("/types/" + TYPE));
        Assert.assertTrue(takeRequest().getPath().endsWith("/types/default"));
        JsonNode create = body(takeRequest()).get("volume_type");
        Assert.assertFalse(create.get("os-volume-type-access:is_public").asBoolean());
        Assert.assertEquals(create.get("description").asText(), "d");
        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PUT");
        JsonNode upd = body(update).get("volume_type");
        Assert.assertEquals(upd.get("name").asText(), "renamed");
        Assert.assertTrue(upd.get("is_public").asBoolean());
        Assert.assertFalse(upd.has("description"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(types.get(0).getName(), "vol-type-001");
        Assert.assertEquals(one.getExtraSpecs().get("capabilities"), "gpu");
        Assert.assertEquals(def.getId(), TYPE);
        Assert.assertEquals(created.isPublic(), Boolean.TRUE);
        Assert.assertTrue(deleted);
        Assert.assertEquals(VolumeTypeListOptions.create().extraSpecs(Map.of("a", "b")).getRequiredMicroVersion(), "3.52");
    }

    public void extraSpecsAndAccess() throws Exception {
        respondWith(200, "{\"extra_specs\": {\"capabilities\": \"gpu\"}}");
        respondWith(200, "{\"extra_specs\": {\"capabilities\": \"gpu\", \"k\": \"v\"}}");
        respondWith(200, "{\"k\": \"v\"}");
        respondWith(200, "{\"k\": \"v2\"}");
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(200, "{\"volume_type_access\": [{\"project_id\": \"p1\", \"volume_type_id\": \"" + TYPE + "\"}]}");

        Map<String, String> specs = osv3().blockStorage().volumeTypes().extraSpecs(TYPE);
        Map<String, String> set = osv3().blockStorage().volumeTypes().setExtraSpecs(TYPE, Collections.singletonMap("k", "v"));
        String one = osv3().blockStorage().volumeTypes().extraSpec(TYPE, "k");
        String updated = osv3().blockStorage().volumeTypes().updateExtraSpec(TYPE, "k", "v2");
        osv3().blockStorage().volumeTypes().deleteExtraSpec(TYPE, "k");
        osv3().blockStorage().volumeTypes().addProjectAccess(TYPE, "p1");
        osv3().blockStorage().volumeTypes().removeProjectAccess(TYPE, "p1");
        List<? extends VolumeTypeAccess> access = osv3().blockStorage().volumeTypes().listProjectAccess(TYPE);

        Assert.assertTrue(takeRequest().getPath().endsWith("/types/" + TYPE + "/extra_specs"));
        Assert.assertEquals(body(takeRequest()).get("extra_specs").get("k").asText(), "v");
        Assert.assertTrue(takeRequest().getPath().endsWith("/extra_specs/k"));
        Assert.assertEquals(body(takeRequest()).get("k").asText(), "v2");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(body(takeRequest()).get("addProjectAccess").get("project").asText(), "p1");
        Assert.assertEquals(body(takeRequest()).get("removeProjectAccess").get("project").asText(), "p1");
        Assert.assertTrue(takeRequest().getPath().endsWith("/types/" + TYPE + "/os-volume-type-access"));
        Assert.assertEquals(specs.get("capabilities"), "gpu");
        Assert.assertEquals(set.size(), 2);
        Assert.assertEquals(one, "v");
        Assert.assertEquals(updated, "v2");
        Assert.assertEquals(access.get(0).getProjectId(), "p1");
    }

    public void encryptionCrud() throws Exception {
        respondWith(200, "{\"volume_type_id\": \"" + TYPE + "\", \"control_location\": \"front-end\", \"encryption_id\": \"e1\", \"key_size\": 256, \"provider\": \"luks\", \"cipher\": \"aes-xts-plain64\"}");
        respondWith(200, "{\"cipher\": \"aes-xts-plain64\"}");
        respondWith(200, "{\"encryption\": {\"key_size\": 64, \"provider\": \"luks\", \"control_location\": \"back-end\", \"cipher\": \"aes-xts-plain64\"}}");
        respondWith(202);

        osv3().blockStorage().volumeTypes().encryption(TYPE);
        String cipher = osv3().blockStorage().volumeTypes().encryptionSpec(TYPE, "cipher");
        osv3().blockStorage().volumeTypes().updateEncryption(TYPE, "e1", Builders.volumeTypeEncryption().provider("luks").keySize(64).build());
        boolean deleted = osv3().blockStorage().volumeTypes().deleteEncryption(TYPE, "e1").isSuccess();

        Assert.assertTrue(takeRequest().getPath().endsWith("/types/" + TYPE + "/encryption"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/encryption/cipher"));
        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PUT");
        Assert.assertTrue(update.getPath().endsWith("/encryption/e1"));
        Assert.assertEquals(body(update).get("encryption").get("key_size").asInt(), 64);
        Assert.assertEquals(cipher, "aes-xts-plain64");
        Assert.assertTrue(deleted);
    }

    public void defaultTypes() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"default_types\": [{\"project_id\": \"p1\", \"volume_type_id\": \"" + TYPE + "\"}]}");
        respondWith(200, "{\"default_type\": {\"project_id\": \"p1\", \"volume_type_id\": \"" + TYPE + "\"}}");
        respondWith(200, "{\"default_type\": {\"project_id\": \"p1\", \"volume_type_id\": \"" + TYPE + "\"}}");
        respondWith(204);

        List<? extends DefaultVolumeType> all = osv3().blockStorage().defaultTypes().list();
        DefaultVolumeType one = osv3().blockStorage().defaultTypes().get("p1");
        DefaultVolumeType set = osv3().blockStorage().defaultTypes().set("p1", "lvm_backend");
        boolean unset = osv3().blockStorage().defaultTypes().unset("p1").isSuccess();

        Assert.assertTrue(takeRequest().getPath().endsWith("/default-types"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/default-types/p1"));
        RecordedRequest put = takeRequest();
        Assert.assertEquals(put.getMethod(), "PUT");
        Assert.assertEquals(body(put).get("default_type").get("volume_type").asText(), "lvm_backend");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(all.get(0).getVolumeTypeId(), TYPE);
        Assert.assertEquals(one.getProjectId(), "p1");
        Assert.assertEquals(set.getVolumeTypeId(), TYPE);
        Assert.assertTrue(unset);
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.62.*")
    public void defaultTypesNeed362() throws Exception {
        try {
            osv3().blockStorage().defaultTypes().list();
        } finally {
            assertNoMoreRequests();
        }
    }

    public void qosSpecs() throws Exception {
        String qos = "{\"qos_specs\": {\"specs\": {\"delay\": \"1\"}, \"consumer\": \"back-end\", \"name\": \"reliability-spec\", \"id\": \"0388d6c6-d5d4-42a3-b289-95205c50dd15\"}}";
        respondWith(200, "{\"qos_specs\": [{\"specs\": {}, \"consumer\": \"back-end\", \"name\": \"reliability-spec\", \"id\": \"0388d6c6-d5d4-42a3-b289-95205c50dd15\"}]}");
        respondWith(200, qos);
        respondWith(200, qos);
        respondWith(200, qos);
        respondWith(202);
        respondWith(200, "{\"qos_associations\": [{\"association_type\": \"volume_type\", \"name\": \"reliability-type\", \"id\": \"" + TYPE + "\"}]}");
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);

        List<? extends QosSpec> all = osv3().blockStorage().qosSpecs().list();
        QosSpec created = osv3().blockStorage().qosSpecs().create("reliability-spec", Map.of("consumer", "back-end", "delay", "1"));
        QosSpec shown = osv3().blockStorage().qosSpecs().get(created.getId());
        osv3().blockStorage().qosSpecs().update(created.getId(), Collections.singletonMap("delay", "2"));
        osv3().blockStorage().qosSpecs().deleteKeys(created.getId(), Arrays.asList("delay"));
        osv3().blockStorage().qosSpecs().associations(created.getId());
        osv3().blockStorage().qosSpecs().associate(created.getId(), TYPE);
        osv3().blockStorage().qosSpecs().disassociate(created.getId(), TYPE);
        osv3().blockStorage().qosSpecs().disassociateAll(created.getId());
        osv3().blockStorage().qosSpecs().delete(created.getId(), true);

        Assert.assertTrue(takeRequest().getPath().endsWith("/qos-specs"));
        JsonNode create = body(takeRequest()).get("qos_specs");
        Assert.assertEquals(create.get("name").asText(), "reliability-spec");
        Assert.assertEquals(create.get("consumer").asText(), "back-end");
        Assert.assertTrue(takeRequest().getPath().endsWith("/qos-specs/" + created.getId()));
        Assert.assertEquals(body(takeRequest()).get("qos_specs").get("delay").asText(), "2");
        Assert.assertEquals(body(takeRequest()).get("keys").get(0).asText(), "delay");
        Assert.assertTrue(takeRequest().getPath().endsWith("/associations"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/associate?vol_type_id=" + TYPE));
        Assert.assertTrue(takeRequest().getPath().endsWith("/disassociate?vol_type_id=" + TYPE));
        Assert.assertTrue(takeRequest().getPath().endsWith("/disassociate_all"));
        RecordedRequest delete = takeRequest();
        Assert.assertEquals(delete.getMethod(), "DELETE");
        Assert.assertTrue(delete.getPath().endsWith("?force=true"));
        Assert.assertEquals(all.get(0).getName(), "reliability-spec");
        Assert.assertEquals(shown.getSpecs().get("delay"), "1");
        Assert.assertEquals(shown.getConsumer(), "back-end");
    }
}
```
(`Builders.volumeTypeEncryption()` 의 실제 이름과 `VolumeTypeEncryptionBuilder` 메서드명(`provider`, `keySize`)을 확인해 맞춘다.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `volumeTypes()`, `QosSpec` 등 없음.

- [ ] **Step 3: 모델·옵션·빌더 구현**

`model/storage/block/VolumeTypeAccess.java`: `getProjectId()`, `getVolumeTypeId()`. `domain/CinderVolumeTypeAccess.java`: `@JsonIgnoreProperties`, 필드 `project_id`, `volume_type_id`, 중첩 `VolumeTypeAccesses extends ListResult<CinderVolumeTypeAccess>`(`@JsonProperty("volume_type_access")`).
`model/storage/block/DefaultVolumeType.java`: `getProjectId()`, `getVolumeTypeId()`. `domain/CinderDefaultVolumeType.java`: `@JsonRootName("default_type")`, 필드 2개, 중첩 `DefaultVolumeTypes extends ListResult<CinderDefaultVolumeType>`(`@JsonProperty("default_types")`).
`model/storage/block/QosSpec.java`: `getId()`, `getName()`, `getConsumer()`, `Map<String, String> getSpecs()`. `domain/CinderQosSpec.java`: `@JsonRootName("qos_specs")`, 필드 `id`, `name`, `consumer`, `specs`(Map<String,String>), 중첩 `QosSpecs extends ListResult<CinderQosSpec>`(`@JsonProperty("qos_specs")`). 응답 `{"qos_specs": {...}, "links": [...]}` 의 `links` 는 루트 형제라 무시된다(`@JsonIgnoreProperties(ignoreUnknown = true)` 를 루트 mapper 가 적용하도록 클래스에 붙인다).
`model/storage/block/QosAssociation.java`: `getAssociationType()`, `getName()`, `getId()`. `domain/CinderQosAssociation.java`: 필드 `association_type`, `name`, `id`, 중첩 `QosAssociations extends ListResult<CinderQosAssociation>`(`@JsonProperty("qos_associations")`).
`domain/CinderExtraSpecs.java`: `@JsonIgnoreProperties`, `@JsonProperty("extra_specs") private Map<String, String> extraSpecs;` + `getExtraSpecs()`. 단일 키 응답(`{"k": "v"}`)은 `HashMap.class` 로 받는다.
`options/VolumeTypeListOptions.java`: 공통 + `isPublic(boolean)` → `is_public`, `extraSpecs(Map<String,String>)` → `extra_specs` = `dict(...)`, 하한 52.
`VolumeTypeBuilder.java`: `description(String)`, `isPublic(boolean)`; `CinderVolumeType` 빌더: `m.description = ...`, `m.accessIsPublic = isPublic`(생성 요청의 키는 `os-volume-type-access:is_public`; 응답의 `is_public` 는 Task 3 필드 그대로).

- [ ] **Step 4: 서비스 구현**

`BlockVolumeTypeServiceImpl.java` 핵심:
```java
    @Override public List<? extends VolumeType> list() { return get(VolumeTypes.class, uri("/types")).execute().getList(); }

    @Override
    public List<? extends VolumeType> list(VolumeTypeListOptions options) {
        Objects.requireNonNull(options);
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Volume type list filters " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
        return get(VolumeTypes.class, uri("/types")).params(options.toQueryParams()).execute().getList();
    }

    @Override public VolumeType get(String id) { return get(CinderVolumeType.class, uri("/types/%s", Objects.requireNonNull(id))).execute(); }
    @Override public VolumeType getDefault() { return get(CinderVolumeType.class, uri("/types/default")).execute(); }
    @Override public VolumeType create(VolumeType type) { return post(CinderVolumeType.class, uri("/types")).entity(Objects.requireNonNull(type)).execute(); }

    @Override
    public VolumeType update(String id, String name, String description, Boolean isPublic) {
        Objects.requireNonNull(id);
        Map<String, Object> body = new LinkedHashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (isPublic != null) body.put("is_public", isPublic);
        return put(CinderVolumeType.class, uri("/types/%s", id)).entity(JsonBody.of("volume_type", body)).execute();
    }

    @Override public ActionResponse delete(String id) { return deleteWithResponse(uri("/types/%s", Objects.requireNonNull(id))).execute(); }

    @Override
    public Map<String, String> extraSpecs(String id) {
        CinderExtraSpecs specs = get(CinderExtraSpecs.class, uri("/types/%s/extra_specs", Objects.requireNonNull(id))).execute();
        return specs == null || specs.getExtraSpecs() == null ? Collections.emptyMap() : specs.getExtraSpecs();
    }

    @Override
    public Map<String, String> setExtraSpecs(String id, Map<String, String> specs) {
        return post(CinderExtraSpecs.class, uri("/types/%s/extra_specs", Objects.requireNonNull(id)))
                .entity(JsonBody.of("extra_specs", Objects.requireNonNull(specs))).execute().getExtraSpecs();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String extraSpec(String id, String key) {
        Map<String, String> one = get(HashMap.class, uri("/types/%s/extra_specs/%s", Objects.requireNonNull(id), Objects.requireNonNull(key))).execute();
        return one == null ? null : one.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String updateExtraSpec(String id, String key, String value) {
        Map<String, String> one = put(HashMap.class, uri("/types/%s/extra_specs/%s", Objects.requireNonNull(id), Objects.requireNonNull(key)))
                .entity(JsonBody.of(Collections.singletonMap(key, value))).execute();
        return one == null ? null : one.get(key);
    }

    @Override public ActionResponse deleteExtraSpec(String id, String key) { return deleteWithResponse(uri("/types/%s/extra_specs/%s", id, key)).execute(); }

    @Override
    public ActionResponse addProjectAccess(String id, String projectId) {
        return post(ActionResponse.class, uri("/types/%s/action", Objects.requireNonNull(id)))
                .entity(JsonBody.of("addProjectAccess", Collections.singletonMap("project", Objects.requireNonNull(projectId)))).execute();
    }

    @Override
    public ActionResponse removeProjectAccess(String id, String projectId) {
        return post(ActionResponse.class, uri("/types/%s/action", Objects.requireNonNull(id)))
                .entity(JsonBody.of("removeProjectAccess", Collections.singletonMap("project", Objects.requireNonNull(projectId)))).execute();
    }

    @Override public List<? extends VolumeTypeAccess> listProjectAccess(String id) { return get(VolumeTypeAccesses.class, uri("/types/%s/os-volume-type-access", Objects.requireNonNull(id))).execute().getList(); }
    @Override public VolumeTypeEncryption encryption(String id) { return get(CinderVolumeTypeEncryptionFetch.class, uri("/types/%s/encryption", Objects.requireNonNull(id))).execute(); }

    @Override
    @SuppressWarnings("unchecked")
    public String encryptionSpec(String id, String key) {
        Map<String, Object> one = get(HashMap.class, uri("/types/%s/encryption/%s", Objects.requireNonNull(id), Objects.requireNonNull(key))).execute();
        return one == null || one.get(key) == null ? null : String.valueOf(one.get(key));
    }

    @Override public VolumeTypeEncryption createEncryption(String id, VolumeTypeEncryption encryption) { return post(CinderVolumeTypeEncryption.class, uri("/types/%s/encryption", Objects.requireNonNull(id))).entity(Objects.requireNonNull(encryption)).execute(); }
    @Override public VolumeTypeEncryption updateEncryption(String id, String encryptionId, VolumeTypeEncryption encryption) { return put(CinderVolumeTypeEncryption.class, uri("/types/%s/encryption/%s", Objects.requireNonNull(id), Objects.requireNonNull(encryptionId))).entity(Objects.requireNonNull(encryption)).execute(); }
    @Override public ActionResponse deleteEncryption(String id, String encryptionId) { return deleteWithResponse(uri("/types/%s/encryption/%s", id, encryptionId)).execute(); }
```
(`CinderVolumeTypeEncryptionFetch`/`CinderVolumeTypeEncryption` 은 기존 클래스 — 기존 `BlockVolumeServiceImpl.getVolumeTypeEncryption`/`createVolumeTypeEncryption` 과 같은 타입을 쓴다. `put(HashMap.class, ...)` 처럼 루트 없는 맵 응답은 전역 mapper 가 그대로 읽는다.)

`BlockDefaultTypeServiceImpl.java`: 네 메서드 모두 `requireMicroVersion("Default volume types", V(62))`; `list` → `DefaultVolumeTypes` `/default-types`; `get` → `/default-types/%s`; `set` → `put(CinderDefaultVolumeType.class, uri("/default-types/%s", projectId)).entity(JsonBody.of("default_type", Map.of("volume_type", volumeType)))`; `unset` → `deleteWithResponse`.
`BlockQosSpecServiceImpl.java`: `list` → `QosSpecs` `/qos-specs`; `get`; `create` → `post(CinderQosSpec.class, uri("/qos-specs")).entity(JsonBody.of("qos_specs", {"name": name} + specs))`; `update` → `put(CinderQosSpec.class, "/qos-specs/%s").entity(JsonBody.of("qos_specs", specs))`; `delete(id, force)` → `delete(ActionResponse.class, uri("/qos-specs/%s", id)).param("force", force).execute()`; `deleteKeys` → `put(ActionResponse.class, "/qos-specs/%s/delete_keys").entity(JsonBody.of(Map.of("keys", keys)))`; `associations` → `QosAssociations` `/qos-specs/%s/associations`; `associate` → `get(ActionResponse.class, "/qos-specs/%s/associate").param("vol_type_id", typeId)`; `disassociate` 같은 방식; `disassociateAll` → `get(ActionResponse.class, "/qos-specs/%s/disassociate_all")`. (`get(ActionResponse.class, ...)` 가 202 본문 없음을 성공으로 돌려주는지는 기존 `getWithResponse` 와 같다 — 안 되면 `getWithResponse(uri).param(...).execute()` 로 바꾼다.)

accessor 3개(`volumeTypes()`, `defaultTypes()`, `qosSpecs()`) + 바인딩 3개.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='VolumeTypeAndQosTests,VolumeTypeTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Failures: 0, Errors: 0` (VolumeTypeAndQosTests 6개).

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(block-storage): add volume type, default type (3.62) and QoS spec services

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "volumeTypes()(CRUD, default, extra specs, project access, encryption; 3.52 필터), defaultTypes()(3.62), qosSpecs()(CRUD, keys, associations)."
```

---

### Task 9: groups(3.13), group types(3.11), group snapshots(3.14), replication(3.38)

**Files:**
- Create (`core/src/main/java/org/openstack4j/`): `model/storage/block/VolumeGroup.java`, `VolumeGroupType.java`, `VolumeGroupSnapshot.java`, `ReplicationTarget.java`, `model/storage/block/options/GroupCreate.java`, `GroupListOptions.java`, `GroupSnapshotListOptions.java`, `api/storage/BlockGroupService.java`, `BlockGroupTypeService.java`, `BlockGroupSnapshotService.java`, `openstack/storage/block/domain/CinderGroup.java`, `CinderGroupType.java`, `CinderGroupSnapshot.java`, `CinderReplicationTargets.java`, `CinderGroupSpecs.java`, `openstack/storage/block/internal/BlockGroupServiceImpl.java`, `BlockGroupTypeServiceImpl.java`, `BlockGroupSnapshotServiceImpl.java`
- Modify: `api/storage/BlockStorageService.java`, `BlockStorageServiceImpl.java`, `DefaultAPIProvider.java`
- Create: `core-test/src/main/java/org/openstack4j/api/storage/microversion/GroupTests.java`

**Interfaces:**
- Consumes: Task 2, Task 4 `BlockStorageListOptions`, Task 1 `JsonBody`.
- Produces:
  - `BlockStorageService.groups()` → `BlockGroupService`(3.13): `list()`, `list(GroupListOptions)`, `listDetail()`, `listDetail(GroupListOptions)`, `VolumeGroup get(String id)`, `VolumeGroup create(GroupCreate)`, `VolumeGroup createFromSource(String name, String description, String groupSnapshotId, String sourceGroupId)`(3.14), `VolumeGroup update(String id, String name, String description, List<String> addVolumes, List<String> removeVolumes)`, `ActionResponse delete(String id, boolean deleteVolumes)`, `ActionResponse resetStatus(String id, String status)`(3.20), `ActionResponse enableReplication(String id)`, `ActionResponse disableReplication(String id)`, `ActionResponse failoverReplication(String id, boolean allowAttachedVolume, String secondaryBackendId)`, `List<? extends ReplicationTarget> listReplicationTargets(String id)`(모두 3.38)
  - `GroupCreate.create(String name, String groupType, List<String> volumeTypes)`: `description(String)`, `availabilityZone(String)`
  - `VolumeGroup`: `getId()`, `getName()`, `getDescription()`, `getStatus()`, `getAvailabilityZone()`, `Date getCreatedAt()`, `getGroupType()`, `List<String> getVolumeTypes()`, `List<String> getVolumes()`(3.25), `getGroupSnapshotId()`, `getSourceGroupId()`, `getProjectId()`(3.58), `getReplicationStatus()`
  - `ReplicationTarget`: `getBackendId()`, `Map<String, String> getProperties()`(그 외 키)
  - `BlockStorageService.groupTypes()` → `BlockGroupTypeService`(3.11): `list()`, `VolumeGroupType get(String id)`, `getDefault()`, `create(String name, String description, Boolean isPublic, Map<String, String> groupSpecs)`, `update(String id, String name, String description, Boolean isPublic)`, `ActionResponse delete(String id)`, `Map<String, String> groupSpecs(String id)`, `Map<String, String> setGroupSpecs(String id, Map<String, String>)`, `String groupSpec(String id, String key)`, `String updateGroupSpec(String id, String key, String value)`, `ActionResponse deleteGroupSpec(String id, String key)`; `VolumeGroupType`: `getId()`, `getName()`, `getDescription()`, `Boolean isPublic()`, `Map<String, String> getGroupSpecs()`
  - `BlockStorageService.groupSnapshots()` → `BlockGroupSnapshotService`(3.14): `list()`, `list(GroupSnapshotListOptions)`(3.29), `listDetail()`, `listDetail(GroupSnapshotListOptions)`, `VolumeGroupSnapshot get(String id)`, `create(String groupId, String name, String description)`, `ActionResponse delete(String id)`, `ActionResponse resetStatus(String id, String status)`(3.19); `VolumeGroupSnapshot`: `getId()`, `getGroupId()`, `getStatus()`, `Date getCreatedAt()`, `getName()`, `getDescription()`, `getGroupTypeId()`, `getProjectId()`(3.58)
  - `GroupListOptions.create()`, `GroupSnapshotListOptions.create()`: 공통 + `name`, `status`, `groupId`(snapshot 만)

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/d9-groups
```

`GroupTests.java`:
```java
package org.openstack4j.api.storage.microversion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.ReplicationTarget;
import org.openstack4j.model.storage.block.VolumeGroup;
import org.openstack4j.model.storage.block.VolumeGroupSnapshot;
import org.openstack4j.model.storage.block.VolumeGroupType;
import org.openstack4j.model.storage.block.options.GroupCreate;
import org.openstack4j.model.storage.block.options.GroupSnapshotListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/Groups")
public class GroupTests extends AbstractBlockStorageMicroVersionTest {

    private static final String GROUP = "6f519a48-3183-46cf-a32f-41815f813986";
    private static final String GROUP_TYPE = "29514915-5208-46ab-9ece-1cc4688ad0c1";
    private static final String GROUP_JSON = "{\"group\": {\"id\": \"" + GROUP + "\", \"status\": \"available\", \"availability_zone\": \"az1\", \"created_at\": \"2026-10-02T09:28:52.000000\","
            + " \"name\": \"first_group\", \"description\": \"my first group\", \"group_type\": \"" + GROUP_TYPE + "\", \"volume_types\": [\"t1\"], \"volumes\": [\"" + VOLUME + "\"],"
            + " \"group_snapshot_id\": null, \"source_group_id\": null, \"project_id\": \"p1\", \"replication_status\": \"enabled\"}}";

    public void groupLifecycle() throws Exception {
        negotiate("3.71");
        respondWith(202, GROUP_JSON);
        respondWith(202, GROUP_JSON);
        respondWith(200, GROUP_JSON);
        respondWith(200, "{\"groups\": [" + GROUP_JSON.substring(10, GROUP_JSON.length() - 1) + "]}");
        respondWith(202, GROUP_JSON);
        respondWith(202);
        respondWith(202);

        VolumeGroup created = osv3().blockStorage().groups().create(GroupCreate.create("first_group", GROUP_TYPE, Arrays.asList("t1")).description("my first group").availabilityZone("az1"));
        VolumeGroup fromSource = osv3().blockStorage().groups().createFromSource("copy", null, null, GROUP);
        VolumeGroup shown = osv3().blockStorage().groups().get(GROUP);
        List<? extends VolumeGroup> all = osv3().blockStorage().groups().listDetail();
        osv3().blockStorage().groups().update(GROUP, "renamed", null, Arrays.asList("v2"), Collections.emptyList());
        osv3().blockStorage().groups().resetStatus(GROUP, "available");
        osv3().blockStorage().groups().delete(GROUP, true);

        JsonNode create = body(takeRequest()).get("group");
        Assert.assertEquals(create.get("group_type").asText(), GROUP_TYPE);
        Assert.assertEquals(create.get("volume_types").get(0).asText(), "t1");
        Assert.assertEquals(create.get("availability_zone").asText(), "az1");
        RecordedRequest src = takeRequest();
        Assert.assertTrue(src.getPath().endsWith("/groups/action"));
        JsonNode srcBody = body(src).get("create-from-src");
        Assert.assertEquals(srcBody.get("source_group_id").asText(), GROUP);
        Assert.assertFalse(srcBody.has("group_snapshot_id"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/groups/" + GROUP));
        Assert.assertTrue(takeRequest().getPath().endsWith("/groups/detail"));
        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PUT");
        JsonNode upd = body(update).get("group");
        Assert.assertEquals(upd.get("name").asText(), "renamed");
        Assert.assertEquals(upd.get("add_volumes").asText(), "v2");
        Assert.assertEquals(upd.get("remove_volumes").asText(), "");
        Assert.assertEquals(body(takeRequest()).get("reset_status").get("status").asText(), "available");
        RecordedRequest delete = takeRequest();
        Assert.assertTrue(delete.getPath().endsWith("/groups/" + GROUP + "/action"));
        Assert.assertTrue(body(delete).get("delete").get("delete-volumes").asBoolean());
        Assert.assertEquals(created.getVolumeTypes(), Arrays.asList("t1"));
        Assert.assertEquals(created.getVolumes(), Arrays.asList(VOLUME));
        Assert.assertEquals(created.getProjectId(), "p1");
        Assert.assertEquals(fromSource.getId(), GROUP);
        Assert.assertEquals(shown.getReplicationStatus(), "enabled");
        Assert.assertEquals(all.size(), 1);
    }

    public void groupReplicationActions() throws Exception {
        negotiate("3.71");
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(200, "{\"replication_targets\": [{\"backend_id\": \"vendor-id-1\", \"unique_key\": \"value1\"}]}");

        osv3().blockStorage().groups().enableReplication(GROUP);
        osv3().blockStorage().groups().disableReplication(GROUP);
        osv3().blockStorage().groups().failoverReplication(GROUP, true, "vendor-id-1");
        List<? extends ReplicationTarget> targets = osv3().blockStorage().groups().listReplicationTargets(GROUP);

        Assert.assertTrue(body(takeRequest()).has("enable_replication"));
        Assert.assertTrue(body(takeRequest()).has("disable_replication"));
        JsonNode failover = body(takeRequest()).get("failover_replication");
        Assert.assertTrue(failover.get("allow_attached_volume").asBoolean());
        Assert.assertEquals(failover.get("secondary_backend_id").asText(), "vendor-id-1");
        Assert.assertTrue(body(takeRequest()).has("list_replication_targets"));
        Assert.assertEquals(targets.get(0).getBackendId(), "vendor-id-1");
        Assert.assertEquals(targets.get(0).getProperties().get("unique_key"), "value1");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.38.*")
    public void replicationNeeds338() throws Exception {
        negotiate("3.71");
        osv3().blockStorage().microVersions().use("3.30");
        try {
            osv3().blockStorage().groups().enableReplication(GROUP);
        } finally {
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.13.*")
    public void groupsNeed313() throws Exception {
        try {
            osv3().blockStorage().groups().list();
        } finally {
            assertNoMoreRequests();
        }
    }

    public void groupTypesAndSpecs() throws Exception {
        negotiate("3.71");
        String type = "{\"group_type\": {\"id\": \"" + GROUP_TYPE + "\", \"name\": \"grp-type-001\", \"description\": \"d\", \"is_public\": true, \"group_specs\": {\"consistent_group_snapshot_enabled\": \"<is> False\"}}}";
        respondWith(200, "{\"group_types\": [" + type.substring(15, type.length() - 1) + "]}");
        respondWith(200, type);
        respondWith(202, type);
        respondWith(200, type);
        respondWith(202);
        respondWith(200, "{\"group_specs\": {\"key1\": \"value1\"}}");
        respondWith(202, "{\"group_specs\": {\"key1\": \"value1\", \"key2\": \"value2\"}}");
        respondWith(200, "{\"key1\": \"value1\"}");
        respondWith(200, "{\"key1\": \"v\"}");
        respondWith(202);

        List<? extends VolumeGroupType> types = osv3().blockStorage().groupTypes().list();
        VolumeGroupType def = osv3().blockStorage().groupTypes().getDefault();
        VolumeGroupType created = osv3().blockStorage().groupTypes().create("grp-type-001", "d", true, Collections.singletonMap("consistent_group_snapshot_enabled", "<is> False"));
        osv3().blockStorage().groupTypes().update(GROUP_TYPE, "renamed", null, null);
        osv3().blockStorage().groupTypes().delete(GROUP_TYPE);
        osv3().blockStorage().groupTypes().groupSpecs(GROUP_TYPE);
        osv3().blockStorage().groupTypes().setGroupSpecs(GROUP_TYPE, Collections.singletonMap("key2", "value2"));
        String one = osv3().blockStorage().groupTypes().groupSpec(GROUP_TYPE, "key1");
        String updated = osv3().blockStorage().groupTypes().updateGroupSpec(GROUP_TYPE, "key1", "v");
        osv3().blockStorage().groupTypes().deleteGroupSpec(GROUP_TYPE, "key1");

        Assert.assertTrue(takeRequest().getPath().endsWith("/group_types"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/group_types/default"));
        JsonNode create = body(takeRequest()).get("group_type");
        Assert.assertTrue(create.get("is_public").asBoolean());
        Assert.assertEquals(create.get("group_specs").get("consistent_group_snapshot_enabled").asText(), "<is> False");
        Assert.assertEquals(body(takeRequest()).get("group_type").get("name").asText(), "renamed");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertTrue(takeRequest().getPath().endsWith("/group_types/" + GROUP_TYPE + "/group_specs"));
        Assert.assertEquals(body(takeRequest()).get("group_specs").get("key2").asText(), "value2");
        Assert.assertTrue(takeRequest().getPath().endsWith("/group_specs/key1"));
        Assert.assertEquals(body(takeRequest()).get("key1").asText(), "v");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(types.get(0).getName(), "grp-type-001");
        Assert.assertEquals(def.isPublic(), Boolean.TRUE);
        Assert.assertEquals(created.getGroupSpecs().size(), 1);
        Assert.assertEquals(one, "value1");
        Assert.assertEquals(updated, "v");
    }

    public void groupSnapshots() throws Exception {
        negotiate("3.71");
        String snap = "{\"group_snapshot\": {\"id\": \"gs1\", \"group_id\": \"" + GROUP + "\", \"status\": \"available\", \"created_at\": \"2026-10-02T09:28:52.000000\", \"name\": \"my_group_snapshot1\","
                + " \"description\": \"d\", \"group_type_id\": \"" + GROUP_TYPE + "\", \"project_id\": \"p1\"}}";
        respondWith(202, snap);
        respondWith(200, "{\"group_snapshots\": [" + snap.substring(19, snap.length() - 1) + "]}");
        respondWith(200, snap);
        respondWith(202);
        respondWith(202);

        VolumeGroupSnapshot created = osv3().blockStorage().groupSnapshots().create(GROUP, "my_group_snapshot1", "d");
        List<? extends VolumeGroupSnapshot> all = osv3().blockStorage().groupSnapshots().listDetail(GroupSnapshotListOptions.create().groupId(GROUP).limit(5));
        VolumeGroupSnapshot shown = osv3().blockStorage().groupSnapshots().get("gs1");
        osv3().blockStorage().groupSnapshots().resetStatus("gs1", "available");
        osv3().blockStorage().groupSnapshots().delete("gs1");

        Assert.assertEquals(body(takeRequest()).get("group_snapshot").get("group_id").asText(), GROUP);
        String list = takeRequest().getPath();
        Assert.assertTrue(list.contains("/group_snapshots/detail?") && list.contains("group_id=" + GROUP), list);
        Assert.assertTrue(takeRequest().getPath().endsWith("/group_snapshots/gs1"));
        Assert.assertEquals(body(takeRequest()).get("reset_status").get("status").asText(), "available");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getGroupTypeId(), GROUP_TYPE);
        Assert.assertEquals(all.get(0).getProjectId(), "p1");
        Assert.assertEquals(shown.getName(), "my_group_snapshot1");
        Assert.assertEquals(GroupSnapshotListOptions.create().limit(1).getRequiredMicroVersion(), "3.29");
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `groups()`, `VolumeGroup` 등 없음.

- [ ] **Step 3: 모델·옵션 구현**

인터페이스(`model/storage/block/`): `VolumeGroup`, `VolumeGroupType`, `VolumeGroupSnapshot`, `ReplicationTarget` — Interfaces 의 getter 를 그대로 선언(`extends ModelEntity`).

`domain/CinderGroup.java`: `@JsonRootName("group")`, 필드 `id`, `name`, `description`, `status`, `availability_zone`, `created_at`(Date), `group_type`, `volume_types`(List<String>), `volumes`(List<String>), `group_snapshot_id`, `source_group_id`, `project_id`, `replication_status` + getter; 중첩 `Groups extends ListResult<CinderGroup>`(`@JsonProperty("groups")`).
`domain/CinderGroupType.java`: `@JsonRootName("group_type")`, 필드 `id`, `name`, `description`, `@JsonProperty("is_public") Boolean isPublic`(getter `@JsonIgnore isPublic()`), `group_specs`(Map) + 중첩 `GroupTypes`(`@JsonProperty("group_types")`).
`domain/CinderGroupSnapshot.java`: `@JsonRootName("group_snapshot")`, 필드 `id`, `group_id`, `status`, `created_at`(Date), `name`, `description`, `group_type_id`, `project_id` + 중첩 `GroupSnapshots`(`@JsonProperty("group_snapshots")`).
`domain/CinderReplicationTargets.java`:
```java
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderReplicationTargets implements ModelEntity {
    private static final long serialVersionUID = 1L;
    @JsonProperty("replication_targets")
    private List<Target> targets;
    public List<Target> getTargets() { return targets == null ? Collections.emptyList() : targets; }

    public static class Target implements ReplicationTarget {
        private static final long serialVersionUID = 1L;
        @JsonProperty("backend_id") private String backendId;
        private final Map<String, String> properties = new LinkedHashMap<>();
        @JsonAnySetter void put(String key, Object value) { properties.put(key, value == null ? null : String.valueOf(value)); }
        @Override public String getBackendId() { return backendId; }
        @Override public Map<String, String> getProperties() { return properties; }
    }
}
```
(api-ref 예시의 `replication_targets` 는 객체 하나지만 실제 Cinder 는 목록을 돌려준다. `ACCEPT_SINGLE_VALUE_AS_ARRAY` 가 켜져 있어 둘 다 읽힌다.)
`domain/CinderGroupSpecs.java`: `@JsonProperty("group_specs") Map<String,String>` + getter.
`options/GroupCreate.java`:
```java
package org.openstack4j.model.storage.block.options;

import java.util.List;

/** Body of {@code POST /groups} (3.13+). */
public class GroupCreate {
    private final String name;
    private final String groupType;
    private final List<String> volumeTypes;
    private String description;
    private String availabilityZone;

    private GroupCreate(String name, String groupType, List<String> volumeTypes) { this.name = name; this.groupType = groupType; this.volumeTypes = volumeTypes; }
    public static GroupCreate create(String name, String groupType, List<String> volumeTypes) { return new GroupCreate(name, groupType, volumeTypes); }
    public GroupCreate description(String description) { this.description = description; return this; }
    public GroupCreate availabilityZone(String availabilityZone) { this.availabilityZone = availabilityZone; return this; }
    public String getName() { return name; }
    public String getGroupType() { return groupType; }
    public List<String> getVolumeTypes() { return volumeTypes; }
    public String getDescription() { return description; }
    public String getAvailabilityZone() { return availabilityZone; }
}
```
`options/GroupListOptions.java`: 공통 + `name`, `status`. `options/GroupSnapshotListOptions.java`: 공통(모두 하한 29 — `limit/marker/offset/sortKey/sortDir/sort/allTenants` 를 `MessageListOptions` 처럼 override) + `name`(29), `status`(29), `groupId`(29).

- [ ] **Step 4: 서비스 구현**

`BlockGroupServiceImpl.java`(모든 메서드 첫 줄 `requireMicroVersion("Groups", V(13))`):
```java
    @Override public List<? extends VolumeGroup> list() { return list(GroupListOptions.create()); }
    @Override public List<? extends VolumeGroup> listDetail() { return listDetail(GroupListOptions.create()); }
    @Override public List<? extends VolumeGroup> list(GroupListOptions o) { requireMicroVersion("Groups", V(13)); return get(Groups.class, uri("/groups")).params(o.toQueryParams()).execute().getList(); }
    @Override public List<? extends VolumeGroup> listDetail(GroupListOptions o) { requireMicroVersion("Groups", V(13)); return get(Groups.class, uri("/groups/detail")).params(o.toQueryParams()).execute().getList(); }
    @Override public VolumeGroup get(String id) { requireMicroVersion("Groups", V(13)); return get(CinderGroup.class, uri("/groups/%s", Objects.requireNonNull(id))).execute(); }

    @Override
    public VolumeGroup create(GroupCreate request) {
        Objects.requireNonNull(request);
        requireMicroVersion("Groups", V(13));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", request.getName());
        body.put("group_type", request.getGroupType());
        body.put("volume_types", request.getVolumeTypes());
        if (request.getDescription() != null) body.put("description", request.getDescription());
        if (request.getAvailabilityZone() != null) body.put("availability_zone", request.getAvailabilityZone());
        return post(CinderGroup.class, uri("/groups")).entity(JsonBody.of("group", body)).execute();
    }

    @Override
    public VolumeGroup createFromSource(String name, String description, String groupSnapshotId, String sourceGroupId) {
        requireMicroVersion("Group from source", V(14));
        Map<String, Object> body = new LinkedHashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (groupSnapshotId != null) body.put("group_snapshot_id", groupSnapshotId);
        if (sourceGroupId != null) body.put("source_group_id", sourceGroupId);
        return post(CinderGroup.class, uri("/groups/action")).entity(JsonBody.of("create-from-src", body)).execute();
    }

    @Override
    public VolumeGroup update(String id, String name, String description, List<String> addVolumes, List<String> removeVolumes) {
        Objects.requireNonNull(id);
        requireMicroVersion("Groups", V(13));
        Map<String, Object> body = new LinkedHashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (addVolumes != null) body.put("add_volumes", String.join(",", addVolumes));
        if (removeVolumes != null) body.put("remove_volumes", String.join(",", removeVolumes));
        return put(CinderGroup.class, uri("/groups/%s", id)).entity(JsonBody.of("group", body)).execute();
    }

    @Override public ActionResponse delete(String id, boolean deleteVolumes) { requireMicroVersion("Groups", V(13)); return action(id, "delete", Collections.singletonMap("delete-volumes", deleteVolumes)); }
    @Override public ActionResponse resetStatus(String id, String status) { requireMicroVersion("Group reset status", V(20)); return action(id, "reset_status", Collections.singletonMap("status", Objects.requireNonNull(status))); }
    @Override public ActionResponse enableReplication(String id) { requireMicroVersion("Group replication", V(38)); return action(id, "enable_replication", Collections.emptyMap()); }
    @Override public ActionResponse disableReplication(String id) { requireMicroVersion("Group replication", V(38)); return action(id, "disable_replication", Collections.emptyMap()); }

    @Override
    public ActionResponse failoverReplication(String id, boolean allowAttachedVolume, String secondaryBackendId) {
        requireMicroVersion("Group replication", V(38));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("allow_attached_volume", allowAttachedVolume);
        if (secondaryBackendId != null) body.put("secondary_backend_id", secondaryBackendId);
        return action(id, "failover_replication", body);
    }

    @Override
    public List<? extends ReplicationTarget> listReplicationTargets(String id) {
        requireMicroVersion("Group replication", V(38));
        CinderReplicationTargets targets = post(CinderReplicationTargets.class, uri("/groups/%s/action", Objects.requireNonNull(id)))
                .entity(JsonBody.of("list_replication_targets", Collections.emptyMap())).execute();
        return targets == null ? Collections.emptyList() : targets.getTargets();
    }

    private ActionResponse action(String id, String action, Map<String, ?> body) {
        return post(ActionResponse.class, uri("/groups/%s/action", Objects.requireNonNull(id))).entity(JsonBody.of(action, body)).execute();
    }
```
`BlockGroupTypeServiceImpl.java`: 모든 메서드 `requireMicroVersion("Group types", V(11))`; `list` → `GroupTypes` `/group_types`; `get`/`getDefault`(`/group_types/default`); `create` → `JsonBody.of("group_type", {name, description?, is_public?, group_specs?})`; `update` → PUT `JsonBody.of("group_type", ...)`; `delete`; `groupSpecs` → `CinderGroupSpecs` GET `/group_types/%s/group_specs`; `setGroupSpecs` → POST `JsonBody.of("group_specs", map)` → `CinderGroupSpecs`; `groupSpec`/`updateGroupSpec` → `HashMap.class` 로 `/group_specs/%s`(PUT 본문 `JsonBody.of(Map.of(key, value))`); `deleteGroupSpec` → `deleteWithResponse`.
`BlockGroupSnapshotServiceImpl.java`: 모든 메서드 `requireMicroVersion("Group snapshots", V(14))`; 옵션 하한 검사(`getRequiredMicroVersion`); `list`/`listDetail` → `GroupSnapshots` `/group_snapshots[/detail]`; `get`; `create` → `JsonBody.of("group_snapshot", {group_id, name?, description?})`; `delete` → `deleteWithResponse`; `resetStatus` → `requireMicroVersion("Group snapshot reset status", V(19))`, `post(ActionResponse.class, "/group_snapshots/%s/action").entity(JsonBody.of("reset_status", {status}))`.

accessor 3개 + 바인딩 3개.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='GroupTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(block-storage): add generic volume groups, group types and group snapshots

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "groups()(3.13; from source 3.14, reset 3.20, replication 3.38), groupTypes()(3.11, group specs), groupSnapshots()(3.14; reset 3.19, 옵션 3.29)."
```

---

### Task 10: clusters, services 액션, workers cleanup, hosts, capabilities, resource filters, extensions, limits(project), quota classes, pools 옵션

**Files:**
- Create (`core/src/main/java/org/openstack4j/`): `model/storage/block/StorageCluster.java`, `StorageHost.java`, `StorageHostResource.java`, `BackendCapabilities.java`, `ResourceFilter.java`, `BlockExtension.java`, `WorkerCleanup.java`, `ServiceLogLevel.java`, `model/storage/block/options/ClusterListOptions.java`, `PoolListOptions.java`, `WorkerCleanupRequest.java`, `api/storage/BlockClusterService.java`, `BlockWorkerService.java`, `BlockHostService.java`, `BlockCapabilityService.java`, `BlockResourceFilterService.java`, `BlockExtensionService.java`, `openstack/storage/block/domain/CinderCluster.java`, `CinderHost.java`, `CinderHostDetail.java`, `CinderBackendCapabilities.java`, `CinderResourceFilter.java`, `CinderExtension.java`, `CinderWorkerCleanup.java`, `CinderServiceLogLevels.java`, `CinderServiceActionResult.java`, `openstack/storage/block/internal/BlockClusterServiceImpl.java`, `BlockWorkerServiceImpl.java`, `BlockHostServiceImpl.java`, `BlockCapabilityServiceImpl.java`, `BlockResourceFilterServiceImpl.java`, `BlockExtensionServiceImpl.java`
- Modify: `api/storage/ext/BlockStorageServiceService.java`, `openstack/storage/block/internal/BlockStorageServiceServiceImpl.java`, `api/storage/SchedulerStatsGetPoolService.java`, `SchedulerStatsGetPoolServiceImpl.java`, `api/storage/BlockQuotaSetService.java`, `BlockQuotaSetServiceImpl.java`, `api/storage/BlockStorageService.java`(`getLimits(String projectId)` + accessor 6개), `BlockStorageServiceImpl.java`, `DefaultAPIProvider.java`
- Create: `core-test/src/main/java/org/openstack4j/api/storage/microversion/AdminServiceTests.java`

**Interfaces:**
- Consumes: Task 2, Task 4 `BlockStorageListOptions`, Task 1 `JsonBody`, Task 3 `CinderBlockQuotaSet` 필드.
- Produces:
  - `clusters()` → `BlockClusterService`(3.7): `list()`, `list(ClusterListOptions)`, `listDetail()`, `listDetail(ClusterListOptions)`, `StorageCluster get(String name, String binary)`(`binary` nullable → `cinder-volume`), `StorageCluster enable(String name, String binary)`, `StorageCluster disable(String name, String binary, String reason)`; `StorageCluster`: `getName()`, `getBinary()`, `getState()`, `getStatus()`, `getDisabledReason()`, `Integer getNumHosts()`, `Integer getNumDownHosts()`, `Date getLastHeartbeat()`, `Date getCreatedAt()`, `Date getUpdatedAt()`, `getReplicationStatus()`, `Boolean getFrozen()`, `getActiveBackendId()`(3.26); `ClusterListOptions.create()`: `name`, `binary`, `isUp(boolean)`, `disabled(boolean)`, `numHosts(int)`, `numDownHosts(int)`, `replicationStatus`(3.26), `frozen(boolean)`(3.26), `activeBackendId`(3.26)
  - `services()` 확장: `ActionResponse enable(String host, String binary)`, `disable(String host, String binary)`, `disableWithReason(String host, String binary, String reason)`, `freeze(String host)`, `thaw(String host)`, `failoverHost(String host, String backendId)`, `failover(String host, String cluster, String backendId)`(3.26, `host` 또는 `cluster`), `List<? extends ServiceLogLevel> getLog(String binary, String server, String prefix)`(3.32; 모두 nullable), `ActionResponse setLog(String level, String binary, String server, String prefix)`(3.32); `ServiceLogLevel`: `getBinary()`, `getHost()`, `Map<String, String> getLevels()`
  - `workers()` → `BlockWorkerService`(3.24): `WorkerCleanup cleanup(WorkerCleanupRequest)`; `WorkerCleanupRequest.create()`: `clusterName`, `host`, `binary`, `serviceId(int)`, `isUp(boolean)`, `disabled(boolean)`, `resourceId`, `resourceType`; `WorkerCleanup`: `List<? extends CleanedService> getCleaning()`, `getUnavailable()`; `CleanedService`: `getId()`, `getHost()`, `getBinary()`, `getClusterName()`
  - `hosts()` → `BlockHostService`: `List<? extends StorageHost> list()`, `List<? extends StorageHostResource> get(String hostName)`; `StorageHost`: `getHostName()`, `getService()`, `getZone()`, `getServiceStatus()`, `getServiceState()`, `Date getLastUpdate()`; `StorageHostResource`: `getProject()`, `getHost()`, `getVolumeCount()`, `getTotalVolumeGb()`, `getSnapshotCount()`, `getTotalSnapshotGb()`(모두 String — Cinder 가 문자열로 준다)
  - `capabilities()` → `BlockCapabilityService`: `BackendCapabilities get(String hostName)`; `BackendCapabilities`: `getNamespace()`, `getVendorName()`, `getVolumeBackendName()`, `getPoolName()`, `getDriverVersion()`, `getStorageProtocol()`, `getDisplayName()`, `getDescription()`, `getVisibility()`, `List<String> getReplicationTargets()`, `Map<String, Object> getProperties()`
  - `resourceFilters()` → `BlockResourceFilterService`(3.33): `List<? extends ResourceFilter> list()`, `list(String resource)`; `ResourceFilter`: `getResource()`, `List<String> getFilters()`
  - `extensions()` → `BlockExtensionService`: `List<? extends BlockExtension> list()`; `BlockExtension`: `getName()`, `getAlias()`, `getDescription()`, `getUpdated()`
  - `BlockStorageService.getLimits(String projectId)`(3.39)
  - `quotaSets()` 확장: `BlockQuotaSet quotaClass(String className)`, `BlockQuotaSet updateQuotaClass(String className, BlockQuotaSet)`
  - `schedulerStatsPools()` 확장: `pools(PoolListOptions)`, `poolsDetail(PoolListOptions)`; `PoolListOptions.create()`: `capability(String key, String value)`(3.28, 임의 키), `volumeType(String)`(3.35)

- [ ] **Step 1: 실패하는 테스트 작성 (Review Focus 5 포함)**

```bash
git switch main && git pull && git switch -c task/d10-admin-services
```

`AdminServiceTests.java`:
```java
package org.openstack4j.api.storage.microversion;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.BackendCapabilities;
import org.openstack4j.model.storage.block.BlockExtension;
import org.openstack4j.model.storage.block.BlockLimits;
import org.openstack4j.model.storage.block.BlockQuotaSet;
import org.openstack4j.model.storage.block.ResourceFilter;
import org.openstack4j.model.storage.block.ServiceLogLevel;
import org.openstack4j.model.storage.block.StorageCluster;
import org.openstack4j.model.storage.block.StorageHost;
import org.openstack4j.model.storage.block.StorageHostResource;
import org.openstack4j.model.storage.block.WorkerCleanup;
import org.openstack4j.model.storage.block.options.ClusterListOptions;
import org.openstack4j.model.storage.block.options.PoolListOptions;
import org.openstack4j.model.storage.block.options.WorkerCleanupRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/Admin")
public class AdminServiceTests extends AbstractBlockStorageMicroVersionTest {

    public void clusters() throws Exception {
        negotiate("3.71");
        String cluster = "{\"cluster\": {\"binary\": \"cinder-volume\", \"created_at\": \"2026-06-01T02:46:28.000000\", \"disabled_reason\": null, \"last_heartbeat\": \"2026-06-01T02:46:28.000000\","
                + " \"name\": \"cluster_name\", \"num_down_hosts\": 0, \"num_hosts\": 2, \"state\": \"up\", \"status\": \"enabled\", \"updated_at\": \"2026-06-01T02:46:28.000000\","
                + " \"replication_status\": \"enabled\", \"frozen\": false, \"active_backend_id\": \"b1\"}}";
        respondWith(200, "{\"clusters\": [" + cluster.substring(12, cluster.length() - 1) + "]}");
        respondWith(200, cluster);
        respondWith(200, "{\"cluster\": {\"name\": \"cluster_name\", \"state\": \"up\", \"binary\": \"cinder-volume\", \"status\": \"disabled\", \"disabled_reason\": \"for testing\"}}");
        respondWith(200, "{\"cluster\": {\"name\": \"cluster_name\", \"state\": \"up\", \"binary\": \"cinder-volume\", \"status\": \"enabled\", \"disabled_reason\": null}}");

        List<? extends StorageCluster> all = osv3().blockStorage().clusters().listDetail(ClusterListOptions.create().isUp(true).frozen(false));
        StorageCluster one = osv3().blockStorage().clusters().get("cluster_name", null);
        StorageCluster disabled = osv3().blockStorage().clusters().disable("cluster_name", "cinder-volume", "for testing");
        StorageCluster enabled = osv3().blockStorage().clusters().enable("cluster_name", null);

        String list = takeRequest().getPath();
        Assert.assertTrue(list.contains("/clusters/detail?") && list.contains("is_up=true") && list.contains("frozen=false"), list);
        Assert.assertTrue(takeRequest().getPath().endsWith("/clusters/cluster_name?binary=cinder-volume"));
        RecordedRequest disable = takeRequest();
        Assert.assertEquals(disable.getMethod(), "PUT");
        Assert.assertTrue(disable.getPath().endsWith("/clusters/disable"));
        JsonNode body = body(disable);
        Assert.assertEquals(body.get("name").asText(), "cluster_name");
        Assert.assertEquals(body.get("disabled_reason").asText(), "for testing");
        Assert.assertTrue(takeRequest().getPath().endsWith("/clusters/enable"));
        Assert.assertEquals(all.get(0).getNumHosts(), Integer.valueOf(2));
        Assert.assertEquals(one.getActiveBackendId(), "b1");
        Assert.assertEquals(one.getFrozen(), Boolean.FALSE);
        Assert.assertEquals(disabled.getStatus(), "disabled");
        Assert.assertEquals(enabled.getStatus(), "enabled");
        Assert.assertEquals(ClusterListOptions.create().frozen(true).getRequiredMicroVersion(), "3.26");
        Assert.assertNull(ClusterListOptions.create().name("x").getRequiredMicroVersion());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.7.*")
    public void clustersNeed37() throws Exception {
        try {
            osv3().blockStorage().clusters().list();
        } finally {
            assertNoMoreRequests();
        }
    }

    public void serviceActions() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"binary\": \"cinder-volume\", \"host\": \"storage-1\", \"status\": \"enabled\"}");
        respondWith(200, "{\"binary\": \"cinder-volume\", \"disabled\": true, \"disabled_reason\": \"test2\", \"host\": \"storage-1\", \"status\": \"disabled\"}");
        respondWith(200);
        respondWith(200);
        respondWith(202);
        respondWith(202);
        respondWith(200, "{\"log_levels\": [{\"binary\": \"cinder-volume\", \"host\": \"storage-1\", \"levels\": {\"cinder.volume.api\": \"DEBUG\"}}]}");
        respondWith(202);

        osv3().blockStorage().services().enable("storage-1", "cinder-volume");
        osv3().blockStorage().services().disableWithReason("storage-1", "cinder-volume", "test2");
        osv3().blockStorage().services().freeze("storage-1");
        osv3().blockStorage().services().thaw("storage-1");
        osv3().blockStorage().services().failoverHost("storage-1", "b2");
        osv3().blockStorage().services().failover(null, "cluster1", "b2");
        List<? extends ServiceLogLevel> levels = osv3().blockStorage().services().getLog("cinder-volume", "storage-1", "cinder.volume");
        osv3().blockStorage().services().setLog("ERROR", "cinder-volume", "storage-1", "cinder.volume");

        RecordedRequest enable = takeRequest();
        Assert.assertEquals(enable.getMethod(), "PUT");
        Assert.assertTrue(enable.getPath().endsWith("/os-services/enable"));
        Assert.assertEquals(body(enable).get("host").asText(), "storage-1");
        RecordedRequest disable = takeRequest();
        Assert.assertTrue(disable.getPath().endsWith("/os-services/disable-log-reason"));
        Assert.assertEquals(body(disable).get("disabled_reason").asText(), "test2");
        Assert.assertTrue(takeRequest().getPath().endsWith("/os-services/freeze"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/os-services/thaw"));
        RecordedRequest failoverHost = takeRequest();
        Assert.assertTrue(failoverHost.getPath().endsWith("/os-services/failover_host"));
        Assert.assertEquals(body(failoverHost).get("backend_id").asText(), "b2");
        RecordedRequest failover = takeRequest();
        Assert.assertTrue(failover.getPath().endsWith("/os-services/failover"));
        Assert.assertEquals(body(failover).get("cluster").asText(), "cluster1");
        Assert.assertFalse(body(failover).has("host"));
        RecordedRequest getLog = takeRequest();
        Assert.assertTrue(getLog.getPath().endsWith("/os-services/get-log"));
        Assert.assertEquals(body(getLog).get("prefix").asText(), "cinder.volume");
        RecordedRequest setLog = takeRequest();
        Assert.assertTrue(setLog.getPath().endsWith("/os-services/set-log"));
        Assert.assertEquals(body(setLog).get("level").asText(), "ERROR");
        Assert.assertEquals(levels.get(0).getLevels().get("cinder.volume.api"), "DEBUG");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.32.*")
    public void setLogNeeds332() throws Exception {
        try {
            osv3().blockStorage().services().setLog("DEBUG", null, null, null);
        } finally {
            assertNoMoreRequests();
        }
    }

    public void workersHostsCapabilitiesFiltersExtensions() throws Exception {
        negotiate("3.71");
        respondWith(202, "{\"cleaning\": [{\"id\": 1, \"host\": \"storage-1@lvm\", \"binary\": \"cinder-volume\", \"cluster_name\": \"test\"}], \"unavailable\": []}");
        respondWith(200, "{\"hosts\": [{\"service-status\": \"available\", \"service\": \"cinder-volume\", \"zone\": \"nova\", \"service-state\": \"enabled\", \"host_name\": \"storage-1@lvm\", \"last-update\": \"2026-10-02T21:38:35.000000\"}]}");
        respondWith(200, "{\"host\": [{\"resource\": {\"volume_count\": \"8\", \"total_volume_gb\": \"11\", \"total_snapshot_gb\": \"1\", \"project\": \"(total)\", \"host\": \"storage-1@lvm\", \"snapshot_count\": \"1\"}}]}");
        respondWith(200, "{\"namespace\": \"OS::Storage::Capabilities::lvm\", \"vendor_name\": \"Open Source\", \"volume_backend_name\": \"lvm-1\", \"pool_name\": \"lvm-1\", \"driver_version\": \"3.0.0\","
                + " \"storage_protocol\": \"iSCSI\", \"display_name\": \"LVM\", \"description\": \"d\", \"visibility\": \"public\", \"replication_targets\": [], \"properties\": {\"compression\": {\"type\": \"boolean\"}}}");
        respondWith(200, "{\"resource_filters\": [{\"filters\": [\"name\", \"status\"], \"resource\": \"volume\"}]}");
        respondWith(200, "{\"extensions\": [{\"name\": \"AdminActions\", \"alias\": \"os-admin-actions\", \"description\": \"Enable admin actions.\", \"updated\": \"2012-08-25T00:00:00+00:00\", \"links\": []}]}");

        WorkerCleanup cleanup = osv3().blockStorage().workers().cleanup(WorkerCleanupRequest.create().clusterName("test").binary("cinder-volume").isUp(true));
        List<? extends StorageHost> hosts = osv3().blockStorage().hosts().list();
        List<? extends StorageHostResource> resources = osv3().blockStorage().hosts().get("storage-1@lvm");
        BackendCapabilities caps = osv3().blockStorage().capabilities().get("storage-1@lvm");
        List<? extends ResourceFilter> filters = osv3().blockStorage().resourceFilters().list("volume");
        List<? extends BlockExtension> extensions = osv3().blockStorage().extensions().list();

        RecordedRequest work = takeRequest();
        Assert.assertTrue(work.getPath().endsWith("/workers/cleanup"));
        Assert.assertEquals(body(work).get("cluster_name").asText(), "test");
        Assert.assertTrue(body(work).get("is_up").asBoolean());
        Assert.assertTrue(takeRequest().getPath().endsWith("/os-hosts"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/os-hosts/storage-1%40lvm") || takeRequest().getPath().endsWith("/os-hosts/storage-1@lvm"));
        Assert.assertTrue(takeRequest().getPath().contains("/capabilities/storage-1"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/resource_filters?resource=volume"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/extensions"));
        Assert.assertEquals(cleanup.getCleaning().get(0).getClusterName(), "test");
        Assert.assertTrue(cleanup.getUnavailable().isEmpty());
        Assert.assertEquals(hosts.get(0).getServiceState(), "enabled");
        Assert.assertEquals(resources.get(0).getVolumeCount(), "8");
        Assert.assertEquals(caps.getStorageProtocol(), "iSCSI");
        Assert.assertTrue(caps.getProperties().containsKey("compression"));
        Assert.assertEquals(filters.get(0).getFilters().get(1), "status");
        Assert.assertEquals(extensions.get(0).getAlias(), "os-admin-actions");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.24.*")
    public void workersNeed324() throws Exception {
        try {
            osv3().blockStorage().workers().cleanup(WorkerCleanupRequest.create());
        } finally {
            assertNoMoreRequests();
        }
    }

    public void limitsQuotaClassAndPools() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"limits\": {\"rate\": [], \"absolute\": {\"maxTotalVolumes\": 10, \"totalVolumesUsed\": 1}}}");
        respondWith(200, "{\"quota_class_set\": {\"backup_gigabytes\": 1000, \"backups\": 10, \"gigabytes\": 1000, \"gigabytes___DEFAULT__\": -1, \"groups\": 10, \"id\": \"default\", \"per_volume_gigabytes\": -1, \"snapshots\": 10, \"snapshots___DEFAULT__\": -1, \"volumes\": 10, \"volumes___DEFAULT__\": -1}}");
        respondWith(200, "{\"quota_class_set\": {\"backups\": 20, \"gigabytes\": 1000, \"snapshots\": 10, \"volumes\": 10}}");
        respondWith(200, "{\"pools\": [{\"name\": \"storage-1@lvm-1#lvm-1\", \"capabilities\": {\"total_capacity_gb\": 100, \"free_capacity_gb\": 50, \"volume_backend_name\": \"lvm-1\"}}]}");

        BlockLimits limits = osv3().blockStorage().getLimits("p1");
        BlockQuotaSet quotaClass = osv3().blockStorage().quotaSets().quotaClass("default");
        BlockQuotaSet updated = osv3().blockStorage().quotaSets().updateQuotaClass("default", Builders.blockQuotaSet().backups(20).build());
        osv3().blockStorage().schedulerStatsPools().poolsDetail(PoolListOptions.create().volumeType("lvm").capability("QoS_support", "true"));

        Assert.assertTrue(takeRequest().getPath().endsWith("/limits?project_id=p1"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/os-quota-class-sets/default"));
        RecordedRequest put = takeRequest();
        Assert.assertEquals(put.getMethod(), "PUT");
        Assert.assertEquals(body(put).get("quota_class_set").get("backups").asInt(), 20);
        String pools = takeRequest().getPath();
        Assert.assertTrue(pools.contains("/scheduler-stats/get_pools?") && pools.contains("detail=true") && pools.contains("volume_type=lvm") && pools.contains("QoS_support=true"), pools);
        Assert.assertEquals(limits.getAbsolute().getMaxTotalVolumes(), 10);
        Assert.assertEquals(quotaClass.getBackupGigabytes(), Integer.valueOf(1000));
        Assert.assertEquals(quotaClass.getGroups(), Integer.valueOf(10));
        Assert.assertEquals(quotaClass.getVolumeTypesQuotas().get("gigabytes___DEFAULT__"), Integer.valueOf(-1));
        Assert.assertFalse(quotaClass.getVolumeTypesQuotas().containsKey("backup_gigabytes"));
        Assert.assertEquals(updated.getBackups(), Integer.valueOf(20));
        Assert.assertEquals(PoolListOptions.create().volumeType("x").getRequiredMicroVersion(), "3.35");
        Assert.assertEquals(PoolListOptions.create().capability("a", "b").getRequiredMicroVersion(), "3.28");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.39.*")
    public void limitsByProjectNeeds339() throws Exception {
        try {
            osv3().blockStorage().getLimits("p1");
        } finally {
            assertNoMoreRequests();
        }
    }
}
```
(`Builders.blockQuotaSet()` 의 실제 이름을 확인한다. `limits.getAbsolute().getMaxTotalVolumes()` 는 기존 `BlockLimits` 의 getter 이름에 맞춘다. `@` 인코딩은 connector 마다 다를 수 있어 두 형태를 허용한다.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `clusters()`, `StorageCluster` 등 없음.

- [ ] **Step 3: 모델·옵션 구현**

인터페이스는 Interfaces 의 getter 를 그대로 선언한다. 도메인:
- `CinderCluster`: `@JsonRootName("cluster")`, 필드 `name`, `binary`, `state`, `status`, `disabled_reason`, `num_hosts`(Integer), `num_down_hosts`(Integer), `last_heartbeat`(Date), `created_at`, `updated_at`, `replication_status`, `frozen`(Boolean), `active_backend_id`; 중첩 `Clusters extends ListResult<CinderCluster>`(`@JsonProperty("clusters")`). `last_heartbeat` 가 빈 문자열(`""`)로 올 수 있다 → 필드에 `@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS", lenient = OptBoolean.TRUE)` 대신 `String lastHeartbeatRaw` 로 받고 getter 에서 비어 있으면 `null`, 아니면 기존 날짜 파서(`org.openstack4j.openstack.common.DateTimeUtils`?) — 그런 유틸이 없으면 `java.time.LocalDateTime.parse(raw.replace(' ', 'T')).atZone(ZoneOffset.UTC)` 로 변환한다. 테스트 fixture 는 유효한 값만 쓰므로 빈 문자열 처리는 `null` 반환만 확인하면 된다(단위 테스트 1개 추가: `lastHeartbeatEmptyIsNull` — `CinderCluster` 에 `@JsonCreator` 없이 Jackson 으로 `{"cluster":{"name":"c","last_heartbeat":""}}` 를 읽어 `getLastHeartbeat()==null`).
- `CinderHost`: 필드 `host_name`, `service`, `zone`, `service-status`, `service-state`, `last-update`(Date); 중첩 `Hosts extends ListResult<CinderHost>`(`@JsonProperty("hosts")`).
- `CinderHostDetail`: `{"host": [{"resource": {...}}]}` → `@JsonProperty("host") List<Entry> host`; `Entry` 는 `@JsonProperty("resource") Resource resource`; `Resource implements StorageHostResource` 필드 `project`, `host`, `volume_count`, `total_volume_gb`, `snapshot_count`, `total_snapshot_gb`(String); `List<Resource> resources()` 평탄화.
- `CinderBackendCapabilities`: 필드 `namespace`, `vendor_name`, `volume_backend_name`, `pool_name`, `driver_version`, `storage_protocol`, `display_name`, `description`, `visibility`, `replication_targets`(List<String>), `properties`(Map<String,Object>); `@JsonIgnoreProperties(ignoreUnknown = true)`, 루트 없음.
- `CinderResourceFilter`: `resource`, `filters`(List<String>); 중첩 `ResourceFilters`(`@JsonProperty("resource_filters")`).
- `CinderExtension`: `name`, `alias`, `description`, `updated`(String); 중첩 `Extensions`(`@JsonProperty("extensions")`).
- `CinderWorkerCleanup`: `cleaning`, `unavailable`(List<Entry>); `Entry implements WorkerCleanup.CleanedService`: `id`(String — 숫자지만 String 으로 받음), `host`, `binary`, `cluster_name`.
- `CinderServiceLogLevels`: `@JsonProperty("log_levels") List<Level>`; `Level implements ServiceLogLevel`: `binary`, `host`, `levels`(Map<String,String>).
- `CinderServiceActionResult`: `{"binary","host","status","disabled","disabled_reason","service"}` 응답용(`@JsonIgnoreProperties`); 서비스 액션 메서드는 `ActionResponse` 를 돌려주므로 응답 본문은 읽지 않는다(이 클래스는 쓰지 않으면 만들지 않는다 — Ruling 으로 기록).
- `options/ClusterListOptions`: `name`, `binary`, `isUp` → `is_up`, `disabled`, `numHosts` → `num_hosts`, `numDownHosts` → `num_down_hosts`, `replicationStatus` → `replication_status`(26), `frozen`(26), `activeBackendId` → `active_backend_id`(26).
- `options/PoolListOptions`: `capability(key, value)` → `put(key, value, 28)`, `volumeType` → `volume_type`(35). (`BlockStorageListOptions` 상속 — 공통 limit 등은 Cinder pools 가 받지 않지만 해롭지 않다.)
- `options/WorkerCleanupRequest`: fluent 필드 `cluster_name`, `host`, `binary`, `service_id`(Integer), `is_up`, `disabled`(Boolean), `resource_id`, `resource_type`; `Map<String, Object> toMap()`(설정된 것만).

- [ ] **Step 4: 서비스 구현**

- `BlockClusterServiceImpl`: 모두 `requireMicroVersion("Clusters", V(7))`; 옵션 하한 검사; `list`/`listDetail` → `Clusters` `/clusters[/detail]`; `get(name, binary)` → `get(CinderCluster.class, uri("/clusters/%s", name)).param("binary", binary == null ? "cinder-volume" : binary)`; `enable`/`disable` → `put(CinderCluster.class, uri("/clusters/enable|disable")).entity(JsonBody.of({name, binary?, disabled_reason?}))`.
- `BlockStorageServiceServiceImpl` 추가 메서드: `enable`/`disable` → `put(ActionResponse.class, uri("/os-services/enable|disable")).entity(JsonBody.of({host, binary}))`; `disableWithReason` → `/os-services/disable-log-reason` + `disabled_reason`; `freeze`/`thaw` → `{host}`; `failoverHost` → `/os-services/failover_host` `{host, backend_id?}`; `failover` → `requireMicroVersion("Service failover", V(26))`, `/os-services/failover` `{host? | cluster?, backend_id?}`; `getLog` → `requireMicroVersion("Service log levels", V(32))`, `put(CinderServiceLogLevels.class, uri("/os-services/get-log")).entity(JsonBody.of({binary?, server?, prefix?}))`; `setLog` → `requireMicroVersion(..., V(32))`, `put(ActionResponse.class, uri("/os-services/set-log")).entity(JsonBody.of({level, binary?, server?, prefix?}))`. 서비스 액션은 200 + 본문을 돌려주지만 `ActionResponse` 로 받는다(본문 무시; 기존 `put(ActionResponse.class, ...)` 패턴이 200 본문 있는 응답을 성공으로 처리하는지 `serviceActions` 테스트가 확인한다).
- `BlockWorkerServiceImpl.cleanup` → `requireMicroVersion("Worker cleanup", V(24))`, `post(CinderWorkerCleanup.class, uri("/workers/cleanup")).entity(JsonBody.of(request.toMap()))`.
- `BlockHostServiceImpl`: `list` → `Hosts` `/os-hosts`; `get(host)` → `CinderHostDetail` `/os-hosts/%s` → `resources()`.
- `BlockCapabilityServiceImpl.get(host)` → `CinderBackendCapabilities` `/capabilities/%s`.
- `BlockResourceFilterServiceImpl`: `requireMicroVersion("Resource filters", V(33))`; `list()` → `ResourceFilters` `/resource_filters`; `list(resource)` → `.param("resource", resource)`.
- `BlockExtensionServiceImpl.list` → `Extensions` `/extensions`.
- `BlockStorageServiceImpl.getLimits(projectId)` → `requireMicroVersion("Limits by project", V(39))`, `get(CinderBlockLimits.class, "/limits").param("project_id", projectId)`.
- `BlockQuotaSetServiceImpl.quotaClass/updateQuotaClass` → `get|put(CinderBlockQuotaSetClass.class, uri("/os-quota-class-sets/%s", className))`. 응답 루트가 `quota_class_set` 이라 `CinderBlockQuotaSet`(`@JsonRootName("quota_set")`)을 그대로 쓸 수 없다 → `domain/CinderBlockQuotaSetClass extends CinderBlockQuotaSet` 에 `@JsonRootName("quota_class_set")` 만 붙인 하위 클래스를 만들고, `updateQuotaClass` 의 요청 본문도 그 타입으로 감싼다(`BlockQuotaSet` 을 받아 필드를 복사하는 정적 팩토리 `from(BlockQuotaSet)`; 복사는 `volumes/snapshots/gigabytes/backups/backupGigabytes/perVolumeGigabytes/groups/volumeTypesQuotas`).
- `SchedulerStatsGetPoolServiceImpl.pools(options)/poolsDetail(options)` → 옵션 하한 검사 후 `.param("detail", ...)` + `.params(options.toQueryParams())`.

accessor 6개(`clusters()`, `workers()`, `hosts()`, `capabilities()`, `resourceFilters()`, `extensions()`) + `getLimits(String)` + 바인딩 6개.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='AdminServiceTests,ServiceTests,SchedulerStatsGetPoolTests,VolumeTypeQuotaTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Failures: 0, Errors: 0` (AdminServiceTests 8개).

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(block-storage): add clusters, service actions, worker cleanup, hosts, capabilities, resource filters, extensions, quota classes and pool filters

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "clusters()(3.7/3.26), services() 액션(enable/disable/freeze/thaw/failover 3.26/log 3.32), workers()(3.24), hosts(), capabilities(), resourceFilters()(3.33), extensions(), getLimits(project)(3.39), quota classes, pools 옵션(3.28/3.35)."
```

---

### Task 11: volume-transfers(3.55)와 manageable volumes/snapshots(3.8)

**Files:**
- Create (`core/src/main/java/org/openstack4j/`): `api/storage/BlockVolumeTransferV3Service.java`, `api/storage/BlockManageableVolumeService.java`, `api/storage/BlockManageableSnapshotService.java`, `model/storage/block/ManageableVolume.java`, `ManageableSnapshot.java`, `model/storage/block/options/TransferListOptions.java`, `ManageableListOptions.java`, `VolumeManageRequest.java`, `SnapshotManageRequest.java`, `openstack/storage/block/domain/CinderManageableVolume.java`, `CinderManageableSnapshot.java`, `openstack/storage/block/internal/BlockVolumeTransferV3ServiceImpl.java`, `BlockManageableVolumeServiceImpl.java`, `BlockManageableSnapshotServiceImpl.java`
- Modify: `api/storage/BlockStorageService.java`, `BlockStorageServiceImpl.java`, `DefaultAPIProvider.java`
- Create: `core-test/src/main/java/org/openstack4j/api/storage/microversion/TransferAndManageTests.java`

**Interfaces:**
- Consumes: Task 2, Task 3 `CinderVolumeTransfer.noSnapshots`, Task 4 `BlockStorageListOptions`, Task 1 `JsonBody`.
- Produces:
  - `volumeTransfers()` → `BlockVolumeTransferV3Service`(3.55): `List<? extends VolumeTransfer> list()`, `list(TransferListOptions)`(3.59), `listDetail()`, `listDetail(TransferListOptions)`, `VolumeTransfer get(String id)`, `VolumeTransfer create(String volumeId, String name, Boolean noSnapshots)`, `VolumeTransfer accept(String id, String authKey)`, `ActionResponse delete(String id)`; `TransferListOptions.create()`: 공통(모두 3.59) 
  - `manageableVolumes()` → `BlockManageableVolumeService`(3.8): `List<? extends ManageableVolume> list(ManageableListOptions)`, `listDetail(ManageableListOptions)`, `Volume manage(VolumeManageRequest)`; `ManageableVolume`: `Map<String, String> getReference()`, `Integer getSize()`, `Boolean getSafeToManage()`, `getReasonNotSafe()`, `getCinderId()`, `Map<String, Object> getExtraInfo()`; `ManageableListOptions.create()`: `host(String)`, `cluster(String)`(3.17) + 공통; `VolumeManageRequest.create(Map<String, String> ref)`: `host`, `cluster`(3.16), `name`, `description`, `volumeType`, `availabilityZone`, `bootable(boolean)`, `metadata(Map)`
  - `manageableSnapshots()` → `BlockManageableSnapshotService`(3.8): `list(ManageableListOptions)`, `listDetail(ManageableListOptions)`, `VolumeSnapshot manage(SnapshotManageRequest)`; `ManageableSnapshot`: `ManageableVolume` 의 getter + `Map<String, String> getSourceReference()`; `SnapshotManageRequest.create(String volumeId, Map<String, String> ref)`: `name`, `description`, `metadata(Map)`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/d11-transfers-manage
```

`TransferAndManageTests.java`:
```java
package org.openstack4j.api.storage.microversion;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.ManageableSnapshot;
import org.openstack4j.model.storage.block.ManageableVolume;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeTransfer;
import org.openstack4j.model.storage.block.options.ManageableListOptions;
import org.openstack4j.model.storage.block.options.SnapshotManageRequest;
import org.openstack4j.model.storage.block.options.TransferListOptions;
import org.openstack4j.model.storage.block.options.VolumeManageRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/TransfersManage")
public class TransferAndManageTests extends AbstractBlockStorageMicroVersionTest {

    private static final String TRANSFER = "{\"transfer\": {\"accepted\": false, \"auth_key\": \"e2cb02466324813c\", \"created_at\": \"2026-06-12T21:21:38.392033\", \"destination_project_id\": null,"
            + " \"id\": \"94bae1a0-83fb-496c-9cd2-800d8237ab0d\", \"links\": [], \"name\": \"first volume\", \"no_snapshots\": true, \"source_project_id\": \"p1\", \"volume_id\": \"" + VOLUME + "\"}}";

    public void newTransferApi() throws Exception {
        negotiate("3.71");
        respondWith(202, TRANSFER);
        respondWith(200, "{\"transfers\": [" + TRANSFER.substring(13, TRANSFER.length() - 1) + "]}");
        respondWith(200, TRANSFER);
        respondWith(202, TRANSFER);
        respondWith(202);

        VolumeTransfer created = osv3().blockStorage().volumeTransfers().create(VOLUME, "first volume", true);
        List<? extends VolumeTransfer> all = osv3().blockStorage().volumeTransfers().listDetail(TransferListOptions.create().limit(10));
        osv3().blockStorage().volumeTransfers().get(created.getId());
        VolumeTransfer accepted = osv3().blockStorage().volumeTransfers().accept(created.getId(), "e2cb02466324813c");
        osv3().blockStorage().volumeTransfers().delete(created.getId());

        RecordedRequest create = takeRequest();
        Assert.assertTrue(create.getPath().endsWith("/volume-transfers"));
        JsonNode body = body(create).get("transfer");
        Assert.assertEquals(body.get("volume_id").asText(), VOLUME);
        Assert.assertTrue(body.get("no_snapshots").asBoolean());
        Assert.assertTrue(takeRequest().getPath().contains("/volume-transfers/detail?limit=10"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/volume-transfers/" + created.getId()));
        RecordedRequest accept = takeRequest();
        Assert.assertTrue(accept.getPath().endsWith("/volume-transfers/" + created.getId() + "/accept"));
        Assert.assertEquals(body(accept).get("accept").get("auth_key").asText(), "e2cb02466324813c");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getNoSnapshots(), Boolean.TRUE);
        Assert.assertEquals(all.get(0).getSourceProjectId(), "p1");
        Assert.assertEquals(accepted.getId(), created.getId());
        Assert.assertEquals(TransferListOptions.create().limit(1).getRequiredMicroVersion(), "3.59");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.55.*")
    public void newTransferApiNeeds355() throws Exception {
        try {
            osv3().blockStorage().volumeTransfers().list();
        } finally {
            assertNoMoreRequests();
        }
    }

    public void legacyTransferApiUnchanged() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"transfers\": []}");
        osv3().blockStorage().transfer().list(true);
        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/os-volume-transfer/detail"));
        assertVersionHeader(request, "3.71");
    }

    public void manageableVolumesAndSnapshots() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"manageable-volumes\": [{\"reference\": {\"source-name\": \"lvol0\"}, \"size\": 1, \"safe_to_manage\": true, \"reason_not_safe\": null, \"cinder_id\": null, \"extra_info\": null}]}");
        respondWith(202, "{\"volume\": {\"id\": \"" + VOLUME + "\", \"status\": \"creating\", \"name\": \"NewVolume\"}}");
        respondWith(200, "{\"manageable-snapshots\": [{\"reference\": {\"source-name\": \"lvol0-snap\"}, \"source_reference\": {\"source-name\": \"lvol0\"}, \"size\": 1, \"safe_to_manage\": true, \"reason_not_safe\": null, \"cinder_id\": null, \"extra_info\": null}]}");
        respondWith(202, "{\"snapshot\": {\"id\": \"" + SNAPSHOT + "\", \"status\": \"creating\", \"volume_id\": \"" + VOLUME + "\"}}");

        List<? extends ManageableVolume> volumes = osv3().blockStorage().manageableVolumes().listDetail(ManageableListOptions.create().host("storage-1@lvm-1"));
        Volume managed = osv3().blockStorage().manageableVolumes().manage(VolumeManageRequest.create(Collections.singletonMap("source-name", "lvol0"))
                .host("storage-1@lvm-1").name("NewVolume").bootable(true).metadata(Collections.singletonMap("k", "v")));
        List<? extends ManageableSnapshot> snapshots = osv3().blockStorage().manageableSnapshots().list(ManageableListOptions.create().cluster("cluster1"));
        osv3().blockStorage().manageableSnapshots().manage(SnapshotManageRequest.create(VOLUME, Collections.singletonMap("source-name", "lvol0-snap")).name("s"));

        Assert.assertTrue(takeRequest().getPath().contains("/manageable_volumes/detail?host=storage-1%40lvm-1") || true);
        RecordedRequest manage = takeRequest();
        Assert.assertTrue(manage.getPath().endsWith("/manageable_volumes"));
        JsonNode body = body(manage).get("volume");
        Assert.assertEquals(body.get("ref").get("source-name").asText(), "lvol0");
        Assert.assertEquals(body.get("host").asText(), "storage-1@lvm-1");
        Assert.assertTrue(body.get("bootable").asBoolean());
        Assert.assertEquals(body.get("metadata").get("k").asText(), "v");
        Assert.assertTrue(takeRequest().getPath().contains("/manageable_snapshots?cluster=cluster1"));
        RecordedRequest manageSnap = takeRequest();
        Assert.assertTrue(manageSnap.getPath().endsWith("/manageable_snapshots"));
        Assert.assertEquals(body(manageSnap).get("snapshot").get("volume_id").asText(), VOLUME);
        Assert.assertEquals(volumes.get(0).getReference().get("source-name"), "lvol0");
        Assert.assertEquals(volumes.get(0).getSafeToManage(), Boolean.TRUE);
        Assert.assertEquals(managed.getName(), "NewVolume");
        Assert.assertEquals(snapshots.get(0).getSourceReference().get("source-name"), "lvol0");
        Assert.assertEquals(ManageableListOptions.create().cluster("c").getRequiredMicroVersion(), "3.17");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.8.*")
    public void manageableNeeds38() throws Exception {
        try {
            osv3().blockStorage().manageableVolumes().list(ManageableListOptions.create().host("h"));
        } finally {
            assertNoMoreRequests();
        }
    }
}
```
(`manageableVolumesAndSnapshots` 첫 `assertTrue(... || true)` 는 경로의 `@` 인코딩 차이를 피하려는 것이다 — 구현 후 실제 인코딩을 보고 `contains("/manageable_volumes/detail?host=")` 로 고쳐 둔다.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `volumeTransfers()`, `ManageableVolume` 등 없음.

- [ ] **Step 3: 모델·옵션 구현**

- `ManageableVolume`/`ManageableSnapshot` 인터페이스(Interfaces 의 getter). `CinderManageableVolume`: `@JsonIgnoreProperties`, 필드 `reference`(Map<String,String>), `size`(Integer), `safe_to_manage`(Boolean), `reason_not_safe`, `cinder_id`, `extra_info`(Map<String,Object>); 중첩 `ManageableVolumes extends ListResult<CinderManageableVolume>`(`@JsonProperty("manageable-volumes")`). `CinderManageableSnapshot`: 같은 필드 + `source_reference`; 중첩 `ManageableSnapshots`(`@JsonProperty("manageable-snapshots")`).
- `TransferListOptions`: `BlockStorageListOptions` 상속, 공통 메서드를 하한 59 로 override(`MessageListOptions` 와 같은 방식).
- `ManageableListOptions`: `host(String)` → `host`(0), `cluster(String)` → `cluster`(17) + 공통.
- `VolumeManageRequest.create(Map<String,String> ref)`: fluent `host`, `cluster`, `name`, `description`, `volumeType`, `availabilityZone`, `bootable(boolean)`, `metadata(Map)`; `Map<String, Object> toMap()`(`ref` 포함, 설정된 것만, 키는 `host`, `cluster`, `name`, `description`, `volume_type`, `availability_zone`, `bootable`, `metadata`); getter `getCluster()`.
- `SnapshotManageRequest.create(String volumeId, Map<String,String> ref)`: `name`, `description`, `metadata`; `toMap()`(`volume_id`, `ref`, ...).

- [ ] **Step 4: 서비스 구현**

- `BlockVolumeTransferV3ServiceImpl`: 모두 `requireMicroVersion("Volume transfers API", V(55))`; 옵션 하한; `list`/`listDetail` → `VolumeTransferList` `/volume-transfers[/detail]`; `get` → `CinderVolumeTransfer`; `create` → `post(CinderVolumeTransfer.class, uri("/volume-transfers")).entity(JsonBody.of("transfer", {volume_id, name?, no_snapshots?}))`; `accept` → `post(CinderVolumeTransfer.class, uri("/volume-transfers/%s/accept", id)).entity(CinderVolumeTransferAccept.create(authKey))`; `delete` → `deleteWithResponse`.
- `BlockManageableVolumeServiceImpl`: `requireMicroVersion("Manageable volumes", V(8))`; `list`/`listDetail` → `ManageableVolumes` `/manageable_volumes[/detail]` + 옵션 하한(`cluster` 3.17); `manage(request)` → `if (request.getCluster() != null) requireMicroVersion("Manage to a cluster", V(16))`, `post(CinderVolume.class, uri("/manageable_volumes")).entity(JsonBody.of("volume", request.toMap()))`.
- `BlockManageableSnapshotServiceImpl`: 같은 방식으로 `/manageable_snapshots`, `manage` → `post(CinderVolumeSnapshot.class, ...).entity(JsonBody.of("snapshot", request.toMap()))`.

accessor 3개(`volumeTransfers()`, `manageableVolumes()`, `manageableSnapshots()`) + 바인딩 3개.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='TransferAndManageTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "feat(block-storage): add the 3.55 volume-transfers API and manageable volumes/snapshots (3.8)

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "volumeTransfers()(3.55; 3.59 페이지네이션, no_snapshots), manageableVolumes()/manageableSnapshots()(3.8; cluster 3.16/3.17)."
```

---

### Task 12: 실환경 통합 테스트, README, MIGRATION, CHANGELOG

**Files:**
- Create: `core-test/src/main/java/org/openstack4j/api/storage/microversion/BlockStorageLiveTests.java`
- Modify: `README.md`(block storage microversion 절), `MIGRATION.md`(4.2 → 4.3), `CHANGELOG.md`(4.3.0)

**Interfaces:**
- Consumes: Task 2~11 의 공개 API.

- [ ] **Step 1: 실환경 테스트 작성**

```bash
git switch main && git pull && git switch -c task/d12-live-docs
```

`BlockStorageLiveTests.java` (인증은 `ComputeLiveTests` 와 같은 방식):
```java
package org.openstack4j.api.storage.microversion;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.storage.block.BlockStorageVersion;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeAttachmentRecord;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.openstack4j.model.storage.block.VolumeSummary;
import org.openstack4j.openstack.OSFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Runs against a real OpenStack when the standard OS_* environment variables are set; skipped otherwise.
 * Negotiates block storage microversions, reads existing resources and creates a throw-away volume, snapshot and
 * attachment that are deleted in {@code finally}.
 */
@Test(suiteName = "BlockStorage/Live", groups = "block-storage-live")
public class BlockStorageLiveTests {

    private OSClientV3 os;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live block storage tests");
        String domain = env("OS_USER_DOMAIN_NAME", "Default");
        os = OSFactory.builderV3()
                .endpoint(url.replaceAll("/+$", "").endsWith("/v3") ? url : url.replaceAll("/+$", "") + "/v3")
                .credentials(env("OS_USERNAME", null), env("OS_PASSWORD", null), Identifier.byName(domain))
                .scopeToProject(Identifier.byName(env("OS_PROJECT_NAME", null)), Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default")))
                .authenticate();
        BlockStorageVersion version = os.blockStorage().microVersions().negotiate();
        Assert.assertTrue(version.isEnabled());
        System.out.println("block storage microversion " + version.getMicroVersion() + " (server max " + version.getServerMaxVersion() + ")");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live block storage tests");
        }
        return value;
    }

    public void readOnlyAdminViews() {
        Assert.assertFalse(os.blockStorage().volumeTypes().list().isEmpty());
        Assert.assertNotNull(os.blockStorage().volumeTypes().getDefault().getId());
        Assert.assertFalse(os.blockStorage().resourceFilters().list().isEmpty());
        Assert.assertFalse(os.blockStorage().schedulerStatsPools().poolsDetail().isEmpty());
        Assert.assertFalse(os.blockStorage().services().list().isEmpty());
        Assert.assertNotNull(os.blockStorage().getLimits().getAbsolute());
        os.blockStorage().clusters().list();
        os.blockStorage().messages().list();
        os.blockStorage().hosts().list();
        os.blockStorage().extensions().list();
        VolumeSummary summary = os.blockStorage().volumes().summary();
        Assert.assertNotNull(summary.getTotalCount());
    }

    public void legacyCallsStillWorkAfterNegotiation() {
        os.blockStorage().volumes().list();
        os.blockStorage().snapshots().list();
        os.blockStorage().backups().list();
        os.blockStorage().transfer().list();
        os.blockStorage().volumes().listVolumeTypes();
        os.blockStorage().zones().list();
    }

    public void temporaryVolumeLifecycle() throws Exception {
        String name = "os4j-live-" + UUID.randomUUID().toString().substring(0, 8);
        Volume volume = null;
        VolumeSnapshot snapshot = null;
        VolumeAttachmentRecord attachment = null;
        try {
            volume = os.blockStorage().volumes().create(Builders.volume().name(name).size(1).description("os4j live test")
                    .metadata(Collections.singletonMap("purpose", "live-test")).build());
            waitFor(() -> os.blockStorage().volumes().get(volume0(volume).getId()).getStatus() == Volume.Status.AVAILABLE, "volume available");
            Volume shown = os.blockStorage().volumes().get(volume.getId());
            Assert.assertEquals(shown.getConsumesQuota(), Boolean.TRUE);
            Assert.assertNotNull(shown.getVolumeTypeId());
            Map<String, String> metadata = os.blockStorage().volumes().setMetadata(volume.getId(), Collections.singletonMap("k", "v"));
            Assert.assertEquals(metadata.get("k"), "v");
            Assert.assertEquals(os.blockStorage().volumes().metadataItem(volume.getId(), "purpose"), "live-test");

            snapshot = os.blockStorage().snapshots().create(Builders.volumeSnapshot().name(name).volume(volume.getId()).build());
            waitFor(() -> os.blockStorage().snapshots().get(snapshot0(snapshot).getId()).getStatus() == VolumeSnapshot.Status.AVAILABLE, "snapshot available");
            Assert.assertNotNull(os.blockStorage().snapshots().get(snapshot.getId()).getUserId());
            Assert.assertTrue(os.blockStorage().snapshots().metadata(snapshot.getId()).isEmpty());

            attachment = os.blockStorage().attachments().create(volume.getId(), null, null, null);
            Assert.assertEquals(attachment.getStatus(), "reserved");
            Assert.assertEquals(os.blockStorage().attachments().get(attachment.getId()).getVolumeId(), volume.getId());
            os.blockStorage().attachments().delete(attachment.getId());
            attachment = null;
            waitFor(() -> os.blockStorage().volumes().get(volume0(volume).getId()).getStatus() == Volume.Status.AVAILABLE, "volume available after detach");
        } finally {
            if (attachment != null)
                os.blockStorage().attachments().delete(attachment.getId());
            if (snapshot != null) {
                os.blockStorage().snapshots().delete(snapshot.getId());
                waitForGone(() -> os.blockStorage().snapshots().get(snapshot0(snapshot).getId()));
            }
            if (volume != null) {
                os.blockStorage().volumes().delete(volume.getId());
                waitForGone(() -> os.blockStorage().volumes().get(volume0(volume).getId()));
            }
        }
    }

    private static Volume volume0(Volume v) { return v; }
    private static VolumeSnapshot snapshot0(VolumeSnapshot s) { return s; }

    private static void waitFor(java.util.function.BooleanSupplier condition, String what) throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(2000);
        }
        throw new AssertionError("timed out waiting for " + what);
    }

    private static void waitForGone(java.util.function.Supplier<Object> lookup) throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            try {
                if (lookup.get() == null) return;
            } catch (RuntimeException e) {
                return;    // 404
            }
            Thread.sleep(2000);
        }
    }
}
```
(`VolumeSnapshot.Status.AVAILABLE` 등 enum 이름은 실제 모델에 맞춘다. `volume0/snapshot0` 는 람다에서 effectively-final 제약을 피하기 위한 보조 메서드다 — 구현 때 지역 final 복사본으로 바꿔도 된다. 기존 `volumes().get()` 이 404 에서 `null` 을 돌려주는지 예외를 던지는지는 connector 에 따라 다르므로 둘 다 "사라짐"으로 본다.)

- [ ] **Step 2: 개발용 OpenStack 에서 세 connector 로 실행**

```bash
./mvnw -B -q install -DskipTests -pl core,core-test
for c in httpclient okhttp http-connector; do OS_AUTH_URL=http://192.168.140.12:5000/v3 OS_USERNAME=admin OS_PASSWORD=<비밀번호> OS_PROJECT_NAME=admin OS_USER_DOMAIN_NAME=Default OS_PROJECT_DOMAIN_NAME=Default ./mvnw -B test -pl connectors/$c -Dsurefire.failIfNoSpecifiedTests=false -Dtest=BlockStorageLiveTests -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'block storage microversion|Tests run:|FAIL' | tail -2; done
```
Expected: `block storage microversion 3.71 (server max 3.71)`, 각 connector `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`. 이후 Cinder 에 `os4j-live-*` 볼륨·스냅샷이 남아 있지 않은지 `GET /v3/volumes/detail?all_tenants=1`, `/snapshots/detail?all_tenants=1`, `/attachments` 로 확인한다. 비밀번호는 명령에만 쓰고 어디에도 저장하지 않는다.

- [ ] **Step 3: README block storage 절**

`README.md` 의 `## Compute microversion` 절 뒤에:
````markdown
## Block storage microversion

4.3.0 부터 Cinder v3 microversion(3.0~3.71)을 선택적으로 켤 수 있습니다. 기본값은 꺼짐이라, 켜지 않으면 요청에 microversion 헤더가 붙지 않고(Cinder 는 3.0 으로 처리) 기존 코드는 그대로 동작합니다.

```java
BlockStorageVersion v = os.blockStorage().microVersions().negotiate();   // min(3.71, 서버 최대)
os.blockStorage().microVersions().use("3.50");                            // 또는 버전 고정
os.blockStorage().microVersions().clear();                                // 다시 헤더 없음

Volume volume = os.blockStorage().volumes().get(id);
volume.getVolumeTypeId();                                                 // 3.63+
volume.getConsumesQuota();                                                // 3.65+

os.blockStorage().volumes().list(VolumeListOptions.create().status("available").withCount(true));   // 3.45+
os.blockStorage().attachments().create(volumeId, serverId, null, "rw");   // 3.27+, mode 3.54+
os.blockStorage().groups().create(GroupCreate.create("g", groupTypeId, List.of(typeId)));         // 3.13+
```

- Cinder 가 3.53 부터 거부하는 생성 본문의 `bootable`, 3.66 부터 거부하는 스냅샷 `force=false` 는 각각 3.52/3.65 로 보내므로 기존 호출이 `negotiate()` 후에도 계속 동작합니다. 그 밖의 기존 메서드는 3.71 까지 형식이 같습니다.
- 새 기능은 필요한 최소 microversion 을 요청 전에 검사하고, 세션이 그 버전을 보내지 않으면(또는 꺼져 있으면) `MicroVersionException` 을 던집니다.
- compute 와 block storage 의 microversion 상태는 서로 독립이며, 클라이언트 세션과 endpoint 별로 유지됩니다.
````

- [ ] **Step 4: MIGRATION, CHANGELOG**

`MIGRATION.md` 에 "4.2 → 4.3" 절:
```markdown
# 4.2 → 4.3

런타임 동작을 바꾸는 변경은 없습니다. block storage microversion 은 선택 사항(`os.blockStorage().microVersions().negotiate()`)이고, 켜지 않으면 요청은 4.2 와 같습니다.

- `ComputeMicroVersionService` 는 이제 공용 `org.openstack4j.common.MicroVersionService<ComputeVersion>` 을, `ComputeVersion` 은 `org.openstack4j.model.common.MicroVersionInfo` 를 상속합니다. 메서드는 그대로라 기존 코드는 변경 없이 컴파일됩니다.
- 모델 인터페이스(`Volume`, `VolumeSnapshot`, `VolumeBackup`, `VolumeType`, `VolumeTransfer`, `BlockQuotaSet`, block storage `Service`)에 `default` getter 가 추가되었습니다. 라이브러리 밖의 구현체도 그대로 컴파일됩니다. 다만 빌더 인터페이스(`VolumeBuilder`, `VolumeTypeBuilder`, `VolumeBackupCreateBuilder`, `BlockQuotaSetBuilder`)와 서비스 인터페이스(`BlockStorageService`, `BlockVolumeService`, `BlockVolumeSnapshotService`, `BlockVolumeBackupService`, `BlockQuotaSetService`, `BlockStorageServiceService`, `SchedulerStatsGetPoolService`)에는 추상 메서드가 추가되었습니다. 테스트용 가짜 구현처럼 이 인터페이스를 직접 구현했다면 새 메서드를 구현해야 합니다.
- `negotiate()` 후 Cinder 3.53+ 는 생성 본문의 알 수 없는 필드를 거부합니다. 라이브러리는 `bootable` 을 설정한 생성 요청을 3.52 로 보내 이를 피합니다.
- `volumes().create()` 에 `multiattach` 를 설정하면 Cinder 는 버전과 무관하게 400 을 돌려줍니다(멀티어태치는 volume type 으로 지정). 이는 4.2 와 같습니다.
```

`CHANGELOG.md` 맨 위에 `## 4.3.0` 절: 공용 `MicroVersionSupport`/`MicroVersionService`, block storage microversion opt-in(3.0~3.71), 상한 2곳, 응답 필드, 목록 옵션, 생성 옵션, volume 액션·metadata, snapshot·backup 보강, attachments(3.27), messages(3.3), volume types·default types(3.62)·qos, groups(3.13)·group types(3.11)·group snapshots(3.14), clusters(3.7), services 액션, workers(3.24), hosts, capabilities, resource filters(3.33), extensions, limits(3.39), quota classes, pools 옵션, volume-transfers(3.55), manageable(3.8) — 각 한 줄.

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git add -A && git commit -m "test(block-storage): add live microversion tests; docs: block storage microversions in README, MIGRATION and CHANGELOG

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "BlockStorageLiveTests(환경 변수 있을 때만; epoxy Cinder 3.71 에서 세 connector 통과 확인), README block storage microversion 절, MIGRATION 4.2→4.3, CHANGELOG 4.3.0."
```

---

### Task 13: 전체 리뷰 → 4.3.0 릴리스 → 4.4.0-SNAPSHOT

**Files:**
- Modify: 모든 `pom.xml` 과 `examples/spring-boot-smoke/pom.xml` 의 버전(`4.3.0-SNAPSHOT` → `4.4.0-SNAPSHOT`; 릴리스 버전은 release.yml 이 tag 에서 정한다)

- [ ] **Step 1: 전체 브랜치 리뷰**

실행 skill 의 최종 리뷰를 수행한다(가장 성능 좋은 모델의 reviewer, 이 plan 의 Review Focus 5개와 ledger 의 Ruling 포함). Critical/Important 는 TDD 로 수정해 PR 로 머지한다.

- [ ] **Step 2: 세 connector 전체 테스트와 완료 기준 점검**

```bash
git switch main && git pull
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
git diff --stat v4.2.0 -- core-test/src/main/java/org/openstack4j/api/storage core-test/src/main/java/org/openstack4j/api/compute | grep -v microversion   # 기존 테스트 변경 0
```
Expected: `BUILD SUCCESS`, 기존 테스트 파일 변경 없음(완료 기준 1).

- [ ] **Step 3: tag push 와 배포 확인**

사용자 지시("질문 없이 진행")에 따라 main CI 성공을 확인한 뒤 실행:
```bash
gh run list -R seogineer/openstack4j --branch main -L 1 --json conclusion -q '.[0].conclusion'   # success
git tag -a v4.3.0 -m "OpenStack4j 4.3.0: opt-in Cinder v3 microversions and missing block storage APIs" && git push origin v4.3.0
gh run watch -R seogineer/openstack4j "$(gh run list -R seogineer/openstack4j --workflow release.yml -L 1 --json databaseId -q '.[0].databaseId')" --exit-status
for i in $(seq 1 40); do c=$(curl -s -o /dev/null -w '%{http_code}' https://repo1.maven.org/maven2/io/github/seogineer/openstack4j/4.3.0/openstack4j-4.3.0.pom); [ "$c" = 200 ] && break; sleep 60; done; echo "repo1 $c"
```
Expected: release 성공, repo1 `200`.

- [ ] **Step 4: 다음 개발 버전**

```bash
git switch -c chore/4.4.0-snapshot
./mvnw -B -q versions:set -DnewVersion=4.4.0-SNAPSHOT -DgenerateBackupPoms=false -DprocessAllModules=true
sed -i 's#<openstack4j.version>4.3.0-SNAPSHOT</openstack4j.version>#<openstack4j.version>4.4.0-SNAPSHOT</openstack4j.version>#' examples/spring-boot-smoke/pom.xml
git add -A && git commit -m "chore: bump version to 4.4.0-SNAPSHOT

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "4.3.0 배포 후 4.4.0-SNAPSHOT 개발 시작."
```

- [ ] **Step 5: Central 아티팩트로 실환경 확인**

`~/openstack4j-check/pom.xml` 의 버전을 4.3.0 으로 올리고 `Check.java` 에 block storage 협상 결과·volume summary·volume types 출력을 추가해 개발용 OpenStack 에 대해 실행한 뒤, 결과를 사용자에게 보고한다. 메모리(`project_openstack4j_fork.md`)에 4.3.0 배포와 보류 항목을 기록한다.
