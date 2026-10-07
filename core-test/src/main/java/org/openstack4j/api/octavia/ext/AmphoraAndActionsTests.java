package org.openstack4j.api.octavia.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.octavia.ext.Amphora;
import org.openstack4j.model.octavia.ext.AmphoraStats;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Octavia/Ext/Amphorae")
public class AmphoraAndActionsTests extends AbstractOctaviaExtTest {

    private static final String AMP = "{\"id\": \"6bd55cd3\", \"load_balancer_id\": \"09eedfc6\", \"compute_id\": \"f0f79f90\", \"lb_network_ip\": \"192.168.1.2\", \"vrrp_ip\": \"192.168.1.5\","
            + " \"ha_ip\": \"192.168.1.10\", \"role\": \"MASTER\", \"status\": \"ALLOCATED\", \"vrrp_id\": 1, \"vrrp_priority\": 100, \"cached_zone\": \"zone1\", \"image_id\": \"c1c2ad6f\"}";

    public void amphoraPaths() throws Exception {
        respondWith(200, "{\"amphorae\": [" + AMP + "]}");
        respondWith(200, "{\"amphora\": " + AMP + "}");
        respondWith(200, "{\"amphora_stats\": [{\"active_connections\": 48629, \"bytes_in\": 65671420, \"bytes_out\": 774771186, \"id\": \"6bd55cd3\", \"listener_id\": \"bbe44114\","
                + " \"loadbalancer_id\": \"65b5a7c3\", \"request_errors\": 0, \"total_connections\": 26189172}]}");
        respondWith(202);
        respondWith(202);
        respondWith(204);

        var amphorae = osv3().octavia().amphorae();
        List<? extends Amphora> all = amphorae.list();
        Amphora one = amphorae.get("6bd55cd3");
        List<? extends AmphoraStats> stats = amphorae.stats("6bd55cd3");
        Assert.assertTrue(amphorae.configure("6bd55cd3").isSuccess());
        Assert.assertTrue(amphorae.failover("6bd55cd3").isSuccess());
        Assert.assertTrue(amphorae.delete("6bd55cd3").isSuccess());

        expect("GET", "/v2.0/octavia/amphorae");
        expect("GET", "/v2.0/octavia/amphorae/6bd55cd3");
        expect("GET", "/v2.0/octavia/amphorae/6bd55cd3/stats");
        expect("PUT", "/v2.0/octavia/amphorae/6bd55cd3/config");
        expect("PUT", "/v2.0/octavia/amphorae/6bd55cd3/failover");
        expect("DELETE", "/v2.0/octavia/amphorae/6bd55cd3");
        Assert.assertEquals(all.get(0).getRole(), "MASTER");
        Assert.assertEquals(one.getVrrpPriority(), Integer.valueOf(100));
        Assert.assertEquals(stats.get(0).getTotalConnections(), Long.valueOf(26189172));
    }

    public void listenerStatsAndFailover() throws Exception {
        respondWith(200, "{\"stats\": {\"bytes_in\": 65671420, \"total_connections\": 26189172, \"active_connections\": 48629, \"bytes_out\": 774771186, \"request_errors\": 0}}");
        respondWith(202);

        Assert.assertEquals(osv3().octavia().listenerV2().stats("l1").getTotalConnections(), Integer.valueOf(26189172));
        Assert.assertTrue(osv3().octavia().loadBalancerV2().failover("lb1").isSuccess());

        expect("GET", "/v2.0/lbaas/listeners/l1/stats");
        expect("PUT", "/v2.0/lbaas/loadbalancers/lb1/failover");
    }

    public void batchUpdateMembers() throws Exception {
        respondWith(202);
        respondWith(202);
        List<Map<String, Object>> members = List.of(Map.of("address", "192.0.2.16", "protocol_port", 80, "weight", 20));

        Assert.assertTrue(osv3().octavia().lbPoolV2().updateMembers("p1", members, false).isSuccess());
        osv3().octavia().lbPoolV2().updateMembers("p1", members, true);

        RecordedRequest replace = expect("PUT", "/v2.0/lbaas/pools/p1/members");
        Assert.assertEquals(body(replace).get("members").get(0).get("address").asText(), "192.0.2.16");
        Assert.assertTrue(decodedPath(expect("PUT", "/v2.0/lbaas/pools/p1/members?additive_only=true")).contains("additive_only=true"));
    }
}
