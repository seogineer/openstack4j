package org.openstack4j.api.baremetal;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.baremetal.BaremetalVersion;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@Test(suiteName = "Baremetal/MicroVersions")
public class BaremetalMicroVersionTests extends AbstractBaremetalTest {

    private static final String ROOT = "{\"name\": \"OpenStack Ironic API\", \"versions\": [{\"id\": \"v1\", \"status\": \"CURRENT\", \"version\": \"1.96\", \"min_version\": \"1.1\", \"links\": []}],"
            + " \"default_version\": {\"id\": \"v1\", \"status\": \"CURRENT\", \"version\": \"1.96\", \"min_version\": \"1.1\", \"links\": []}}";

    @AfterMethod
    public void clear() {
        osv3().baremetal().microVersions().clear();
    }

    public void offByDefault() throws Exception {
        respondWith(200, "{\"nodes\": []}");
        osv3().baremetal().nodes().list();
        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().startsWith("/v1/nodes"), request.getPath());
        Assert.assertNull(request.getHeader("OpenStack-API-Version"));
        Assert.assertNull(request.getHeader("X-OpenStack-Ironic-API-Version"));
        Assert.assertFalse(osv3().baremetal().microVersions().get().isEnabled());
    }

    public void negotiateUsesServerMax() throws Exception {
        respondWith(200, ROOT);
        respondWith(200, "{\"nodes\": []}");
        BaremetalVersion version = osv3().baremetal().microVersions().negotiate();
        osv3().baremetal().nodes().list();

        Assert.assertEquals(takeRequest().getPath(), "/");
        RecordedRequest list = takeRequest();
        Assert.assertEquals(list.getHeader("OpenStack-API-Version"), "baremetal 1.96");
        Assert.assertEquals(list.getHeader("X-OpenStack-Ironic-API-Version"), "1.96");
        Assert.assertEquals(version.getMicroVersion(), "1.96");
        Assert.assertEquals(version.getServerMaxVersion(), "1.96");
        Assert.assertTrue(version.isEnabled());

        // the server range is discovered once per session; pinning reuses it
        respondWith(200, "{\"nodes\": []}");
        osv3().baremetal().microVersions().use("1.80");
        osv3().baremetal().nodes().list();
        Assert.assertEquals(takeRequest().getHeader("X-OpenStack-Ironic-API-Version"), "1.80");
    }
}
