package org.openstack4j.api.octavia.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.openstack4j.model.common.ActionResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Regressions from the Octavia final review: the amphora load balancer key and the 404 rules. */
@Test(suiteName = "Octavia/Ext/ReviewFixes")
public class OctaviaReviewFixTests extends AbstractOctaviaExtTest {

    private static final String JSON_404 = "{\"faultcode\": \"Client\", \"faultstring\": \"Load Balancer lb1 not found.\", \"debuginfo\": null}";

    /** The api-ref response table and the real API use loadbalancer_id; the api-ref example uses load_balancer_id. */
    public void amphoraReadsLoadbalancerId() throws Exception {
        respondWith(200, "{\"amphora\": {\"id\": \"a1\", \"loadbalancer_id\": \"lb1\", \"role\": \"MASTER\"}}");
        Assert.assertEquals(osv3().octavia().amphorae().get("a1").getLoadBalancerId(), "lb1");
        takeRequest();
    }

    private void assertRaises(Runnable call) throws Exception {
        respondWith(404, JSON_404);
        try {
            call.run();
            Assert.fail("expected the 404 to surface");
        } catch (RuntimeException expected) {
            Assert.assertTrue(String.valueOf(expected.getMessage()).contains("not found"), String.valueOf(expected.getMessage()));
        }
        takeRequest();
    }

    private void assertFailed(Supplier<ActionResponse> call) throws Exception {
        respondWith(Collections.singletonMap("Content-Type", "text/html; charset=UTF-8"), 404, "<html><body><h1>404 Not Found</h1></body></html>");
        ActionResponse response = call.get();
        takeRequest();
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(response.getCode(), 404);
    }

    public void listRaises() throws Exception { assertRaises(() -> osv3().octavia().l7Policies().listRules("missing")); }
    public void defaultsRaise() throws Exception { assertRaises(() -> osv3().octavia().quotas().defaults()); }
    public void listenerStatsRaise() throws Exception { assertRaises(() -> osv3().octavia().listenerV2().stats("missing")); }

    public void getReturnsNull() throws Exception {
        respondWith(404, JSON_404);
        Assert.assertNull(osv3().octavia().quotas().get("missing"));
        takeRequest();
    }

    public void actionsReportFailure() throws Exception {
        assertFailed(() -> osv3().octavia().loadBalancerV2().failover("missing"));
        assertFailed(() -> osv3().octavia().amphorae().failover("missing"));
        assertFailed(() -> osv3().octavia().lbPoolV2().updateMembers("missing", List.of(Map.of("address", "192.0.2.1", "protocol_port", 80)), false));
        assertFailed(() -> osv3().octavia().flavors().delete("missing"));
    }
}
