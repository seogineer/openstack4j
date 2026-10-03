# Neutron in-tree 확장 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Neutron 본체(in-tree)의 빠진 API 147 개를 `os.networking()` 에 추가하고 4.5.0 으로 릴리스한다.

**Architecture:** 새 자원은 `NetworkingService` 의 새 accessor 와 `openstack.networking.internal.ext` 의 구현으로, 기존 자원의 하위 경로는 기존 서비스에 메서드를 더해 붙인다. 반복되는 CRUD 는 공용 기반 클래스 `BaseNeutronExtService`(list/show/create/update/remove helper)와 공용 옵션 기반 `NeutronAttributes`(null 을 넣지 않는 fluent Map)로 줄인다. 모델은 `model.network.ext` 인터페이스 + `openstack.networking.domain.ext.Neutron*` 구현.

**Tech Stack:** Java 17, Maven, Jackson(전역 mapper: NON_NULL, `@JsonRootName` 클래스는 UNWRAP_ROOT_VALUE), TestNG + OkHttp MockWebServer(core-test), connector 3종.

**Spec:** `docs/superpowers/specs/2026-10-04-networking-extensions-design.md`

## Global Constraints

- 기존 코드는 바꾸지 않고 추가만 한다(`@Deprecated` 없음). 기존 요청 본문은 그대로다.
- Neutron 은 microversion 이 없다. 기능 유무는 `GET /v2.0/extensions`.
- 생성·수정 본문은 옵션 클래스의 `toMap()` + `JsonBody`; **null 값은 넣지 않는다**(명시적으로 지우려면 `attribute(key, null)`).
- 응답 래핑: 단건 `@JsonRootName`, 목록 `ListResult`. 루트에 다른 키가 같이 오는 응답은 wrapper 클래스.
- 세 connector(httpclient, okhttp, http-connector)에서 같은 결과. 경로 비교는 URL-decoded 로.
- 각 테스트는 모든 요청을 `takeRequest()` 한다.
- 회사 코드(openstackit-java)는 복사하지 않는다.
- PR 은 `.superpowers/pr.sh "<body>"`(git-ignored; push → PR → CI → squash-merge → main pull). 커밋 끝에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` 와 `Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt`.
- 전체 빌드 확인 후에만 커밋한다: `./mvnw -B --no-transfer-progress install > /tmp/fN.log 2>&1; grep -q 'BUILD SUCCESS' /tmp/fN.log`.

## Review Focus

1. **기존 요청 본문 불변** — router 생성·수정, port, floating IP, quota, port forwarding 생성 본문이 바뀌면 안 된다(새 `Router.getExternalGateways()` 는 응답 전용). → Task 6 `existingRouterCreateBodyUnchanged`.
2. **옵션의 null** — 설정하지 않은 필드는 본문에서 빠져야 한다(Neutron 은 알 수 없는/잘못된 null 에 400). → Task 1 `NeutronAttributesTests.unsetFieldsAreOmitted`.
3. **목록 필터** — `list(Map)` 의 필터가 query 로 가고, 같은 키를 여러 번 넘기는 tags 류는 다루지 않는다(Map 이라 한 값). → Task 3 `subnetPoolFiltersBecomeQuery`(세 connector).
4. **루트 래핑이 없는 응답** — `PUT /subnetpools/{id}/add_prefixes` 는 `{"prefixes": [...]}`, `PUT /subnetpools/{id}/onboard_network_subnets` 는 목록, `GET /qos/rule-types/{type}` 은 `rule_type` 아래 `drivers`. → Task 3 `prefixOperationsReturnPrefixes`, Task 2 `ruleTypeDetailsReadDrivers`.
5. **오류 응답** — Neutron 오류 본문 `{"NeutronError": {...}}`(예: auto-allocated topology 가 기본 외부망 없음)를 예외로 받아야 하고 빈 결과로 삼키면 안 된다. → Task 1 `autoAllocatedTopologyErrorIsRaised`.

---

### Task 1: 공용 기반, extensions·service providers·auto-allocated topology, quota default/details, floating IP pools, port forwarding update

**Files:**
- Create (`core/src/main/java/org/openstack4j/`):
  - `model/network/options/NeutronAttributes.java` — 옵션 공용 기반
  - `openstack/networking/internal/ext/BaseNeutronExtService.java` — 공용 helper
  - 모델: `model/network/ext/NeutronExtension.java`, `ServiceProvider.java`, `AutoAllocatedTopology.java`, `FloatingIPPool.java`, `QuotaDetail.java`
  - 도메인: `openstack/networking/domain/ext/NeutronExtensionEntity.java`(이름 충돌 회피), `NeutronServiceProvider.java`, `NeutronAutoAllocatedTopology.java`, `NeutronFloatingIPPool.java`, `NeutronQuotaDetails.java`
  - API/구현: `api/networking/ext/NeutronExtensionService.java` + `openstack/networking/internal/ext/NeutronExtensionServiceImpl.java`, `ServiceProviderService`/`Impl`, `AutoAllocatedTopologyService`/`Impl`
- Modify: `api/networking/NetworkingService.java`, `openstack/networking/internal/NetworkingServiceImpl.java`, `openstack/provider/DefaultAPIProvider.java`, `api/networking/ext/NetQuotaService.java` + impl, `api/networking/NetFloatingIPService.java` + impl, `api/networking/ext/PortForwardingService.java` + impl
- Create (core-test `src/main/java/org/openstack4j/api/network/ext2/`): `AbstractNetworkingExtTest.java`, `NeutronAttributesTests.java`, `NetworkingBasicsTests.java`

**Interfaces:**
- Produces:
  - `NeutronAttributes<S extends NeutronAttributes<S>>`: `protected S put(String key, Object value)`(null 이면 무시), `public S attribute(String key, Object value)`(null 도 그대로 — 명시적 지우기), `public Map<String, Object> toMap()`(복사본), `protected abstract S self()`
  - `BaseNeutronExtService`(extends `BaseNetworkingServices`): `protected <E> List<E> listOf(Class<? extends ListResult<E>> type, String path, Map<String, String> filters)`(filters null 허용), `protected <E> E show(Class<E> type, String path)`, `protected <E> E create(Class<E> type, String path, String root, NeutronAttributes<?> attributes)`, `protected <E> E update(Class<E> type, String path, String root, NeutronAttributes<?> attributes)`, `protected ActionResponse remove(String path)`, `protected static String id(String value)`(requireNonNull + 경로 segment 그대로)
  - `NetworkingService`: `NeutronExtensionService extensions()`, `ServiceProviderService serviceProviders()`, `AutoAllocatedTopologyService autoAllocatedTopology()`
  - `NeutronExtensionService`: `List<? extends NeutronExtension> list()`, `NeutronExtension get(String alias)`, `boolean isEnabled(String alias)`(목록 한 번 조회)
  - `NeutronExtension`: `getAlias()`, `getName()`, `getDescription()`, `String getUpdated()`(links 는 비어 있어 모델에 두지 않는다)
  - `ServiceProviderService.list()` → `List<? extends ServiceProvider>`; `ServiceProvider`: `getServiceType()`, `getName()`, `Boolean isDefault()`
  - `AutoAllocatedTopologyService`: `AutoAllocatedTopology get(String projectId)`, `AutoAllocatedTopology validate(String projectId)`(`?fields=dry-run`), `ActionResponse delete(String projectId)`; `AutoAllocatedTopology`: `getId()`, `getProjectId()`, `String getDryRun()`
  - `NetQuotaService`: `NetQuota getDefault(String projectId)`, `Map<String, ? extends QuotaDetail> getDetails(String projectId)`; `QuotaDetail`: `Integer getLimit()`, `Integer getUsed()`, `Integer getReserved()`
  - `NetFloatingIPService.listPools()` → `List<? extends FloatingIPPool>`; `FloatingIPPool`: `getSubnetId()`, `getSubnetName()`, `getNetworkId()`, `getProjectId()`
  - `PortForwardingService.update(String floatingIpId, String id, PortForwardingUpdate update)` → `PortForwarding`; `PortForwardingUpdate extends NeutronAttributes<PortForwardingUpdate>`: `create()`, `internalPortId`, `internalIpAddress`, `internalPort(int)`, `externalPort(int)`, `internalPortRange(String)`, `externalPortRange(String)`, `protocol`, `description`

- [ ] **Step 1: 테스트 기반과 실패하는 테스트 작성 (Review Focus 2, 5)**

```bash
git switch main && git pull && git switch -c task/f1-basics
mkdir -p core-test/src/main/java/org/openstack4j/api/network/ext2
```

`AbstractNetworkingExtTest.java`:
```java
package org.openstack4j.api.network.ext2;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;

/** Base for the Neutron extension tests: Neutron on the mock server's network port, v3 token. */
public abstract class AbstractNetworkingExtTest extends AbstractTest {

    protected static final String PROJECT = "2580a7b51d564c1d848ee27fda2db713";

    @Override
    protected Service service() {
        return Service.NETWORK;
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

    /** Asserts method and path suffix of the next request and returns it. */
    protected RecordedRequest expect(String method, String pathSuffix) throws InterruptedException {
        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), method, decodedPath(request));
        Assert.assertTrue(decodedPath(request).endsWith(pathSuffix), decodedPath(request) + " does not end with " + pathSuffix);
        return request;
    }
}
```

`NeutronAttributesTests.java`:
```java
package org.openstack4j.api.network.ext2;

import java.util.Map;

import org.openstack4j.model.network.options.PortForwardingUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/Attributes")
public class NeutronAttributesTests {

    public void unsetFieldsAreOmitted() {
        Map<String, Object> map = PortForwardingUpdate.create().internalPort(8080).description(null).toMap();
        Assert.assertEquals(map, Map.of("internal_port", 8080));
    }

    public void explicitAttributeKeepsNull() {
        Map<String, Object> map = PortForwardingUpdate.create().attribute("description", null).toMap();
        Assert.assertTrue(map.containsKey("description"));
        Assert.assertNull(map.get("description"));
    }

    public void toMapIsACopy() {
        PortForwardingUpdate update = PortForwardingUpdate.create().protocol("tcp");
        update.toMap().put("protocol", "udp");
        Assert.assertEquals(update.toMap().get("protocol"), "tcp");
    }
}
```

`NetworkingBasicsTests.java`:
```java
package org.openstack4j.api.network.ext2;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.AutoAllocatedTopology;
import org.openstack4j.model.network.ext.FloatingIPPool;
import org.openstack4j.model.network.ext.NeutronExtension;
import org.openstack4j.model.network.ext.QuotaDetail;
import org.openstack4j.model.network.ext.ServiceProvider;
import org.openstack4j.model.network.options.PortForwardingUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/Basics")
public class NetworkingBasicsTests extends AbstractNetworkingExtTest {

    private static final String EXTENSIONS = "{\"extensions\": [{\"name\": \"Address group\", \"alias\": \"address-group\", \"description\": \"Support address group\", \"updated\": \"2020-11-16T10:00:00-00:00\", \"links\": []},"
            + " {\"name\": \"Quality of Service\", \"alias\": \"qos\", \"description\": \"The Quality of Service extension.\", \"updated\": \"2015-06-08T10:00:00-00:00\", \"links\": []}]}";

    public void extensions() throws Exception {
        respondWith(200, EXTENSIONS);
        respondWith(200, "{\"extension\": {\"name\": \"Quality of Service\", \"alias\": \"qos\", \"description\": \"The Quality of Service extension.\", \"updated\": \"2015-06-08T10:00:00-00:00\", \"links\": []}}");
        respondWith(200, EXTENSIONS);
        respondWith(200, EXTENSIONS);

        List<? extends NeutronExtension> all = osv3().networking().extensions().list();
        NeutronExtension qos = osv3().networking().extensions().get("qos");
        boolean hasGroups = osv3().networking().extensions().isEnabled("address-group");
        boolean hasLogging = osv3().networking().extensions().isEnabled("logging");

        expect("GET", "/v2.0/extensions");
        expect("GET", "/v2.0/extensions/qos");
        expect("GET", "/v2.0/extensions");
        expect("GET", "/v2.0/extensions");
        Assert.assertEquals(all.size(), 2);
        Assert.assertEquals(all.get(0).getAlias(), "address-group");
        Assert.assertEquals(qos.getName(), "Quality of Service");
        Assert.assertEquals(qos.getUpdated(), "2015-06-08T10:00:00-00:00");
        Assert.assertTrue(hasGroups);
        Assert.assertFalse(hasLogging);
    }

    public void serviceProvidersAndFloatingIpPools() throws Exception {
        respondWith(200, "{\"service_providers\": [{\"service_type\": \"L3_ROUTER_NAT\", \"name\": \"ovn\", \"default\": true}]}");
        respondWith(200, "{\"floatingip_pools\":[{\"subnet_id\":\"3260d5f7-ec44-44fa-821e-e9df107ce5a3\",\"subnet_name\":\"external-subnet\","
                + "\"tenant_id\":\"" + PROJECT + "\",\"network_id\":\"8e5c1b8c-0d0b-4d6b-9a52-6f8b2d6f2e3a\",\"project_id\":\"" + PROJECT + "\"}]}");

        List<? extends ServiceProvider> providers = osv3().networking().serviceProviders().list();
        List<? extends FloatingIPPool> pools = osv3().networking().floatingip().listPools();

        expect("GET", "/v2.0/service-providers");
        expect("GET", "/v2.0/floatingip_pools");
        Assert.assertEquals(providers.get(0).getServiceType(), "L3_ROUTER_NAT");
        Assert.assertTrue(providers.get(0).isDefault());
        Assert.assertEquals(pools.get(0).getSubnetName(), "external-subnet");
        Assert.assertEquals(pools.get(0).getProjectId(), PROJECT);
    }

    public void autoAllocatedTopology() throws Exception {
        respondWith(200, "{\"auto_allocated_topology\": {\"id\": \"31483d41-5c2b-481c-beef-ab501bd2e0da\", \"tenant_id\": \"" + PROJECT + "\", \"project_id\": \"" + PROJECT + "\"}}");
        respondWith(200, "{\"auto_allocated_topology\": {\"dry-run\": \"pass\"}}");
        respondWith(204);

        AutoAllocatedTopology topology = osv3().networking().autoAllocatedTopology().get(PROJECT);
        AutoAllocatedTopology check = osv3().networking().autoAllocatedTopology().validate(PROJECT);
        boolean deleted = osv3().networking().autoAllocatedTopology().delete(PROJECT).isSuccess();

        expect("GET", "/v2.0/auto-allocated-topology/" + PROJECT);
        expect("GET", "/v2.0/auto-allocated-topology/" + PROJECT + "?fields=dry-run");
        expect("DELETE", "/v2.0/auto-allocated-topology/" + PROJECT);
        Assert.assertEquals(topology.getId(), "31483d41-5c2b-481c-beef-ab501bd2e0da");
        Assert.assertEquals(topology.getProjectId(), PROJECT);
        Assert.assertEquals(check.getDryRun(), "pass");
        Assert.assertTrue(deleted);
    }

    public void autoAllocatedTopologyErrorIsRaised() throws Exception {
        respondWith(400, "{\"NeutronError\": {\"type\": \"AutoAllocationFailure\", \"message\": \"Deployment error: No default router:external network.\", \"detail\": \"\"}}");
        try {
            osv3().networking().autoAllocatedTopology().validate(PROJECT);
            Assert.fail("expected the Neutron error to surface");
        } catch (RuntimeException expected) {
            Assert.assertTrue(String.valueOf(expected.getMessage()).contains("No default router:external network"), expected.getMessage());
        }
        takeRequest();
    }

    public void quotaDefaultAndDetails() throws Exception {
        respondWith(200, "{\"quota\": {\"network\": 100, \"subnet\": 100, \"subnetpool\": -1, \"port\": 500, \"router\": 10, \"floatingip\": 50, \"rbac_policy\": 10, \"security_group\": 10, \"security_group_rule\": 100}}");
        respondWith(200, "{\"quota\": {\"network\": {\"limit\": 100, \"used\": 3, \"reserved\": 0}, \"subnetpool\": {\"limit\": -1, \"used\": 0, \"reserved\": 0}}}");

        org.openstack4j.model.network.NetQuota defaults = osv3().networking().quotas().getDefault(PROJECT);
        Map<String, ? extends QuotaDetail> details = osv3().networking().quotas().getDetails(PROJECT);

        expect("GET", "/v2.0/quotas/" + PROJECT + "/default");
        expect("GET", "/v2.0/quotas/" + PROJECT + "/details.json");
        Assert.assertEquals(defaults.getNetwork(), 100);
        Assert.assertEquals(details.get("network").getUsed(), Integer.valueOf(3));
        Assert.assertEquals(details.get("subnetpool").getLimit(), Integer.valueOf(-1));
    }

    public void portForwardingUpdate() throws Exception {
        respondWith(200, "{\"port_forwarding\": {\"id\": \"pf1\", \"protocol\": \"tcp\", \"internal_ip_address\": \"10.0.0.11\", \"internal_port\": 25,"
                + " \"internal_port_id\": \"1238be08-a2a8-4b8d-addf-fb5e2250e480\", \"external_port\": 2230, \"description\": \"changed\"}}");

        osv3().networking().floatingip().portForwarding().update("fip1", "pf1", PortForwardingUpdate.create().internalPort(25).description("changed"));

        RecordedRequest request = expect("PUT", "/v2.0/floatingips/fip1/port_forwardings/pf1");
        Assert.assertEquals(body(request).get("port_forwarding").get("internal_port").asInt(), 25);
        Assert.assertEquals(body(request).get("port_forwarding").get("description").asText(), "changed");
        Assert.assertEquals(body(request).get("port_forwarding").size(), 2);
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -5`
Expected: `PortForwardingUpdate`, `NeutronExtension`, `extensions()` 등 없음.

- [ ] **Step 3: 공용 기반 구현**

`model/network/options/NeutronAttributes.java`:
```java
package org.openstack4j.model.network.options;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base of the Neutron create/update options: a fluent map that leaves unset fields out of the request body.
 *
 * @param <S> the concrete options type
 */
public abstract class NeutronAttributes<S extends NeutronAttributes<S>> {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    protected abstract S self();

    /** Sets a field; a null value leaves the field out of the body. */
    protected S put(String key, Object value) {
        if (value != null)
            fields.put(key, value);
        return self();
    }

    /**
     * Sets any field by its API name, including fields this class has no setter for. Unlike the typed setters,
     * a null value is sent as JSON null, which clears the field on update.
     */
    public S attribute(String key, Object value) {
        fields.put(key, value);
        return self();
    }

    /** @return a copy of the fields to send */
    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(fields);
    }
}
```

`openstack/networking/internal/ext/BaseNeutronExtService.java`:
```java
package org.openstack4j.openstack.networking.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.options.NeutronAttributes;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.networking.internal.BaseNetworkingServices;

/** Shared list/show/create/update/delete helpers for the Neutron extension services. */
public abstract class BaseNeutronExtService extends BaseNetworkingServices {

    protected static String id(String value) {
        return Objects.requireNonNull(value, "id");
    }

    protected <E> List<E> listOf(Class<? extends ListResult<E>> type, String path, Map<String, String> filters) {
        ListResult<E> result = get(type, path).params(filters == null ? Collections.emptyMap() : filters).execute();
        return result == null ? Collections.emptyList() : result.getList();
    }

    protected <E> E show(Class<E> type, String path) {
        return get(type, path).execute();
    }

    protected <E> E create(Class<E> type, String path, String root, NeutronAttributes<?> attributes) {
        return post(type, path).entity(JsonBody.of(root, Objects.requireNonNull(attributes, "attributes").toMap())).execute();
    }

    protected <E> E update(Class<E> type, String path, String root, NeutronAttributes<?> attributes) {
        return put(type, path).entity(JsonBody.of(root, Objects.requireNonNull(attributes, "attributes").toMap())).execute();
    }

    protected ActionResponse remove(String path) {
        return deleteWithResponse(path).execute();
    }
}
```
(`ListResult.getList()` 은 `value()` 가 null 이면 빈 리스트를 준다 — 기존 동작 확인.)

`model/network/options/PortForwardingUpdate.java`:
```java
package org.openstack4j.model.network.options;

/** Body of {@code PUT /floatingips/{id}/port_forwardings/{id}}; only the fields set are sent. */
public class PortForwardingUpdate extends NeutronAttributes<PortForwardingUpdate> {

    public static PortForwardingUpdate create() {
        return new PortForwardingUpdate();
    }

    @Override
    protected PortForwardingUpdate self() {
        return this;
    }

    public PortForwardingUpdate internalPortId(String id) { return put("internal_port_id", id); }
    public PortForwardingUpdate internalIpAddress(String ip) { return put("internal_ip_address", ip); }
    public PortForwardingUpdate internalPort(Integer port) { return put("internal_port", port); }
    public PortForwardingUpdate externalPort(Integer port) { return put("external_port", port); }
    /** For example {@code "100:200"} (floating-ip-port-forwarding-port-ranges). */
    public PortForwardingUpdate internalPortRange(String range) { return put("internal_port_range", range); }
    public PortForwardingUpdate externalPortRange(String range) { return put("external_port_range", range); }
    public PortForwardingUpdate protocol(String protocol) { return put("protocol", protocol); }
    public PortForwardingUpdate description(String description) { return put("description", description); }
}
```

- [ ] **Step 4: 모델과 서비스 구현**

모델 인터페이스(`extends ModelEntity`)와 도메인 클래스(`@JsonIgnoreProperties(ignoreUnknown = true)`, 필드 `@JsonProperty`, 목록 `ListResult` 중첩 클래스)는 아래 표대로 쓴다. 생성은 `.superpowers/gen_neutron.py`(Step 4a)로 하고 결과를 읽어 확인한다.

| 인터페이스 | 도메인 클래스 | 루트 / 목록(클래스) | 필드: JSON → getter(type) |
|---|---|---|---|
| `NeutronExtension` | `NeutronExtensionEntity` | `extension` / `extensions`(`Extensions`) | alias→getAlias, name→getName, description→getDescription, updated→getUpdated (String) |
| `ServiceProvider` | `NeutronServiceProvider` | 없음 / `service_providers`(`ServiceProviders`) | service_type→getServiceType, name→getName, default→isDefault(Boolean) |
| `AutoAllocatedTopology` | `NeutronAutoAllocatedTopology` | `auto_allocated_topology` / 없음 | id→getId, project_id→getProjectId, dry-run→getDryRun |
| `FloatingIPPool` | `NeutronFloatingIPPool` | 없음 / `floatingip_pools`(`FloatingIPPools`) | subnet_id→getSubnetId, subnet_name→getSubnetName, network_id→getNetworkId, project_id→getProjectId |
| `QuotaDetail` | `NeutronQuotaDetails.Detail`(정적 중첩) | — | limit→getLimit, used→getUsed, reserved→getReserved (Integer) |

`NeutronQuotaDetails`(`@JsonRootName("quota")` 없이 wrapper): `@JsonProperty("quota") Map<String, Detail> quota;` + `getQuota()`.

- [ ] **Step 4a: 모델 생성기 (dev 도구, git-ignored)**

`.superpowers/gen_neutron.py`:
```python
#!/usr/bin/env python3
"""gen_neutron.py SPEC.json — writes Neutron model interfaces and domain classes from a compact spec.

SPEC: [{"iface": "AddressScope", "cls": "NeutronAddressScope", "doc": "...", "root": "address_scope" | null,
        "list": "address_scopes" | null, "listCls": "AddressScopes",
        "fields": [["id", "getId", "String"], ["ip_version", "getIpVersion", "Integer"], ...]}]
Types may be any Java type; java.util.List/Map/Date imports are added when used. Existing files are left alone.
"""
import json, os, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'core', 'src', 'main', 'java', 'org', 'openstack4j')

