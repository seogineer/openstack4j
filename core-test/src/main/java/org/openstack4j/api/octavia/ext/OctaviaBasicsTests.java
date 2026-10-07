package org.openstack4j.api.octavia.ext;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.octavia.ext.OctaviaQuota;
import org.openstack4j.model.octavia.options.OctaviaQuotaOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Octavia/Ext/Basics")
public class OctaviaBasicsTests extends AbstractOctaviaExtTest {

    private static final String PROJECT = "e3cd678b11784734bc366148aa37580e";

    public void providers() throws Exception {
        respondWith(200, "{\"providers\": [{\"name\": \"amphora\", \"description\": \"The Octavia Amphora driver.\"}, {\"name\": \"octavia\", \"description\": \"Deprecated alias.\"}]}");
        respondWith(200, "{\"flavor_capabilities\": [{\"name\": \"loadbalancer_topology\", \"description\": \"SINGLE or ACTIVE_STANDBY\"}]}");
        respondWith(200, "{\"availability_zone_capabilities\": [{\"name\": \"compute_zone\", \"description\": \"The compute availability zone.\"}]}");

        var providers = osv3().octavia().providers();
        Assert.assertEquals(providers.list().get(0).getName(), "amphora");
        Assert.assertEquals(providers.flavorCapabilities("amphora").get(0).getName(), "loadbalancer_topology");
        Assert.assertEquals(providers.availabilityZoneCapabilities("amphora").get(0).getName(), "compute_zone");

        expect("GET", "/v2.0/lbaas/providers");
        expect("GET", "/v2.0/lbaas/providers/amphora/flavor_capabilities");
        expect("GET", "/v2.0/lbaas/providers/amphora/availability_zone_capabilities");
    }

    public void quotasDistinguishNullFromUnlimited() throws Exception {
        respondWith(200, "{\"quotas\": [{\"loadbalancer\": 5, \"member\": 50, \"healthmonitor\": -1, \"listener\": null, \"project_id\": \"" + PROJECT + "\", \"pool\": null, \"l7policy\": 3, \"l7rule\": null}]}");
        respondWith(200, "{\"quota\": {\"loadbalancer\": 50, \"listener\": -1, \"member\": -1, \"pool\": -1, \"healthmonitor\": -1, \"l7policy\": -1, \"l7rule\": -1}}");
        respondWith(200, "{\"quota\": {\"loadbalancer\": 5, \"listener\": -1, \"member\": 50, \"pool\": -1, \"healthmonitor\": -1, \"l7policy\": 20, \"l7rule\": -1}}");
        respondWith(202, "{\"quota\": {\"loadbalancer\": 10, \"listener\": -1, \"member\": 50, \"pool\": -1, \"healthmonitor\": -1, \"l7policy\": 15, \"l7rule\": 25}}");
        respondWith(202);

        var quotas = osv3().octavia().quotas();
        List<? extends OctaviaQuota> all = quotas.list();
        OctaviaQuota defaults = quotas.defaults();
        quotas.get(PROJECT);
        OctaviaQuota updated = quotas.update(PROJECT, OctaviaQuotaOptions.create().loadbalancer(10).l7rule(25));
        Assert.assertTrue(quotas.reset(PROJECT).isSuccess());

        expect("GET", "/v2.0/lbaas/quotas");
        expect("GET", "/v2.0/lbaas/quotas/defaults");
        expect("GET", "/v2.0/lbaas/quotas/" + PROJECT);
        RecordedRequest update = expect("PUT", "/v2.0/lbaas/quotas/" + PROJECT);
        Assert.assertEquals(body(update).toString(), "{\"quota\":{\"loadbalancer\":10,\"l7rule\":25}}");
        expect("DELETE", "/v2.0/lbaas/quotas/" + PROJECT);
        Assert.assertNull(all.get(0).getListener());
        Assert.assertEquals(all.get(0).getHealthmonitor(), Integer.valueOf(-1));
        Assert.assertEquals(all.get(0).getProjectId(), PROJECT);
        Assert.assertEquals(defaults.getLoadbalancer(), Integer.valueOf(50));
        Assert.assertEquals(updated.getL7rule(), Integer.valueOf(25));
    }
}
