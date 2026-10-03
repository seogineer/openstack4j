package org.openstack4j.api.network.ext2;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.network.options.ConntrackHelperOptions;
import org.openstack4j.model.network.options.SegmentOptions;
import org.openstack4j.openstack.networking.domain.NeutronHostRoute;
import org.testng.Assert;
import org.testng.annotations.Test;

/** A 404 (extension disabled or parent missing) must surface; only get(id) keeps returning null for a missing resource. */
@Test(suiteName = "Network/Ext2/NotFound")
public class NotFoundPropagationTests extends AbstractNetworkingExtTest {

    private static final String NOT_FOUND = "{\"NeutronError\": {\"type\": \"HTTPNotFound\", \"message\": \"The resource could not be found.\", \"detail\": \"\"}}";

    private void assertNotFound(Runnable call) throws Exception {
        respondWith(404, NOT_FOUND);
        try {
            call.run();
            Assert.fail("expected the 404 to surface");
        } catch (RuntimeException expected) {
            Assert.assertTrue(String.valueOf(expected.getMessage()).contains("could not be found"), String.valueOf(expected.getMessage()));
        }
        takeRequest();
    }

    public void listOnDisabledExtension() throws Exception { assertNotFound(() -> osv3().networking().segments().list()); }
    public void listUnderMissingParent() throws Exception { assertNotFound(() -> osv3().networking().qosRules().listDscpMarkingRules("missing")); }
    public void createOnDisabledExtension() throws Exception { assertNotFound(() -> osv3().networking().segments().create(SegmentOptions.create("n1", "vlan"))); }
    public void updateMissing() throws Exception { assertNotFound(() -> osv3().networking().segments().update("missing", SegmentOptions.update().name("x"))); }
    public void prefixActionOnMissingPool() throws Exception { assertNotFound(() -> osv3().networking().subnetPools().addPrefixes("missing", List.of("10.0.0.0/24"))); }
    public void addressActionOnMissingGroup() throws Exception { assertNotFound(() -> osv3().networking().addressGroups().addAddresses("missing", List.of("10.0.0.1/32"))); }
    public void routerActionWithoutExtension() throws Exception { assertNotFound(() -> osv3().networking().router().addExtraRoutes("r1", List.of(new NeutronHostRoute("10.0.0.0/24", "10.0.1.1")))); }
    public void conntrackListWithoutExtension() throws Exception { assertNotFound(() -> osv3().networking().router().listConntrackHelpers("r1")); }
    public void conntrackCreateWithoutExtension() throws Exception { assertNotFound(() -> osv3().networking().router().createConntrackHelper("r1", ConntrackHelperOptions.create("tcp", 21, "ftp"))); }
    public void bindingsOfMissingPort() throws Exception { assertNotFound(() -> osv3().networking().port().listBindings("missing")); }
    public void quotaDetailsWithoutExtension() throws Exception { assertNotFound(() -> osv3().networking().quotas().getDetails(PROJECT)); }
    public void loggableResourcesWithoutExtension() throws Exception { assertNotFound(() -> osv3().networking().logging().loggableResources()); }
    public void agentRoutersWithoutScheduler() throws Exception { assertNotFound(() -> osv3().networking().agent().listL3Routers("a1")); }
    public void localIpAssociationOnMissingIp() throws Exception { assertNotFound(() -> osv3().networking().localIps().associatePort("missing", "p1", null)); }

    public void getOfMissingResourceStaysNull() throws Exception {
        respondWith(404, NOT_FOUND);
        Assert.assertNull(osv3().networking().segments().get("missing"));
        takeRequest();
    }
}
