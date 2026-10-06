# Glance v2 누락 API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Glance v2 의 빠진 API 59 개를 `os.imagesV2()` 에 추가하고 4.6.0 으로 릴리스한다.

**Architecture:** 새 하위 서비스(`info()`, `cache()`, `schemas()`, `metadefs()`)는 `openstack.image.v2.internal.ext` 에, 이미지 하위 경로와 `versions()` 는 기존 `ImageServiceImpl` 에 붙인다. 공용 기반 `BaseImageExtService`(404 전달 helper)와 옵션 기반 `ImageAttributes`(null 생략)를 F 와 같은 모양으로 둔다. Glance 단건 응답은 루트 래핑이 없으므로 도메인 클래스에 `@JsonRootName` 을 붙이지 않는다.

**Tech Stack:** Java 17, Maven, Jackson(NON_NULL), TestNG + MockWebServer, connector 3종.

**Spec:** `docs/superpowers/specs/2026-10-07-image-extensions-design.md`

## Global Constraints

- 기존 코드는 바꾸지 않고 추가만 한다(`@Deprecated` 없음). 기존 요청 본문 불변.
- Glance 는 microversion 헤더가 없다. 버전 확인은 `versions().supports("2.x")` 로 사용자가 한다.
- 목록·생성·수정·동작은 404 를 예외로, 단건 `get` 은 null, 삭제는 실패 `ActionResponse`.
- 옵션은 설정한 필드만 보낸다(`attribute(k, null)` 은 명시적 null).
- 세 connector 에서 같은 결과. 경로 비교는 URL-decoded. 모든 요청을 `takeRequest()` 한다.
- 커밋 끝에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`, `Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt`. PR 은 `.superpowers/pr.sh`. 전체 빌드 `BUILD SUCCESS` 후에만 커밋.
- 회사 코드 복사 금지.

## Review Focus

1. **`/versions` 는 `/v2` 밖에 있다** — 기존 서비스는 endpoint 에 `/v2` 를 붙이므로 그대로 쓰면 `/v2/versions` 로 간다. endpoint 가 `…:9292`, `…:9292/v2`, `…:9292/v2/` 어느 것이든 `/versions` 로 가야 한다. → Task 1 `versionsGoesToRoot`.
2. **문자열 불리언** — stores 의 `"default": "true"`, `"read-only": "true"` 는 문자열이다. `Boolean` 으로 읽혀야 한다. → Task 1 `storesReadStringBooleans`.
3. **루트 배열 응답** — `GET /v2/images/{id}/locations` 는 JSON 배열이다. → Task 2 `locationsReadRootArray`.
4. **stage 본문** — `PUT …/stage` 는 `application/octet-stream` 원문이어야 한다(JSON 아님). → Task 2 `stageSendsOctetStream`(세 connector).
5. **tags append 헤더** — `createTags(ns, tags, append)` 의 `X-Openstack-Append` 가 `true`/`false` 로 가고, false 면 기존 태그를 바꾼다는 Javadoc. → Task 7 `createTagsSendsAppendHeader`.

---

### Task 1: 기반, `versions()`, `info()`

**Files:**
- Create: `core/src/main/java/org/openstack4j/model/image/v2/options/ImageAttributes.java`; `openstack/image/v2/internal/ext/BaseImageExtService.java`; `openstack/image/v2/internal/ImageVersionDiscovery.java`; 모델 `model/image/v2/ext/ImageVersion.java`, `ImageVersions.java`, `ImageStore.java`, `ImageUsage.java`; 도메인 `openstack/image/v2/domain/ext/GlanceVersions.java`, `GlanceImageStore.java`(목록 `Stores`), `GlanceImageUsage.java`(wrapper `Usage`), `GlanceImportMethods.java`; `api/image/v2/ext/ImageInfoService.java`, `openstack/image/v2/internal/ext/ImageInfoServiceImpl.java`
- Modify: `api/image/v2/ImageService.java`, `openstack/image/v2/internal/ImageServiceImpl.java`, `openstack/provider/DefaultAPIProvider.java`
- Create (core-test `org.openstack4j.api.image.v2.ext`): `AbstractImageExtTest.java`, `ImageBasicsTests.java`

**Interfaces:**
- Produces:
  - `ImageAttributes<S>`: F 의 `NeutronAttributes` 와 같은 API(`put`, `attribute`, `toMap`, `self`).
  - `BaseImageExtService`(extends `BaseImageServices`): `listOf(Class<? extends ListResult<E>>, String path, Map<String,String> filters)`(404 전달), `show(Class<E>, String)`(404 → null), `showStrict(Class<E>, String)`(404 전달), `create(Class<E>, String path, ImageAttributes<?>)`(본문은 루트 없이 `toMap()` 그대로, POST, 404 전달), `replace(Class<E>, String path, ImageAttributes<?>)`(PUT, 404 전달), `remove(String path)`(ActionResponse), `static <T> ExecutionOptions<T> propagate404()`, `static String id(String)`
  - `ImageService`: `ImageVersions versions()`, `ImageInfoService info()`
  - `ImageVersions`: `List<? extends ImageVersion> getVersions()`, `String getCurrent()`(CURRENT 의 `"2.17"` — 앞의 `v` 제거), `boolean supports(String version)`(major.minor 숫자 비교 ≤ CURRENT); `ImageVersion`: `getId()`, `getStatus()`
  - `ImageInfoService`: `List<String> importMethods()`, `List<? extends ImageStore> stores()`, `List<? extends ImageStore> storesDetail()`, `Map<String, ? extends ImageUsage> usage()`
  - `ImageStore`: `getId()`, `getDescription()`, `Boolean isDefault()`, `Boolean isReadOnly()`, `getType()`, `Integer getWeight()`, `Map<String, Object> getProperties()`
  - `ImageUsage`: `Long getLimit()`, `Long getUsage()`

- [ ] **Step 1: 실패하는 테스트 작성 (Review Focus 1, 2)**

```bash
git switch main && git pull && git switch -c task/g1-basics
mkdir -p core-test/src/main/java/org/openstack4j/api/image/v2/ext
```

`AbstractImageExtTest.java`:
```java
package org.openstack4j.api.image.v2.ext;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;

/** Base for the Glance extension tests: Glance on the mock server's image port, v3 token. */
public abstract class AbstractImageExtTest extends AbstractTest {

    @Override
    protected Service service() {
        return Service.IMAGE;
    }

    protected JsonNode body(RecordedRequest request) throws IOException {
        return new ObjectMapper().readTree(request.getBody().clone().readUtf8());
    }

    protected static String decodedPath(RecordedRequest request) {
        return URLDecoder.decode(request.getPath(), StandardCharsets.UTF_8);
    }

    protected RecordedRequest expect(String method, String pathSuffix) throws InterruptedException {
        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), method, decodedPath(request));
        Assert.assertTrue(decodedPath(request).endsWith(pathSuffix), decodedPath(request) + " does not end with " + pathSuffix);
        return request;
    }
}
```

`ImageBasicsTests.java`:
```java
package org.openstack4j.api.image.v2.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.image.v2.ext.ImageStore;
import org.openstack4j.model.image.v2.ext.ImageUsage;
import org.openstack4j.model.image.v2.ext.ImageVersions;
import org.openstack4j.openstack.image.v2.internal.ImageVersionDiscovery;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/Basics")
public class ImageBasicsTests extends AbstractImageExtTest {

    private static final String VERSIONS = "{\"versions\": [{\"id\": \"v2.17\", \"status\": \"CURRENT\", \"links\": []}, {\"id\": \"v2.15\", \"status\": \"SUPPORTED\", \"links\": []},"
            + " {\"id\": \"v2.6\", \"status\": \"SUPPORTED\", \"links\": []}]}";

