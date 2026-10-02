package org.openstack4j.api.compute.microversion;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.ComputeVersion;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/MicroVersion")
public class ComputeMicroVersionTests extends AbstractComputeMicroVersionTest {

    private static final String FLAVORS = "{\"flavors\": []}";

    public void noHeadersAndNoDiscoveryWhenDisabled() throws Exception {
        respondWith(200, FLAVORS);

        osv3().compute().flavors().list();
        RecordedRequest request = takeRequest();

        Assert.assertTrue(request.getPath().contains("/flavors"));
        assertNoVersionHeaders(request);
        assertNoMoreRequests();
        Assert.assertFalse(osv3().compute().microVersions().get().isEnabled());
        Assert.assertNull(osv3().compute().microVersions().get().getMicroVersion());
    }

    public void negotiateUsesMinOfLibraryAndServer() throws Exception {
        negotiate("2.100");
        respondWith(200, FLAVORS);

        osv3().compute().flavors().list();

        assertVersionHeaders(takeRequest(), "2.100");
        ComputeVersion version = osv3().compute().microVersions().get();
        Assert.assertEquals(version.getMicroVersion(), "2.100");
        Assert.assertEquals(version.getServerMaxVersion(), "2.100");
        Assert.assertTrue(version.isEnabled());
        Assert.assertFalse(version.isPinned());
    }

    public void newerServerIsCappedAtLibraryLatest() throws Exception {
        negotiate("2.120");
        Assert.assertEquals(osv3().compute().microVersions().get().getMicroVersion(), "2.104");
    }

    public void olderServerMaxIsUsedAsIs() throws Exception {
        negotiate("2.50");
        respondWith(200, FLAVORS);
        osv3().compute().flavors().list();
        assertVersionHeaders(takeRequest(), "2.50");
    }

    public void useThenClear() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.60");
        respondWith(200, FLAVORS);
        respondWith(200, FLAVORS);

        osv3().compute().flavors().list();
        assertVersionHeaders(takeRequest(), "2.60");
        osv3().compute().microVersions().clear();
        osv3().compute().flavors().list();
        assertNoVersionHeaders(takeRequest());
        Assert.assertFalse(osv3().compute().microVersions().get().isEnabled());
    }

    public void useWithoutNegotiateDiscoversOnce() throws Exception {
        respondWithNovaVersions("2.100");

        ComputeVersion version = osv3().compute().microVersions().use("2.53");

        Assert.assertEquals(takeRequest().getPath(), "/");
        Assert.assertEquals(version.getMicroVersion(), "2.53");
        Assert.assertTrue(version.isPinned());
        assertNoMoreRequests();
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.101.*")
    public void useAboveServerMaxIsRejected() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.101");
    }

    @Test(expectedExceptions = MicroVersionException.class)
    public void useWithGarbageIsRejected() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("latest");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*microversion.*")
    public void serverWithoutMicroVersionsCannotNegotiate() throws Exception {
        respondWith(200, "{\"versions\":[{\"id\":\"v2.0\",\"status\":\"CURRENT\",\"version\":\"\",\"min_version\":\"\"}]}");
        try {
            osv3().compute().microVersions().negotiate();
        } finally {
            takeRequest();
        }
    }
}