def camel(json_name):
    parts = json_name.replace('-', '_').split('_')
    return parts[0] + ''.join(p[:1].upper() + p[1:] for p in parts[1:])

def imports(types):
    out = []
    joined = ' '.join(types)
    for name in ('List', 'Map', 'Date'):
        if name + '<' in joined or name == 'Date' and 'Date' in joined.split():
            out.append('java.util.' + name)
    return sorted(set(out))

def write(path, text):
    if os.path.exists(path):
        print('skip', os.path.relpath(path, ROOT)); return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w').write(text); print('wrote', os.path.relpath(path, ROOT))

for m in json.load(open(sys.argv[1])):
    types = [f[2] for f in m['fields']]
    imp = ''.join('import %s;\n' % i for i in imports(types))
    getters = ''.join('    %s %s();\n' % (t, g) for _, g, t in m['fields'])
    write(os.path.join(ROOT, 'model', 'network', 'ext', m['iface'] + '.java'),
          'package org.openstack4j.model.network.ext;\n\n%s%simport org.openstack4j.model.ModelEntity;\n\n/** %s */\npublic interface %s extends ModelEntity {\n%s}\n'
          % (imp, '\n' if imp else '', m['doc'], m['iface'], getters))
    fields = ''.join('    @JsonProperty("%s") private %s %s;\n' % (j, t, camel(j)) for j, _, t in m['fields'])
    methods = ''.join('    @Override public %s %s() { return %s; }\n' % (t, g, camel(j)) for j, g, t in m['fields'])
    dimp = set(imports(types))
    lst = ''
    if m.get('list'):
        dimp.add('java.util.List')
        lst = ('\n    public static class %s extends ListResult<%s> {\n        private static final long serialVersionUID = 1L;\n'
               '        @JsonProperty("%s")\n        private List<%s> list;\n\n        @Override\n        protected List<%s> value() {\n            return list;\n        }\n    }\n'
               % (m['listCls'], m['cls'], m['list'], m['cls'], m['cls']))
    jimp = ['com.fasterxml.jackson.annotation.JsonIgnoreProperties', 'com.fasterxml.jackson.annotation.JsonProperty']
    if m.get('root'):
        jimp.append('com.fasterxml.jackson.annotation.JsonRootName')
    oimp = ['org.openstack4j.model.network.ext.' + m['iface']] + (['org.openstack4j.openstack.common.ListResult'] if m.get('list') else [])
    head = 'package org.openstack4j.openstack.networking.domain.ext;\n\n'
    head += ''.join('import %s;\n' % i for i in sorted(dimp)) + ('\n' if dimp else '')
    head += ''.join('import %s;\n' % i for i in jimp + oimp)
    root = '@JsonRootName("%s")\n' % m['root'] if m.get('root') else ''
    write(os.path.join(ROOT, 'openstack', 'networking', 'domain', 'ext', m['cls'] + '.java'),
          '%s\n%s@JsonIgnoreProperties(ignoreUnknown = true)\npublic class %s implements %s {\n\n    private static final long serialVersionUID = 1L;\n\n%s\n%s%s}\n'
          % (head, root, m['cls'], m['iface'], fields, methods, lst))
```
이 작업의 spec(`/tmp/f1.json`)은 위 표를 그대로 옮긴다(`dry-run` 의 필드 이름은 생성기가 `dryRun` 으로 바꾼다). 실행: `python3 .superpowers/gen_neutron.py /tmp/f1.json`.

`NeutronExtensionServiceImpl`:
```java
package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;

import org.openstack4j.api.networking.ext.NeutronExtensionService;
import org.openstack4j.model.network.ext.NeutronExtension;
import org.openstack4j.openstack.networking.domain.ext.NeutronExtensionEntity;
import org.openstack4j.openstack.networking.domain.ext.NeutronExtensionEntity.Extensions;

public class NeutronExtensionServiceImpl extends BaseNeutronExtService implements NeutronExtensionService {

    @Override
    public List<? extends NeutronExtension> list() {
        return listOf(Extensions.class, "/extensions", null);
    }

    @Override
    public NeutronExtension get(String alias) {
        return show(NeutronExtensionEntity.class, "/extensions/" + id(alias));
    }

    @Override
    public boolean isEnabled(String alias) {
        return list().stream().anyMatch(e -> alias.equals(e.getAlias()));
    }
}
```
`ServiceProviderServiceImpl.list()` = `listOf(ServiceProviders.class, "/service-providers", null)`.
`AutoAllocatedTopologyServiceImpl`:
```java
    @Override public AutoAllocatedTopology get(String projectId) { return show(NeutronAutoAllocatedTopology.class, "/auto-allocated-topology/" + id(projectId)); }
    @Override public AutoAllocatedTopology validate(String projectId) { return get(NeutronAutoAllocatedTopology.class, "/auto-allocated-topology/" + id(projectId)).param("fields", "dry-run").execute(); }
    @Override public ActionResponse delete(String projectId) { return remove("/auto-allocated-topology/" + id(projectId)); }
```
(`validate` 는 Neutron 이 실패를 4xx 오류 본문으로 주므로 기존 오류 처리로 예외가 난다 — `autoAllocatedTopologyErrorIsRaised`. 메시지에 Neutron 의 `message` 가 들어가는지 확인; 기존 `ResponseException` 매핑이 `NeutronError` 를 읽지 못하면 그 테스트가 실패하므로 그때 원인을 찾아 Ruling 으로 남긴다.)

`NetQuotaServiceImpl`:
```java
    @Override
    public NetQuota getDefault(String projectId) {
        return get(NeutronNetQuota.class, uri("/quotas/%s/default", Objects.requireNonNull(projectId))).execute();
    }

    @Override
    public Map<String, ? extends QuotaDetail> getDetails(String projectId) {
        NeutronQuotaDetails details = get(NeutronQuotaDetails.class, uri("/quotas/%s/details.json", Objects.requireNonNull(projectId))).execute();
        return details == null || details.getQuota() == null ? Collections.emptyMap() : details.getQuota();
    }
```
(`NeutronNetQuota` 의 실제 클래스 이름은 기존 `get(String)` 구현에서 확인한다.)
`FloatingIPServiceImpl.listPools()` = `get(FloatingIPPools.class, uri("/floatingip_pools")).execute().getList()`.
`PortForwardingServiceImpl.update(...)` = `put(FloatingIPPortForwarding.class, uri("/floatingips/%s/port_forwardings/%s", fip, id)).entity(JsonBody.of("port_forwarding", update.toMap())).execute()`(기존 create 가 쓰는 도메인 클래스 이름을 따른다).

인터페이스 3개(`NeutronExtensionService`, `ServiceProviderService`, `AutoAllocatedTopologyService`, 모두 `extends RestService`, Javadoc 에 경로), `NetworkingService` accessor 3개 + `NetworkingServiceImpl`(`Apis.get(...)`), `DefaultAPIProvider` binding 3개.

- [ ] **Step 5: 통과 확인 (세 connector)**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && for c in httpclient okhttp http-connector; do ./mvnw -B test -pl connectors/$c -Dsurefire.failIfNoSpecifiedTests=false -Dtest='NeutronAttributesTests,NetworkingBasicsTests,NetQuotaTest,NetFloatingIPServiceTests,PortForwardingTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:' | tail -1; done`
Expected: 세 connector 모두 `Failures: 0, Errors: 0`.

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/f1.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/f1.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/f1.log
git add -A && git commit -m "feat(networking): add extension discovery, service providers, auto-allocated topology, quota details, floating IP pools

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "networking 공용 기반(BaseNeutronExtService, NeutronAttributes), extensions()/serviceProviders()/autoAllocatedTopology(), quotas default·details, floatingip pools, port forwarding update."
```

---

### Task 2: QoS 규칙 — rule types, dscp marking, minimum bandwidth, minimum packet rate, packet rate limit, alias 규칙

**Files:**
- Create: 모델 `model/network/ext/QosRuleType.java`, `QosDscpMarkingRule.java`, `QosMinimumBandwidthRule.java`, `QosMinimumPacketRateRule.java`, `QosPacketRateLimitRule.java`; 도메인 `openstack/networking/domain/ext/NeutronQosRuleType.java`, `NeutronQosDscpMarkingRule.java`, `NeutronQosMinimumBandwidthRule.java`, `NeutronQosMinimumPacketRateRule.java`, `NeutronQosPacketRateLimitRule.java`; 옵션 `model/network/options/QosRuleOptions.java`; `api/networking/ext/QosRuleService.java`, `openstack/networking/internal/ext/QosRuleServiceImpl.java`
- Modify: `NetworkingService`, `NetworkingServiceImpl`, `DefaultAPIProvider`
- Create: core-test `api/network/ext2/QosRuleTests.java`

**Interfaces:**
- Consumes: Task 1 `BaseNeutronExtService`, `NeutronAttributes`, `AbstractNetworkingExtTest.expect`.
- Produces:
  - `NetworkingService.qosRules()` → `QosRuleService`
  - `QosRuleService`: `List<? extends QosRuleType> ruleTypes()`, `QosRuleType ruleType(String type)`; 종류 K ∈ {`DscpMarking`→`QosDscpMarkingRule`, `MinimumBandwidth`→`QosMinimumBandwidthRule`, `MinimumPacketRate`→`QosMinimumPacketRateRule`, `PacketRateLimit`→`QosPacketRateLimitRule`} 마다 `List<? extends R> listKRules(String policyId)`, `R getKRule(String policyId, String ruleId)`, `R createKRule(String policyId, QosRuleOptions options)`, `R updateKRule(String policyId, String ruleId, QosRuleOptions options)`, `ActionResponse deleteKRule(String policyId, String ruleId)`; alias 종류 A ∈ {`BandwidthLimit`→기존 `NetQosPolicyBandwidthLimitRule`, `DscpMarking`, `MinimumBandwidth`, `MinimumPacketRate`} 마다 `R getAliasARule(String ruleId)`, `R updateAliasARule(String ruleId, QosRuleOptions options)`, `ActionResponse deleteAliasARule(String ruleId)`
  - `QosRuleType`: `getType()`, `List<Map<String, Object>> getDrivers()`
  - `QosDscpMarkingRule`: `getId()`, `Integer getDscpMark()`, `getQosPolicyId()`; `QosMinimumBandwidthRule`: `getId()`, `Long getMinKbps()`, `getDirection()`, `getQosPolicyId()`; `QosMinimumPacketRateRule`: `getId()`, `Long getMinKpps()`, `getDirection()`, `getQosPolicyId()`; `QosPacketRateLimitRule`: `getId()`, `Long getMaxKpps()`, `Long getMaxBurstKpps()`, `getDirection()`, `getQosPolicyId()`
  - `QosRuleOptions extends NeutronAttributes<QosRuleOptions>`: `dscpMarking(int dscpMark)`, `minimumBandwidth(long minKbps)`, `minimumPacketRate(long minKpps)`, `packetRateLimit(long maxKpps)`, `update()`; setters `dscpMark(Integer)`, `minKbps(Long)`, `minKpps(Long)`, `maxKpps(Long)`, `maxBurstKpps(Long)`, `maxKbps(Long)`, `maxBurstKbps(Long)`, `direction(String)`(egress/ingress/any)

- [ ] **Step 1: 실패하는 테스트 작성 (Review Focus 4)**

```bash
git switch main && git pull && git switch -c task/f2-qos-rules
```

`QosRuleTests.java`:
```java
package org.openstack4j.api.network.ext2;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.QosDscpMarkingRule;
import org.openstack4j.model.network.ext.QosMinimumBandwidthRule;
import org.openstack4j.model.network.ext.QosMinimumPacketRateRule;
import org.openstack4j.model.network.ext.QosPacketRateLimitRule;
import org.openstack4j.model.network.ext.QosRuleType;
import org.openstack4j.model.network.options.QosRuleOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/QosRules")
public class QosRuleTests extends AbstractNetworkingExtTest {

    private static final String POLICY = "23f44e76-cb35-42fe-99fe-e73de56721f1";
    private static final String P = "/v2.0/qos/policies/" + POLICY;

    public void ruleTypes() throws Exception {
        respondWith(200, "{\"rule_types\": [{\"type\": \"bandwidth_limit\"}, {\"type\": \"minimum_bandwidth\"}, {\"type\": \"dscp_marking\"}]}");
        List<? extends QosRuleType> types = osv3().networking().qosRules().ruleTypes();
        expect("GET", "/v2.0/qos/rule-types");
        Assert.assertEquals(types.size(), 3);
        Assert.assertEquals(types.get(2).getType(), "dscp_marking");
    }

    public void ruleTypeDetailsReadDrivers() throws Exception {
        respondWith(200, "{\"rule_type\": {\"type\": \"dscp_marking\", \"drivers\": [{\"name\": \"OVNQosDriver\", \"supported_parameters\": "
                + "[{\"parameter_name\": \"dscp_mark\", \"parameter_values\": [0, 8, 10], \"parameter_type\": \"choices\"}]}]}}");
        QosRuleType type = osv3().networking().qosRules().ruleType("dscp_marking");
        expect("GET", "/v2.0/qos/rule-types/dscp_marking");
        Assert.assertEquals(type.getType(), "dscp_marking");
        Assert.assertEquals(type.getDrivers().get(0).get("name"), "OVNQosDriver");
    }

    public void dscpMarkingRules() throws Exception {
        String rule = "{\"id\": \"5f126d84-551a-4dcf-bb01-0e9c0df0c794\", \"dscp_mark\": 26}";
        respondWith(201, "{\"dscp_marking_rule\": " + rule + "}");
        respondWith(200, "{\"dscp_marking_rules\": [" + rule + "]}");
        respondWith(200, "{\"dscp_marking_rule\": " + rule + "}");
        respondWith(200, "{\"dscp_marking_rule\": {\"id\": \"5f126d84-551a-4dcf-bb01-0e9c0df0c794\", \"dscp_mark\": 16}}");
        respondWith(204);

        var qos = osv3().networking().qosRules();
        QosDscpMarkingRule created = qos.createDscpMarkingRule(POLICY, QosRuleOptions.dscpMarking(26));
        List<? extends QosDscpMarkingRule> all = qos.listDscpMarkingRules(POLICY);
        qos.getDscpMarkingRule(POLICY, created.getId());
        QosDscpMarkingRule updated = qos.updateDscpMarkingRule(POLICY, created.getId(), QosRuleOptions.update().dscpMark(16));
        boolean deleted = qos.deleteDscpMarkingRule(POLICY, created.getId()).isSuccess();

        RecordedRequest create = expect("POST", P + "/dscp_marking_rules");
        Assert.assertEquals(body(create).get("dscp_marking_rule").get("dscp_mark").asInt(), 26);
        Assert.assertEquals(body(create).get("dscp_marking_rule").size(), 1);
        expect("GET", P + "/dscp_marking_rules");
        expect("GET", P + "/dscp_marking_rules/" + created.getId());
        RecordedRequest update = expect("PUT", P + "/dscp_marking_rules/" + created.getId());
        Assert.assertEquals(body(update).get("dscp_marking_rule").get("dscp_mark").asInt(), 16);
        expect("DELETE", P + "/dscp_marking_rules/" + created.getId());
        Assert.assertEquals(created.getDscpMark(), Integer.valueOf(26));
        Assert.assertEquals(all.size(), 1);
        Assert.assertEquals(updated.getDscpMark(), Integer.valueOf(16));
        Assert.assertTrue(deleted);
    }

    public void minimumBandwidthAndPacketRules() throws Exception {
        respondWith(201, "{\"minimum_bandwidth_rule\": {\"id\": \"mb1\", \"min_kbps\": 10000, \"direction\": \"egress\"}}");
        respondWith(200, "{\"minimum_bandwidth_rules\": [{\"id\": \"mb1\", \"min_kbps\": 10000, \"direction\": \"egress\"}]}");
        respondWith(201, "{\"minimum_packet_rate_rule\": {\"id\": \"mp1\", \"min_kpps\": 1000, \"direction\": \"any\"}}");
        respondWith(200, "{\"minimum_packet_rate_rule\": {\"id\": \"mp1\", \"min_kpps\": 2000, \"direction\": \"any\"}}");
        respondWith(201, "{\"packet_rate_limit_rule\": {\"id\": \"pl1\", \"max_kpps\": 10000, \"max_burst_kpps\": 500, \"direction\": \"egress\"}}");
        respondWith(204);

        var qos = osv3().networking().qosRules();
        QosMinimumBandwidthRule mb = qos.createMinimumBandwidthRule(POLICY, QosRuleOptions.minimumBandwidth(10000).direction("egress"));
        qos.listMinimumBandwidthRules(POLICY);
        QosMinimumPacketRateRule mp = qos.createMinimumPacketRateRule(POLICY, QosRuleOptions.minimumPacketRate(1000).direction("any"));
        QosMinimumPacketRateRule mp2 = qos.updateMinimumPacketRateRule(POLICY, "mp1", QosRuleOptions.update().minKpps(2000L));
        QosPacketRateLimitRule pl = qos.createPacketRateLimitRule(POLICY, QosRuleOptions.packetRateLimit(10000).maxBurstKpps(500L));
        qos.deletePacketRateLimitRule(POLICY, "pl1");

        RecordedRequest mbCreate = expect("POST", P + "/minimum_bandwidth_rules");
        Assert.assertEquals(body(mbCreate).get("minimum_bandwidth_rule").get("min_kbps").asLong(), 10000L);
        Assert.assertEquals(body(mbCreate).get("minimum_bandwidth_rule").get("direction").asText(), "egress");
        expect("GET", P + "/minimum_bandwidth_rules");
        expect("POST", P + "/minimum_packet_rate_rules");
        expect("PUT", P + "/minimum_packet_rate_rules/mp1");
        RecordedRequest plCreate = expect("POST", P + "/packet_rate_limit_rules");
        Assert.assertEquals(body(plCreate).get("packet_rate_limit_rule").get("max_burst_kpps").asLong(), 500L);
        expect("DELETE", P + "/packet_rate_limit_rules/pl1");
        Assert.assertEquals(mb.getMinKbps(), Long.valueOf(10000));
        Assert.assertEquals(mp.getDirection(), "any");
        Assert.assertEquals(mp2.getMinKpps(), Long.valueOf(2000));
        Assert.assertEquals(pl.getMaxBurstKpps(), Long.valueOf(500));
    }

