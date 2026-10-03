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
