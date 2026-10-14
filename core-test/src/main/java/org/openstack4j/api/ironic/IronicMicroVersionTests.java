package org.openstack4j.api.ironic;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.ironic.BaremetalVersion;
import org.openstack4j.openstack.ironic.internal.IronicMicroVersions;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@Test(suiteName = "Ironic/MicroVersions")
public class IronicMicroVersionTests extends AbstractIronicTest {

    private static final String ROOT = "{\"name\": \"OpenStack Ironic API\", \"versions\": [{\"id\": \"v1\", \"status\": \"CURRENT\", \"version\": \"1.96\", \"min_version\": \"1.1\", \"links\": []}],"
            + " \"default_version\": {\"id\": \"v1\", \"status\": \"CURRENT\", \"version\": \"1.96\", \"min_version\": \"1.1\", \"links\": []}}";

    @AfterMethod
    public void clear() {
        osv3().ironic().microVersions().clear();
    }

    public void offByDefault() throws Exception {
        respondWith(200, "{\"nodes\": []}");
        osv3().ironic().nodes().list();
        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().startsWith("/v1/nodes"), request.getPath());
        Assert.assertNull(request.getHeader("OpenStack-API-Version"));
        Assert.assertNull(request.getHeader("X-OpenStack-Ironic-API-Version"));
        Assert.assertFalse(osv3().ironic().microVersions().get().isEnabled());
    }

    public void negotiateUsesServerMax() throws Exception {
        respondWith(200, ROOT);
        respondWith(200, "{\"nodes\": []}");
        BaremetalVersion version = osv3().ironic().microVersions().negotiate();
        osv3().ironic().nodes().list();

        Assert.assertEquals(takeRequest().getPath(), "/");
        RecordedRequest list = takeRequest();
        Assert.assertEquals(list.getHeader("OpenStack-API-Version"), "baremetal 1.96");
        Assert.assertEquals(list.getHeader("X-OpenStack-Ironic-API-Version"), "1.96");
        Assert.assertEquals(version.getMicroVersion(), "1.96");
        Assert.assertEquals(version.getServerMaxVersion(), "1.96");
        Assert.assertTrue(version.isEnabled());

        // the server range is discovered once per session; pinning reuses it
        respondWith(200, "{\"nodes\": []}");
        osv3().ironic().microVersions().use("1.80");
        osv3().ironic().nodes().list();
        Assert.assertEquals(takeRequest().getHeader("X-OpenStack-Ironic-API-Version"), "1.80");
    }

    public void errorBeforeNegotiateGuidesToIronicAccessor() {
        try {
            IronicMicroVersions.SUPPORT.require("a feature", IronicMicroVersions.V(50), null);
            Assert.fail("expected MicroVersionException");
        } catch (MicroVersionException e) {
            Assert.assertTrue(e.getMessage().contains("os.ironic().microVersions().negotiate()"), e.getMessage());
            Assert.assertFalse(e.getMessage().contains("baremetal()"), e.getMessage());
        }
    }
}