    public void versionsGoesToRoot() throws Exception {
        respondWith(300, VERSIONS);
        ImageVersions versions = osv3().imagesV2().versions();
        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getPath(), "/versions");
        Assert.assertEquals(versions.getCurrent(), "2.17");
        Assert.assertEquals(versions.getVersions().size(), 3);
        Assert.assertTrue(versions.supports("2.6"));
        Assert.assertTrue(versions.supports("2.17"));
        Assert.assertFalse(versions.supports("2.18"));
        Assert.assertFalse(versions.supports("3.0"));
    }

    public void rootUrlStripsVersionSegment() {
        Assert.assertEquals(ImageVersionDiscovery.rootUrl("http://g:9292"), "http://g:9292");
        Assert.assertEquals(ImageVersionDiscovery.rootUrl("http://g:9292/v2"), "http://g:9292");
        Assert.assertEquals(ImageVersionDiscovery.rootUrl("http://g:9292/v2/"), "http://g:9292");
        Assert.assertEquals(ImageVersionDiscovery.rootUrl("http://g/image/v2.1"), "http://g/image");
    }

    public void importMethods() throws Exception {
        respondWith(200, "{\"import-methods\": {\"description\": \"Import methods available.\", \"type\": \"array\", \"value\": [\"glance-direct\", \"web-download\"]}}");
        List<String> methods = osv3().imagesV2().info().importMethods();
        expect("GET", "/v2/info/import");
        Assert.assertEquals(methods, List.of("glance-direct", "web-download"));
    }

    public void storesReadStringBooleans() throws Exception {
        respondWith(200, "{\"stores\": [{\"id\": \"reliable\", \"description\": \"d\"}, {\"id\": \"fast\", \"description\": \"q\", \"default\": \"true\"},"
                + " {\"id\": \"special\", \"description\": \"s\", \"read-only\": \"true\"}]}");
        respondWith(200, "{\"stores\": [{\"id\": \"reliable\", \"type\": \"rbd\", \"description\": \"d\", \"default\": \"true\", \"weight\": 100,"
                + " \"properties\": {\"pool\": \"pool1\", \"chunk_size\": 65536, \"thin_provisioning\": false}}]}");

        List<? extends ImageStore> stores = osv3().imagesV2().info().stores();
        List<? extends ImageStore> detail = osv3().imagesV2().info().storesDetail();

        expect("GET", "/v2/info/stores");
        expect("GET", "/v2/info/stores/detail");
        Assert.assertNull(stores.get(0).isDefault());
        Assert.assertTrue(stores.get(1).isDefault());
        Assert.assertTrue(stores.get(2).isReadOnly());
        Assert.assertEquals(detail.get(0).getType(), "rbd");
        Assert.assertEquals(detail.get(0).getWeight(), Integer.valueOf(100));
        Assert.assertEquals(detail.get(0).getProperties().get("pool"), "pool1");
    }

    public void usage() throws Exception {
        respondWith(200, "{\"usage\": {\"image_size_total\": {\"limit\": 1024, \"usage\": 256}, \"image_count_total\": {\"limit\": 10, \"usage\": 2}}}");
        Map<String, ? extends ImageUsage> usage = osv3().imagesV2().info().usage();
        expect("GET", "/v2/info/usage");
        Assert.assertEquals(usage.get("image_size_total").getUsage(), Long.valueOf(256));
        Assert.assertEquals(usage.get("image_count_total").getLimit(), Long.valueOf(10));
    }

    public void infoNotFoundIsRaised() throws Exception {
        respondWith(404, "<html><body><h1>404 Not Found</h1></body></html>");
        try {
            osv3().imagesV2().info().usage();
            Assert.fail("expected the 404 to surface");
        } catch (RuntimeException expected) {
            Assert.assertNotNull(expected.getMessage());
        }
        takeRequest();
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `ImageVersions`, `ImageVersionDiscovery`, `info()` 등 없음.

- [ ] **Step 3: 기반 구현**

`ImageAttributes`(패키지 `org.openstack4j.model.image.v2.options`): F 의 `NeutronAttributes` 와 같은 코드(클래스 이름·패키지·Javadoc 의 "Neutron" → "Glance").

`BaseImageExtService`:
```java
package org.openstack4j.openstack.image.v2.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.image.v2.options.ImageAttributes;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.openstack.image.v2.internal.BaseImageServices;
import org.openstack4j.openstack.internal.microversion.JsonBody;

/**
 * Shared helpers of the Glance extension services. A 404 from a list, create, update or action means the API is
 * disabled (older server, cache middleware off) or the parent is missing, so it is raised; single gets keep
 * returning null for a missing resource.
 */
public abstract class BaseImageExtService extends BaseImageServices {

    public static <T> ExecutionOptions<T> propagate404() {
        return ExecutionOptions.create(PropagateOnStatus.on(404));
    }

    protected static String id(String value) {
        return Objects.requireNonNull(value, "id");
    }

    protected <E> List<E> listOf(Class<? extends ListResult<E>> type, String path, Map<String, String> filters) {
        ListResult<E> result = get(type, path).params(filters == null ? Collections.emptyMap() : filters).execute(propagate404());
        return result == null ? Collections.emptyList() : result.getList();
    }

    protected <E> E show(Class<E> type, String path) {
        return get(type, path).execute();
    }

    protected <E> E showStrict(Class<E> type, String path) {
        return get(type, path).execute(propagate404());
    }

    protected <E> E create(Class<E> type, String path, ImageAttributes<?> attributes) {
        return post(type, path).entity(JsonBody.of(Objects.requireNonNull(attributes).toMap())).execute(propagate404());
    }

    protected <E> E replace(Class<E> type, String path, ImageAttributes<?> attributes) {
        return put(type, path).entity(JsonBody.of(Objects.requireNonNull(attributes).toMap())).execute(propagate404());
    }

    protected ActionResponse remove(String path) {
        return deleteWithResponse(path).execute();
    }
}
```

`ImageVersionDiscovery`:
```java
package org.openstack4j.openstack.image.v2.internal;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceVersions;
import org.openstack4j.openstack.internal.BaseOpenStackService;

/** Fetches {@code GET /versions} from the Glance root (outside {@code /v2}). */
public final class ImageVersionDiscovery extends BaseOpenStackService {

    public ImageVersionDiscovery() {
        super(ServiceType.IMAGE, ImageVersionDiscovery::rootUrl);
    }

    /** The endpoint without a trailing {@code /v2}, {@code /v2.x} or slash. */
    public static String rootUrl(String endpoint) {
        String trimmed = endpoint.replaceAll("/+$", "");
        return trimmed.replaceAll("/v2(\\.\\d+)?$", "");
    }

    /** Glance answers 300 Multiple Choices with the versions document as body. */
    public GlanceVersions fetch() {
        return request(HttpMethod.GET, GlanceVersions.class, "/versions").execute();
    }
}
```
(D 의 `BlockStorageVersionDiscovery` 와 같은 방식. 300 응답 본문을 읽는지 `versionsGoesToRoot` 로 확인한다.)

- [ ] **Step 4: 모델·서비스 구현**

생성기: `.superpowers/gen_neutron.py` 를 패키지 환경 변수로 일반화한다 — `MODEL_PKG`(기본 `org.openstack4j.model.network.ext`), `DOMAIN_PKG`(기본 `org.openstack4j.openstack.networking.domain.ext`); 경로는 패키지에서 만든다. 실행: `MODEL_PKG=org.openstack4j.model.image.v2.ext DOMAIN_PKG=org.openstack4j.openstack.image.v2.domain.ext python3 .superpowers/gen_neutron.py /tmp/g1.json`.

`/tmp/g1.json`:
```json
[
 {"iface": "ImageVersion", "cls": "GlanceVersion", "doc": "A Glance API version.", "root": null, "list": null,
  "fields": [["id", "getId", "String"], ["status", "getStatus", "String"]]},
 {"iface": "ImageStore", "cls": "GlanceImageStore", "doc": "A Glance image store (multi-store).", "root": null, "list": "stores", "listCls": "Stores",
  "fields": [["id", "getId", "String"], ["description", "getDescription", "String"], ["default", "isDefault", "Boolean"], ["read-only", "isReadOnly", "Boolean"],
             ["type", "getType", "String"], ["weight", "getWeight", "Integer"], ["properties", "getProperties", "Map<String, Object>"]]},
 {"iface": "ImageUsage", "cls": "GlanceImageUsageEntry", "doc": "Usage of one image quota: limit and usage.", "root": null, "list": null,
  "fields": [["limit", "getLimit", "Long"], ["usage", "getUsage", "Long"]]}
]
```
직접 작성:
- `ImageVersions`(model): `List<? extends ImageVersion> getVersions()`, `String getCurrent()`, `boolean supports(String version)`.
- `GlanceVersions`: `@JsonProperty("versions") List<GlanceVersion> versions`; `getCurrent()` = CURRENT 항목의 id 에서 `v` 제거(없으면 SUPPORTED 중 최대); `supports(v)` = `compare(v, current) <= 0` (각 부분을 정수로 비교).
- `GlanceImportMethods`: `@JsonProperty("import-methods") Map<String, Object> methods;` + `List<String> getValue()`(`methods.get("value")`).
- `GlanceImageUsage`: `@JsonProperty("usage") Map<String, GlanceImageUsageEntry> usage;` + getter.

`ImageInfoServiceImpl`:
```java
package org.openstack4j.openstack.image.v2.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.image.v2.ext.ImageInfoService;
import org.openstack4j.model.image.v2.ext.ImageStore;
import org.openstack4j.model.image.v2.ext.ImageUsage;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceImageStore.Stores;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceImageUsage;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceImportMethods;

public class ImageInfoServiceImpl extends BaseImageExtService implements ImageInfoService {

    @Override
    public List<String> importMethods() {
        GlanceImportMethods methods = showStrict(GlanceImportMethods.class, "/info/import");
        return methods == null ? Collections.emptyList() : methods.getValue();
    }

    @Override public List<? extends ImageStore> stores() { return listOf(Stores.class, "/info/stores", null); }
    @Override public List<? extends ImageStore> storesDetail() { return listOf(Stores.class, "/info/stores/detail", null); }

    @Override
    public Map<String, ? extends ImageUsage> usage() {
        GlanceImageUsage usage = showStrict(GlanceImageUsage.class, "/info/usage");
        return usage == null || usage.getUsage() == null ? Collections.emptyMap() : usage.getUsage();
    }
}
```
`ImageServiceImpl` 에 `versions()` = `new ImageVersionDiscovery().fetch()`, `info()` = `Apis.get(ImageInfoService.class)`; `ImageService` 선언 2개; binding 1개(`ImageInfoService`).

- [ ] **Step 5: 통과 확인 (세 connector)**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && for c in httpclient okhttp http-connector; do ./mvnw -B test -pl connectors/$c -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ImageBasicsTests,ImageV2Tests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:' | tail -1; done`
Expected: 세 connector 모두 `Failures: 0, Errors: 0`.

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/g1.log 2>&1; grep -q 'BUILD SUCCESS' /tmp/g1.log
git add -A && git commit -m "feat(image): add Glance version discovery and info (import methods, stores, usage)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "imagesV2(): versions()(root /versions, supports()), info()(import methods, stores, stores detail, usage); BaseImageExtService·ImageAttributes."
```

---

### Task 2: image import, stage, locations, image tasks, store 삭제

**Files:**
- Create: 모델 `model/image/v2/ext/ImageLocation.java`; 도메인 `GlanceImageLocation.java`; 옵션 `model/image/v2/options/ImageImportOptions.java`
- Modify: `ImageService.java`, `ImageServiceImpl.java`
- Create: core-test `ImageImportTests.java`

**Interfaces:**
- Produces (`ImageService`): `ActionResponse importImage(String imageId, ImageImportOptions options)`, `ActionResponse stage(String imageId, Payload<?> payload)`, `List<? extends ImageLocation> listLocations(String imageId)`, `ActionResponse addLocation(String imageId, String url, Map<String, Object> validationData)`, `List<? extends Task> listTasks(String imageId)`, `ActionResponse deleteFromStore(String storeId, String imageId)`
- `ImageLocation`: `getUrl()`, `Map<String, Object> getMetadata()`
- `ImageImportOptions extends ImageAttributes<ImageImportOptions>`: `glanceDirect()`, `webDownload(String uri)`, `copyImage(List<String> stores)`, `glanceDownload(String regionName, String imageId)`; `serviceInterface(String)`, `stores(List<String>)`, `allStores(boolean)`, `allStoresMustSucceed(boolean)`

- [ ] **Step 1: 실패하는 테스트 작성 (Review Focus 3, 4)**

```bash
git switch main && git pull && git switch -c task/g2-import
```

`ImageImportTests.java`:
```java
package org.openstack4j.api.image.v2.ext;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.image.v2.Task;
import org.openstack4j.model.image.v2.ext.ImageLocation;
import org.openstack4j.model.image.v2.options.ImageImportOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/Import")
public class ImageImportTests extends AbstractImageExtTest {

    private static final String IMAGE = "fe05d6c9-ef02-4161-9056-81ed046f3024";

    public void importMethodsBodies() throws Exception {
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);

        var images = osv3().imagesV2();
        Assert.assertTrue(images.importImage(IMAGE, ImageImportOptions.glanceDirect()).isSuccess());
        images.importImage(IMAGE, ImageImportOptions.webDownload("https://example.com/cirros.img").allStores(true).allStoresMustSucceed(true));
        images.importImage(IMAGE, ImageImportOptions.copyImage(List.of("fast", "cheap")).allStoresMustSucceed(false));
        images.importImage(IMAGE, ImageImportOptions.glanceDownload("RegionTwo", "remote1").serviceInterface("public"));

        JsonNode direct = body(expect("POST", "/v2/images/" + IMAGE + "/import"));
        Assert.assertEquals(direct.toString(), "{\"method\":{\"name\":\"glance-direct\"}}");
        JsonNode web = body(expect("POST", "/v2/images/" + IMAGE + "/import"));
        Assert.assertEquals(web.get("method").get("uri").asText(), "https://example.com/cirros.img");
        Assert.assertTrue(web.get("all_stores").asBoolean());
        JsonNode copy = body(expect("POST", "/v2/images/" + IMAGE + "/import"));
        Assert.assertEquals(copy.get("method").get("name").asText(), "copy-image");
        Assert.assertEquals(copy.get("stores").get(1).asText(), "cheap");
        Assert.assertFalse(copy.get("all_stores_must_succeed").asBoolean());
        JsonNode download = body(expect("POST", "/v2/images/" + IMAGE + "/import"));
        Assert.assertEquals(download.get("method").get("glance_region").asText(), "RegionTwo");
        Assert.assertEquals(download.get("method").get("glance_image_id").asText(), "remote1");
        Assert.assertEquals(download.get("method").get("glance_service_interface").asText(), "public");
    }

    public void stageSendsOctetStream() throws Exception {
        respondWith(204);
        byte[] data = "image-bytes".getBytes(StandardCharsets.UTF_8);
        Assert.assertTrue(osv3().imagesV2().stage(IMAGE, Payloads.create(new ByteArrayInputStream(data))).isSuccess());
        RecordedRequest request = expect("PUT", "/v2/images/" + IMAGE + "/stage");
        Assert.assertTrue(request.getHeader("Content-Type").startsWith("application/octet-stream"), request.getHeader("Content-Type"));
        Assert.assertEquals(request.getBody().readUtf8(), "image-bytes");
    }

    public void locationsReadRootArray() throws Exception {
        respondWith(200, "[{\"url\": \"cinder://lvmdriver-1/39e6ffab\", \"metadata\": {\"store\": \"lvmdriver-1\"}}]");
        respondWith(202);

        List<? extends ImageLocation> locations = osv3().imagesV2().listLocations(IMAGE);
        boolean added = osv3().imagesV2().addLocation(IMAGE, "cinder://lvmdriver-1/39e6ffab", Map.of("os_hash_algo", "sha512", "os_hash_value", "c504")).isSuccess();

        expect("GET", "/v2/images/" + IMAGE + "/locations");
        RecordedRequest add = expect("POST", "/v2/images/" + IMAGE + "/locations");
        Assert.assertEquals(body(add).get("url").asText(), "cinder://lvmdriver-1/39e6ffab");
        Assert.assertEquals(body(add).get("validation_data").get("os_hash_algo").asText(), "sha512");
        Assert.assertEquals(locations.get(0).getMetadata().get("store"), "lvmdriver-1");
        Assert.assertTrue(added);
    }

    public void addLocationWithoutValidationDataOmitsIt() throws Exception {
        respondWith(202);
        osv3().imagesV2().addLocation(IMAGE, "file:///var/lib/glance/x", null);
        Assert.assertFalse(body(takeRequest()).has("validation_data"));
    }

    public void imageTasksAndStoreDelete() throws Exception {
        respondWith(200, "{\"tasks\": [{\"id\": \"ee22890e\", \"image_id\": \"" + IMAGE + "\", \"request-id\": \"r1\", \"user\": \"u1\", \"type\": \"api_image_import\","
                + " \"status\": \"processing\", \"owner\": \"o1\", \"expires_at\": null, \"created_at\": \"2020-12-18T05:20:38.000000\", \"updated_at\": \"2020-12-18T05:25:39.000000\", \"deleted\": false, \"message\": \"\"}]}");
        respondWith(204);

        List<? extends Task> tasks = osv3().imagesV2().listTasks(IMAGE);
        boolean deleted = osv3().imagesV2().deleteFromStore("fast", IMAGE).isSuccess();

        expect("GET", "/v2/images/" + IMAGE + "/tasks");
        expect("DELETE", "/v2/stores/fast/" + IMAGE);
        Assert.assertEquals(tasks.get(0).getId(), "ee22890e");
        Assert.assertTrue(deleted);
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `ImageImportOptions`, `stage` 등 없음.

- [ ] **Step 3: 구현**

`/tmp/g2.json`:
```json
[
 {"iface": "ImageLocation", "cls": "GlanceImageLocation", "doc": "A location of image data in a store.", "root": null, "list": null,
  "fields": [["url", "getUrl", "String"], ["metadata", "getMetadata", "Map<String, Object>"]]}
]
```
`ImageImportOptions`:
```java
package org.openstack4j.model.image.v2.options;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Body of {@code POST /v2/images/{id}/import} (interoperable image import, API 2.6+). */
public class ImageImportOptions extends ImageAttributes<ImageImportOptions> {

    private final Map<String, Object> method = new LinkedHashMap<>();

    private ImageImportOptions(String name) {
        method.put("name", name);
        attribute("method", method);
    }

    /** Imports data previously sent with {@code stage()}. */
    public static ImageImportOptions glanceDirect() { return new ImageImportOptions("glance-direct"); }

    /** Glance downloads the image from a URI. */
    public static ImageImportOptions webDownload(String uri) {
        ImageImportOptions options = new ImageImportOptions("web-download");
        options.method.put("uri", Objects.requireNonNull(uri));
        return options;
    }

    /** Copies an existing image into more stores. */
    public static ImageImportOptions copyImage(List<String> stores) {
        return new ImageImportOptions("copy-image").stores(Objects.requireNonNull(stores));
    }

    /** Downloads the image from the Glance of another region. */
    public static ImageImportOptions glanceDownload(String regionName, String imageId) {
        ImageImportOptions options = new ImageImportOptions("glance-download");
        options.method.put("glance_region", Objects.requireNonNull(regionName));
        options.method.put("glance_image_id", Objects.requireNonNull(imageId));
        return options;
    }

    @Override
    protected ImageImportOptions self() {
        return this;
    }

    /** For glance-download: the interface of the remote Glance endpoint (public, internal, admin). */
    public ImageImportOptions serviceInterface(String serviceInterface) {
        if (serviceInterface != null)
            method.put("glance_service_interface", serviceInterface);
        return this;
    }

    public ImageImportOptions stores(List<String> stores) { return put("stores", stores); }
    public ImageImportOptions allStores(boolean allStores) { return put("all_stores", allStores); }
    public ImageImportOptions allStoresMustSucceed(boolean mustSucceed) { return put("all_stores_must_succeed", mustSucceed); }
}
```
(`attribute("method", method)` 은 같은 Map 객체를 넣으므로 이후 `method.put` 이 `toMap()` 결과에 반영된다 — `toMap()` 은 바깥 Map 만 복사한다.)

`ImageServiceImpl` 에 추가:
```java
    @Override
    public ActionResponse importImage(String imageId, ImageImportOptions options) {
        return postWithResponse(uri("/images/%s/import", Objects.requireNonNull(imageId))).entity(JsonBody.of(Objects.requireNonNull(options).toMap())).execute();
    }

    @Override
    public ActionResponse stage(String imageId, Payload<?> payload) {
        return put(ActionResponse.class, uri("/images/%s/stage", Objects.requireNonNull(imageId)))
                .header(HEADER_CONTENT_TYPE, CONTENT_TYPE_OCTECT_STREAM).entity(Objects.requireNonNull(payload)).execute();
    }

    @Override
    public List<? extends ImageLocation> listLocations(String imageId) {
        GlanceImageLocation[] locations = get(GlanceImageLocation[].class, uri("/images/%s/locations", Objects.requireNonNull(imageId)))
                .execute(BaseImageExtService.propagate404());
        return locations == null ? Collections.emptyList() : Arrays.asList(locations);
    }

    @Override
    public ActionResponse addLocation(String imageId, String url, Map<String, Object> validationData) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("url", Objects.requireNonNull(url));
        if (validationData != null)
            body.put("validation_data", validationData);
        return postWithResponse(uri("/images/%s/locations", Objects.requireNonNull(imageId))).entity(JsonBody.of(body)).execute();
    }

    @Override
    public List<? extends Task> listTasks(String imageId) {
        return get(GlanceTask.Tasks.class, uri("/images/%s/tasks", Objects.requireNonNull(imageId))).execute(BaseImageExtService.propagate404()).getList();
    }

    @Override
    public ActionResponse deleteFromStore(String storeId, String imageId) {
        return deleteWithResponse(uri("/stores/%s/%s", Objects.requireNonNull(storeId), Objects.requireNonNull(imageId))).execute();
    }
```
(`stage` 는 기존 `upload` 와 같은 방식. `entity(Payload)` 가 content type 을 덮으면 `upload` 처럼 header 로 지정한 값이 이긴다 — `stageSendsOctetStream` 으로 세 connector 확인.) `ImageService` 선언 6개(Javadoc: 최소 API 버전).

- [ ] **Step 4: 통과 확인 (세 connector)**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && for c in httpclient okhttp http-connector; do ./mvnw -B test -pl connectors/$c -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ImageImportTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:' | tail -1; done`
Expected: 세 connector 모두 `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/g2.log 2>&1; grep -q 'BUILD SUCCESS' /tmp/g2.log
git add -A && git commit -m "feat(image): add image import, staging, locations, image tasks and store deletion

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "imagesV2(): importImage(glance-direct/web-download/copy-image/glance-download), stage, locations list/add, image tasks, deleteFromStore."
```

---

### Task 3: cache API

**Files:**
- Create: 모델 `ImageCacheState.java`, `CachedImageEntry.java`; 도메인 `GlanceImageCacheState.java`; `api/image/v2/ext/ImageCacheService.java` + impl
- Modify: `ImageService`(`cache()`), `ImageServiceImpl`, `DefaultAPIProvider`
- Create: core-test `ImageCacheTests.java`

**Interfaces:**
- Produces: `ImageService.cache()` → `ImageCacheService`: `ImageCacheState list()`, `ActionResponse queue(String imageId)`, `ActionResponse delete(String imageId)`, `ActionResponse clear()`, `ActionResponse clear(String target)`(헤더 `x-image-cache-clear-target`: cache/queue), `ActionResponse clean()`, `ActionResponse prune()`
- `ImageCacheState`: `List<? extends CachedImageEntry> getCachedImages()`, `List<String> getQueuedImages()`; `CachedImageEntry`: `getImageId()`, `Integer getHits()`, `Double getLastAccessed()`, `Double getLastModified()`, `Long getSize()`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/g3-cache
```

`ImageCacheTests.java`:
```java
package org.openstack4j.api.image.v2.ext;

import org.openstack4j.model.image.v2.ext.ImageCacheState;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/Cache")
public class ImageCacheTests extends AbstractImageExtTest {

    public void cacheLifecycle() throws Exception {
        respondWith(200, "{\"cached_images\": [{\"image_id\": \"fe05d6c9\", \"hits\": 0, \"last_accessed\": 1651504844.0860524, \"last_modified\": 1651504844.0860524, \"size\": 987654}],"
                + " \"queued_images\": [\"e34e6e2f\", \"6b9fbf2b\"]}");
        for (int i = 0; i < 6; i++)
            respondWith(i < 2 ? 202 : 204);

        var cache = osv3().imagesV2().cache();
        ImageCacheState state = cache.list();
        Assert.assertTrue(cache.queue("e34e6e2f").isSuccess());
        Assert.assertTrue(cache.clean().isSuccess());
        cache.prune();
        cache.delete("fe05d6c9");
        cache.clear();
        cache.clear("queue");

        expect("GET", "/v2/cache");
        expect("PUT", "/v2/cache/e34e6e2f");
        expect("POST", "/v2/cache/clean");
        expect("POST", "/v2/cache/prune");
        expect("DELETE", "/v2/cache/fe05d6c9");
        Assert.assertNull(expect("DELETE", "/v2/cache").getHeader("x-image-cache-clear-target"));
        Assert.assertEquals(expect("DELETE", "/v2/cache").getHeader("x-image-cache-clear-target"), "queue");
        Assert.assertEquals(state.getCachedImages().get(0).getSize(), Long.valueOf(987654));
        Assert.assertEquals(state.getCachedImages().get(0).getLastAccessed(), 1651504844.0860524, 1e-6);
        Assert.assertEquals(state.getQueuedImages().size(), 2);
    }

    public void cacheDisabledIsRaised() throws Exception {
        respondWith(404, "<html><body><h1>404 Not Found</h1></body></html>");
        try {
            osv3().imagesV2().cache().list();
            Assert.fail("expected the 404 to surface");
        } catch (RuntimeException expected) {
            Assert.assertNotNull(expected.getMessage());
        }
        takeRequest();
    }
}
```
(`respondWith(i < 2 ? 202 : 204)` 의 순서는 queue(202), clean(202), prune·delete·clear·clear(204) 이다 — `ActionResponse` 는 2xx 면 성공.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -3`
Expected: `cache()`, `ImageCacheState` 없음.

- [ ] **Step 3: 구현**

`/tmp/g3.json`:
```json
[
 {"iface": "CachedImageEntry", "cls": "GlanceCachedImageEntry", "doc": "An image in the Glance cache.", "root": null, "list": null,
  "fields": [["image_id", "getImageId", "String"], ["hits", "getHits", "Integer"], ["last_accessed", "getLastAccessed", "Double"], ["last_modified", "getLastModified", "Double"], ["size", "getSize", "Long"]]}
]
```
직접 작성 `ImageCacheState`(model) / `GlanceImageCacheState`: `@JsonProperty("cached_images") List<GlanceCachedImageEntry> cachedImages; @JsonProperty("queued_images") List<String> queuedImages;` + getter.

`ImageCacheServiceImpl`:
```java
package org.openstack4j.openstack.image.v2.internal.ext;

import org.openstack4j.api.image.v2.ext.ImageCacheService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.image.v2.ext.ImageCacheState;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceImageCacheState;

public class ImageCacheServiceImpl extends BaseImageExtService implements ImageCacheService {

    @Override public ImageCacheState list() { return showStrict(GlanceImageCacheState.class, "/cache"); }
    @Override public ActionResponse queue(String imageId) { return putWithResponse("/cache/" + id(imageId)).execute(); }
    @Override public ActionResponse delete(String imageId) { return remove("/cache/" + id(imageId)); }
    @Override public ActionResponse clear() { return remove("/cache"); }
    @Override public ActionResponse clear(String target) { return deleteWithResponse("/cache").header("x-image-cache-clear-target", target).execute(); }
    @Override public ActionResponse clean() { return postWithResponse("/cache/clean").execute(); }
    @Override public ActionResponse prune() { return postWithResponse("/cache/prune").execute(); }
}
```
`ImageCacheService`(Javadoc: API 2.14, 관리자, 캐시 middleware 필요), `ImageService.cache()`, binding.

- [ ] **Step 4: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ImageCacheTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/g3.log 2>&1; grep -q 'BUILD SUCCESS' /tmp/g3.log
git add -A && git commit -m "feat(image): add the Glance cache API

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "imagesV2().cache(): list, queue, delete, clear(target), clean, prune (API 2.14)."
```

---

### Task 4: schemas

**Files:**
- Create: `api/image/v2/ext/ImageSchemaService.java` + `openstack/image/v2/internal/ext/ImageSchemaServiceImpl.java`
- Modify: `ImageService`(`schemas()`), `ImageServiceImpl`, `DefaultAPIProvider`
- Create: core-test `ImageSchemaTests.java`

**Interfaces:**
- Produces: `ImageService.schemas()` → `ImageSchemaService`: `image()`, `images()`, `member()`, `members()`, `task()`, `tasks()`, `metadefNamespace()`, `metadefNamespaces()`, `metadefObject()`, `metadefObjects()`, `metadefProperty()`, `metadefProperties()`, `metadefResourceType()`, `metadefResourceTypes()`, `metadefTag()`, `metadefTags()` — 모두 `Map<String, Object>`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/g4-schemas
```

`ImageSchemaTests.java`:
```java
package org.openstack4j.api.image.v2.ext;

import java.util.Map;
import java.util.function.Supplier;

import org.openstack4j.api.image.v2.ext.ImageSchemaService;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/Schemas")
public class ImageSchemaTests extends AbstractImageExtTest {

    public void everySchemaPath() throws Exception {
        ImageSchemaService schemas = osv3().imagesV2().schemas();
        Object[][] cases = {
                {(Supplier<Map<String, Object>>) schemas::image, "/v2/schemas/image"},
                {(Supplier<Map<String, Object>>) schemas::images, "/v2/schemas/images"},
                {(Supplier<Map<String, Object>>) schemas::member, "/v2/schemas/member"},
                {(Supplier<Map<String, Object>>) schemas::members, "/v2/schemas/members"},
                {(Supplier<Map<String, Object>>) schemas::task, "/v2/schemas/task"},
                {(Supplier<Map<String, Object>>) schemas::tasks, "/v2/schemas/tasks"},
                {(Supplier<Map<String, Object>>) schemas::metadefNamespace, "/v2/schemas/metadefs/namespace"},
                {(Supplier<Map<String, Object>>) schemas::metadefNamespaces, "/v2/schemas/metadefs/namespaces"},
                {(Supplier<Map<String, Object>>) schemas::metadefObject, "/v2/schemas/metadefs/object"},
                {(Supplier<Map<String, Object>>) schemas::metadefObjects, "/v2/schemas/metadefs/objects"},
                {(Supplier<Map<String, Object>>) schemas::metadefProperty, "/v2/schemas/metadefs/property"},
                {(Supplier<Map<String, Object>>) schemas::metadefProperties, "/v2/schemas/metadefs/properties"},
                {(Supplier<Map<String, Object>>) schemas::metadefResourceType, "/v2/schemas/metadefs/resource_type"},
                {(Supplier<Map<String, Object>>) schemas::metadefResourceTypes, "/v2/schemas/metadefs/resource_types"},
                {(Supplier<Map<String, Object>>) schemas::metadefTag, "/v2/schemas/metadefs/tag"},
                {(Supplier<Map<String, Object>>) schemas::metadefTags, "/v2/schemas/metadefs/tags"}};
        for (Object[] c : cases) {
            respondWith(200, "{\"name\": \"x\", \"properties\": {\"id\": {\"type\": \"string\"}}, \"additionalProperties\": {\"type\": \"string\"}}");
            @SuppressWarnings("unchecked")
            Map<String, Object> schema = ((Supplier<Map<String, Object>>) c[0]).get();
            expect("GET", (String) c[1]);
            Assert.assertEquals(schema.get("name"), "x");
            Assert.assertTrue(schema.get("properties") instanceof Map);
        }
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -3`
Expected: `ImageSchemaService` 없음.

- [ ] **Step 3: 구현**

```java
package org.openstack4j.openstack.image.v2.internal.ext;

import java.util.Collections;
import java.util.Map;

import org.openstack4j.api.image.v2.ext.ImageSchemaService;

public class ImageSchemaServiceImpl extends BaseImageExtService implements ImageSchemaService {

    @SuppressWarnings("unchecked")
    private Map<String, Object> schema(String name) {
        Map<String, Object> schema = showStrict(Map.class, "/schemas/" + name);
        return schema == null ? Collections.emptyMap() : schema;
    }

    @Override public Map<String, Object> image() { return schema("image"); }
    @Override public Map<String, Object> images() { return schema("images"); }
    @Override public Map<String, Object> member() { return schema("member"); }
    @Override public Map<String, Object> members() { return schema("members"); }
    @Override public Map<String, Object> task() { return schema("task"); }
    @Override public Map<String, Object> tasks() { return schema("tasks"); }
    @Override public Map<String, Object> metadefNamespace() { return schema("metadefs/namespace"); }
    @Override public Map<String, Object> metadefNamespaces() { return schema("metadefs/namespaces"); }
    @Override public Map<String, Object> metadefObject() { return schema("metadefs/object"); }
    @Override public Map<String, Object> metadefObjects() { return schema("metadefs/objects"); }
    @Override public Map<String, Object> metadefProperty() { return schema("metadefs/property"); }
    @Override public Map<String, Object> metadefProperties() { return schema("metadefs/properties"); }
    @Override public Map<String, Object> metadefResourceType() { return schema("metadefs/resource_type"); }
    @Override public Map<String, Object> metadefResourceTypes() { return schema("metadefs/resource_types"); }
    @Override public Map<String, Object> metadefTag() { return schema("metadefs/tag"); }
    @Override public Map<String, Object> metadefTags() { return schema("metadefs/tags"); }
}
```
인터페이스(Javadoc: JSON schema 원문), `ImageService.schemas()`, binding.

- [ ] **Step 4: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ImageSchemaTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Tests run: 1, Failures: 0, Errors: 0`

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/g4.log 2>&1; grep -q 'BUILD SUCCESS' /tmp/g4.log
git add -A && git commit -m "feat(image): add the Glance JSON schemas

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "imagesV2().schemas(): image/member/task 와 metadef 10종 JSON schema(16)."
```

---

### Task 5: metadef namespaces, resource types

**Files:**
- Create: 모델 `MetadefNamespace.java`, `MetadefResourceType.java`, `MetadefResourceTypeAssociation.java`; 도메인 `GlanceMetadefNamespace.java`(목록 `Namespaces`), `GlanceMetadefResourceType.java`(목록 `ResourceTypes`), `GlanceMetadefResourceTypeAssociation.java`(목록 `Associations`); 옵션 `MetadefNamespaceOptions.java`; `api/image/v2/ext/MetadefService.java` + `openstack/image/v2/internal/ext/MetadefServiceImpl.java`
- Modify: `ImageService`(`metadefs()`), `ImageServiceImpl`, `DefaultAPIProvider`
- Create: core-test `MetadefNamespaceTests.java`

**Interfaces:**
- Produces: `ImageService.metadefs()` → `MetadefService`(Task 6·7 이 메서드를 더한다):
  - `List<? extends MetadefNamespace> listNamespaces()`, `listNamespaces(Map<String, String> filters)`, `MetadefNamespace getNamespace(String namespace)`, `MetadefNamespace createNamespace(MetadefNamespaceOptions options)`, `MetadefNamespace updateNamespace(String namespace, MetadefNamespaceOptions options)`, `ActionResponse deleteNamespace(String namespace)`
  - `List<? extends MetadefResourceType> listResourceTypes()`, `List<? extends MetadefResourceTypeAssociation> listResourceTypeAssociations(String namespace)`, `MetadefResourceTypeAssociation associateResourceType(String namespace, String name, String prefix, String propertiesTarget)`(prefix·target null 이면 생략), `ActionResponse dissociateResourceType(String namespace, String name)`
- `MetadefNamespace`: `getNamespace()`, `getDisplayName()`, `getDescription()`, `getVisibility()`, `Boolean isProtected()`, `getOwner()`, `getCreatedAt()`, `getUpdatedAt()`, `Map<String, Object> getProperties()`, `List<Map<String, Object>> getObjects()`, `List<Map<String, Object>> getResourceTypeAssociations()`, `List<Map<String, Object>> getTags()`
- `MetadefResourceType`: `getName()`, `getCreatedAt()`, `getUpdatedAt()`; `MetadefResourceTypeAssociation`: `getName()`, `getPrefix()`, `getPropertiesTarget()`, `getCreatedAt()`, `getUpdatedAt()`
- `MetadefNamespaceOptions`: `create(String namespace)`, `update(String namespace)`(PUT 은 전체 본문이라 namespace 이름이 필요); `displayName`, `description`, `visibility`(public/private), `protectedNamespace(Boolean)`(JSON `protected`)

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/g5-metadef-namespaces
```

`MetadefNamespaceTests.java`:
```java
package org.openstack4j.api.image.v2.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.image.v2.ext.MetadefNamespace;
import org.openstack4j.model.image.v2.ext.MetadefResourceTypeAssociation;
import org.openstack4j.model.image.v2.options.MetadefNamespaceOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/MetadefNamespaces")
public class MetadefNamespaceTests extends AbstractImageExtTest {

    private static final String NS = "OS::Compute::Libvirt";
    private static final String NS_JSON = "{\"created_at\": \"2016-06-28T14:57:10Z\", \"description\": \"The libvirt compute driver options.\", \"display_name\": \"libvirt Driver Options\","
            + " \"namespace\": \"" + NS + "\", \"owner\": \"admin\", \"properties\": {\"boot_menu\": {\"description\": \"d\", \"enum\": [\"true\", \"false\"], \"title\": \"Boot Menu\", \"type\": \"string\"}},"
            + " \"protected\": true, \"resource_type_associations\": [{\"created_at\": \"2016-06-28T14:57:10Z\", \"name\": \"OS::Glance::Image\", \"prefix\": \"hw_\"}],"
            + " \"schema\": \"/v2/schemas/metadefs/namespace\", \"self\": \"/v2/metadefs/namespaces/" + NS + "\", \"updated_at\": \"2016-06-28T14:57:10Z\", \"visibility\": \"public\"}";

    public void namespaces() throws Exception {
        respondWith(201, NS_JSON);
        respondWith(200, "{\"first\": \"/v2/metadefs/namespaces\", \"namespaces\": [" + NS_JSON + "], \"schema\": \"/v2/schemas/metadefs/namespaces\"}");
        respondWith(200, "{\"namespaces\": []}");
        respondWith(200, NS_JSON);
        respondWith(200, NS_JSON);
        respondWith(204);

        var metadefs = osv3().imagesV2().metadefs();
        MetadefNamespace created = metadefs.createNamespace(MetadefNamespaceOptions.create(NS).displayName("libvirt Driver Options").visibility("public").protectedNamespace(true));
        List<? extends MetadefNamespace> all = metadefs.listNamespaces();
        metadefs.listNamespaces(Map.of("resource_types", "OS::Glance::Image"));
        metadefs.getNamespace(NS);
        metadefs.updateNamespace(NS, MetadefNamespaceOptions.update(NS).description("changed"));
        metadefs.deleteNamespace(NS);

        RecordedRequest create = expect("POST", "/v2/metadefs/namespaces");
        Assert.assertEquals(body(create).get("namespace").asText(), NS);
        Assert.assertTrue(body(create).get("protected").asBoolean());
        Assert.assertFalse(body(create).has("description"));
        expect("GET", "/v2/metadefs/namespaces");
        Assert.assertTrue(decodedPath(takeRequest()).endsWith("/v2/metadefs/namespaces?resource_types=OS::Glance::Image"));
        expect("GET", "/v2/metadefs/namespaces/" + NS);
        RecordedRequest update = expect("PUT", "/v2/metadefs/namespaces/" + NS);
        Assert.assertEquals(body(update).get("namespace").asText(), NS);
        Assert.assertEquals(body(update).get("description").asText(), "changed");
        expect("DELETE", "/v2/metadefs/namespaces/" + NS);
        Assert.assertTrue(created.isProtected());
        Assert.assertEquals(created.getDisplayName(), "libvirt Driver Options");
        Assert.assertEquals(((Map<?, ?>) created.getProperties().get("boot_menu")).get("title"), "Boot Menu");
        Assert.assertEquals(all.get(0).getResourceTypeAssociations().get(0).get("prefix"), "hw_");
    }

    public void resourceTypes() throws Exception {
        respondWith(200, "{\"resource_types\": [{\"created_at\": \"2014-08-28T18:13:04Z\", \"name\": \"OS::Glance::Image\", \"updated_at\": \"2014-08-28T18:13:04Z\"}]}");
        respondWith(200, "{\"resource_type_associations\": [{\"created_at\": \"2018-03-05T18:20:44Z\", \"name\": \"OS::Nova::Flavor\", \"prefix\": \"hw:\"}]}");
        respondWith(201, "{\"created_at\": \"2014-09-19T16:09:13Z\", \"name\": \"OS::Cinder::Volume\", \"prefix\": \"hw_\", \"properties_target\": \"image\", \"updated_at\": \"2014-09-19T16:09:13Z\"}");
        respondWith(204);

        var metadefs = osv3().imagesV2().metadefs();
        Assert.assertEquals(metadefs.listResourceTypes().get(0).getName(), "OS::Glance::Image");
        Assert.assertEquals(metadefs.listResourceTypeAssociations(NS).get(0).getPrefix(), "hw:");
        MetadefResourceTypeAssociation association = metadefs.associateResourceType(NS, "OS::Cinder::Volume", "hw_", "image");
        Assert.assertTrue(metadefs.dissociateResourceType(NS, "OS::Cinder::Volume").isSuccess());

        expect("GET", "/v2/metadefs/resource_types");
        expect("GET", "/v2/metadefs/namespaces/" + NS + "/resource_types");
        RecordedRequest associate = expect("POST", "/v2/metadefs/namespaces/" + NS + "/resource_types");
        Assert.assertEquals(body(associate).get("name").asText(), "OS::Cinder::Volume");
        Assert.assertEquals(body(associate).get("properties_target").asText(), "image");
        expect("DELETE", "/v2/metadefs/namespaces/" + NS + "/resource_types/OS::Cinder::Volume");
        Assert.assertEquals(association.getPropertiesTarget(), "image");
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -3`
Expected: `metadefs()` 등 없음.

- [ ] **Step 3: 모델 생성**

`/tmp/g5.json`:
```json
[
 {"iface": "MetadefNamespace", "cls": "GlanceMetadefNamespace", "doc": "A metadata definition namespace.", "root": null, "list": "namespaces", "listCls": "Namespaces",
  "fields": [["namespace", "getNamespace", "String"], ["display_name", "getDisplayName", "String"], ["description", "getDescription", "String"],
             ["visibility", "getVisibility", "String"], ["protected", "isProtected", "Boolean"], ["owner", "getOwner", "String"],
             ["created_at", "getCreatedAt", "String"], ["updated_at", "getUpdatedAt", "String"], ["properties", "getProperties", "Map<String, Object>"],
             ["objects", "getObjects", "List<Map<String, Object>>"], ["resource_type_associations", "getResourceTypeAssociations", "List<Map<String, Object>>"],
             ["tags", "getTags", "List<Map<String, Object>>"]]},
 {"iface": "MetadefResourceType", "cls": "GlanceMetadefResourceType", "doc": "A resource type metadefs can apply to (for example OS::Glance::Image).", "root": null, "list": "resource_types", "listCls": "ResourceTypes",
  "fields": [["name", "getName", "String"], ["created_at", "getCreatedAt", "String"], ["updated_at", "getUpdatedAt", "String"]]},
 {"iface": "MetadefResourceTypeAssociation", "cls": "GlanceMetadefResourceTypeAssociation", "doc": "An association of a namespace with a resource type.", "root": null, "list": "resource_type_associations", "listCls": "Associations",
  "fields": [["name", "getName", "String"], ["prefix", "getPrefix", "String"], ["properties_target", "getPropertiesTarget", "String"], ["created_at", "getCreatedAt", "String"], ["updated_at", "getUpdatedAt", "String"]]}
]
```
(필드 `protected` 는 예약어라 생성기가 `protectedValue` 로 바꾼다 — 생성기의 RESERVED 에 `protected` 를 더한다.)

- [ ] **Step 4: 옵션과 서비스 구현**

`MetadefNamespaceOptions`:
```java
package org.openstack4j.model.image.v2.options;

import java.util.Objects;

/** Body of a metadef namespace create or (full) update. */
public class MetadefNamespaceOptions extends ImageAttributes<MetadefNamespaceOptions> {

    public static MetadefNamespaceOptions create(String namespace) {
        return new MetadefNamespaceOptions().put("namespace", Objects.requireNonNull(namespace));
    }

    /** PUT replaces the namespace, so the (possibly new) namespace name is required. */
    public static MetadefNamespaceOptions update(String namespace) {
        return create(namespace);
    }

    @Override
    protected MetadefNamespaceOptions self() {
        return this;
    }

    public MetadefNamespaceOptions displayName(String displayName) { return put("display_name", displayName); }
    public MetadefNamespaceOptions description(String description) { return put("description", description); }
    /** public or private. */
    public MetadefNamespaceOptions visibility(String visibility) { return put("visibility", visibility); }
    /** A protected namespace cannot be deleted. */
    public MetadefNamespaceOptions protectedNamespace(Boolean isProtected) { return put("protected", isProtected); }
}
```
`MetadefServiceImpl`(Task 6·7 이 이어서 채운다):
```java
package org.openstack4j.openstack.image.v2.internal.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.image.v2.ext.MetadefService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.image.v2.ext.MetadefNamespace;
import org.openstack4j.model.image.v2.ext.MetadefResourceType;
import org.openstack4j.model.image.v2.ext.MetadefResourceTypeAssociation;
import org.openstack4j.model.image.v2.options.MetadefNamespaceOptions;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceMetadefNamespace;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceMetadefNamespace.Namespaces;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceMetadefResourceType.ResourceTypes;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceMetadefResourceTypeAssociation;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceMetadefResourceTypeAssociation.Associations;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class MetadefServiceImpl extends BaseImageExtService implements MetadefService {

    private static final String NS = "/metadefs/namespaces";

    protected static String ns(String namespace) {
        return NS + "/" + id(namespace);
    }

    @Override public List<? extends MetadefNamespace> listNamespaces() { return listOf(Namespaces.class, NS, null); }
    @Override public List<? extends MetadefNamespace> listNamespaces(Map<String, String> filters) { return listOf(Namespaces.class, NS, filters); }
    @Override public MetadefNamespace getNamespace(String namespace) { return show(GlanceMetadefNamespace.class, ns(namespace)); }
    @Override public MetadefNamespace createNamespace(MetadefNamespaceOptions options) { return create(GlanceMetadefNamespace.class, NS, options); }
    @Override public MetadefNamespace updateNamespace(String namespace, MetadefNamespaceOptions options) { return replace(GlanceMetadefNamespace.class, ns(namespace), options); }
    @Override public ActionResponse deleteNamespace(String namespace) { return remove(ns(namespace)); }

    @Override public List<? extends MetadefResourceType> listResourceTypes() { return listOf(ResourceTypes.class, "/metadefs/resource_types", null); }
    @Override public List<? extends MetadefResourceTypeAssociation> listResourceTypeAssociations(String namespace) { return listOf(Associations.class, ns(namespace) + "/resource_types", null); }

    @Override
    public MetadefResourceTypeAssociation associateResourceType(String namespace, String name, String prefix, String propertiesTarget) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", id(name));
        if (prefix != null) body.put("prefix", prefix);
        if (propertiesTarget != null) body.put("properties_target", propertiesTarget);
        return post(GlanceMetadefResourceTypeAssociation.class, ns(namespace) + "/resource_types").entity(JsonBody.of(body)).execute(propagate404());
    }

    @Override public ActionResponse dissociateResourceType(String namespace, String name) { return remove(ns(namespace) + "/resource_types/" + id(name)); }
}
```
`MetadefService`(Javadoc: metadefs 개요, 경로), `ImageService.metadefs()`, binding.

- [ ] **Step 5: 통과 확인 (세 connector — `::` 경로)**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && for c in httpclient okhttp http-connector; do ./mvnw -B test -pl connectors/$c -Dsurefire.failIfNoSpecifiedTests=false -Dtest='MetadefNamespaceTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:' | tail -1; done`
Expected: 세 connector 모두 `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/g5.log 2>&1; grep -q 'BUILD SUCCESS' /tmp/g5.log
git add -A && git commit -m "feat(image): add metadef namespaces and resource types

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "imagesV2().metadefs(): namespaces CRUD·필터, resource types 목록·연결·해제."
```

---

### Task 6: metadef objects, properties

**Files:**
- Create: 모델 `MetadefObject.java`, `MetadefProperty.java`; 도메인 `GlanceMetadefObject.java`(목록 `Objects`), `GlanceMetadefProperty.java`(직접 작성), `GlanceMetadefProperties.java`(wrapper); 옵션 `MetadefObjectOptions.java`, `MetadefPropertyOptions.java`
- Modify: `MetadefService.java`, `MetadefServiceImpl.java`
- Create: core-test `MetadefObjectPropertyTests.java`

**Interfaces:**
- Produces (`MetadefService` 에 추가):
  - `List<? extends MetadefObject> listObjects(String namespace)`, `MetadefObject getObject(String namespace, String name)`, `MetadefObject createObject(String namespace, MetadefObjectOptions options)`, `MetadefObject updateObject(String namespace, String name, MetadefObjectOptions options)`, `ActionResponse deleteObject(String namespace, String name)`
  - `Map<String, ? extends MetadefProperty> listProperties(String namespace)`, `MetadefProperty getProperty(String namespace, String name)`, `MetadefProperty createProperty(String namespace, MetadefPropertyOptions options)`, `MetadefProperty updateProperty(String namespace, String name, MetadefPropertyOptions options)`, `ActionResponse deleteProperty(String namespace, String name)`
- `MetadefObject`: `getName()`, `getDescription()`, `List<String> getRequired()`, `Map<String, Object> getProperties()`, `getCreatedAt()`, `getUpdatedAt()`
- `MetadefProperty`: `getName()`, `getTitle()`, `getDescription()`, `getType()`, `Map<String, Object> getSchema()`(응답 전체 — enum, minimum, items …)
- `MetadefObjectOptions`: `create(String name)`(update 도 같은 팩토리 — PUT 전체 본문); `description`, `required(List<String>)`, `properties(Map<String, Object>)`
- `MetadefPropertyOptions`: `create(String name, String title, String type)`; `description`, `enumValues(List<?>)`(JSON `enum`), `minimum(Number)`, `maximum(Number)`, `defaultValue(Object)`(JSON `default`), `readonly(Boolean)`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/g6-metadef-objects
```

`MetadefObjectPropertyTests.java`:
```java
package org.openstack4j.api.image.v2.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.image.v2.ext.MetadefObject;
import org.openstack4j.model.image.v2.ext.MetadefProperty;
import org.openstack4j.model.image.v2.options.MetadefObjectOptions;
import org.openstack4j.model.image.v2.options.MetadefPropertyOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/MetadefObjects")
public class MetadefObjectPropertyTests extends AbstractImageExtTest {

    private static final String NS = "OS::Compute::Quota";
    private static final String P = "/v2/metadefs/namespaces/" + NS;

    public void objects() throws Exception {
        String object = "{\"created_at\": \"2014-09-19T18:20:56Z\", \"description\": \"CPU limits\", \"name\": \"CPU Limits\", \"required\": [],"
                + " \"properties\": {\"quota:cpu_period\": {\"description\": \"d\", \"maximum\": 1000000, \"minimum\": 1000, \"title\": \"Quota: CPU Period\", \"type\": \"integer\"}},"
                + " \"schema\": \"/v2/schemas/metadefs/object\", \"self\": \"/v2/metadefs/namespaces/" + NS + "/objects/CPU Limits\", \"updated_at\": \"2014-09-19T18:20:56Z\"}";
        respondWith(201, object);
        respondWith(200, "{\"objects\": [" + object + "], \"schema\": \"v2/schemas/metadefs/objects\"}");
        respondWith(200, object);
        respondWith(200, object);
        respondWith(204);

        var metadefs = osv3().imagesV2().metadefs();
        MetadefObject created = metadefs.createObject(NS, MetadefObjectOptions.create("CPU Limits").description("CPU limits")
                .properties(Map.of("quota:cpu_period", Map.of("type", "integer", "title", "Quota: CPU Period"))));
        List<? extends MetadefObject> all = metadefs.listObjects(NS);
        metadefs.getObject(NS, "CPU Limits");
        metadefs.updateObject(NS, "CPU Limits", MetadefObjectOptions.create("CPU Limits").required(List.of("quota:cpu_period")));
        metadefs.deleteObject(NS, "CPU Limits");

        RecordedRequest create = expect("POST", P + "/objects");
        Assert.assertEquals(body(create).get("properties").get("quota:cpu_period").get("type").asText(), "integer");
        expect("GET", P + "/objects");
        expect("GET", P + "/objects/CPU Limits");
        Assert.assertEquals(body(expect("PUT", P + "/objects/CPU Limits")).get("required").get(0).asText(), "quota:cpu_period");
        expect("DELETE", P + "/objects/CPU Limits");
        Assert.assertEquals(created.getName(), "CPU Limits");
        Assert.assertTrue(created.getRequired().isEmpty());
        Assert.assertEquals(((Map<?, ?>) all.get(0).getProperties().get("quota:cpu_period")).get("minimum"), 1000);
    }

    public void properties() throws Exception {
        String property = "{\"name\": \"hypervisor_type\", \"title\": \"Hypervisor Type\", \"description\": \"The hypervisor type.\", \"type\": \"string\","
                + " \"enum\": [\"xen\", \"qemu\", \"kvm\"]}";
        respondWith(201, property);
        respondWith(200, "{\"properties\": {\"hw_disk_bus\": {\"description\": \"d\", \"enum\": [\"scsi\", \"virtio\"], \"title\": \"Disk Bus\", \"type\": \"string\"},"
                + " \"hw_rng_model\": {\"default\": \"virtio\", \"title\": \"Random Number Generator Device\", \"type\": \"string\"}}}");
        respondWith(200, property);
        respondWith(200, property);
        respondWith(204);

        var metadefs = osv3().imagesV2().metadefs();
        MetadefProperty created = metadefs.createProperty(NS, MetadefPropertyOptions.create("hypervisor_type", "Hypervisor Type", "string")
                .description("The hypervisor type.").enumValues(List.of("xen", "qemu", "kvm")));
        Map<String, ? extends MetadefProperty> all = metadefs.listProperties(NS);
        MetadefProperty one = metadefs.getProperty(NS, "hypervisor_type");
        metadefs.updateProperty(NS, "hypervisor_type", MetadefPropertyOptions.create("hypervisor_type", "Hypervisor Type", "string").defaultValue("kvm"));
        metadefs.deleteProperty(NS, "hypervisor_type");

        RecordedRequest create = expect("POST", P + "/properties");
        Assert.assertEquals(body(create).get("enum").get(2).asText(), "kvm");
        Assert.assertEquals(body(create).get("name").asText(), "hypervisor_type");
        expect("GET", P + "/properties");
        expect("GET", P + "/properties/hypervisor_type");
        Assert.assertEquals(body(expect("PUT", P + "/properties/hypervisor_type")).get("default").asText(), "kvm");
        expect("DELETE", P + "/properties/hypervisor_type");
        Assert.assertEquals(created.getTitle(), "Hypervisor Type");
        Assert.assertEquals(created.getSchema().get("enum"), List.of("xen", "qemu", "kvm"));
        Assert.assertEquals(all.size(), 2);
        Assert.assertEquals(all.get("hw_disk_bus").getName(), "hw_disk_bus");
        Assert.assertEquals(all.get("hw_rng_model").getSchema().get("default"), "virtio");
        Assert.assertEquals(one.getType(), "string");
    }
}
```
(목록 응답의 각 property 에는 `name` 이 없다 — `listProperties` 가 Map 키를 `name` 으로 채운다.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -3`
Expected: `MetadefObject`, `createProperty` 등 없음.

- [ ] **Step 3: 모델**

`/tmp/g6.json`:
```json
[
 {"iface": "MetadefObject", "cls": "GlanceMetadefObject", "doc": "A metadef object: a named group of properties.", "root": null, "list": "objects", "listCls": "MetadefObjects",
  "fields": [["name", "getName", "String"], ["description", "getDescription", "String"], ["required", "getRequired", "List<String>"],
             ["properties", "getProperties", "Map<String, Object>"], ["created_at", "getCreatedAt", "String"], ["updated_at", "getUpdatedAt", "String"]]}
]
```
`MetadefProperty`(model): `getName()`, `getTitle()`, `getDescription()`, `getType()`, `Map<String, Object> getSchema()`.
`GlanceMetadefProperty`(직접 작성):
```java
package org.openstack4j.openstack.image.v2.domain.ext;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.openstack4j.model.image.v2.ext.MetadefProperty;

/** A metadef property: a JSON schema fragment; the common fields have getters, everything is in {@link #getSchema()}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceMetadefProperty implements MetadefProperty {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> schema = new LinkedHashMap<>();

    @JsonAnySetter
    public void set(String key, Object value) {
        schema.put(key, value);
    }

    /** Fills the name from the map key of a list response. */
    public GlanceMetadefProperty named(String name) {
        schema.putIfAbsent("name", name);
        return this;
    }

    @Override public String getName() { return (String) schema.get("name"); }
    @Override public String getTitle() { return (String) schema.get("title"); }
    @Override public String getDescription() { return (String) schema.get("description"); }
    @Override public String getType() { return (String) schema.get("type"); }
    @Override public Map<String, Object> getSchema() { return schema; }
}
```
`GlanceMetadefProperties`: `@JsonProperty("properties") Map<String, GlanceMetadefProperty> properties;` + getter.

옵션 2개(형태는 `MetadefNamespaceOptions` 와 같다): `MetadefObjectOptions.create(String name)`(`name`; `description`, `required(List<String>)`, `properties(Map<String, Object>)`), `MetadefPropertyOptions.create(String name, String title, String type)`(`name`, `title`, `type`; `description`, `enumValues(List<?>)`→`enum`, `minimum(Number)`, `maximum(Number)`, `defaultValue(Object)`→`default`, `readonly(Boolean)`).

- [ ] **Step 4: 서비스 구현 (`MetadefServiceImpl` 에 추가)**

```java
    @Override public List<? extends MetadefObject> listObjects(String namespace) { return listOf(MetadefObjects.class, ns(namespace) + "/objects", null); }
    @Override public MetadefObject getObject(String namespace, String name) { return show(GlanceMetadefObject.class, ns(namespace) + "/objects/" + id(name)); }
    @Override public MetadefObject createObject(String namespace, MetadefObjectOptions options) { return create(GlanceMetadefObject.class, ns(namespace) + "/objects", options); }
    @Override public MetadefObject updateObject(String namespace, String name, MetadefObjectOptions options) { return replace(GlanceMetadefObject.class, ns(namespace) + "/objects/" + id(name), options); }
    @Override public ActionResponse deleteObject(String namespace, String name) { return remove(ns(namespace) + "/objects/" + id(name)); }

    @Override
    public Map<String, ? extends MetadefProperty> listProperties(String namespace) {
        GlanceMetadefProperties result = showStrict(GlanceMetadefProperties.class, ns(namespace) + "/properties");
        Map<String, GlanceMetadefProperty> out = new LinkedHashMap<>();
        if (result != null && result.getProperties() != null)
            result.getProperties().forEach((name, property) -> out.put(name, property.named(name)));
        return out;
    }

    @Override public MetadefProperty getProperty(String namespace, String name) { return show(GlanceMetadefProperty.class, ns(namespace) + "/properties/" + id(name)); }
    @Override public MetadefProperty createProperty(String namespace, MetadefPropertyOptions options) { return create(GlanceMetadefProperty.class, ns(namespace) + "/properties", options); }
    @Override public MetadefProperty updateProperty(String namespace, String name, MetadefPropertyOptions options) { return replace(GlanceMetadefProperty.class, ns(namespace) + "/properties/" + id(name), options); }
    @Override public ActionResponse deleteProperty(String namespace, String name) { return remove(ns(namespace) + "/properties/" + id(name)); }
```
(object 이름의 공백은 connector 가 경로에서 인코딩한다 — `expect` 는 decoded 경로로 비교. 세 connector 에서 확인.) `MetadefService` 선언 10개.

- [ ] **Step 5: 통과 확인 (세 connector — 공백 경로)**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && for c in httpclient okhttp http-connector; do ./mvnw -B test -pl connectors/$c -Dsurefire.failIfNoSpecifiedTests=false -Dtest='MetadefObjectPropertyTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:' | tail -1; done`
Expected: 세 connector 모두 `Tests run: 2, Failures: 0, Errors: 0`. 공백이 있는 경로에서 connector 가 예외를 내면(예: httpclient 의 URI 파싱) 이름을 `%20` 으로 인코딩하는 segment helper 를 넣고(F Task 4 와 같은 방식) Ruling 으로 남긴다.

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/g6.log 2>&1; grep -q 'BUILD SUCCESS' /tmp/g6.log
git add -A && git commit -m "feat(image): add metadef objects and properties

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "imagesV2().metadefs(): objects CRUD, properties CRUD(목록은 이름→property Map, schema 원문 보존)."
```

---

### Task 7: metadef tags

**Files:**
- Create: 모델 `MetadefTag.java`; 도메인 `GlanceMetadefTag.java`(목록 `Tags`)
- Modify: `MetadefService.java`, `MetadefServiceImpl.java`
- Create: core-test `MetadefTagTests.java`

**Interfaces:**
- Produces (`MetadefService` 에 추가): `List<? extends MetadefTag> listTags(String namespace)`, `MetadefTag getTag(String namespace, String name)`, `MetadefTag createTag(String namespace, String name)`, `List<? extends MetadefTag> createTags(String namespace, List<String> names, boolean append)`, `MetadefTag updateTag(String namespace, String name, String newName)`, `ActionResponse deleteTag(String namespace, String name)`, `ActionResponse deleteAllTags(String namespace)`
- `MetadefTag`: `getName()`, `getCreatedAt()`, `getUpdatedAt()`

- [ ] **Step 1: 실패하는 테스트 작성 (Review Focus 5)**

```bash
git switch main && git pull && git switch -c task/g7-metadef-tags
```

`MetadefTagTests.java`:
```java
package org.openstack4j.api.image.v2.ext;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.image.v2.ext.MetadefTag;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/MetadefTags")
public class MetadefTagTests extends AbstractImageExtTest {

    private static final String P = "/v2/metadefs/namespaces/ns1";
    private static final String TAG = "{\"created_at\": \"2015-05-06T23:16:12Z\", \"name\": \"sample-tag2\", \"updated_at\": \"2015-05-06T23:16:12Z\"}";

    public void tagLifecycle() throws Exception {
        respondWith(201, TAG);
        respondWith(200, "{\"tags\": [{\"name\": \"sample-tag1\"}, {\"name\": \"sample-tag2\"}]}");
        respondWith(200, TAG);
        respondWith(200, "{\"created_at\": \"2015-05-06T23:16:12Z\", \"name\": \"renamed\", \"updated_at\": \"2015-05-07T00:00:00Z\"}");
        respondWith(204);
        respondWith(204);

        var metadefs = osv3().imagesV2().metadefs();
        MetadefTag created = metadefs.createTag("ns1", "sample-tag2");
        List<? extends MetadefTag> all = metadefs.listTags("ns1");
        metadefs.getTag("ns1", "sample-tag2");
        MetadefTag renamed = metadefs.updateTag("ns1", "sample-tag2", "renamed");
        metadefs.deleteTag("ns1", "renamed");
        metadefs.deleteAllTags("ns1");

        RecordedRequest create = expect("POST", P + "/tags/sample-tag2");
        Assert.assertEquals(create.getBodySize(), 0);
        expect("GET", P + "/tags");
        expect("GET", P + "/tags/sample-tag2");
        Assert.assertEquals(body(expect("PUT", P + "/tags/sample-tag2")).get("name").asText(), "renamed");
        expect("DELETE", P + "/tags/renamed");
        expect("DELETE", P + "/tags");
        Assert.assertEquals(created.getName(), "sample-tag2");
        Assert.assertEquals(all.size(), 2);
        Assert.assertEquals(renamed.getName(), "renamed");
    }

    public void createTagsSendsAppendHeader() throws Exception {
        respondWith(201, "{\"tags\": [{\"name\": \"a\"}, {\"name\": \"b\"}]}");
        respondWith(201, "{\"tags\": [{\"name\": \"c\"}]}");

        List<? extends MetadefTag> replaced = osv3().imagesV2().metadefs().createTags("ns1", List.of("a", "b"), false);
        osv3().imagesV2().metadefs().createTags("ns1", List.of("c"), true);

        RecordedRequest first = expect("POST", P + "/tags");
        Assert.assertEquals(first.getHeader("X-Openstack-Append"), "false");
        Assert.assertEquals(body(first).get("tags").get(1).get("name").asText(), "b");
        Assert.assertEquals(expect("POST", P + "/tags").getHeader("X-Openstack-Append"), "true");
        Assert.assertEquals(replaced.get(0).getName(), "a");
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -3`
Expected: `MetadefTag`, `createTag` 등 없음.

- [ ] **Step 3: 구현**

`/tmp/g7.json`:
```json
[
 {"iface": "MetadefTag", "cls": "GlanceMetadefTag", "doc": "A metadef tag.", "root": null, "list": "tags", "listCls": "Tags",
  "fields": [["name", "getName", "String"], ["created_at", "getCreatedAt", "String"], ["updated_at", "getUpdatedAt", "String"]]}
]
```
`MetadefServiceImpl` 에 추가:
```java
    private static String tags(String namespace) {
        return ns(namespace) + "/tags";
    }

    @Override public List<? extends MetadefTag> listTags(String namespace) { return listOf(Tags.class, tags(namespace), null); }
    @Override public MetadefTag getTag(String namespace, String name) { return show(GlanceMetadefTag.class, tags(namespace) + "/" + id(name)); }
    @Override public MetadefTag createTag(String namespace, String name) { return post(GlanceMetadefTag.class, tags(namespace) + "/" + id(name)).execute(propagate404()); }

    @Override
    public List<? extends MetadefTag> createTags(String namespace, List<String> names, boolean append) {
        List<Map<String, String>> tags = names.stream().map(n -> Collections.singletonMap("name", n)).collect(Collectors.toList());
        Tags result = post(Tags.class, tags(namespace)).header("X-Openstack-Append", String.valueOf(append))
                .entity(JsonBody.of(Collections.singletonMap("tags", tags))).execute(propagate404());
        return result == null ? Collections.emptyList() : result.getList();
    }

    @Override
    public MetadefTag updateTag(String namespace, String name, String newName) {
        return put(GlanceMetadefTag.class, tags(namespace) + "/" + id(name))
                .entity(JsonBody.of(Collections.singletonMap("name", id(newName)))).execute(propagate404());
    }

    @Override public ActionResponse deleteTag(String namespace, String name) { return remove(tags(namespace) + "/" + id(name)); }
    @Override public ActionResponse deleteAllTags(String namespace) { return remove(tags(namespace)); }
```
`MetadefService` 선언 7개(`createTags` Javadoc: append=false 면 namespace 의 기존 태그를 모두 이 목록으로 바꾼다).

- [ ] **Step 4: 통과 확인 (세 connector — 빈 본문 POST·헤더)**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && for c in httpclient okhttp http-connector; do ./mvnw -B test -pl connectors/$c -Dsurefire.failIfNoSpecifiedTests=false -Dtest='MetadefTagTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:' | tail -1; done`
Expected: 세 connector 모두 `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 5: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/g7.log 2>&1; grep -q 'BUILD SUCCESS' /tmp/g7.log
git add -A && git commit -m "feat(image): add metadef tags

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "imagesV2().metadefs(): tags list/get/create(단건·일괄 X-Openstack-Append)/update/delete/deleteAll."
```

---

### Task 8: 실환경 통합 테스트와 문서

**Files:**
- Create: core-test `ImageExtensionsLiveTests.java`
- Modify: `README.md`(`## Image 확장` 절), `MIGRATION.md`(`# 4.5 → 4.6`), `CHANGELOG.md`(`## 4.6.0`)

- [ ] **Step 1: 실환경 테스트 작성**

`ImageExtensionsLiveTests.java` — `NetworkingExtensionsLiveTests` 와 같은 접속부(`OS_AUTH_URL`, `OS_TOKEN` 또는 password, `env()` helper, `groups = "image-live"`)에 아래 테스트:
```java
    public void versionsAndInfo() {
        ImageVersions versions = os.imagesV2().versions();
        Assert.assertTrue(versions.supports("2.6"), versions.getCurrent());
        Assert.assertTrue(os.imagesV2().info().importMethods().contains("glance-direct"));
        Assert.assertNotNull(os.imagesV2().info().usage());
    }

    public void stageAndImport() throws Exception {
        Image image = os.imagesV2().create(Builders.imageV2().name("os4j-live-import").containerFormat(ContainerFormat.BARE).diskFormat(DiskFormat.RAW).build());
        try {
            Assert.assertTrue(os.imagesV2().stage(image.getId(), Payloads.create(new ByteArrayInputStream(new byte[1024]))).isSuccess());
            Assert.assertTrue(os.imagesV2().importImage(image.getId(), ImageImportOptions.glanceDirect()).isSuccess());
            for (int i = 0; i < 30 && os.imagesV2().get(image.getId()).getStatus() != Image.ImageStatus.ACTIVE; i++)
                Thread.sleep(1000);
            Assert.assertEquals(os.imagesV2().get(image.getId()).getStatus(), Image.ImageStatus.ACTIVE);
            Assert.assertFalse(os.imagesV2().listTasks(image.getId()).isEmpty());
            if (os.imagesV2().versions().supports("2.17"))
                Assert.assertNotNull(os.imagesV2().listLocations(image.getId()));
        } finally {
            Assert.assertTrue(os.imagesV2().delete(image.getId()).isSuccess());
        }
    }

    public void schemas() {
        Assert.assertEquals(os.imagesV2().schemas().image().get("name"), "image");
        Assert.assertEquals(os.imagesV2().schemas().metadefNamespace().get("name"), "namespace");
    }

    public void metadefLifecycle() {
        String ns = "OS4J::Live::" + System.nanoTime();
        var metadefs = os.imagesV2().metadefs();
        metadefs.createNamespace(MetadefNamespaceOptions.create(ns).displayName("os4j live").visibility("private"));
        try {
            metadefs.createObject(ns, MetadefObjectOptions.create("obj").description("o"));
            metadefs.createProperty(ns, MetadefPropertyOptions.create("prop", "Prop", "string"));
            metadefs.createTags(ns, List.of("t1", "t2"), false);
            metadefs.associateResourceType(ns, "OS::Glance::Image", "os4j_", null);
            Assert.assertEquals(metadefs.listObjects(ns).size(), 1);
            Assert.assertTrue(metadefs.listProperties(ns).containsKey("prop"));
            Assert.assertEquals(metadefs.listTags(ns).size(), 2);
            Assert.assertEquals(metadefs.listResourceTypeAssociations(ns).get(0).getName(), "OS::Glance::Image");
        } finally {
            Assert.assertTrue(metadefs.deleteNamespace(ns).isSuccess());
        }
    }

    public void cacheWhenEnabled() {
        try {
            Assert.assertNotNull(os.imagesV2().cache().list());
        } catch (RuntimeException e) {
            throw new SkipException("image cache API not enabled: " + e.getMessage());
        }
    }
```
(`Builders.imageV2()`, `ContainerFormat`, `DiskFormat`, `Image.ImageStatus` 이름은 기존 v2 모델에서 확인한다.)

- [ ] **Step 2: 실환경 실행** — 개발용 토큰이 필요하다. `OS_TOKEN` 이 없으면 이 단계는 실행하지 못한 사실을 ledger 와 최종 보고에 남기고(사용자에게 토큰을 요청), 나머지는 진행한다.

Run(토큰이 있을 때): `OS_TOKEN=… OS_AUTH_URL=http://192.168.140.12:5000/v3 OS_PROJECT_NAME=admin ./mvnw -B test -pl connectors/okhttp -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ImageExtensionsLiveTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:' | tail -1`
Expected: `Failures: 0, Errors: 0`(cache 는 skip 가능), `os4j-live` 이미지·`OS4J::Live` namespace 가 남지 않음.

- [ ] **Step 3: 문서** — README `## Image 확장` 절(예시: versions().supports, stage+importImage, metadefs, schemas; 404 규칙), MIGRATION `# 4.5 → 4.6`(`ImageService` 추상 메서드 추가, 404 규칙), CHANGELOG `## 4.6.0`.

- [ ] **Step 4: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/g8.log 2>&1; grep -q 'BUILD SUCCESS' /tmp/g8.log
git add -A && git commit -m "test(image): add live Glance extension tests; docs for image extensions

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "ImageExtensionsLiveTests, README image 확장 절, MIGRATION 4.5→4.6, CHANGELOG 4.6.0."
```

---

### Task 9: 전체 리뷰, 4.6.0 릴리스, 4.7.0-SNAPSHOT

- [ ] **Step 1: 최종 리뷰** — `review-package PLAN_FILE <G′ 시작 커밋> HEAD`, opus 리뷰어(code-reviewer 템플릿, 계획서·스펙, Review Focus, ledger `Ruling:`). Critical/Important 는 RED→GREEN 으로 한 번에 고치고 PR. Minor 는 ledger.
- [ ] **Step 2: main CI 확인 후 `v4.6.0` 태그, release workflow 확인.**
- [ ] **Step 3: 4.7.0-SNAPSHOT bump PR**(F Task 13 Step 3 과 같은 명령, 버전만 4.6.0→4.7.0).
- [ ] **Step 4: Central 확인** — Central 에 4.6.0 pom 이 200 이 되면 `~/openstack4j-check` 를 4.6.0 으로 바꾸고 `imagesV2().versions().getCurrent()`, `info().importMethods()` 출력 추가. 토큰이 없으면 의존성 해석만 확인(`./mvnw -q -f pom.xml dependency:build-classpath`)하고 실행은 보고에 남긴다.
- [ ] **Step 5: 메모리 갱신.**