    public void aliasRules() throws Exception {
        respondWith(200, "{\"bandwidth_limit_rule\": {\"id\": \"bl1\", \"max_kbps\": 1000, \"max_burst_kbps\": 100, \"direction\": \"egress\", \"qos_policy_id\": \"" + POLICY + "\"}}");
        respondWith(200, "{\"dscp_marking_rule\": {\"id\": \"d1\", \"dscp_mark\": 8, \"qos_policy_id\": \"" + POLICY + "\"}}");
        respondWith(200, "{\"minimum_bandwidth_rule\": {\"id\": \"mb1\", \"min_kbps\": 20000, \"direction\": \"egress\"}}");
        respondWith(204);
        respondWith(200, "{\"minimum_packet_rate_rule\": {\"id\": \"mp1\", \"min_kpps\": 1000, \"direction\": \"any\"}}");
        respondWith(204);

        var qos = osv3().networking().qosRules();
        Assert.assertEquals(qos.getAliasBandwidthLimitRule("bl1").getMaxKbps(), Integer.valueOf(1000));
        Assert.assertEquals(qos.getAliasDscpMarkingRule("d1").getQosPolicyId(), POLICY);
        Assert.assertEquals(qos.updateAliasMinimumBandwidthRule("mb1", QosRuleOptions.update().minKbps(20000L)).getMinKbps(), Long.valueOf(20000));
        Assert.assertTrue(qos.deleteAliasBandwidthLimitRule("bl1").isSuccess());
        qos.getAliasMinimumPacketRateRule("mp1");
        qos.deleteAliasDscpMarkingRule("d1");

        expect("GET", "/v2.0/qos/alias_bandwidth_limit_rules/bl1");
        expect("GET", "/v2.0/qos/alias_dscp_marking_rules/d1");
        RecordedRequest update = expect("PUT", "/v2.0/qos/alias_minimum_bandwidth_rules/mb1");
        Assert.assertEquals(body(update).get("minimum_bandwidth_rule").get("min_kbps").asLong(), 20000L);
        expect("DELETE", "/v2.0/qos/alias_bandwidth_limit_rules/bl1");
        expect("GET", "/v2.0/qos/alias_minimum_packet_rate_rules/mp1");
        expect("DELETE", "/v2.0/qos/alias_dscp_marking_rules/d1");
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `qosRules()`, `QosRuleOptions` 등 없음.

- [ ] **Step 3: 모델 생성**

`/tmp/f2.json`(생성기 입력):
```json
[
 {"iface": "QosRuleType", "cls": "NeutronQosRuleType", "doc": "A QoS rule type and the drivers that support it.", "root": "rule_type", "list": "rule_types", "listCls": "RuleTypes",
  "fields": [["type", "getType", "String"], ["drivers", "getDrivers", "List<Map<String, Object>>"]]},
 {"iface": "QosDscpMarkingRule", "cls": "NeutronQosDscpMarkingRule", "doc": "A QoS DSCP marking rule.", "root": "dscp_marking_rule", "list": "dscp_marking_rules", "listCls": "Rules",
  "fields": [["id", "getId", "String"], ["dscp_mark", "getDscpMark", "Integer"], ["qos_policy_id", "getQosPolicyId", "String"]]},
 {"iface": "QosMinimumBandwidthRule", "cls": "NeutronQosMinimumBandwidthRule", "doc": "A QoS minimum bandwidth rule.", "root": "minimum_bandwidth_rule", "list": "minimum_bandwidth_rules", "listCls": "Rules",
  "fields": [["id", "getId", "String"], ["min_kbps", "getMinKbps", "Long"], ["direction", "getDirection", "String"], ["qos_policy_id", "getQosPolicyId", "String"]]},
 {"iface": "QosMinimumPacketRateRule", "cls": "NeutronQosMinimumPacketRateRule", "doc": "A QoS minimum packet rate rule.", "root": "minimum_packet_rate_rule", "list": "minimum_packet_rate_rules", "listCls": "Rules",
  "fields": [["id", "getId", "String"], ["min_kpps", "getMinKpps", "Long"], ["direction", "getDirection", "String"], ["qos_policy_id", "getQosPolicyId", "String"]]},
 {"iface": "QosPacketRateLimitRule", "cls": "NeutronQosPacketRateLimitRule", "doc": "A QoS packet rate limit rule.", "root": "packet_rate_limit_rule", "list": "packet_rate_limit_rules", "listCls": "Rules",
  "fields": [["id", "getId", "String"], ["max_kpps", "getMaxKpps", "Long"], ["max_burst_kpps", "getMaxBurstKpps", "Long"], ["direction", "getDirection", "String"], ["qos_policy_id", "getQosPolicyId", "String"]]}
]
```
Run: `python3 .superpowers/gen_neutron.py /tmp/f2.json`

- [ ] **Step 4: 옵션과 서비스 구현**

`model/network/options/QosRuleOptions.java`:
```java
package org.openstack4j.model.network.options;

/** Body of a QoS rule create or update; one class for every rule kind (Neutron validates the fields per kind). */
public class QosRuleOptions extends NeutronAttributes<QosRuleOptions> {

    public static QosRuleOptions dscpMarking(int dscpMark) { return new QosRuleOptions().dscpMark(dscpMark); }
    public static QosRuleOptions minimumBandwidth(long minKbps) { return new QosRuleOptions().minKbps(minKbps); }
    public static QosRuleOptions minimumPacketRate(long minKpps) { return new QosRuleOptions().minKpps(minKpps); }
    public static QosRuleOptions packetRateLimit(long maxKpps) { return new QosRuleOptions().maxKpps(maxKpps); }
    /** An update that sends only the fields set afterwards. */
    public static QosRuleOptions update() { return new QosRuleOptions(); }

    @Override
    protected QosRuleOptions self() {
        return this;
    }

    public QosRuleOptions dscpMark(Integer dscpMark) { return put("dscp_mark", dscpMark); }
    public QosRuleOptions minKbps(Long minKbps) { return put("min_kbps", minKbps); }
    public QosRuleOptions minKpps(Long minKpps) { return put("min_kpps", minKpps); }
    public QosRuleOptions maxKpps(Long maxKpps) { return put("max_kpps", maxKpps); }
    public QosRuleOptions maxBurstKpps(Long maxBurstKpps) { return put("max_burst_kpps", maxBurstKpps); }
    /** For the alias bandwidth limit rule. */
    public QosRuleOptions maxKbps(Long maxKbps) { return put("max_kbps", maxKbps); }
    public QosRuleOptions maxBurstKbps(Long maxBurstKbps) { return put("max_burst_kbps", maxBurstKbps); }
    /** egress, ingress or any (packet rate rules). */
    public QosRuleOptions direction(String direction) { return put("direction", direction); }
}
```
(`dscpMarking(int)` 등은 `dscpMark(Integer)` 로 박싱된다; `minimumBandwidth(long)` → `minKbps(Long)`.)

`QosRuleServiceImpl`:
```java
package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;

import org.openstack4j.api.networking.ext.QosRuleService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.NetQosPolicyBandwidthLimitRule;
import org.openstack4j.model.network.ext.QosDscpMarkingRule;
import org.openstack4j.model.network.ext.QosMinimumBandwidthRule;
import org.openstack4j.model.network.ext.QosMinimumPacketRateRule;
import org.openstack4j.model.network.ext.QosPacketRateLimitRule;
import org.openstack4j.model.network.ext.QosRuleType;
import org.openstack4j.model.network.options.QosRuleOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronNetQosPolicyBandwidthLimitRule;
import org.openstack4j.openstack.networking.domain.ext.NeutronQosDscpMarkingRule;
import org.openstack4j.openstack.networking.domain.ext.NeutronQosMinimumBandwidthRule;
import org.openstack4j.openstack.networking.domain.ext.NeutronQosMinimumPacketRateRule;
import org.openstack4j.openstack.networking.domain.ext.NeutronQosPacketRateLimitRule;
import org.openstack4j.openstack.networking.domain.ext.NeutronQosRuleType;

public class QosRuleServiceImpl extends BaseNeutronExtService implements QosRuleService {

    private static final String DSCP = "dscp_marking_rule";
    private static final String MIN_BW = "minimum_bandwidth_rule";
    private static final String MIN_PPS = "minimum_packet_rate_rule";
    private static final String PPS_LIMIT = "packet_rate_limit_rule";
    private static final String BW_LIMIT = "bandwidth_limit_rule";

    private static String rules(String policyId, String kind) {
        return "/qos/policies/" + id(policyId) + "/" + kind + "s";
    }

    private static String rule(String policyId, String kind, String ruleId) {
        return rules(policyId, kind) + "/" + id(ruleId);
    }

    private static String alias(String kind, String ruleId) {
        return "/qos/alias_" + kind + "s/" + id(ruleId);
    }

    @Override public List<? extends QosRuleType> ruleTypes() { return listOf(NeutronQosRuleType.RuleTypes.class, "/qos/rule-types", null); }
    @Override public QosRuleType ruleType(String type) { return show(NeutronQosRuleType.class, "/qos/rule-types/" + id(type)); }

    @Override public List<? extends QosDscpMarkingRule> listDscpMarkingRules(String p) { return listOf(NeutronQosDscpMarkingRule.Rules.class, rules(p, DSCP), null); }
    @Override public QosDscpMarkingRule getDscpMarkingRule(String p, String r) { return show(NeutronQosDscpMarkingRule.class, rule(p, DSCP, r)); }
    @Override public QosDscpMarkingRule createDscpMarkingRule(String p, QosRuleOptions o) { return create(NeutronQosDscpMarkingRule.class, rules(p, DSCP), DSCP, o); }
    @Override public QosDscpMarkingRule updateDscpMarkingRule(String p, String r, QosRuleOptions o) { return update(NeutronQosDscpMarkingRule.class, rule(p, DSCP, r), DSCP, o); }
    @Override public ActionResponse deleteDscpMarkingRule(String p, String r) { return remove(rule(p, DSCP, r)); }

    @Override public List<? extends QosMinimumBandwidthRule> listMinimumBandwidthRules(String p) { return listOf(NeutronQosMinimumBandwidthRule.Rules.class, rules(p, MIN_BW), null); }
    @Override public QosMinimumBandwidthRule getMinimumBandwidthRule(String p, String r) { return show(NeutronQosMinimumBandwidthRule.class, rule(p, MIN_BW, r)); }
    @Override public QosMinimumBandwidthRule createMinimumBandwidthRule(String p, QosRuleOptions o) { return create(NeutronQosMinimumBandwidthRule.class, rules(p, MIN_BW), MIN_BW, o); }
    @Override public QosMinimumBandwidthRule updateMinimumBandwidthRule(String p, String r, QosRuleOptions o) { return update(NeutronQosMinimumBandwidthRule.class, rule(p, MIN_BW, r), MIN_BW, o); }
    @Override public ActionResponse deleteMinimumBandwidthRule(String p, String r) { return remove(rule(p, MIN_BW, r)); }

    @Override public List<? extends QosMinimumPacketRateRule> listMinimumPacketRateRules(String p) { return listOf(NeutronQosMinimumPacketRateRule.Rules.class, rules(p, MIN_PPS), null); }
    @Override public QosMinimumPacketRateRule getMinimumPacketRateRule(String p, String r) { return show(NeutronQosMinimumPacketRateRule.class, rule(p, MIN_PPS, r)); }
    @Override public QosMinimumPacketRateRule createMinimumPacketRateRule(String p, QosRuleOptions o) { return create(NeutronQosMinimumPacketRateRule.class, rules(p, MIN_PPS), MIN_PPS, o); }
    @Override public QosMinimumPacketRateRule updateMinimumPacketRateRule(String p, String r, QosRuleOptions o) { return update(NeutronQosMinimumPacketRateRule.class, rule(p, MIN_PPS, r), MIN_PPS, o); }
    @Override public ActionResponse deleteMinimumPacketRateRule(String p, String r) { return remove(rule(p, MIN_PPS, r)); }

    @Override public List<? extends QosPacketRateLimitRule> listPacketRateLimitRules(String p) { return listOf(NeutronQosPacketRateLimitRule.Rules.class, rules(p, PPS_LIMIT), null); }
    @Override public QosPacketRateLimitRule getPacketRateLimitRule(String p, String r) { return show(NeutronQosPacketRateLimitRule.class, rule(p, PPS_LIMIT, r)); }
    @Override public QosPacketRateLimitRule createPacketRateLimitRule(String p, QosRuleOptions o) { return create(NeutronQosPacketRateLimitRule.class, rules(p, PPS_LIMIT), PPS_LIMIT, o); }
    @Override public QosPacketRateLimitRule updatePacketRateLimitRule(String p, String r, QosRuleOptions o) { return update(NeutronQosPacketRateLimitRule.class, rule(p, PPS_LIMIT, r), PPS_LIMIT, o); }
    @Override public ActionResponse deletePacketRateLimitRule(String p, String r) { return remove(rule(p, PPS_LIMIT, r)); }

    @Override public NetQosPolicyBandwidthLimitRule getAliasBandwidthLimitRule(String r) { return show(NeutronNetQosPolicyBandwidthLimitRule.class, alias(BW_LIMIT, r)); }
    @Override public NetQosPolicyBandwidthLimitRule updateAliasBandwidthLimitRule(String r, QosRuleOptions o) { return update(NeutronNetQosPolicyBandwidthLimitRule.class, alias(BW_LIMIT, r), BW_LIMIT, o); }
    @Override public ActionResponse deleteAliasBandwidthLimitRule(String r) { return remove(alias(BW_LIMIT, r)); }
    @Override public QosDscpMarkingRule getAliasDscpMarkingRule(String r) { return show(NeutronQosDscpMarkingRule.class, alias(DSCP, r)); }
    @Override public QosDscpMarkingRule updateAliasDscpMarkingRule(String r, QosRuleOptions o) { return update(NeutronQosDscpMarkingRule.class, alias(DSCP, r), DSCP, o); }
    @Override public ActionResponse deleteAliasDscpMarkingRule(String r) { return remove(alias(DSCP, r)); }
    @Override public QosMinimumBandwidthRule getAliasMinimumBandwidthRule(String r) { return show(NeutronQosMinimumBandwidthRule.class, alias(MIN_BW, r)); }
    @Override public QosMinimumBandwidthRule updateAliasMinimumBandwidthRule(String r, QosRuleOptions o) { return update(NeutronQosMinimumBandwidthRule.class, alias(MIN_BW, r), MIN_BW, o); }
    @Override public ActionResponse deleteAliasMinimumBandwidthRule(String r) { return remove(alias(MIN_BW, r)); }
    @Override public QosMinimumPacketRateRule getAliasMinimumPacketRateRule(String r) { return show(NeutronQosMinimumPacketRateRule.class, alias(MIN_PPS, r)); }
    @Override public QosMinimumPacketRateRule updateAliasMinimumPacketRateRule(String r, QosRuleOptions o) { return update(NeutronQosMinimumPacketRateRule.class, alias(MIN_PPS, r), MIN_PPS, o); }
    @Override public ActionResponse deleteAliasMinimumPacketRateRule(String r) { return remove(alias(MIN_PPS, r)); }
}
```
`QosRuleService`(Interfaces 의 34 메서드, Javadoc: 경로·필요 extension — dscp/min-bw 는 `qos`, packet rate 는 `qos-pps`·`qos-pps-minimum`, alias 는 `qos-rules-alias`), accessor `qosRules()`, binding.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='QosRuleTests,NetQosPolicyTests,NetQosBandwidthLimitRuleTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Failures: 0, Errors: 0`(QosRuleTests 5개).

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/f2.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/f2.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/f2.log
git add -A && git commit -m "feat(networking): add QoS rule types, DSCP, minimum bandwidth/packet rate, packet rate limit and alias rules

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "qosRules(): rule types(목록·상세), dscp marking·minimum bandwidth·minimum packet rate·packet rate limit 규칙 CRUD, alias 규칙 get/update/delete(34)."
```

---

### Task 3: subnet pools(+prefix ops, onboard), address scopes

**Files:**
- Create: 모델 `model/network/ext/SubnetPool.java`, `AddressScope.java`; 도메인 `NeutronSubnetPool.java`, `NeutronAddressScope.java`, `NeutronPrefixes.java`(wrapper `{"prefixes": [...]}`); 옵션 `model/network/options/SubnetPoolOptions.java`, `AddressScopeOptions.java`; `api/networking/ext/SubnetPoolService.java`, `AddressScopeService.java` + impl
- Modify: `NetworkingService`, `NetworkingServiceImpl`, `DefaultAPIProvider`
- Create: core-test `api/network/ext2/SubnetPoolTests.java`

**Interfaces:**
- Produces:
  - `NetworkingService.subnetPools()` → `SubnetPoolService`: `List<? extends SubnetPool> list()`, `list(Map<String, String> filters)`, `SubnetPool get(String id)`, `SubnetPool create(SubnetPoolOptions options)`, `SubnetPool update(String id, SubnetPoolOptions options)`, `ActionResponse delete(String id)`, `List<String> addPrefixes(String id, List<String> prefixes)`, `List<String> removePrefixes(String id, List<String> prefixes)`, `List<Map<String, Object>> onboardNetworkSubnets(String id, String networkId)`
  - `NetworkingService.addressScopes()` → `AddressScopeService`: `list()`, `list(Map)`, `get(id)`, `create(AddressScopeOptions)`, `update(id, AddressScopeOptions)`, `delete(id)`
  - `SubnetPool`: `getId()`, `getName()`, `getDescription()`, `getProjectId()`, `List<String> getPrefixes()`, `Integer getDefaultPrefixlen()`, `Integer getMinPrefixlen()`, `Integer getMaxPrefixlen()`, `Integer getDefaultQuota()`, `String getAddressScopeId()`, `Integer getIpVersion()`, `Boolean isShared()`, `Boolean isDefault()`, `Integer getRevisionNumber()`, `List<String> getTags()`
  - `AddressScope`: `getId()`, `getName()`, `getProjectId()`, `Integer getIpVersion()`, `Boolean isShared()`
  - `SubnetPoolOptions`: `create(String name, List<String> prefixes)`, `update()`; `name`, `description`, `prefixes(List)`, `defaultPrefixlen(Integer)`, `minPrefixlen(Integer)`, `maxPrefixlen(Integer)`, `defaultQuota(Integer)`, `addressScopeId(String)`, `shared(Boolean)`, `isDefault(Boolean)`, `projectId(String)`
  - `AddressScopeOptions`: `create(String name, int ipVersion)`, `update()`; `name`, `shared(Boolean)`, `projectId(String)`

- [ ] **Step 1: 실패하는 테스트 작성 (Review Focus 3, 4)**

```bash
git switch main && git pull && git switch -c task/f3-subnet-pools
```

`SubnetPoolTests.java`:
```java
package org.openstack4j.api.network.ext2;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.AddressScope;
import org.openstack4j.model.network.ext.SubnetPool;
import org.openstack4j.model.network.options.AddressScopeOptions;
import org.openstack4j.model.network.options.SubnetPoolOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/SubnetPools")
public class SubnetPoolTests extends AbstractNetworkingExtTest {

    private static final String POOL = "f49a1319-423a-4ee6-ba54-1d95a4f6cc68";
    private static final String POOL_JSON = "{\"address_scope_id\": null, \"default_prefixlen\": 25, \"default_quota\": null, \"description\": \"\", \"id\": \"" + POOL + "\","
            + " \"ip_version\": 4, \"is_default\": false, \"max_prefixlen\": 30, \"min_prefixlen\": 24, \"name\": \"my-subnet-pool\", \"prefixes\": [\"10.10.0.0/21\", \"192.168.0.0/16\"],"
            + " \"project_id\": \"" + PROJECT + "\", \"revision_number\": 1, \"shared\": false, \"tags\": [\"t1\"]}";

    public void subnetPoolLifecycle() throws Exception {
        respondWith(201, "{\"subnetpool\": " + POOL_JSON + "}");
        respondWith(200, "{\"subnetpools\": [" + POOL_JSON + "]}");
        respondWith(200, "{\"subnetpool\": " + POOL_JSON + "}");
        respondWith(200, "{\"subnetpool\": " + POOL_JSON + "}");
        respondWith(204);

        var pools = osv3().networking().subnetPools();
        SubnetPool created = pools.create(SubnetPoolOptions.create("my-subnet-pool", List.of("10.10.0.0/21", "192.168.0.0/16")).defaultPrefixlen(25).minPrefixlen(24).maxPrefixlen(30));
        List<? extends SubnetPool> all = pools.list();
        pools.get(POOL);
        pools.update(POOL, SubnetPoolOptions.update().description("changed"));
        boolean deleted = pools.delete(POOL).isSuccess();

        RecordedRequest create = expect("POST", "/v2.0/subnetpools");
        var body = body(create).get("subnetpool");
        Assert.assertEquals(body.get("name").asText(), "my-subnet-pool");
        Assert.assertEquals(body.get("prefixes").get(1).asText(), "192.168.0.0/16");
        Assert.assertEquals(body.get("default_prefixlen").asInt(), 25);
        Assert.assertFalse(body.has("address_scope_id"));
        expect("GET", "/v2.0/subnetpools");
        expect("GET", "/v2.0/subnetpools/" + POOL);
        RecordedRequest update = expect("PUT", "/v2.0/subnetpools/" + POOL);
        Assert.assertEquals(body(update).get("subnetpool").size(), 1);
        expect("DELETE", "/v2.0/subnetpools/" + POOL);
        Assert.assertEquals(created.getPrefixes().size(), 2);
        Assert.assertEquals(created.getMaxPrefixlen(), Integer.valueOf(30));
        Assert.assertNull(created.getAddressScopeId());
        Assert.assertFalse(all.get(0).isDefault());
        Assert.assertEquals(all.get(0).getTags(), List.of("t1"));
        Assert.assertTrue(deleted);
    }

    public void subnetPoolFiltersBecomeQuery() throws Exception {
        respondWith(200, "{\"subnetpools\": []}");
        osv3().networking().subnetPools().list(Map.of("ip_version", "4"));
        Assert.assertTrue(decodedPath(takeRequest()).endsWith("/v2.0/subnetpools?ip_version=4"));
    }

    public void prefixOperationsReturnPrefixes() throws Exception {
        respondWith(200, "{\"prefixes\": [\"192.168.0.0/23\", \"172.16.0.0/21\"]}");
        respondWith(200, "{\"prefixes\": [\"172.16.0.0/21\"]}");
        respondWith(200, "[{\"cidr\": \"192.168.0.0/24\", \"id\": \"s1\"}]");

        List<String> added = osv3().networking().subnetPools().addPrefixes(POOL, List.of("192.168.0.0/24", "192.168.1.0/24", "172.16.0.0/21"));
        List<String> removed = osv3().networking().subnetPools().removePrefixes(POOL, List.of("192.168.0.0/23"));
        List<Map<String, Object>> onboarded = osv3().networking().subnetPools().onboardNetworkSubnets(POOL, "net1");

        RecordedRequest add = expect("PUT", "/v2.0/subnetpools/" + POOL + "/add_prefixes");
        Assert.assertEquals(body(add).get("prefixes").size(), 3);
        expect("PUT", "/v2.0/subnetpools/" + POOL + "/remove_prefixes");
        RecordedRequest onboard = expect("PUT", "/v2.0/subnetpools/" + POOL + "/onboard_network_subnets");
        Assert.assertEquals(body(onboard).get("network_id").asText(), "net1");
        Assert.assertEquals(added, List.of("192.168.0.0/23", "172.16.0.0/21"));
        Assert.assertEquals(removed, List.of("172.16.0.0/21"));
        Assert.assertEquals(onboarded.get(0).get("cidr"), "192.168.0.0/24");
    }

    public void addressScopes() throws Exception {
        String scope = "{\"name\": \"address-scope-2\", \"tenant_id\": \"" + PROJECT + "\", \"ip_version\": 4, \"shared\": true, \"project_id\": \"" + PROJECT + "\", \"id\": \"as1\"}";
        respondWith(201, "{\"address_scope\": " + scope + "}");
        respondWith(200, "{\"address_scopes\": [" + scope + "]}");
        respondWith(200, "{\"address_scope\": " + scope + "}");
        respondWith(200, "{\"address_scope\": " + scope + "}");
        respondWith(204);

        var scopes = osv3().networking().addressScopes();
        AddressScope created = scopes.create(AddressScopeOptions.create("address-scope-2", 4).shared(true));
        scopes.list();
        scopes.get("as1");
        scopes.update("as1", AddressScopeOptions.update().name("renamed"));
        scopes.delete("as1");

        RecordedRequest create = expect("POST", "/v2.0/address-scopes");
        Assert.assertEquals(body(create).get("address_scope").get("ip_version").asInt(), 4);
        Assert.assertTrue(body(create).get("address_scope").get("shared").asBoolean());
        expect("GET", "/v2.0/address-scopes");
        expect("GET", "/v2.0/address-scopes/as1");
        Assert.assertEquals(body(expect("PUT", "/v2.0/address-scopes/as1")).get("address_scope").get("name").asText(), "renamed");
        expect("DELETE", "/v2.0/address-scopes/as1");
        Assert.assertTrue(created.isShared());
        Assert.assertEquals(created.getIpVersion(), Integer.valueOf(4));
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `subnetPools()`, `SubnetPoolOptions` 등 없음.

- [ ] **Step 3: 모델 생성**

`/tmp/f3.json`:
```json
[
 {"iface": "SubnetPool", "cls": "NeutronSubnetPool", "doc": "A subnet pool: prefixes subnets are allocated from.", "root": "subnetpool", "list": "subnetpools", "listCls": "SubnetPools",
  "fields": [["id", "getId", "String"], ["name", "getName", "String"], ["description", "getDescription", "String"], ["project_id", "getProjectId", "String"],
             ["prefixes", "getPrefixes", "List<String>"], ["default_prefixlen", "getDefaultPrefixlen", "Integer"], ["min_prefixlen", "getMinPrefixlen", "Integer"],
             ["max_prefixlen", "getMaxPrefixlen", "Integer"], ["default_quota", "getDefaultQuota", "Integer"], ["address_scope_id", "getAddressScopeId", "String"],
             ["ip_version", "getIpVersion", "Integer"], ["shared", "isShared", "Boolean"], ["is_default", "isDefault", "Boolean"],
             ["revision_number", "getRevisionNumber", "Integer"], ["tags", "getTags", "List<String>"]]},
 {"iface": "AddressScope", "cls": "NeutronAddressScope", "doc": "An address scope: subnet pools whose prefixes must not overlap.", "root": "address_scope", "list": "address_scopes", "listCls": "AddressScopes",
  "fields": [["id", "getId", "String"], ["name", "getName", "String"], ["project_id", "getProjectId", "String"], ["ip_version", "getIpVersion", "Integer"], ["shared", "isShared", "Boolean"]]}
]
```
(생성기는 `is_default` 를 필드 이름 `isDefault` 로 만든다 — getter 도 `isDefault()` 라 Jackson 이 같은 속성으로 묶는다. `@JsonProperty("is_default")` 가 필드에 있어 이름은 맞다.)

`NeutronPrefixes`(직접 작성):
```java
package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"prefixes": [...]}}: request and response of the subnet pool prefix operations. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronPrefixes implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("prefixes")
    private List<String> prefixes;

    public List<String> getPrefixes() {
        return prefixes;
    }
}
```

- [ ] **Step 4: 옵션과 서비스 구현**

```java
package org.openstack4j.model.network.options;

import java.util.List;
import java.util.Objects;

/** Body of a subnet pool create or update. */
public class SubnetPoolOptions extends NeutronAttributes<SubnetPoolOptions> {

    public static SubnetPoolOptions create(String name, List<String> prefixes) {
        return new SubnetPoolOptions().name(Objects.requireNonNull(name)).prefixes(Objects.requireNonNull(prefixes));
    }

    public static SubnetPoolOptions update() {
        return new SubnetPoolOptions();
    }

    @Override
    protected SubnetPoolOptions self() {
        return this;
    }

    public SubnetPoolOptions name(String name) { return put("name", name); }
    public SubnetPoolOptions description(String description) { return put("description", description); }
    public SubnetPoolOptions prefixes(List<String> prefixes) { return put("prefixes", prefixes); }
    public SubnetPoolOptions defaultPrefixlen(Integer length) { return put("default_prefixlen", length); }
    public SubnetPoolOptions minPrefixlen(Integer length) { return put("min_prefixlen", length); }
    public SubnetPoolOptions maxPrefixlen(Integer length) { return put("max_prefixlen", length); }
    public SubnetPoolOptions defaultQuota(Integer quota) { return put("default_quota", quota); }
    public SubnetPoolOptions addressScopeId(String addressScopeId) { return put("address_scope_id", addressScopeId); }
    public SubnetPoolOptions shared(Boolean shared) { return put("shared", shared); }
    public SubnetPoolOptions isDefault(Boolean isDefault) { return put("is_default", isDefault); }
    public SubnetPoolOptions projectId(String projectId) { return put("project_id", projectId); }
}
```
`AddressScopeOptions`: `create(String name, int ipVersion)` = `new AddressScopeOptions().name(name).put("ip_version", ipVersion)`, `update()`, `name`, `shared(Boolean)`, `projectId`.

`SubnetPoolServiceImpl`:
```java
package org.openstack4j.openstack.networking.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.networking.ext.SubnetPoolService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.SubnetPool;
import org.openstack4j.model.network.options.SubnetPoolOptions;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.networking.domain.ext.NeutronPrefixes;
import org.openstack4j.openstack.networking.domain.ext.NeutronSubnetPool;
import org.openstack4j.openstack.networking.domain.ext.NeutronSubnetPool.SubnetPools;

public class SubnetPoolServiceImpl extends BaseNeutronExtService implements SubnetPoolService {

    private static final String POOLS = "/subnetpools";
    private static final String ROOT = "subnetpool";

    @Override public List<? extends SubnetPool> list() { return listOf(SubnetPools.class, POOLS, null); }
    @Override public List<? extends SubnetPool> list(Map<String, String> filters) { return listOf(SubnetPools.class, POOLS, filters); }
    @Override public SubnetPool get(String id) { return show(NeutronSubnetPool.class, POOLS + "/" + id(id)); }
    @Override public SubnetPool create(SubnetPoolOptions options) { return create(NeutronSubnetPool.class, POOLS, ROOT, options); }
    @Override public SubnetPool update(String id, SubnetPoolOptions options) { return update(NeutronSubnetPool.class, POOLS + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(POOLS + "/" + id(id)); }

    private List<String> prefixes(String id, String action, List<String> prefixes) {
        NeutronPrefixes result = put(NeutronPrefixes.class, POOLS + "/" + id(id) + "/" + action)
                .entity(JsonBody.of(Collections.singletonMap("prefixes", Objects.requireNonNull(prefixes)))).execute();
        return result == null || result.getPrefixes() == null ? Collections.emptyList() : result.getPrefixes();
    }

    @Override public List<String> addPrefixes(String id, List<String> prefixes) { return prefixes(id, "add_prefixes", prefixes); }
    @Override public List<String> removePrefixes(String id, List<String> prefixes) { return prefixes(id, "remove_prefixes", prefixes); }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> onboardNetworkSubnets(String id, String networkId) {
        List<Map<String, Object>> result = put(List.class, POOLS + "/" + id(id) + "/onboard_network_subnets")
                .entity(JsonBody.of(Collections.singletonMap("network_id", Objects.requireNonNull(networkId)))).execute();
        return result == null ? Collections.emptyList() : result;
    }
}
```
`AddressScopeServiceImpl` 은 같은 모양(`/address-scopes`, 루트 `address_scope`, 목록 `AddressScopes`; prefix 메서드 없음). 인터페이스 2개, accessor 2개, binding 2개.

- [ ] **Step 5: 통과 확인 (세 connector — query·PUT 본문)**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && for c in httpclient okhttp http-connector; do ./mvnw -B test -pl connectors/$c -Dsurefire.failIfNoSpecifiedTests=false -Dtest='SubnetPoolTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:' | tail -1; done`
Expected: 세 connector 모두 `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/f3.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/f3.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/f3.log
git add -A && git commit -m "feat(networking): add subnet pools (prefix operations, onboarding) and address scopes

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "subnetPools(): CRUD·필터, add/remove prefixes, onboard network subnets. addressScopes(): CRUD."
```

---

### Task 4: address groups, RBAC policies

**Files:**
- Create: 모델 `AddressGroup.java`, `RbacPolicy.java`; 도메인 `NeutronAddressGroup.java`, `NeutronRbacPolicy.java`; 옵션 `AddressGroupOptions.java`, `RbacPolicyOptions.java`; `api/networking/ext/AddressGroupService.java`, `RbacPolicyService.java` + impl
- Modify: `NetworkingService`, `NetworkingServiceImpl`, `DefaultAPIProvider`
- Create: core-test `api/network/ext2/AddressGroupRbacTests.java`

**Interfaces:**
- Produces:
  - `NetworkingService.addressGroups()` → `AddressGroupService`: `list()`, `list(Map)`, `get(id)`, `create(AddressGroupOptions)`, `update(id, AddressGroupOptions)`, `delete(id)`, `AddressGroup addAddresses(String id, List<String> addresses)`, `AddressGroup removeAddresses(String id, List<String> addresses)`
  - `NetworkingService.rbacPolicies()` → `RbacPolicyService`: `list()`, `list(Map)`, `get(id)`, `create(RbacPolicyOptions)`, `update(id, RbacPolicyOptions)`, `delete(id)`
  - `AddressGroup`: `getId()`, `getName()`, `getDescription()`, `getProjectId()`, `List<String> getAddresses()`
  - `RbacPolicy`: `getId()`, `getObjectType()`, `getObjectId()`, `getAction()`, `getTargetTenant()`, `getProjectId()`
  - `AddressGroupOptions`: `create(String name)`, `update()`; `name`, `description`, `addresses(List<String>)`, `projectId`
  - `RbacPolicyOptions`: `create(String objectType, String objectId, String action, String targetProject)`(target 은 JSON `target_tenant`; `"*"` 는 전체), `update(String targetProject)`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/f4-address-groups-rbac
```

`AddressGroupRbacTests.java`:
```java
package org.openstack4j.api.network.ext2;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.AddressGroup;
import org.openstack4j.model.network.ext.RbacPolicy;
import org.openstack4j.model.network.options.AddressGroupOptions;
import org.openstack4j.model.network.options.RbacPolicyOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/AddressGroupsRbac")
public class AddressGroupRbacTests extends AbstractNetworkingExtTest {

    private static final String AG = "9f60873a-9bb0-45c9-ac25-b1d5d668d955";

    private static String group(String addresses) {
        return "{\"id\": \"" + AG + "\", \"name\": \"address-group-2\", \"project_id\": \"" + PROJECT + "\", \"addresses\": [" + addresses + "], \"description\": \"docs example group\"}";
    }

    public void addressGroups() throws Exception {
        respondWith(201, "{\"address_group\": " + group("") + "}");
        respondWith(200, "{\"address_groups\": [" + group("") + "]}");
        respondWith(200, "{\"address_group\": " + group("\"10.0.2.100/32\"") + "}");
        respondWith(200, "{\"address_group\": " + group("") + "}");
        respondWith(200, "{\"address_group\": " + group("") + "}");
        respondWith(200, "{\"address_group\": " + group("") + "}");
        respondWith(204);

        var groups = osv3().networking().addressGroups();
        AddressGroup created = groups.create(AddressGroupOptions.create("address-group-2").description("docs example group"));
        groups.list();
        AddressGroup added = groups.addAddresses(AG, List.of("10.0.2.100/32"));
        groups.removeAddresses(AG, List.of("10.0.2.100/32"));
        groups.get(AG);
        groups.update(AG, AddressGroupOptions.update().name("renamed"));
        groups.delete(AG);

        RecordedRequest create = expect("POST", "/v2.0/address-groups");
        Assert.assertEquals(body(create).get("address_group").get("name").asText(), "address-group-2");
        Assert.assertFalse(body(create).get("address_group").has("addresses"));
        expect("GET", "/v2.0/address-groups");
        RecordedRequest add = expect("PUT", "/v2.0/address-groups/" + AG + "/add_addresses");
        Assert.assertEquals(body(add).get("addresses").get(0).asText(), "10.0.2.100/32");
        expect("PUT", "/v2.0/address-groups/" + AG + "/remove_addresses");
        expect("GET", "/v2.0/address-groups/" + AG);
        expect("PUT", "/v2.0/address-groups/" + AG);
        expect("DELETE", "/v2.0/address-groups/" + AG);
        Assert.assertEquals(created.getDescription(), "docs example group");
        Assert.assertEquals(added.getAddresses(), List.of("10.0.2.100/32"));
    }

    public void rbacPolicies() throws Exception {
        String policy = "{\"target_tenant\": \"*\", \"tenant_id\": \"" + PROJECT + "\", \"object_type\": \"network\", \"object_id\": \"1f32f072-4d17-4811-b619-3623d018bd40\","
                + " \"action\": \"access_as_external\", \"project_id\": \"" + PROJECT + "\", \"id\": \"6d4c666e-1aad-465e-b670-4d112b760137\"}";
        respondWith(201, "{\"rbac_policy\": " + policy + "}");
        respondWith(200, "{\"rbac_policies\": [" + policy + "]}");
        respondWith(200, "{\"rbac_policy\": " + policy + "}");
        respondWith(200, "{\"rbac_policy\": " + policy + "}");
        respondWith(204);

        var rbac = osv3().networking().rbacPolicies();
        RbacPolicy created = rbac.create(RbacPolicyOptions.create("network", "1f32f072-4d17-4811-b619-3623d018bd40", "access_as_external", "*"));
        List<? extends RbacPolicy> all = rbac.list();
        rbac.get(created.getId());
        rbac.update(created.getId(), RbacPolicyOptions.update("p2"));
        rbac.delete(created.getId());

        RecordedRequest create = expect("POST", "/v2.0/rbac-policies");
        var body = body(create).get("rbac_policy");
        Assert.assertEquals(body.get("object_type").asText(), "network");
        Assert.assertEquals(body.get("action").asText(), "access_as_external");
        Assert.assertEquals(body.get("target_tenant").asText(), "*");
        expect("GET", "/v2.0/rbac-policies");
        expect("GET", "/v2.0/rbac-policies/" + created.getId());
        RecordedRequest update = expect("PUT", "/v2.0/rbac-policies/" + created.getId());
        Assert.assertEquals(body(update).get("rbac_policy").get("target_tenant").asText(), "p2");
        expect("DELETE", "/v2.0/rbac-policies/" + created.getId());
        Assert.assertEquals(created.getTargetTenant(), "*");
        Assert.assertEquals(all.get(0).getObjectId(), "1f32f072-4d17-4811-b619-3623d018bd40");
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `addressGroups()`, `RbacPolicyOptions` 등 없음.

- [ ] **Step 3: 모델 생성**

`/tmp/f4.json`:
```json
[
 {"iface": "AddressGroup", "cls": "NeutronAddressGroup", "doc": "An address group: a set of CIDRs security group rules can reference.", "root": "address_group", "list": "address_groups", "listCls": "AddressGroups",
  "fields": [["id", "getId", "String"], ["name", "getName", "String"], ["description", "getDescription", "String"], ["project_id", "getProjectId", "String"], ["addresses", "getAddresses", "List<String>"]]},
 {"iface": "RbacPolicy", "cls": "NeutronRbacPolicy", "doc": "An RBAC policy sharing a network, QoS policy, security group, address scope, address group or subnet pool.", "root": "rbac_policy", "list": "rbac_policies", "listCls": "RbacPolicies",
  "fields": [["id", "getId", "String"], ["object_type", "getObjectType", "String"], ["object_id", "getObjectId", "String"], ["action", "getAction", "String"], ["target_tenant", "getTargetTenant", "String"], ["project_id", "getProjectId", "String"]]}
]
```

- [ ] **Step 4: 옵션과 서비스 구현**

```java
package org.openstack4j.model.network.options;

import java.util.List;
import java.util.Objects;

/** Body of an address group create or update. */
public class AddressGroupOptions extends NeutronAttributes<AddressGroupOptions> {

    public static AddressGroupOptions create(String name) { return new AddressGroupOptions().name(Objects.requireNonNull(name)); }
    public static AddressGroupOptions update() { return new AddressGroupOptions(); }

    @Override
    protected AddressGroupOptions self() {
        return this;
    }

    public AddressGroupOptions name(String name) { return put("name", name); }
    public AddressGroupOptions description(String description) { return put("description", description); }
    /** Initial addresses on create; use {@code addAddresses}/{@code removeAddresses} afterwards. */
    public AddressGroupOptions addresses(List<String> addresses) { return put("addresses", addresses); }
    public AddressGroupOptions projectId(String projectId) { return put("project_id", projectId); }
}
```
```java
package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of an RBAC policy create or update; the target project is sent as {@code target_tenant} ("*" = every project). */
public class RbacPolicyOptions extends NeutronAttributes<RbacPolicyOptions> {

    public static RbacPolicyOptions create(String objectType, String objectId, String action, String targetProject) {
        return new RbacPolicyOptions().put("object_type", Objects.requireNonNull(objectType)).put("object_id", Objects.requireNonNull(objectId))
                .put("action", Objects.requireNonNull(action)).put("target_tenant", Objects.requireNonNull(targetProject));
    }

    /** Only the target project of an RBAC policy can change. */
    public static RbacPolicyOptions update(String targetProject) {
        return new RbacPolicyOptions().put("target_tenant", Objects.requireNonNull(targetProject));
    }

    @Override
    protected RbacPolicyOptions self() {
        return this;
    }

    public RbacPolicyOptions projectId(String projectId) { return put("project_id", projectId); }
}
```
`AddressGroupServiceImpl`:
```java
package org.openstack4j.openstack.networking.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.networking.ext.AddressGroupService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.AddressGroup;
import org.openstack4j.model.network.options.AddressGroupOptions;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.networking.domain.ext.NeutronAddressGroup;
import org.openstack4j.openstack.networking.domain.ext.NeutronAddressGroup.AddressGroups;

public class AddressGroupServiceImpl extends BaseNeutronExtService implements AddressGroupService {

    private static final String GROUPS = "/address-groups";
    private static final String ROOT = "address_group";

    @Override public List<? extends AddressGroup> list() { return listOf(AddressGroups.class, GROUPS, null); }
    @Override public List<? extends AddressGroup> list(Map<String, String> filters) { return listOf(AddressGroups.class, GROUPS, filters); }
    @Override public AddressGroup get(String id) { return show(NeutronAddressGroup.class, GROUPS + "/" + id(id)); }
    @Override public AddressGroup create(AddressGroupOptions options) { return create(NeutronAddressGroup.class, GROUPS, ROOT, options); }
    @Override public AddressGroup update(String id, AddressGroupOptions options) { return update(NeutronAddressGroup.class, GROUPS + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(GROUPS + "/" + id(id)); }

    private AddressGroup addresses(String id, String action, List<String> addresses) {
        return put(NeutronAddressGroup.class, GROUPS + "/" + id(id) + "/" + action)
                .entity(JsonBody.of(Collections.singletonMap("addresses", Objects.requireNonNull(addresses)))).execute();
    }

    @Override public AddressGroup addAddresses(String id, List<String> addresses) { return addresses(id, "add_addresses", addresses); }
    @Override public AddressGroup removeAddresses(String id, List<String> addresses) { return addresses(id, "remove_addresses", addresses); }
}
```
`RbacPolicyServiceImpl`: 같은 CRUD 모양(`/rbac-policies`, 루트 `rbac_policy`, 목록 `RbacPolicies`). 인터페이스 2개, accessor 2개, binding 2개.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='AddressGroupRbacTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/f4.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/f4.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/f4.log
git add -A && git commit -m "feat(networking): add address groups and RBAC policies

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "addressGroups(): CRUD, add/remove addresses. rbacPolicies(): CRUD."
```

---

### Task 5: default security group rules, security group default statefulness

**Files:**
- Create: 모델 `DefaultSecurityGroupRule.java`, `SecurityGroupDefaultStatefulness.java`; 도메인 `NeutronDefaultSecurityGroupRule.java`, `NeutronSecurityGroupDefaultStatefulness.java`; 옵션 `DefaultSecurityGroupRuleOptions.java`, `SecurityGroupDefaultStatefulnessOptions.java`; `api/networking/ext/DefaultSecurityGroupRuleService.java`, `SecurityGroupDefaultStatefulnessService.java` + impl
- Modify: `NetworkingService`, `NetworkingServiceImpl`, `DefaultAPIProvider`
- Create: core-test `api/network/ext2/SecurityGroupDefaultsTests.java`

**Interfaces:**
- Produces:
  - `NetworkingService.defaultSecurityGroupRules()` → `DefaultSecurityGroupRuleService`: `list()`, `list(Map)`, `get(id)`, `create(DefaultSecurityGroupRuleOptions)`, `delete(id)`(api-ref 에 update 없음)
  - `NetworkingService.securityGroupDefaultStatefulness()` → `SecurityGroupDefaultStatefulnessService`: `list()`, `list(Map)`, `get(id)`, `create(SecurityGroupDefaultStatefulnessOptions)`, `update(id, SecurityGroupDefaultStatefulnessOptions)`, `delete(id)`
  - `DefaultSecurityGroupRule`: `getId()`, `getDirection()`, `getEthertype()`, `getProtocol()`, `Integer getPortRangeMin()`, `Integer getPortRangeMax()`, `getRemoteIpPrefix()`, `getRemoteGroupId()`, `getRemoteAddressGroupId()`, `getDescription()`, `Boolean getUsedInDefaultSg()`, `Boolean getUsedInNonDefaultSg()`
  - `SecurityGroupDefaultStatefulness`: `getId()`, `getProjectId()`, `Boolean isStateful()`
  - `DefaultSecurityGroupRuleOptions`: `create(String direction)`; `ethertype`, `protocol`, `portRangeMin(Integer)`, `portRangeMax(Integer)`, `remoteIpPrefix`, `remoteGroupId`, `remoteAddressGroupId`, `description`, `usedInDefaultSg(Boolean)`, `usedInNonDefaultSg(Boolean)`
  - `SecurityGroupDefaultStatefulnessOptions`: `create(boolean stateful)`, `update(boolean stateful)`; `projectId`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/f5-sg-defaults
```

`SecurityGroupDefaultsTests.java`:
```java
package org.openstack4j.api.network.ext2;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.DefaultSecurityGroupRule;
import org.openstack4j.model.network.ext.SecurityGroupDefaultStatefulness;
import org.openstack4j.model.network.options.DefaultSecurityGroupRuleOptions;
import org.openstack4j.model.network.options.SecurityGroupDefaultStatefulnessOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/SecurityGroupDefaults")
public class SecurityGroupDefaultsTests extends AbstractNetworkingExtTest {

    private static final String RULE = "{\"direction\": \"ingress\", \"ethertype\": \"IPv4\", \"id\": \"2bc0accf-312e-429a-956e-e4407625eb62\", \"port_range_max\": 80, \"port_range_min\": 80,"
            + " \"protocol\": \"tcp\", \"remote_group_id\": null, \"remote_ip_prefix\": null, \"remote_address_group_id\": null, \"used_in_default_sg\": false, \"used_in_non_default_sg\": true, \"description\": \"\"}";

    public void defaultSecurityGroupRules() throws Exception {
        respondWith(201, "{\"default_security_group_rule\": " + RULE + "}");
        respondWith(200, "{\"default_security_group_rules\": [" + RULE + "]}");
        respondWith(200, "{\"default_security_group_rule\": " + RULE + "}");
        respondWith(204);

        var rules = osv3().networking().defaultSecurityGroupRules();
        DefaultSecurityGroupRule created = rules.create(DefaultSecurityGroupRuleOptions.create("ingress").ethertype("IPv4").protocol("tcp")
                .portRangeMin(80).portRangeMax(80).usedInDefaultSg(false).usedInNonDefaultSg(true));
        List<? extends DefaultSecurityGroupRule> all = rules.list();
        rules.get(created.getId());
        rules.delete(created.getId());

        RecordedRequest create = expect("POST", "/v2.0/default-security-group-rules");
        var body = body(create).get("default_security_group_rule");
        Assert.assertEquals(body.get("direction").asText(), "ingress");
        Assert.assertEquals(body.get("port_range_min").asInt(), 80);
        Assert.assertFalse(body.get("used_in_default_sg").asBoolean());
        Assert.assertFalse(body.has("remote_ip_prefix"));
        expect("GET", "/v2.0/default-security-group-rules");
        expect("GET", "/v2.0/default-security-group-rules/" + created.getId());
        expect("DELETE", "/v2.0/default-security-group-rules/" + created.getId());
        Assert.assertEquals(created.getPortRangeMax(), Integer.valueOf(80));
        Assert.assertTrue(all.get(0).getUsedInNonDefaultSg());
        Assert.assertNull(all.get(0).getRemoteIpPrefix());
    }

    public void defaultStatefulness() throws Exception {
        String item = "{\"id\": \"st1\", \"project_id\": \"" + PROJECT + "\", \"stateful\": false}";
        respondWith(201, "{\"security_groups_default_statefulness\": " + item + "}");
        respondWith(200, "{\"security_groups_default_statefulness\": [" + item + "]}");
        respondWith(200, "{\"security_groups_default_statefulness\": " + item + "}");
        respondWith(200, "{\"security_groups_default_statefulness\": {\"id\": \"st1\", \"project_id\": \"" + PROJECT + "\", \"stateful\": true}}");
        respondWith(204);

        var statefulness = osv3().networking().securityGroupDefaultStatefulness();
        SecurityGroupDefaultStatefulness created = statefulness.create(SecurityGroupDefaultStatefulnessOptions.create(false).projectId(PROJECT));
        List<? extends SecurityGroupDefaultStatefulness> all = statefulness.list();
        statefulness.get("st1");
        SecurityGroupDefaultStatefulness updated = statefulness.update("st1", SecurityGroupDefaultStatefulnessOptions.update(true));
        statefulness.delete("st1");

        RecordedRequest create = expect("POST", "/v2.0/security-groups-default-statefulness");
        Assert.assertFalse(body(create).get("security_groups_default_statefulness").get("stateful").asBoolean());
        expect("GET", "/v2.0/security-groups-default-statefulness");
        expect("GET", "/v2.0/security-groups-default-statefulness/st1");
        expect("PUT", "/v2.0/security-groups-default-statefulness/st1");
        expect("DELETE", "/v2.0/security-groups-default-statefulness/st1");
        Assert.assertFalse(created.isStateful());
        Assert.assertEquals(all.size(), 1);
        Assert.assertTrue(updated.isStateful());
    }
}
```
(이 자원은 단건과 목록이 같은 키 `security_groups_default_statefulness` 를 쓴다 — api-ref 의 list 예시를 Step 3 에서 확인하고, 목록 키가 다르면 테스트와 생성기 입력을 그 키로 고친 뒤 Ruling 으로 남긴다.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `defaultSecurityGroupRules()` 등 없음.

- [ ] **Step 3: 목록 키 확인과 모델 생성**

Run: `grep -o '"[a-z_]*": \[' "$NEUTRON_SAMPLES/security-groups-default-statefulness/"*list*`(`NEUTRON_SAMPLES` = scratchpad 의 `neutron/api-ref/source/v2/samples`)
Expected: 목록 키(위 테스트는 `security_groups_default_statefulness`).

단건과 목록 키가 같으면 `@JsonRootName` 을 붙인 단건 클래스로는 목록 응답(배열)을 읽을 수 없으므로(UNWRAP 후 배열) — 목록은 `ListResult` 클래스(`@JsonRootName` 없음)가 키로 읽고 단건은 `@JsonRootName` 으로 읽으니 충돌하지 않는다.

`/tmp/f5.json`:
```json
[
 {"iface": "DefaultSecurityGroupRule", "cls": "NeutronDefaultSecurityGroupRule", "doc": "A rule template copied into new (default and/or non-default) security groups.", "root": "default_security_group_rule", "list": "default_security_group_rules", "listCls": "Rules",
  "fields": [["id", "getId", "String"], ["direction", "getDirection", "String"], ["ethertype", "getEthertype", "String"], ["protocol", "getProtocol", "String"],
             ["port_range_min", "getPortRangeMin", "Integer"], ["port_range_max", "getPortRangeMax", "Integer"], ["remote_ip_prefix", "getRemoteIpPrefix", "String"],
             ["remote_group_id", "getRemoteGroupId", "String"], ["remote_address_group_id", "getRemoteAddressGroupId", "String"], ["description", "getDescription", "String"],
             ["used_in_default_sg", "getUsedInDefaultSg", "Boolean"], ["used_in_non_default_sg", "getUsedInNonDefaultSg", "Boolean"]]},
 {"iface": "SecurityGroupDefaultStatefulness", "cls": "NeutronSecurityGroupDefaultStatefulness", "doc": "The default statefulness of new security groups in a project.", "root": "security_groups_default_statefulness", "list": "security_groups_default_statefulness", "listCls": "Items",
  "fields": [["id", "getId", "String"], ["project_id", "getProjectId", "String"], ["stateful", "isStateful", "Boolean"]]}
]
```

- [ ] **Step 4: 옵션과 서비스 구현**

```java
package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a default security group rule create. */
public class DefaultSecurityGroupRuleOptions extends NeutronAttributes<DefaultSecurityGroupRuleOptions> {

    /** @param direction ingress or egress */
    public static DefaultSecurityGroupRuleOptions create(String direction) {
        return new DefaultSecurityGroupRuleOptions().put("direction", Objects.requireNonNull(direction));
    }

    @Override
    protected DefaultSecurityGroupRuleOptions self() {
        return this;
    }

    public DefaultSecurityGroupRuleOptions ethertype(String ethertype) { return put("ethertype", ethertype); }
    public DefaultSecurityGroupRuleOptions protocol(String protocol) { return put("protocol", protocol); }
    public DefaultSecurityGroupRuleOptions portRangeMin(Integer port) { return put("port_range_min", port); }
    public DefaultSecurityGroupRuleOptions portRangeMax(Integer port) { return put("port_range_max", port); }
    public DefaultSecurityGroupRuleOptions remoteIpPrefix(String prefix) { return put("remote_ip_prefix", prefix); }
    /** "PARENT" refers to the security group the rule is copied into. */
    public DefaultSecurityGroupRuleOptions remoteGroupId(String groupId) { return put("remote_group_id", groupId); }
    public DefaultSecurityGroupRuleOptions remoteAddressGroupId(String addressGroupId) { return put("remote_address_group_id", addressGroupId); }
    public DefaultSecurityGroupRuleOptions description(String description) { return put("description", description); }
    public DefaultSecurityGroupRuleOptions usedInDefaultSg(Boolean used) { return put("used_in_default_sg", used); }
    public DefaultSecurityGroupRuleOptions usedInNonDefaultSg(Boolean used) { return put("used_in_non_default_sg", used); }
}
```
`SecurityGroupDefaultStatefulnessOptions`: `create(boolean stateful)` / `update(boolean stateful)` = `new …().put("stateful", stateful)`, `projectId(String)`.

`DefaultSecurityGroupRuleServiceImpl`(`/default-security-group-rules`, 루트 `default_security_group_rule`, 목록 `Rules`; list/list(Map)/get/create/delete), `SecurityGroupDefaultStatefulnessServiceImpl`(`/security-groups-default-statefulness`, 루트 `security_groups_default_statefulness`, 목록 `Items`; CRUD) — Task 4 `AddressGroupServiceImpl` 의 CRUD 다섯 줄과 같은 형태로 쓴다(prefix/address 메서드 없음). 인터페이스 2개, accessor 2개, binding 2개.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='SecurityGroupDefaultsTests,SecurityGroupTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/f5.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/f5.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/f5.log
git add -A && git commit -m "feat(networking): add default security group rules and security group default statefulness

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "defaultSecurityGroupRules(): list/get/create/delete. securityGroupDefaultStatefulness(): CRUD."
```

---

### Task 6: router 보강 — extra routes, external gateways, conntrack helpers, l3 agents

**Files:**
- Create: 모델 `model/network/ext/ConntrackHelper.java`; 도메인 `NeutronConntrackHelper.java`; 옵션 `model/network/options/ConntrackHelperOptions.java`
- Modify: `model/network/Router.java`(default getter), `openstack/networking/domain/NeutronRouter.java`(WRITE_ONLY 필드), `api/networking/RouterService.java`, `openstack/networking/internal/RouterServiceImpl.java`
- Create: core-test `api/network/ext2/RouterExtensionTests.java`

**Interfaces:**
- Produces (`RouterService`, 모두 추가):
  - `Router addExtraRoutes(String routerId, List<? extends HostRoute> routes)`, `Router removeExtraRoutes(String routerId, List<? extends HostRoute> routes)` — 본문 `{"router": {"routes": [{"destination", "nexthop"}]}}`
  - `Router addExternalGateways(String routerId, List<Map<String, Object>> gateways)`, `Router updateExternalGateways(…)`, `Router removeExternalGateways(…)` — 본문 `{"router": {"external_gateways": [...]}}`(각 항목 `network_id`, `enable_snat`, `external_fixed_ips`)
  - `List<? extends ConntrackHelper> listConntrackHelpers(String routerId)`, `ConntrackHelper getConntrackHelper(String routerId, String helperId)`, `ConntrackHelper createConntrackHelper(String routerId, ConntrackHelperOptions options)`, `ConntrackHelper updateConntrackHelper(String routerId, String helperId, ConntrackHelperOptions options)`, `ActionResponse deleteConntrackHelper(String routerId, String helperId)`
  - `List<? extends Agent> listL3Agents(String routerId)`
  - `Router`: `default List<Map<String, Object>> getExternalGateways()`(external-gateway-multihoming; 응답 전용)
  - `ConntrackHelper`: `getId()`, `getProtocol()`, `Integer getPort()`, `getHelper()`
  - `ConntrackHelperOptions`: `create(String protocol, int port, String helper)`, `update()`; `protocol`, `port(Integer)`, `helper`

- [ ] **Step 1: 기준선 본문 확보와 실패하는 테스트 작성 (Review Focus 1)**

```bash
git switch main && git pull && git switch -c task/f6-router-extensions
```

`RouterExtensionTests.java`:
```java
package org.openstack4j.api.network.ext2;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.ext.ConntrackHelper;
import org.openstack4j.model.network.options.ConntrackHelperOptions;
import org.openstack4j.openstack.networking.domain.NeutronHostRoute;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/RouterExtensions")
public class RouterExtensionTests extends AbstractNetworkingExtTest {

    private static final String R = "/v2.0/routers/r1";

    public void extraRoutes() throws Exception {
        respondWith(200, "{\"router\": {\"id\": \"r1\", \"name\": \"router1\", \"routes\": [{\"destination\": \"10.0.3.0/24\", \"nexthop\": \"10.0.0.13\"}]}}");
        respondWith(200, "{\"router\": {\"id\": \"r1\", \"name\": \"router1\", \"routes\": []}}");

        Router added = osv3().networking().router().addExtraRoutes("r1", List.of(new NeutronHostRoute("10.0.3.0/24", "10.0.0.13")));
        Router removed = osv3().networking().router().removeExtraRoutes("r1", List.of(new NeutronHostRoute("10.0.3.0/24", "10.0.0.13")));

        RecordedRequest add = expect("PUT", R + "/add_extraroutes");
        Assert.assertEquals(body(add).get("router").get("routes").get(0).get("nexthop").asText(), "10.0.0.13");
        Assert.assertEquals(body(add).get("router").size(), 1);
        expect("PUT", R + "/remove_extraroutes");
        Assert.assertEquals(added.getRoutes().get(0).getDestination(), "10.0.3.0/24");
        Assert.assertTrue(removed.getRoutes().isEmpty());
    }

    public void externalGateways() throws Exception {
        String router = "{\"router\": {\"id\": \"r1\", \"name\": \"router1\", \"external_gateway_info\": {\"enable_snat\": false, \"external_fixed_ips\": [], \"network_id\": \"n1\"},"
                + " \"external_gateways\": [{\"enable_snat\": false, \"external_fixed_ips\": [{\"ip_address\": \"192.0.2.2\", \"subnet_id\": \"s1\"}], \"network_id\": \"n1\"}]}}";
        respondWith(200, router);
        respondWith(200, router);
        respondWith(200, router);

        List<Map<String, Object>> gateways = List.of(Map.of("network_id", "n1", "enable_snat", false,
                "external_fixed_ips", List.of(Map.of("ip_address", "192.0.2.2", "subnet_id", "s1"))));
        Router added = osv3().networking().router().addExternalGateways("r1", gateways);
        osv3().networking().router().updateExternalGateways("r1", gateways);
        osv3().networking().router().removeExternalGateways("r1", List.of(Map.of("network_id", "n1")));

        RecordedRequest add = expect("PUT", R + "/add_external_gateways");
        Assert.assertEquals(body(add).get("router").get("external_gateways").get(0).get("external_fixed_ips").get(0).get("subnet_id").asText(), "s1");
        expect("PUT", R + "/update_external_gateways");
        expect("PUT", R + "/remove_external_gateways");
        Assert.assertEquals(added.getExternalGateways().get(0).get("network_id"), "n1");
    }

    public void conntrackHelpers() throws Exception {
        String helper = "{\"protocol\": \"tcp\", \"id\": \"ch1\", \"helper\": \"ftp\", \"port\": 21}";
        respondWith(201, "{\"conntrack_helper\": " + helper + "}");
        respondWith(200, "{\"conntrack_helpers\": [" + helper + "]}");
        respondWith(200, "{\"conntrack_helper\": " + helper + "}");
        respondWith(200, "{\"conntrack_helper\": {\"protocol\": \"tcp\", \"id\": \"ch1\", \"helper\": \"ftp\", \"port\": 2121}}");
        respondWith(204);

        var routers = osv3().networking().router();
        ConntrackHelper created = routers.createConntrackHelper("r1", ConntrackHelperOptions.create("tcp", 21, "ftp"));
        List<? extends ConntrackHelper> all = routers.listConntrackHelpers("r1");
        routers.getConntrackHelper("r1", "ch1");
        ConntrackHelper updated = routers.updateConntrackHelper("r1", "ch1", ConntrackHelperOptions.update().port(2121));
        routers.deleteConntrackHelper("r1", "ch1");

        RecordedRequest create = expect("POST", R + "/conntrack_helpers");
        Assert.assertEquals(body(create).get("conntrack_helper").get("helper").asText(), "ftp");
        expect("GET", R + "/conntrack_helpers");
        expect("GET", R + "/conntrack_helpers/ch1");
        Assert.assertEquals(body(expect("PUT", R + "/conntrack_helpers/ch1")).get("conntrack_helper").size(), 1);
        expect("DELETE", R + "/conntrack_helpers/ch1");
        Assert.assertEquals(created.getPort(), Integer.valueOf(21));
        Assert.assertEquals(all.size(), 1);
        Assert.assertEquals(updated.getPort(), Integer.valueOf(2121));
    }

    public void l3Agents() throws Exception {
        respondWith(200, "{\"agents\": [{\"id\": \"a1\", \"agent_type\": \"L3 agent\", \"host\": \"net1\", \"alive\": true, \"admin_state_up\": true, \"binary\": \"neutron-l3-agent\"}]}");
        Assert.assertEquals(osv3().networking().router().listL3Agents("r1").get(0).getId(), "a1");
        expect("GET", R + "/l3-agents");
    }

    public void existingRouterCreateBodyUnchanged() throws Exception {
        respondWith(201, "{\"router\": {\"id\": \"r1\", \"name\": \"router1\", \"admin_state_up\": true}}");
        osv3().networking().router().create(Builders.router().name("router1").adminStateUp(true).build());
        RecordedRequest create = expect("POST", "/v2.0/routers");
        Assert.assertEquals(create.getBody().readUtf8(), "BASELINE");
    }
}
```
`"BASELINE"` 은 자리표시가 아니라 **이 단계에서 채우는 값**이다: 먼저 위 테스트에서 `existingRouterCreateBodyUnchanged` 만 남기고(나머지 메서드는 잠시 주석) 변경 전 코드로 실행해 실패 메시지의 실제 본문(`expected [BASELINE] but found [...]`)을 얻어 그 문자열로 바꾼다. 그 뒤 주석을 되돌린다. (`NeutronHostRoute(String, String)` 생성자가 없으면 기존 setter/빌더로 만든다 — 기존 클래스에서 확인.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `addExtraRoutes`, `ConntrackHelper` 등 없음.

- [ ] **Step 3: 모델**

`/tmp/f6.json`:
```json
[
 {"iface": "ConntrackHelper", "cls": "NeutronConntrackHelper", "doc": "A router conntrack helper (l3-conntrack-helper).", "root": "conntrack_helper", "list": "conntrack_helpers", "listCls": "ConntrackHelpers",
  "fields": [["id", "getId", "String"], ["protocol", "getProtocol", "String"], ["port", "getPort", "Integer"], ["helper", "getHelper", "String"]]}
]
```
`Router.java` 에 추가:
```java
    /** @return the gateways of a multi-homed router (external-gateway-multihoming); null if the server did not send them */
    default List<Map<String, Object>> getExternalGateways() {
        return null;
    }
```
`NeutronRouter.java` 에 추가:
```java
    @JsonProperty(value = "external_gateways", access = JsonProperty.Access.WRITE_ONLY)
    private List<Map<String, Object>> externalGateways;

    @Override
    public List<Map<String, Object>> getExternalGateways() {
        return externalGateways;
    }
```
(WRITE_ONLY = 역직렬화 전용. 생성·수정 본문에는 나가지 않는다 — `existingRouterCreateBodyUnchanged`.)

`ConntrackHelperOptions`:
```java
package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a router conntrack helper create or update. */
public class ConntrackHelperOptions extends NeutronAttributes<ConntrackHelperOptions> {

    public static ConntrackHelperOptions create(String protocol, int port, String helper) {
        return new ConntrackHelperOptions().protocol(Objects.requireNonNull(protocol)).port(port).helper(Objects.requireNonNull(helper));
    }

    public static ConntrackHelperOptions update() {
        return new ConntrackHelperOptions();
    }

    @Override
    protected ConntrackHelperOptions self() {
        return this;
    }

    public ConntrackHelperOptions protocol(String protocol) { return put("protocol", protocol); }
    public ConntrackHelperOptions port(Integer port) { return put("port", port); }
    public ConntrackHelperOptions helper(String helper) { return put("helper", helper); }
}
```

- [ ] **Step 4: 서비스 구현 (`RouterServiceImpl` 에 추가)**

```java
    private Router routerAction(String routerId, String action, String key, Object value) {
        return put(NeutronRouter.class, uri("/routers/%s/%s", Objects.requireNonNull(routerId), action))
                .entity(JsonBody.of("router", Collections.singletonMap(key, Objects.requireNonNull(value)))).execute();
    }

    private static List<Map<String, String>> routes(List<? extends HostRoute> routes) {
        return routes.stream().map(r -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("destination", r.getDestination());
            m.put("nexthop", r.getNexthop());
            return m;
        }).collect(Collectors.toList());
    }

    @Override public Router addExtraRoutes(String routerId, List<? extends HostRoute> r) { return routerAction(routerId, "add_extraroutes", "routes", routes(r)); }
    @Override public Router removeExtraRoutes(String routerId, List<? extends HostRoute> r) { return routerAction(routerId, "remove_extraroutes", "routes", routes(r)); }
    @Override public Router addExternalGateways(String routerId, List<Map<String, Object>> g) { return routerAction(routerId, "add_external_gateways", "external_gateways", g); }
    @Override public Router updateExternalGateways(String routerId, List<Map<String, Object>> g) { return routerAction(routerId, "update_external_gateways", "external_gateways", g); }
    @Override public Router removeExternalGateways(String routerId, List<Map<String, Object>> g) { return routerAction(routerId, "remove_external_gateways", "external_gateways", g); }

    private static String helpers(String routerId) {
        return "/routers/" + Objects.requireNonNull(routerId) + "/conntrack_helpers";
    }

    @Override public List<? extends ConntrackHelper> listConntrackHelpers(String routerId) { return get(ConntrackHelpers.class, helpers(routerId)).execute().getList(); }
    @Override public ConntrackHelper getConntrackHelper(String routerId, String id) { return get(NeutronConntrackHelper.class, helpers(routerId) + "/" + Objects.requireNonNull(id)).execute(); }

    @Override
    public ConntrackHelper createConntrackHelper(String routerId, ConntrackHelperOptions options) {
        return post(NeutronConntrackHelper.class, helpers(routerId)).entity(JsonBody.of("conntrack_helper", options.toMap())).execute();
    }

    @Override
    public ConntrackHelper updateConntrackHelper(String routerId, String id, ConntrackHelperOptions options) {
        return put(NeutronConntrackHelper.class, helpers(routerId) + "/" + Objects.requireNonNull(id)).entity(JsonBody.of("conntrack_helper", options.toMap())).execute();
    }

    @Override public ActionResponse deleteConntrackHelper(String routerId, String id) { return deleteWithResponse(helpers(routerId) + "/" + Objects.requireNonNull(id)).execute(); }
    @Override public List<? extends Agent> listL3Agents(String routerId) { return get(Agents.class, uri("/routers/%s/l3-agents", Objects.requireNonNull(routerId))).execute().getList(); }
```
(`RouterServiceImpl` 은 `BaseNetworkingServices` 를 상속하므로 `BaseNeutronExtService` helper 대신 직접 호출한다.) `RouterService` 에 Interfaces 의 11 개 선언(Javadoc: 필요 extension — extraroute-atomic, external-gateway-multihoming, l3-conntrack-helper, l3_agent_scheduler).

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='RouterExtensionTests,NetworkTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Failures: 0, Errors: 0`(RouterExtensionTests 5개; `existingRouterCreateBodyUnchanged` 는 기준선과 같아야 한다).

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/f6.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/f6.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/f6.log
git add -A && git commit -m "feat(networking): add router extra routes, external gateways, conntrack helpers and l3 agents

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "router(): add/remove extraroutes, add/update/remove external gateways, conntrack helpers CRUD, l3 agents; Router.getExternalGateways()(응답 전용 — 생성 본문 불변 테스트)."
```

---

### Task 7: agent 스케줄링, port bindings

**Files:**
- Create: 모델 `model/network/ext/PortBinding.java`; 도메인 `NeutronPortBinding.java`; 옵션 `model/network/options/PortBindingOptions.java`
- Modify: `api/networking/ext/AgentService.java` + `AgentServiceImpl.java`, `api/networking/PortService.java` + `PortServiceImpl.java`
- Create: core-test `api/network/ext2/AgentAndBindingTests.java`

**Interfaces:**
- Produces:
  - `AgentService`: `ActionResponse delete(String agentId)`, `List<? extends Network> listDhcpNetworks(String agentId)`, `List<? extends Router> listL3Routers(String agentId)`, `ActionResponse addRouterToL3Agent(String agentId, String routerId)`, `ActionResponse removeRouterFromL3Agent(String agentId, String routerId)`, `List<? extends Agent> listDhcpAgentsHostingNetwork(String networkId)`
  - `PortService`: `List<? extends PortBinding> listBindings(String portId)`, `PortBinding createBinding(String portId, PortBindingOptions options)`, `PortBinding activateBinding(String portId, String host)`, `ActionResponse deleteBinding(String portId, String host)`
  - `PortBinding`: `getHost()`, `getStatus()`, `getVifType()`, `getVnicType()`, `Map<String, Object> getProfile()`, `Map<String, Object> getVifDetails()`
  - `PortBindingOptions`: `create(String host)`; `vnicType`, `profile(Map<String, Object>)`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/f7-agents-bindings
```

`AgentAndBindingTests.java`:
```java
package org.openstack4j.api.network.ext2;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.PortBinding;
import org.openstack4j.model.network.options.PortBindingOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/AgentsBindings")
public class AgentAndBindingTests extends AbstractNetworkingExtTest {

    public void agentScheduling() throws Exception {
        respondWith(204);
        respondWith(200, "{\"networks\": [{\"id\": \"n1\", \"name\": \"net1\", \"status\": \"ACTIVE\"}]}");
        respondWith(200, "{\"routers\": [{\"id\": \"r1\", \"name\": \"router1\"}]}");
        respondWith(201);
        respondWith(204);
        respondWith(200, "{\"agents\": [{\"id\": \"a2\", \"agent_type\": \"DHCP agent\", \"host\": \"net1\", \"alive\": true}]}");

        var agents = osv3().networking().agent();
        boolean deleted = agents.delete("a1").isSuccess();
        Assert.assertEquals(agents.listDhcpNetworks("a1").get(0).getName(), "net1");
        Assert.assertEquals(agents.listL3Routers("a1").get(0).getId(), "r1");
        Assert.assertTrue(agents.addRouterToL3Agent("a1", "r1").isSuccess());
        Assert.assertTrue(agents.removeRouterFromL3Agent("a1", "r1").isSuccess());
        Assert.assertEquals(agents.listDhcpAgentsHostingNetwork("n1").get(0).getId(), "a2");

        expect("DELETE", "/v2.0/agents/a1");
        expect("GET", "/v2.0/agents/a1/dhcp-networks");
        expect("GET", "/v2.0/agents/a1/l3-routers");
        RecordedRequest add = expect("POST", "/v2.0/agents/a1/l3-routers");
        Assert.assertEquals(body(add).get("router_id").asText(), "r1");
        expect("DELETE", "/v2.0/agents/a1/l3-routers/r1");
        expect("GET", "/v2.0/networks/n1/dhcp-agents");
        Assert.assertTrue(deleted);
    }

    public void portBindings() throws Exception {
        String binding = "{\"host\": \"compute\", \"vif_type\": \"ovs\", \"vnic_type\": \"normal\", \"status\": \"%s\", \"profile\": {},"
                + " \"vif_details\": {\"connectivity\": \"l2\", \"port_filter\": true, \"ovs_hybrid_plug\": false, \"datapath_type\": \"system\", \"bridge_name\": \"br-int\"}}";
        respondWith(200, "{\"bindings\": [" + String.format(binding, "ACTIVE") + "]}");
        respondWith(201, "{\"binding\": " + String.format(binding, "INACTIVE") + "}");
        respondWith(200, "{\"binding\": " + String.format(binding, "ACTIVE") + "}");
        respondWith(204);

        var ports = osv3().networking().port();
        List<? extends PortBinding> all = ports.listBindings("p1");
        PortBinding created = ports.createBinding("p1", PortBindingOptions.create("compute").vnicType("normal"));
        PortBinding active = ports.activateBinding("p1", "compute");
        boolean deleted = ports.deleteBinding("p1", "compute").isSuccess();

        expect("GET", "/v2.0/ports/p1/bindings");
        RecordedRequest create = expect("POST", "/v2.0/ports/p1/bindings");
        Assert.assertEquals(body(create).get("binding").get("host").asText(), "compute");
        expect("PUT", "/v2.0/ports/p1/bindings/compute/activate");
        expect("DELETE", "/v2.0/ports/p1/bindings/compute");
        Assert.assertEquals(all.get(0).getVifDetails().get("bridge_name"), "br-int");
        Assert.assertEquals(created.getStatus(), "INACTIVE");
        Assert.assertEquals(active.getStatus(), "ACTIVE");
        Assert.assertEquals(active.getProfile(), Map.of());
        Assert.assertTrue(deleted);
    }
}
```
(api-ref 의 create 경로는 끝에 `/` 가 붙은 `POST /v2.0/ports/{port_id}/bindings/` 다. 끝 슬래시 없이 보내도 Neutron 이 받는지는 Task 12 실환경에서 port binding 생성이 어려우므로 확인하지 못한다 — 구현은 api-ref 대로 `/bindings/` 로 보내고 테스트의 `expect` 는 decoded 경로가 `/bindings` 또는 `/bindings/` 로 끝나는지 본다: 위 `expect("POST", "/v2.0/ports/p1/bindings")` 를 구현 후 실제 경로에 맞춰 `/bindings/` 로 고정한다.)

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `listDhcpNetworks`, `PortBinding` 등 없음.

- [ ] **Step 3: 모델과 옵션**

`/tmp/f7.json`:
```json
[
 {"iface": "PortBinding", "cls": "NeutronPortBinding", "doc": "A port binding on a host (binding-extended); a port has one ACTIVE binding and, during live migration, an INACTIVE one.", "root": "binding", "list": "bindings", "listCls": "PortBindings",
  "fields": [["host", "getHost", "String"], ["status", "getStatus", "String"], ["vif_type", "getVifType", "String"], ["vnic_type", "getVnicType", "String"],
             ["profile", "getProfile", "Map<String, Object>"], ["vif_details", "getVifDetails", "Map<String, Object>"]]}
]
```
`PortBindingOptions`: `create(String host)` = `new PortBindingOptions().put("host", requireNonNull(host))`, `vnicType(String)`, `profile(Map<String, Object>)`.

- [ ] **Step 4: 서비스 구현**

`AgentServiceImpl` 에 추가:
```java
    @Override public ActionResponse delete(String agentId) { return deleteWithResponse(uri("/agents/%s", Objects.requireNonNull(agentId))).execute(); }
    @Override public List<? extends Network> listDhcpNetworks(String agentId) { return get(Networks.class, uri("/agents/%s/dhcp-networks", Objects.requireNonNull(agentId))).execute().getList(); }
    @Override public List<? extends Router> listL3Routers(String agentId) { return get(Routers.class, uri("/agents/%s/l3-routers", Objects.requireNonNull(agentId))).execute().getList(); }

    @Override
    public ActionResponse addRouterToL3Agent(String agentId, String routerId) {
        return postWithResponse(uri("/agents/%s/l3-routers", Objects.requireNonNull(agentId)))
                .entity(JsonBody.of(Collections.singletonMap("router_id", Objects.requireNonNull(routerId)))).execute();
    }

    @Override
    public ActionResponse removeRouterFromL3Agent(String agentId, String routerId) {
        return deleteWithResponse(uri("/agents/%s/l3-routers/%s", Objects.requireNonNull(agentId), Objects.requireNonNull(routerId))).execute();
    }

    @Override public List<? extends Agent> listDhcpAgentsHostingNetwork(String networkId) { return get(Agents.class, uri("/networks/%s/dhcp-agents", Objects.requireNonNull(networkId))).execute().getList(); }
```
(기존 `attachNetworkToDhcpAgent` 가 쓰는 `postWithResponse(...)` 가 `entity(ModelEntity)` 를 받는지 확인; 받지 못하면 기존처럼 `.json(...)` 으로 `{"router_id": "..."}` 문자열을 보낸다.)
`PortServiceImpl` 에 추가:
```java
    @Override public List<? extends PortBinding> listBindings(String portId) { return get(PortBindings.class, uri("/ports/%s/bindings", Objects.requireNonNull(portId))).execute().getList(); }
    @Override public PortBinding createBinding(String portId, PortBindingOptions options) { return post(NeutronPortBinding.class, uri("/ports/%s/bindings/", Objects.requireNonNull(portId))).entity(JsonBody.of("binding", options.toMap())).execute(); }
    @Override public PortBinding activateBinding(String portId, String host) { return put(NeutronPortBinding.class, uri("/ports/%s/bindings/%s/activate", Objects.requireNonNull(portId), Objects.requireNonNull(host))).execute(); }
    @Override public ActionResponse deleteBinding(String portId, String host) { return deleteWithResponse(uri("/ports/%s/bindings/%s", Objects.requireNonNull(portId), Objects.requireNonNull(host))).execute(); }
```
인터페이스 선언 10 개(Javadoc: l3_agent_scheduler/dhcp_agent_scheduler 는 agent 기반 배포에서만, binding-extended).

- [ ] **Step 5: 통과 확인 (세 connector — 끝 슬래시 경로)**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && for c in httpclient okhttp http-connector; do ./mvnw -B test -pl connectors/$c -Dsurefire.failIfNoSpecifiedTests=false -Dtest='AgentAndBindingTests,PortTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:' | tail -1; done`
Expected: 세 connector 모두 `Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/f7.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/f7.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/f7.log
git add -A && git commit -m "feat(networking): add agent scheduling and port bindings

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "agent(): delete, dhcp networks, l3 routers(list/add/remove), network 의 dhcp agents. port(): bindings list/create/activate/delete."
```

---

### Task 8: segments, network segment ranges

**Files:**
- Create: 모델 `Segment.java`, `NetworkSegmentRange.java`; 도메인 `NeutronSegment.java`, `NeutronNetworkSegmentRange.java`; 옵션 `SegmentOptions.java`, `NetworkSegmentRangeOptions.java`; `api/networking/ext/SegmentService.java`, `NetworkSegmentRangeService.java` + impl
- Modify: `NetworkingService`, `NetworkingServiceImpl`, `DefaultAPIProvider`
- Create: core-test `api/network/ext2/SegmentTests.java`

**Interfaces:**
- Produces:
  - `NetworkingService.segments()` → `SegmentService`, `NetworkingService.networkSegmentRanges()` → `NetworkSegmentRangeService`: 각 `list()`, `list(Map)`, `get(id)`, `create(Options)`, `update(id, Options)`, `delete(id)`
  - `Segment`: `getId()`, `getName()`, `getDescription()`, `getNetworkId()`, `getNetworkType()`, `getPhysicalNetwork()`, `Integer getSegmentationId()`, `Integer getRevisionNumber()`
  - `NetworkSegmentRange`: `getId()`, `getName()`, `getDescription()`, `Boolean isDefault()`, `Boolean isShared()`, `getProjectId()`, `getNetworkType()`, `getPhysicalNetwork()`, `Integer getMinimum()`, `Integer getMaximum()`, `List<Integer> getAvailable()`, `Map<String, String> getUsed()`
  - `SegmentOptions`: `create(String networkId, String networkType)`, `update()`; `name`, `description`, `physicalNetwork`, `segmentationId(Integer)`
  - `NetworkSegmentRangeOptions`: `create(String networkType, int minimum, int maximum)`, `update()`; `name`, `description`, `shared(Boolean)`, `projectId`, `physicalNetwork`, `minimum(Integer)`, `maximum(Integer)`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/f8-segments
```

`SegmentTests.java`:
```java
package org.openstack4j.api.network.ext2;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.NetworkSegmentRange;
import org.openstack4j.model.network.ext.Segment;
import org.openstack4j.model.network.options.NetworkSegmentRangeOptions;
import org.openstack4j.model.network.options.SegmentOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/Segments")
public class SegmentTests extends AbstractNetworkingExtTest {

    public void segments() throws Exception {
        String segment = "{\"name\": null, \"network_id\": \"n1\", \"segmentation_id\": 2, \"network_type\": \"vlan\", \"physical_network\": \"public\", \"revision_number\": 1, \"id\": \"seg1\", \"description\": null}";
        respondWith(201, "{\"segment\": " + segment + "}");
        respondWith(200, "{\"segments\": [" + segment + "]}");
        respondWith(200, "{\"segment\": " + segment + "}");
        respondWith(200, "{\"segment\": " + segment + "}");
        respondWith(204);

        var segments = osv3().networking().segments();
        Segment created = segments.create(SegmentOptions.create("n1", "vlan").physicalNetwork("public").segmentationId(2));
        List<? extends Segment> all = segments.list();
        segments.get("seg1");
        segments.update("seg1", SegmentOptions.update().name("renamed"));
        segments.delete("seg1");

        RecordedRequest create = expect("POST", "/v2.0/segments");
        var body = body(create).get("segment");
        Assert.assertEquals(body.get("network_id").asText(), "n1");
        Assert.assertEquals(body.get("segmentation_id").asInt(), 2);
        expect("GET", "/v2.0/segments");
        expect("GET", "/v2.0/segments/seg1");
        expect("PUT", "/v2.0/segments/seg1");
        expect("DELETE", "/v2.0/segments/seg1");
        Assert.assertEquals(created.getSegmentationId(), Integer.valueOf(2));
        Assert.assertNull(all.get(0).getName());
    }

    public void networkSegmentRanges() throws Exception {
        String range = "{\"id\": \"rg1\", \"name\": \"range_vlan_physnet1\", \"description\": \"d\", \"default\": false, \"shared\": false, \"project_id\": \"" + PROJECT + "\","
                + " \"network_type\": \"vlan\", \"physical_network\": \"physnet1\", \"minimum\": 10, \"maximum\": 20, \"available\": [10, 11, 19, 20], \"used\": {\"17\": \"p1\"}}";
        respondWith(201, "{\"network_segment_range\": " + range + "}");
        respondWith(200, "{\"network_segment_ranges\": [" + range + "]}");
        respondWith(200, "{\"network_segment_range\": " + range + "}");
        respondWith(200, "{\"network_segment_range\": " + range + "}");
        respondWith(204);

        var ranges = osv3().networking().networkSegmentRanges();
        NetworkSegmentRange created = ranges.create(NetworkSegmentRangeOptions.create("vlan", 10, 20).physicalNetwork("physnet1").name("range_vlan_physnet1"));
        ranges.list();
        ranges.get("rg1");
        ranges.update("rg1", NetworkSegmentRangeOptions.update().maximum(30));
        ranges.delete("rg1");

        RecordedRequest create = expect("POST", "/v2.0/network_segment_ranges");
        Assert.assertEquals(body(create).get("network_segment_range").get("minimum").asInt(), 10);
        expect("GET", "/v2.0/network_segment_ranges");
        expect("GET", "/v2.0/network_segment_ranges/rg1");
        Assert.assertEquals(body(expect("PUT", "/v2.0/network_segment_ranges/rg1")).get("network_segment_range").get("maximum").asInt(), 30);
        expect("DELETE", "/v2.0/network_segment_ranges/rg1");
        Assert.assertFalse(created.isDefault());
        Assert.assertEquals(created.getAvailable(), List.of(10, 11, 19, 20));
        Assert.assertEquals(created.getUsed().get("17"), "p1");
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `segments()` 등 없음.

- [ ] **Step 3: 모델 생성**

`/tmp/f8.json`:
```json
[
 {"iface": "Segment", "cls": "NeutronSegment", "doc": "A network segment (routed provider networks).", "root": "segment", "list": "segments", "listCls": "Segments",
  "fields": [["id", "getId", "String"], ["name", "getName", "String"], ["description", "getDescription", "String"], ["network_id", "getNetworkId", "String"],
             ["network_type", "getNetworkType", "String"], ["physical_network", "getPhysicalNetwork", "String"], ["segmentation_id", "getSegmentationId", "Integer"],
             ["revision_number", "getRevisionNumber", "Integer"]]},
 {"iface": "NetworkSegmentRange", "cls": "NeutronNetworkSegmentRange", "doc": "A range of segmentation ids project networks may use.", "root": "network_segment_range", "list": "network_segment_ranges", "listCls": "NetworkSegmentRanges",
  "fields": [["id", "getId", "String"], ["name", "getName", "String"], ["description", "getDescription", "String"], ["default", "isDefault", "Boolean"],
             ["shared", "isShared", "Boolean"], ["project_id", "getProjectId", "String"], ["network_type", "getNetworkType", "String"],
             ["physical_network", "getPhysicalNetwork", "String"], ["minimum", "getMinimum", "Integer"], ["maximum", "getMaximum", "Integer"],
             ["available", "getAvailable", "List<Integer>"], ["used", "getUsed", "Map<String, String>"]]}
]
```
(필드 이름 `default` 는 Java 예약어다 — 생성기 결과에서 필드 이름을 `isDefault` 로 바꾼다; `@JsonProperty("default")` 는 그대로.)

- [ ] **Step 4: 옵션과 서비스 구현**

`SegmentOptions`:
```java
package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a segment create or update. */
public class SegmentOptions extends NeutronAttributes<SegmentOptions> {

    public static SegmentOptions create(String networkId, String networkType) {
        return new SegmentOptions().put("network_id", Objects.requireNonNull(networkId)).put("network_type", Objects.requireNonNull(networkType));
    }

    public static SegmentOptions update() {
        return new SegmentOptions();
    }

    @Override
    protected SegmentOptions self() {
        return this;
    }

    public SegmentOptions name(String name) { return put("name", name); }
    public SegmentOptions description(String description) { return put("description", description); }
    public SegmentOptions physicalNetwork(String physicalNetwork) { return put("physical_network", physicalNetwork); }
    public SegmentOptions segmentationId(Integer segmentationId) { return put("segmentation_id", segmentationId); }
}
```
`NetworkSegmentRangeOptions`: `create(String networkType, int minimum, int maximum)` = `put("network_type", …).put("minimum", minimum).put("maximum", maximum)`, `update()`, `name`, `description`, `shared(Boolean)`, `projectId`, `physicalNetwork`, `minimum(Integer)`, `maximum(Integer)`.

`SegmentServiceImpl`(`/segments`, 루트 `segment`, 목록 `Segments`), `NetworkSegmentRangeServiceImpl`(`/network_segment_ranges`, 루트 `network_segment_range`, 목록 `NetworkSegmentRanges`) — 각각 아래 여섯 줄(자원 이름만 바꿔서):
```java
    @Override public List<? extends Segment> list() { return listOf(Segments.class, PATH, null); }
    @Override public List<? extends Segment> list(Map<String, String> filters) { return listOf(Segments.class, PATH, filters); }
    @Override public Segment get(String id) { return show(NeutronSegment.class, PATH + "/" + id(id)); }
    @Override public Segment create(SegmentOptions options) { return create(NeutronSegment.class, PATH, ROOT, options); }
    @Override public Segment update(String id, SegmentOptions options) { return update(NeutronSegment.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }
```
인터페이스 2개, accessor 2개, binding 2개.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='SegmentTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/f8.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/f8.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/f8.log
git add -A && git commit -m "feat(networking): add segments and network segment ranges

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "segments(), networkSegmentRanges(): CRUD."
```

---

### Task 9: local IPs(+port associations), NDP proxies

**Files:**
- Create: 모델 `LocalIp.java`, `LocalIpPortAssociation.java`, `NdpProxy.java`; 도메인 `NeutronLocalIp.java`, `NeutronLocalIpPortAssociation.java`, `NeutronNdpProxy.java`; 옵션 `LocalIpOptions.java`, `NdpProxyOptions.java`; `api/networking/ext/LocalIpService.java`, `NdpProxyService.java` + impl
- Modify: `NetworkingService`, `NetworkingServiceImpl`, `DefaultAPIProvider`
- Create: core-test `api/network/ext2/LocalIpNdpTests.java`

**Interfaces:**
- Produces:
  - `NetworkingService.localIps()` → `LocalIpService`: CRUD(`list()`, `list(Map)`, `get`, `create(LocalIpOptions)`, `update(id, LocalIpOptions)`, `delete`), `List<? extends LocalIpPortAssociation> portAssociations(String localIpId)`, `LocalIpPortAssociation associatePort(String localIpId, String fixedPortId, String fixedIp)`(fixedIp null 이면 생략), `ActionResponse disassociatePort(String localIpId, String fixedPortId)`
  - `NetworkingService.ndpProxies()` → `NdpProxyService`: CRUD(`create(NdpProxyOptions)`, `update(id, NdpProxyOptions)`)
  - `LocalIp`: `getId()`, `getName()`, `getDescription()`, `getProjectId()`, `getLocalPortId()`, `getNetworkId()`, `getLocalIpAddress()`, `getIpMode()`, `Integer getRevisionNumber()`
  - `LocalIpPortAssociation`: `getLocalIpId()`, `getLocalIpAddress()`, `getFixedPortId()`, `getFixedIp()`, `getHost()`
  - `NdpProxy`: `getId()`, `getName()`, `getDescription()`, `getRouterId()`, `getPortId()`, `getIpAddress()`, `getProjectId()`, `Integer getRevisionNumber()`
  - `LocalIpOptions`: `create()`, `update()`; `name`, `description`, `networkId`, `localPortId`, `localIpAddress`, `ipMode`(translate/passthrough)
  - `NdpProxyOptions`: `create(String routerId, String portId)`, `update()`; `name`, `description`, `ipAddress`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/f9-local-ips-ndp
```

`LocalIpNdpTests.java`:
```java
package org.openstack4j.api.network.ext2;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.LocalIp;
import org.openstack4j.model.network.ext.LocalIpPortAssociation;
import org.openstack4j.model.network.ext.NdpProxy;
import org.openstack4j.model.network.options.LocalIpOptions;
import org.openstack4j.model.network.options.NdpProxyOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/LocalIpsNdp")
public class LocalIpNdpTests extends AbstractNetworkingExtTest {

    private static final String LIP = "d23abc8d-2991-4a55-ba98-2aaea84cc72f";

    public void localIps() throws Exception {
        String lip = "{\"id\": \"" + LIP + "\", \"name\": \"test_local_ip\", \"description\": \"local ip for testing\", \"project_id\": \"" + PROJECT + "\","
                + " \"local_port_id\": \"2f245a7b-796b-4f26-9cf9-9e82d248fda7\", \"network_id\": \"ce705c24-c1ef-408a-bda3-7bbd946164ab\", \"local_ip_address\": \"172.24.4.228\","
                + " \"ip_mode\": \"translate\", \"revision_number\": 1}";
        String assoc = "{\"local_ip_id\": \"" + LIP + "\", \"local_ip_address\": \"172.24.4.228\", \"fixed_port_id\": \"fp1\", \"fixed_ip\": \"10.0.0.5\", \"host\": \"host1\"}";
        respondWith(201, "{\"local_ip\": " + lip + "}");
        respondWith(200, "{\"local_ips\": [" + lip + "]}");
        respondWith(200, "{\"local_ip\": " + lip + "}");
        respondWith(200, "{\"local_ip\": " + lip + "}");
        respondWith(201, "{\"port_association\": " + assoc + "}");
        respondWith(200, "{\"port_associations\": [" + assoc + "]}");
        respondWith(204);
        respondWith(204);

        var lips = osv3().networking().localIps();
        LocalIp created = lips.create(LocalIpOptions.create().name("test_local_ip").networkId("ce705c24-c1ef-408a-bda3-7bbd946164ab"));
        lips.list();
        lips.get(LIP);
        lips.update(LIP, LocalIpOptions.update().description("changed"));
        LocalIpPortAssociation association = lips.associatePort(LIP, "fp1", "10.0.0.5");
        List<? extends LocalIpPortAssociation> associations = lips.portAssociations(LIP);
        lips.disassociatePort(LIP, "fp1");
        lips.delete(LIP);

        RecordedRequest create = expect("POST", "/v2.0/local_ips");
        Assert.assertEquals(body(create).get("local_ip").get("network_id").asText(), "ce705c24-c1ef-408a-bda3-7bbd946164ab");
        expect("GET", "/v2.0/local_ips");
        expect("GET", "/v2.0/local_ips/" + LIP);
        expect("PUT", "/v2.0/local_ips/" + LIP);
        RecordedRequest associate = expect("POST", "/v2.0/local_ips/" + LIP + "/port_associations");
        Assert.assertEquals(body(associate).get("port_association").get("fixed_ip").asText(), "10.0.0.5");
        expect("GET", "/v2.0/local_ips/" + LIP + "/port_associations");
        expect("DELETE", "/v2.0/local_ips/" + LIP + "/port_associations/fp1");
        expect("DELETE", "/v2.0/local_ips/" + LIP);
        Assert.assertEquals(created.getIpMode(), "translate");
        Assert.assertEquals(association.getHost(), "host1");
        Assert.assertEquals(associations.get(0).getFixedPortId(), "fp1");
    }

    public void associatePortWithoutFixedIpOmitsIt() throws Exception {
        respondWith(201, "{\"port_association\": {\"local_ip_id\": \"" + LIP + "\", \"fixed_port_id\": \"fp1\"}}");
        osv3().networking().localIps().associatePort(LIP, "fp1", null);
        Assert.assertFalse(body(takeRequest()).get("port_association").has("fixed_ip"));
    }

    public void ndpProxies() throws Exception {
        String proxy = "{\"name\": \"proxy1\", \"id\": \"np1\", \"router_id\": \"r1\", \"port_id\": \"p1\", \"ip_address\": \"2001::1:56\", \"revision_number\": 1,"
                + " \"project_id\": \"" + PROJECT + "\", \"description\": \"\"}";
        respondWith(201, "{\"ndp_proxy\": " + proxy + "}");
        respondWith(200, "{\"ndp_proxies\": [" + proxy + "]}");
        respondWith(200, "{\"ndp_proxy\": " + proxy + "}");
        respondWith(200, "{\"ndp_proxy\": " + proxy + "}");
        respondWith(204);

        var proxies = osv3().networking().ndpProxies();
        NdpProxy created = proxies.create(NdpProxyOptions.create("r1", "p1").name("proxy1"));
        proxies.list();
        proxies.get("np1");
        proxies.update("np1", NdpProxyOptions.update().description("changed"));
        proxies.delete("np1");

        RecordedRequest create = expect("POST", "/v2.0/ndp_proxies");
        Assert.assertEquals(body(create).get("ndp_proxy").get("router_id").asText(), "r1");
        expect("GET", "/v2.0/ndp_proxies");
        expect("GET", "/v2.0/ndp_proxies/np1");
        expect("PUT", "/v2.0/ndp_proxies/np1");
        expect("DELETE", "/v2.0/ndp_proxies/np1");
        Assert.assertEquals(created.getIpAddress(), "2001::1:56");
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `localIps()` 등 없음.

- [ ] **Step 3: 모델 생성**

`/tmp/f9.json`:
```json
[
 {"iface": "LocalIp", "cls": "NeutronLocalIp", "doc": "A local IP: a virtual IP shared by many ports and reachable without routing.", "root": "local_ip", "list": "local_ips", "listCls": "LocalIps",
  "fields": [["id", "getId", "String"], ["name", "getName", "String"], ["description", "getDescription", "String"], ["project_id", "getProjectId", "String"],
             ["local_port_id", "getLocalPortId", "String"], ["network_id", "getNetworkId", "String"], ["local_ip_address", "getLocalIpAddress", "String"],
             ["ip_mode", "getIpMode", "String"], ["revision_number", "getRevisionNumber", "Integer"]]},
 {"iface": "LocalIpPortAssociation", "cls": "NeutronLocalIpPortAssociation", "doc": "An association of a local IP with a port.", "root": "port_association", "list": "port_associations", "listCls": "Associations",
  "fields": [["local_ip_id", "getLocalIpId", "String"], ["local_ip_address", "getLocalIpAddress", "String"], ["fixed_port_id", "getFixedPortId", "String"],
             ["fixed_ip", "getFixedIp", "String"], ["host", "getHost", "String"]]},
 {"iface": "NdpProxy", "cls": "NeutronNdpProxy", "doc": "A router NDP proxy publishing an IPv6 address of an internal port.", "root": "ndp_proxy", "list": "ndp_proxies", "listCls": "NdpProxies",
  "fields": [["id", "getId", "String"], ["name", "getName", "String"], ["description", "getDescription", "String"], ["router_id", "getRouterId", "String"],
             ["port_id", "getPortId", "String"], ["ip_address", "getIpAddress", "String"], ["project_id", "getProjectId", "String"], ["revision_number", "getRevisionNumber", "Integer"]]}
]
```

- [ ] **Step 4: 옵션과 서비스 구현**

`LocalIpOptions`(`create()`/`update()` 둘 다 빈 옵션; `name`, `description`, `networkId`→`network_id`, `localPortId`→`local_port_id`, `localIpAddress`→`local_ip_address`, `ipMode`→`ip_mode`), `NdpProxyOptions`(`create(String routerId, String portId)` → `router_id`, `port_id`; `update()`; `name`, `description`, `ipAddress`→`ip_address`) — 형태는 Task 8 `SegmentOptions` 와 같다(`extends NeutronAttributes<…>`, `self()`).

`LocalIpServiceImpl`: CRUD 여섯 줄(`/local_ips`, 루트 `local_ip`, 목록 `LocalIps`) + 
```java
    @Override
    public List<? extends LocalIpPortAssociation> portAssociations(String localIpId) {
        return listOf(Associations.class, PATH + "/" + id(localIpId) + "/port_associations", null);
    }

    @Override
    public LocalIpPortAssociation associatePort(String localIpId, String fixedPortId, String fixedIp) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fixed_port_id", id(fixedPortId));
        if (fixedIp != null)
            body.put("fixed_ip", fixedIp);
        return post(NeutronLocalIpPortAssociation.class, PATH + "/" + id(localIpId) + "/port_associations").entity(JsonBody.of("port_association", body)).execute();
    }

    @Override
    public ActionResponse disassociatePort(String localIpId, String fixedPortId) {
        return remove(PATH + "/" + id(localIpId) + "/port_associations/" + id(fixedPortId));
    }
```
`NdpProxyServiceImpl`: CRUD 여섯 줄(`/ndp_proxies`, 루트 `ndp_proxy`, 목록 `NdpProxies`). 인터페이스 2개, accessor 2개, binding 2개.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='LocalIpNdpTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/f9.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/f9.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/f9.log
git add -A && git commit -m "feat(networking): add local IPs with port associations and NDP proxies

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "localIps(): CRUD, port associations list/associate/disassociate. ndpProxies(): CRUD."
```

---

### Task 10: service flavors, service profiles

**Files:**
- Create: 모델 `ServiceFlavor.java`, `ServiceProfile.java`; 도메인 `NeutronServiceFlavor.java`, `NeutronServiceProfile.java`; 옵션 `ServiceFlavorOptions.java`, `ServiceProfileOptions.java`; `api/networking/ext/ServiceFlavorService.java`, `ServiceProfileService.java` + impl
- Modify: `NetworkingService`, `NetworkingServiceImpl`, `DefaultAPIProvider`
- Create: core-test `api/network/ext2/ServiceFlavorTests.java`

**Interfaces:**
- Produces:
  - `NetworkingService.serviceFlavors()` → `ServiceFlavorService`: CRUD(`create(ServiceFlavorOptions)`, `update(id, ServiceFlavorOptions)`), `ActionResponse associateProfile(String flavorId, String profileId)`, `ActionResponse disassociateProfile(String flavorId, String profileId)`
  - `NetworkingService.serviceProfiles()` → `ServiceProfileService`: CRUD(`create(ServiceProfileOptions)`, `update(id, ServiceProfileOptions)`)
  - `ServiceFlavor`: `getId()`, `getName()`, `getDescription()`, `getServiceType()`, `Boolean isEnabled()`, `List<String> getServiceProfiles()`
  - `ServiceProfile`: `getId()`, `getDescription()`, `getDriver()`, `getMetainfo()`, `Boolean isEnabled()`
  - `ServiceFlavorOptions`: `create(String name, String serviceType)`, `update()`; `name`, `description`, `enabled(Boolean)`
  - `ServiceProfileOptions`: `create()`, `update()`; `description`, `driver`, `metainfo`, `enabled(Boolean)`

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/f10-service-flavors
```

`ServiceFlavorTests.java`:
```java
package org.openstack4j.api.network.ext2;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.ServiceFlavor;
import org.openstack4j.model.network.ext.ServiceProfile;
import org.openstack4j.model.network.options.ServiceFlavorOptions;
import org.openstack4j.model.network.options.ServiceProfileOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/ServiceFlavors")
public class ServiceFlavorTests extends AbstractNetworkingExtTest {

    public void serviceFlavors() throws Exception {
        String flavor = "{\"description\": \"\", \"enabled\": true, \"service_profiles\": [\"sp1\"], \"service_type\": \"L3_ROUTER_NAT\", \"id\": \"f1\", \"name\": \"router-flavor\"}";
        respondWith(201, "{\"flavor\": " + flavor + "}");
        respondWith(200, "{\"flavors\": [" + flavor + "]}");
        respondWith(200, "{\"flavor\": " + flavor + "}");
        respondWith(200, "{\"flavor\": " + flavor + "}");
        respondWith(201, "{\"service_profile\": {\"id\": \"sp1\"}}");
        respondWith(204);
        respondWith(204);

        var flavors = osv3().networking().serviceFlavors();
        ServiceFlavor created = flavors.create(ServiceFlavorOptions.create("router-flavor", "L3_ROUTER_NAT"));
        flavors.list();
        flavors.get("f1");
        flavors.update("f1", ServiceFlavorOptions.update().enabled(false));
        Assert.assertTrue(flavors.associateProfile("f1", "sp1").isSuccess());
        Assert.assertTrue(flavors.disassociateProfile("f1", "sp1").isSuccess());
        flavors.delete("f1");

        RecordedRequest create = expect("POST", "/v2.0/flavors");
        Assert.assertEquals(body(create).get("flavor").get("service_type").asText(), "L3_ROUTER_NAT");
        expect("GET", "/v2.0/flavors");
        expect("GET", "/v2.0/flavors/f1");
        Assert.assertFalse(body(expect("PUT", "/v2.0/flavors/f1")).get("flavor").get("enabled").asBoolean());
        RecordedRequest associate = expect("POST", "/v2.0/flavors/f1/service_profiles");
        Assert.assertEquals(body(associate).get("service_profile").get("id").asText(), "sp1");
        expect("DELETE", "/v2.0/flavors/f1/service_profiles/sp1");
        expect("DELETE", "/v2.0/flavors/f1");
        Assert.assertEquals(created.getServiceProfiles(), java.util.List.of("sp1"));
        Assert.assertTrue(created.isEnabled());
    }

    public void serviceProfiles() throws Exception {
        String profile = "{\"enabled\": true, \"metainfo\": \"{'foo': 'bar'}\", \"driver\": \"neutron.services.l3_router.service_providers.single_node.SingleNodeDriver\", \"id\": \"sp1\", \"description\": \"d\"}";
        respondWith(201, "{\"service_profile\": " + profile + "}");
        respondWith(200, "{\"service_profiles\": [" + profile + "]}");
        respondWith(200, "{\"service_profile\": " + profile + "}");
        respondWith(200, "{\"service_profile\": " + profile + "}");
        respondWith(204);

        var profiles = osv3().networking().serviceProfiles();
        ServiceProfile created = profiles.create(ServiceProfileOptions.create().driver("neutron.services.l3_router.service_providers.single_node.SingleNodeDriver").description("d"));
        profiles.list();
        profiles.get("sp1");
        profiles.update("sp1", ServiceProfileOptions.update().enabled(false));
        profiles.delete("sp1");

        RecordedRequest create = expect("POST", "/v2.0/service_profiles");
        Assert.assertEquals(body(create).get("service_profile").get("description").asText(), "d");
        expect("GET", "/v2.0/service_profiles");
        expect("GET", "/v2.0/service_profiles/sp1");
        expect("PUT", "/v2.0/service_profiles/sp1");
        expect("DELETE", "/v2.0/service_profiles/sp1");
        Assert.assertTrue(created.getDriver().endsWith("SingleNodeDriver"));
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `serviceFlavors()` 등 없음.

- [ ] **Step 3: 모델 생성**

`/tmp/f10.json`:
```json
[
 {"iface": "ServiceFlavor", "cls": "NeutronServiceFlavor", "doc": "A Neutron service flavor (for example a router flavor), not a compute flavor.", "root": "flavor", "list": "flavors", "listCls": "Flavors",
  "fields": [["id", "getId", "String"], ["name", "getName", "String"], ["description", "getDescription", "String"], ["service_type", "getServiceType", "String"],
             ["enabled", "isEnabled", "Boolean"], ["service_profiles", "getServiceProfiles", "List<String>"]]},
 {"iface": "ServiceProfile", "cls": "NeutronServiceProfile", "doc": "A service profile: the driver and metadata behind a service flavor.", "root": "service_profile", "list": "service_profiles", "listCls": "ServiceProfiles",
  "fields": [["id", "getId", "String"], ["description", "getDescription", "String"], ["driver", "getDriver", "String"], ["metainfo", "getMetainfo", "String"], ["enabled", "isEnabled", "Boolean"]]}
]
```

- [ ] **Step 4: 옵션과 서비스 구현**

`ServiceFlavorOptions`(`create(String name, String serviceType)` → `name`, `service_type`; `update()`; `name`, `description`, `enabled(Boolean)`), `ServiceProfileOptions`(`create()`/`update()`; `description`, `driver`, `metainfo`, `enabled(Boolean)`) — 형태는 Task 8 `SegmentOptions` 와 같다.

`ServiceFlavorServiceImpl`: CRUD 여섯 줄(`/flavors`, 루트 `flavor`, 목록 `Flavors`) +
```java
    @Override
    public ActionResponse associateProfile(String flavorId, String profileId) {
        return postWithResponse(PATH + "/" + id(flavorId) + "/service_profiles")
                .entity(JsonBody.of("service_profile", Collections.singletonMap("id", id(profileId)))).execute();
    }

    @Override
    public ActionResponse disassociateProfile(String flavorId, String profileId) {
        return remove(PATH + "/" + id(flavorId) + "/service_profiles/" + id(profileId));
    }
```
`ServiceProfileServiceImpl`: CRUD 여섯 줄(`/service_profiles`, 루트 `service_profile`, 목록 `ServiceProfiles`). 인터페이스 2개(Javadoc: 관리자 전용, `flavors` extension), accessor 2개, binding 2개.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ServiceFlavorTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/f10.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/f10.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/f10.log
git add -A && git commit -m "feat(networking): add service flavors and service profiles

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "serviceFlavors(): CRUD, profile associate/disassociate. serviceProfiles(): CRUD."
```

---

### Task 11: metering, logging

**Files:**
- Create: 모델 `MeteringLabel.java`, `MeteringLabelRule.java`, `NetworkLog.java`; 도메인 `NeutronMeteringLabel.java`, `NeutronMeteringLabelRule.java`, `NeutronNetworkLog.java`, `NeutronLoggableResources.java`; 옵션 `MeteringLabelOptions.java`, `MeteringLabelRuleOptions.java`, `NetworkLogOptions.java`; `api/networking/ext/MeteringService.java`, `NetworkLoggingService.java` + impl
- Modify: `NetworkingService`, `NetworkingServiceImpl`, `DefaultAPIProvider`
- Create: core-test `api/network/ext2/MeteringLoggingTests.java`

**Interfaces:**
- Produces:
  - `NetworkingService.metering()` → `MeteringService`: `listLabels()`, `listLabels(Map)`, `getLabel(id)`, `createLabel(MeteringLabelOptions)`, `deleteLabel(id)`, `listRules()`, `listRules(Map)`, `getRule(id)`, `createRule(MeteringLabelRuleOptions)`, `deleteRule(id)`
  - `NetworkingService.logging()` → `NetworkLoggingService`: `list()`, `list(Map)`, `get(id)`, `create(NetworkLogOptions)`, `update(id, NetworkLogOptions)`, `delete(id)`, `List<String> loggableResources()`
  - `MeteringLabel`: `getId()`, `getName()`, `getDescription()`, `getProjectId()`, `Boolean isShared()`
  - `MeteringLabelRule`: `getId()`, `getMeteringLabelId()`, `getDirection()`, `getRemoteIpPrefix()`, `getSourceIpPrefix()`, `getDestinationIpPrefix()`, `Boolean isExcluded()`
  - `NetworkLog`: `getId()`, `getName()`, `getDescription()`, `getProjectId()`, `Boolean isEnabled()`, `getResourceType()`, `getResourceId()`, `getTargetId()`, `getEvent()`, `Integer getRevisionNumber()`
  - `MeteringLabelOptions.create(String name)`: `description`, `shared(Boolean)`, `projectId`
  - `MeteringLabelRuleOptions.create(String meteringLabelId, String direction)`: `remoteIpPrefix`, `sourceIpPrefix`, `destinationIpPrefix`, `excluded(Boolean)`
  - `NetworkLogOptions`: `create(String resourceType)`, `update()`; `name`, `description`, `enabled(Boolean)`, `resourceId`, `targetId`, `event`(ALL/ACCEPT/DROP)

- [ ] **Step 1: 실패하는 테스트 작성**

```bash
git switch main && git pull && git switch -c task/f11-metering-logging
```

`MeteringLoggingTests.java`:
```java
package org.openstack4j.api.network.ext2;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.MeteringLabel;
import org.openstack4j.model.network.ext.MeteringLabelRule;
import org.openstack4j.model.network.ext.NetworkLog;
import org.openstack4j.model.network.options.MeteringLabelOptions;
import org.openstack4j.model.network.options.MeteringLabelRuleOptions;
import org.openstack4j.model.network.options.NetworkLogOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/MeteringLogging")
public class MeteringLoggingTests extends AbstractNetworkingExtTest {

    public void metering() throws Exception {
        String label = "{\"project_id\": \"" + PROJECT + "\", \"description\": \"d\", \"name\": \"label1\", \"id\": \"ml1\", \"shared\": false}";
        String rule = "{\"remote_ip_prefix\": \"10.0.1.0/24\", \"direction\": \"ingress\", \"metering_label_id\": \"ml1\", \"id\": \"mr1\", \"excluded\": false}";
        respondWith(201, "{\"metering_label\": " + label + "}");
        respondWith(200, "{\"metering_labels\": [" + label + "]}");
        respondWith(200, "{\"metering_label\": " + label + "}");
        respondWith(201, "{\"metering_label_rule\": " + rule + "}");
        respondWith(200, "{\"metering_label_rules\": [" + rule + "]}");
        respondWith(200, "{\"metering_label_rule\": " + rule + "}");
        respondWith(204);
        respondWith(204);

        var metering = osv3().networking().metering();
        MeteringLabel created = metering.createLabel(MeteringLabelOptions.create("label1").description("d"));
        metering.listLabels();
        metering.getLabel("ml1");
        MeteringLabelRule createdRule = metering.createRule(MeteringLabelRuleOptions.create("ml1", "ingress").remoteIpPrefix("10.0.1.0/24"));
        List<? extends MeteringLabelRule> rules = metering.listRules();
        metering.getRule("mr1");
        metering.deleteRule("mr1");
        metering.deleteLabel("ml1");

        RecordedRequest createLabel = expect("POST", "/v2.0/metering/metering-labels");
        Assert.assertEquals(body(createLabel).get("metering_label").get("name").asText(), "label1");
        expect("GET", "/v2.0/metering/metering-labels");
        expect("GET", "/v2.0/metering/metering-labels/ml1");
        RecordedRequest createRule = expect("POST", "/v2.0/metering/metering-label-rules");
        Assert.assertEquals(body(createRule).get("metering_label_rule").get("metering_label_id").asText(), "ml1");
        Assert.assertFalse(body(createRule).get("metering_label_rule").has("excluded"));
        expect("GET", "/v2.0/metering/metering-label-rules");
        expect("GET", "/v2.0/metering/metering-label-rules/mr1");
        expect("DELETE", "/v2.0/metering/metering-label-rules/mr1");
        expect("DELETE", "/v2.0/metering/metering-labels/ml1");
        Assert.assertFalse(created.isShared());
        Assert.assertEquals(createdRule.getDirection(), "ingress");
        Assert.assertFalse(rules.get(0).isExcluded());
    }

    public void logging() throws Exception {
        String log = "{\"name\": \"security group log\", \"description\": \"\", \"id\": \"lg1\", \"project_id\": \"" + PROJECT + "\", \"enabled\": true, \"revision_number\": 1,"
                + " \"resource_type\": \"security_group\", \"resource_id\": null, \"target_id\": null, \"event\": \"ALL\"}";
        respondWith(201, "{\"log\": " + log + "}");
        respondWith(200, "{\"logs\": [" + log + "]}");
        respondWith(200, "{\"log\": " + log + "}");
        respondWith(200, "{\"log\": " + log + "}");
        respondWith(204);
        respondWith(200, "{\"loggable_resources\": [{\"type\": \"security_group\"}, {\"type\": \"firewall_group\"}]}");

        var logging = osv3().networking().logging();
        NetworkLog created = logging.create(NetworkLogOptions.create("security_group").name("security group log").event("ALL"));
        logging.list();
        logging.get("lg1");
        logging.update("lg1", NetworkLogOptions.update().enabled(false));
        logging.delete("lg1");
        List<String> types = logging.loggableResources();

        RecordedRequest create = expect("POST", "/v2.0/log/logs");
        Assert.assertEquals(body(create).get("log").get("resource_type").asText(), "security_group");
        expect("GET", "/v2.0/log/logs");
        expect("GET", "/v2.0/log/logs/lg1");
        Assert.assertFalse(body(expect("PUT", "/v2.0/log/logs/lg1")).get("log").get("enabled").asBoolean());
        expect("DELETE", "/v2.0/log/logs/lg1");
        expect("GET", "/v2.0/log/loggable-resources");
        Assert.assertEquals(created.getEvent(), "ALL");
        Assert.assertNull(created.getResourceId());
        Assert.assertEquals(types, List.of("security_group", "firewall_group"));
    }
}
```

- [ ] **Step 2: 실행 → 컴파일 실패 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test 2>&1 | grep -A1 'cannot find symbol' | grep 'symbol:' | sort -u | head -4`
Expected: `metering()`, `logging()` 등 없음.

- [ ] **Step 3: 모델 생성**

`/tmp/f11.json`:
```json
[
 {"iface": "MeteringLabel", "cls": "NeutronMeteringLabel", "doc": "A metering label counting router traffic.", "root": "metering_label", "list": "metering_labels", "listCls": "MeteringLabels",
  "fields": [["id", "getId", "String"], ["name", "getName", "String"], ["description", "getDescription", "String"], ["project_id", "getProjectId", "String"], ["shared", "isShared", "Boolean"]]},
 {"iface": "MeteringLabelRule", "cls": "NeutronMeteringLabelRule", "doc": "A metering label rule selecting the traffic a label counts.", "root": "metering_label_rule", "list": "metering_label_rules", "listCls": "MeteringLabelRules",
  "fields": [["id", "getId", "String"], ["metering_label_id", "getMeteringLabelId", "String"], ["direction", "getDirection", "String"], ["remote_ip_prefix", "getRemoteIpPrefix", "String"],
             ["source_ip_prefix", "getSourceIpPrefix", "String"], ["destination_ip_prefix", "getDestinationIpPrefix", "String"], ["excluded", "isExcluded", "Boolean"]]},
 {"iface": "NetworkLog", "cls": "NeutronNetworkLog", "doc": "A network log (security group or firewall group logging).", "root": "log", "list": "logs", "listCls": "Logs",
  "fields": [["id", "getId", "String"], ["name", "getName", "String"], ["description", "getDescription", "String"], ["project_id", "getProjectId", "String"],
             ["enabled", "isEnabled", "Boolean"], ["resource_type", "getResourceType", "String"], ["resource_id", "getResourceId", "String"],
             ["target_id", "getTargetId", "String"], ["event", "getEvent", "String"], ["revision_number", "getRevisionNumber", "Integer"]]}
]
```
`NeutronLoggableResources`(직접 작성): `@JsonProperty("loggable_resources") List<Map<String, String>> resources;` + `List<String> getTypes()`(각 항목의 `type`).

- [ ] **Step 4: 옵션과 서비스 구현**

옵션 3개(형태는 Task 8 `SegmentOptions`): `MeteringLabelOptions.create(String name)` → `name`; `description`, `shared(Boolean)`, `projectId`. `MeteringLabelRuleOptions.create(String meteringLabelId, String direction)` → `metering_label_id`, `direction`; `remoteIpPrefix`, `sourceIpPrefix`, `destinationIpPrefix`, `excluded(Boolean)`. `NetworkLogOptions.create(String resourceType)` → `resource_type`; `update()`; `name`, `description`, `enabled(Boolean)`, `resourceId`, `targetId`, `event`.

`MeteringServiceImpl`:
```java
package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.MeteringService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.MeteringLabel;
import org.openstack4j.model.network.ext.MeteringLabelRule;
import org.openstack4j.model.network.options.MeteringLabelOptions;
import org.openstack4j.model.network.options.MeteringLabelRuleOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronMeteringLabel;
import org.openstack4j.openstack.networking.domain.ext.NeutronMeteringLabel.MeteringLabels;
import org.openstack4j.openstack.networking.domain.ext.NeutronMeteringLabelRule;
import org.openstack4j.openstack.networking.domain.ext.NeutronMeteringLabelRule.MeteringLabelRules;

public class MeteringServiceImpl extends BaseNeutronExtService implements MeteringService {

    private static final String LABELS = "/metering/metering-labels";
    private static final String RULES = "/metering/metering-label-rules";

    @Override public List<? extends MeteringLabel> listLabels() { return listOf(MeteringLabels.class, LABELS, null); }
    @Override public List<? extends MeteringLabel> listLabels(Map<String, String> filters) { return listOf(MeteringLabels.class, LABELS, filters); }
    @Override public MeteringLabel getLabel(String id) { return show(NeutronMeteringLabel.class, LABELS + "/" + id(id)); }
    @Override public MeteringLabel createLabel(MeteringLabelOptions options) { return create(NeutronMeteringLabel.class, LABELS, "metering_label", options); }
    @Override public ActionResponse deleteLabel(String id) { return remove(LABELS + "/" + id(id)); }
    @Override public List<? extends MeteringLabelRule> listRules() { return listOf(MeteringLabelRules.class, RULES, null); }
    @Override public List<? extends MeteringLabelRule> listRules(Map<String, String> filters) { return listOf(MeteringLabelRules.class, RULES, filters); }
    @Override public MeteringLabelRule getRule(String id) { return show(NeutronMeteringLabelRule.class, RULES + "/" + id(id)); }
    @Override public MeteringLabelRule createRule(MeteringLabelRuleOptions options) { return create(NeutronMeteringLabelRule.class, RULES, "metering_label_rule", options); }
    @Override public ActionResponse deleteRule(String id) { return remove(RULES + "/" + id(id)); }
}
```
`NetworkLoggingServiceImpl`: CRUD 여섯 줄(`/log/logs`, 루트 `log`, 목록 `Logs`) +
```java
    @Override
    public List<String> loggableResources() {
        NeutronLoggableResources resources = show(NeutronLoggableResources.class, "/log/loggable-resources");
        return resources == null ? Collections.emptyList() : resources.getTypes();
    }
```
인터페이스 2개, accessor 2개, binding 2개.

- [ ] **Step 5: 통과 확인**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && ./mvnw -B test -pl connectors/httpclient -Dsurefire.failIfNoSpecifiedTests=false -Dtest='MeteringLoggingTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|FAIL' | tail -2`
Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/f11.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/f11.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/f11.log
git add -A && git commit -m "feat(networking): add metering labels/rules and network logging

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "metering(): label·rule list/get/create/delete. logging(): CRUD, loggable resources."
```

---

### Task 12: 실환경 통합 테스트와 문서

**Files:**
- Create: core-test `api/network/ext2/NetworkingExtensionsLiveTests.java`
- Modify: `README.md`(새 `## Networking 확장` 절), `MIGRATION.md`(`# 4.4 → 4.5`), `CHANGELOG.md`(`## 4.5.0`)

**Interfaces:**
- Consumes: Task 1~11 의 공개 API.
- 환경 변수: `IdentityExtensionsLiveTests` 와 같다(`OS_AUTH_URL`, `OS_TOKEN` 또는 `OS_USERNAME`/`OS_PASSWORD`, `OS_PROJECT_NAME`, `OS_USER_DOMAIN_NAME`/`OS_PROJECT_DOMAIN_NAME`).

- [ ] **Step 1: 실환경 테스트 작성**

`NetworkingExtensionsLiveTests.java`:
```java
package org.openstack4j.api.network.ext2;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.ext.AddressGroup;
import org.openstack4j.model.network.ext.AddressScope;
import org.openstack4j.model.network.ext.QosDscpMarkingRule;
import org.openstack4j.model.network.ext.RbacPolicy;
import org.openstack4j.model.network.ext.ServiceFlavor;
import org.openstack4j.model.network.ext.ServiceProfile;
import org.openstack4j.model.network.ext.SubnetPool;
import org.openstack4j.model.network.ext.NetQosPolicy;
import org.openstack4j.model.network.options.AddressGroupOptions;
import org.openstack4j.model.network.options.AddressScopeOptions;
import org.openstack4j.model.network.options.QosRuleOptions;
import org.openstack4j.model.network.options.RbacPolicyOptions;
import org.openstack4j.model.network.options.ServiceFlavorOptions;
import org.openstack4j.model.network.options.ServiceProfileOptions;
import org.openstack4j.model.network.options.SubnetPoolOptions;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.networking.domain.NeutronHostRoute;
import org.openstack4j.openstack.networking.domain.ext.NeutronNetQosPolicy;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Runs against a real Neutron when OS_AUTH_URL is set; every temporary resource is removed in finally. */
@Test(suiteName = "Network/Ext2/Live", groups = "networking-live", singleThreaded = true)
public class NetworkingExtensionsLiveTests {

    private OSClientV3 os;
    private String projectId;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live networking tests");
        String authUrl = url.replaceAll("/+$", "").endsWith("/v3") ? url.replaceAll("/+$", "") : url.replaceAll("/+$", "") + "/v3";
        String token = System.getenv("OS_TOKEN");
        Identifier project = Identifier.byName(env("OS_PROJECT_NAME", null));
        Identifier projectDomain = Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default"));
        os = token != null && !token.isEmpty()
                ? OSFactory.builderV3().endpoint(authUrl).token(token).scopeToProject(project, projectDomain).authenticate()
                : OSFactory.builderV3().endpoint(authUrl).credentials(env("OS_USERNAME", null), env("OS_PASSWORD", null),
                Identifier.byName(env("OS_USER_DOMAIN_NAME", "Default"))).scopeToProject(project, projectDomain).authenticate();
        projectId = os.getToken().getProject().getId();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live networking tests");
        }
        return value;
    }

    private void requireExtension(String alias) {
        if (!os.networking().extensions().isEnabled(alias))
            throw new SkipException(alias + " is not enabled on this Neutron");
    }

    public void discovery() {
        Assert.assertTrue(os.networking().extensions().isEnabled("qos"));
        Assert.assertFalse(os.networking().serviceProviders().list().isEmpty());
        Assert.assertNotNull(os.networking().floatingip().listPools());
        Assert.assertTrue(os.networking().quotas().getDefault(projectId).getNetwork() != 0);
        Assert.assertNotNull(os.networking().quotas().getDetails(projectId).get("network").getLimit());
        Assert.assertFalse(os.networking().defaultSecurityGroupRules().list().isEmpty());
        Assert.assertFalse(os.networking().qosRules().ruleTypes().isEmpty());
    }

    public void qosPolicyRules() {
        NetQosPolicy policy = os.networking().netQosPolicy().create(NeutronNetQosPolicy.builder().name("os4j-live-qos").build());
        try {
            QosDscpMarkingRule dscp = os.networking().qosRules().createDscpMarkingRule(policy.getId(), QosRuleOptions.dscpMarking(26));
            Assert.assertEquals(os.networking().qosRules().getDscpMarkingRule(policy.getId(), dscp.getId()).getDscpMark(), Integer.valueOf(26));
            os.networking().qosRules().createMinimumBandwidthRule(policy.getId(), QosRuleOptions.minimumBandwidth(1000).direction("egress"));
            Assert.assertEquals(os.networking().qosRules().listMinimumBandwidthRules(policy.getId()).size(), 1);
        } finally {
            Assert.assertTrue(os.networking().netQosPolicy().delete(policy.getId()).isSuccess());
        }
    }

    public void addressScopeAndSubnetPool() {
        AddressScope scope = os.networking().addressScopes().create(AddressScopeOptions.create("os4j-live-scope", 4));
        try {
            SubnetPool pool = os.networking().subnetPools().create(SubnetPoolOptions.create("os4j-live-pool", List.of("10.250.0.0/24"))
                    .defaultPrefixlen(28).addressScopeId(scope.getId()));
            try {
                List<String> prefixes = os.networking().subnetPools().addPrefixes(pool.getId(), List.of("10.250.1.0/24"));
                Assert.assertTrue(prefixes.contains("10.250.0.0/23") || prefixes.size() == 2, prefixes.toString());
                os.networking().subnetPools().removePrefixes(pool.getId(), List.of("10.250.1.0/24"));
            } finally {
                Assert.assertTrue(os.networking().subnetPools().delete(pool.getId()).isSuccess());
            }
        } finally {
            Assert.assertTrue(os.networking().addressScopes().delete(scope.getId()).isSuccess());
        }
    }

    public void addressGroup() {
        AddressGroup group = os.networking().addressGroups().create(AddressGroupOptions.create("os4j-live-group"));
        try {
            Assert.assertEquals(os.networking().addressGroups().addAddresses(group.getId(), List.of("10.251.0.10/32")).getAddresses(), List.of("10.251.0.10/32"));
            Assert.assertTrue(os.networking().addressGroups().removeAddresses(group.getId(), List.of("10.251.0.10/32")).getAddresses().isEmpty());
        } finally {
            Assert.assertTrue(os.networking().addressGroups().delete(group.getId()).isSuccess());
        }
    }

    public void rbacOnTemporaryNetwork() {
        Network network = os.networking().network().create(Builders.network().name("os4j-live-net").adminStateUp(true).build());
        try {
            RbacPolicy policy = os.networking().rbacPolicies().create(RbacPolicyOptions.create("network", network.getId(), "access_as_shared", projectId));
            try {
                Assert.assertEquals(os.networking().rbacPolicies().get(policy.getId()).getObjectId(), network.getId());
            } finally {
                Assert.assertTrue(os.networking().rbacPolicies().delete(policy.getId()).isSuccess());
            }
        } finally {
            Assert.assertTrue(os.networking().network().delete(network.getId()).isSuccess());
        }
    }

    /** add/remove_extraroutes need extraroute-atomic; the dev Neutron (OVN) only has extraroute, so this skips there. */
    public void routerExtraRoutes() {
        requireExtension("extraroute-atomic");
        Router router = os.networking().router().create(Builders.router().name("os4j-live-router").adminStateUp(true).build());
        try {
            // a nexthop must be on a router subnet; without interfaces Neutron rejects it, which still proves the request shape
            try {
                os.networking().router().addExtraRoutes(router.getId(), List.of(new NeutronHostRoute("10.252.0.0/24", "10.252.1.1")));
            } catch (RuntimeException expected) {
                Assert.assertTrue(String.valueOf(expected.getMessage()).toLowerCase().contains("nexthop"), expected.getMessage());
            }
            Assert.assertTrue(os.networking().router().removeExtraRoutes(router.getId(), List.of()).getRoutes().isEmpty());
        } finally {
            Assert.assertTrue(os.networking().router().delete(router.getId()).isSuccess());
        }
    }

    public void serviceFlavorAndProfile() {
        requireExtension("flavors");
        ServiceProfile profile = os.networking().serviceProfiles().create(ServiceProfileOptions.create().description("os4j-live-profile").metainfo("{}"));
        try {
            ServiceFlavor flavor = os.networking().serviceFlavors().create(ServiceFlavorOptions.create("os4j-live-flavor", "L3_ROUTER_NAT"));
            try {
                Assert.assertTrue(os.networking().serviceFlavors().associateProfile(flavor.getId(), profile.getId()).isSuccess());
                Assert.assertTrue(os.networking().serviceFlavors().disassociateProfile(flavor.getId(), profile.getId()).isSuccess());
            } finally {
                Assert.assertTrue(os.networking().serviceFlavors().delete(flavor.getId()).isSuccess());
            }
        } finally {
            Assert.assertTrue(os.networking().serviceProfiles().delete(profile.getId()).isSuccess());
        }
    }

    public void portBindingsOfExistingPort() {
        var ports = os.networking().port().list();
        if (ports.isEmpty())
            throw new SkipException("no port to inspect");
        Assert.assertNotNull(os.networking().port().listBindings(ports.get(0).getId()));
    }

    public void autoAllocatedTopologyDryRunReportsMissingExternalNetwork() {
        try {
            Assert.assertEquals(os.networking().autoAllocatedTopology().validate(projectId).getDryRun(), "pass");
        } catch (RuntimeException expected) {
            Assert.assertTrue(String.valueOf(expected.getMessage()).contains("external"), expected.getMessage());
        }
    }
}
```
(`Builders.network()`, `Builders.router()` 의 메서드 이름은 기존 빌더에서 확인한다. service profile 은 driver 를 비우면 Neutron 이 기본 driver 를 쓰지 않을 수 있다 — 실패하면 `service-providers` 의 L3_ROUTER_NAT driver 를 넣거나 metainfo 만 주는 대신 driver 를 지정하고 Ruling 으로 남긴다.)

- [ ] **Step 2: 실환경 실행 (개발용 Neutron, 저장된 admin token)**

Run: `./mvnw -B -q install -DskipTests -pl core,core-test && OS_TOKEN=$(cat "$SCRATCH/token.txt") OS_AUTH_URL=http://192.168.140.12:5000/v3 OS_PROJECT_NAME=admin ./mvnw -B test -pl connectors/okhttp -Dsurefire.failIfNoSpecifiedTests=false -Dtest='NetworkingExtensionsLiveTests' -DdependenciesToScan=io.github.seogineer:openstack4j-core-test 2>&1 | grep -E 'Tests run:|LiveTests\.' | tail -6`(`SCRATCH` = 세션 scratchpad; 토큰 만료 시 사용자에게 새 토큰을 요청하지 않고 password 경로는 쓰지 않는다 — 실행하지 못하면 그 사실을 보고한다)
Expected: `Failures: 0, Errors: 0`. 이어서 curl 로 `os4j-live` 이름의 qos policy·address scope·subnet pool·address group·network·router·flavor·service profile 이 남지 않았는지 확인한다.

- [ ] **Step 3: 문서**

`README.md` — `## 빌드` 앞에 새 절:
```markdown
## Networking 확장

4.5.0 부터 Neutron 본체(in-tree)의 API 를 대부분 지원합니다. 기존 메서드와 요청 본문은 바뀌지 않습니다.

```java
os.networking().extensions().isEnabled("qos");
os.networking().qosRules().createDscpMarkingRule(policyId, QosRuleOptions.dscpMarking(26));
SubnetPool pool = os.networking().subnetPools().create(SubnetPoolOptions.create("pool", List.of("10.0.0.0/16")).defaultPrefixlen(24));
os.networking().addressGroups().addAddresses(groupId, List.of("10.0.2.100/32"));
os.networking().rbacPolicies().create(RbacPolicyOptions.create("network", networkId, "access_as_shared", projectId));
os.networking().router().addExtraRoutes(routerId, routes);
os.networking().port().listBindings(portId);
```

- 새 accessor: `extensions()`, `serviceProviders()`, `autoAllocatedTopology()`, `qosRules()`, `subnetPools()`, `addressScopes()`, `addressGroups()`, `rbacPolicies()`, `defaultSecurityGroupRules()`, `securityGroupDefaultStatefulness()`, `segments()`, `networkSegmentRanges()`, `localIps()`, `ndpProxies()`, `serviceFlavors()`, `serviceProfiles()`, `metering()`, `logging()`
- 기존 서비스 보강: router(extra routes, external gateways, conntrack helpers, l3 agents), agent(스케줄링), port(bindings), quotas(default, details), floating IP pools, port forwarding update
- 옵션 클래스(`*Options`)는 설정한 필드만 보냅니다. 값을 지우려면 `attribute("field", null)`.
- 해당 extension 이 꺼진 Neutron 은 404 를 돌려줍니다. `extensions().isEnabled(alias)` 로 먼저 확인하세요.
- VPNaaS, FWaaS v2, BGP, BGPVPN, TaaS, SFC(stadium 프로젝트)는 다음 릴리스에서 다룹니다.
```
`MIGRATION.md` 끝에 `# 4.4 → 4.5`: 런타임 동작 변경 없음; `NetworkingService`, `RouterService`, `AgentService`, `PortService`, `NetQuotaService`, `NetFloatingIPService`, `PortForwardingService` 에 추상 메서드가 추가되어 직접 구현한 가짜 구현은 새 메서드가 필요; `Router` 에는 `default` getter 만 추가; `NeutronRouter` 의 `external_gateways` 는 응답 전용이라 router 생성·수정 본문 불변.
`CHANGELOG.md` 맨 위 `## 4.5.0`: 새 accessor 18 개와 기존 서비스 보강 항목, `BaseNeutronExtService`/`NeutronAttributes`, `NetworkingExtensionsLiveTests`.

- [ ] **Step 4: 전체 빌드, 커밋, PR**

```bash
./mvnw -B --no-transfer-progress install > /tmp/f12.log 2>&1; grep -E 'Tests run: [0-9]+, Failures|BUILD' /tmp/f12.log | tail -4; grep -q 'BUILD SUCCESS' /tmp/f12.log
git add -A && git commit -m "test(networking): add live Neutron extension tests; docs for networking extensions

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "NetworkingExtensionsLiveTests(개발 Neutron 에서 통과, 임시 자원 없음), README networking 확장 절, MIGRATION 4.4→4.5, CHANGELOG 4.5.0."
```

---

### Task 13: 전체 리뷰, 4.5.0 릴리스, 4.6.0-SNAPSHOT

- [ ] **Step 1: 최종 리뷰** — `review-package PLAN_FILE <F 시작 커밋> HEAD` 로 패키지를 만들고 opus 리뷰어에게 code-reviewer 템플릿, 이 계획서·스펙 경로, Review Focus 5개, ledger 의 `Ruling:` 줄을 준다. Critical/Important 는 한 번의 수정 패스(각각 RED→GREEN 테스트 + 전체 빌드)로 고치고 PR 로 머지한다. Minor 는 ledger 에 `Final: minor (deferred)` 로 남긴다.

- [ ] **Step 2: main CI 확인 후 태그**

```bash
git switch main && git pull
gh run list -R seogineer/openstack4j --branch main --limit 1 --json status,conclusion,headSha
git tag v4.5.0 && git push origin v4.5.0
gh run watch -R seogineer/openstack4j $(gh run list -R seogineer/openstack4j --workflow release.yml --limit 1 --json databaseId -q '.[0].databaseId') --exit-status
```
Expected: main 최신 커밋 CI `success`, release workflow `success`.

- [ ] **Step 3: 다음 SNAPSHOT**

```bash
git switch -c chore/4.6.0-snapshot
./mvnw -B -q versions:set -DnewVersion=4.6.0-SNAPSHOT -DgenerateBackupPoms=false
sed -i 's/4\.5\.0-SNAPSHOT/4.6.0-SNAPSHOT/g' examples/spring-boot-smoke/pom.xml
grep -rln "4.5.0-SNAPSHOT" --include=pom.xml . ; ./mvnw -B -q install -DskipTests -pl .,connectors,core,core-test
git add -A && git commit -m "chore: bump to 4.6.0-SNAPSHOT

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UsCoovUSWcrRDpjDWYNkzt"
.superpowers/pr.sh "4.5.0 릴리스 후 4.6.0-SNAPSHOT."
```
Expected: `grep` 출력 없음.

- [ ] **Step 4: Central 확인** — `~/openstack4j-check/pom.xml` 을 4.5.0 으로 바꾸고 `Check.java` 에 `os.networking().extensions().list().size()`, `os.networking().qosRules().ruleTypes()`, `os.networking().quotas().getDetails(projectId)` 출력을 더해 `OS_TOKEN=… ./run.sh` 로 실행한다(Central 반영은 `curl -s -o /dev/null -w '%{http_code}' https://repo1.maven.org/maven2/io/github/seogineer/openstack4j-core/4.5.0/openstack4j-core-4.5.0.pom` 이 200 일 때까지 기다린다).
Expected: 의존성 해석 성공, extension 수 85, rule types 3.

- [ ] **Step 5: 메모리 갱신** — `project_openstack4j_fork.md` 에 F 완료(4.5.0), deferred minors, 다음 F2(stadium) 또는 G(Manila)를 적는다.
