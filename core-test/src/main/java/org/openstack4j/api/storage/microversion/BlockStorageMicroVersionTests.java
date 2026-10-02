package org.openstack4j.api.storage.microversion;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.BlockStorageVersion;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/MicroVersion")
public class BlockStorageMicroVersionTests extends AbstractBlockStorageMicroVersionTest {

    private static final String TYPES = "{\"volume_types\": []}";

    public void noHeaderAndNoDiscoveryWhenDisabled() throws Exception {
        respondWith(200, TYPES);

        osv3().blockStorage().volumes().listVolumeTypes();
        RecordedRequest request = takeRequest();

        Assert.assertTrue(request.getPath().matches("/v3/\\p{XDigit}+/types"), request.getPath());
        assertNoVersionHeader(request);
        assertNoMoreRequests();
        Assert.assertFalse(osv3().blockStorage().microVersions().get().isEnabled());
        Assert.assertNull(osv3().blockStorage().microVersions().get().getMicroVersion());
    }

    public void negotiateUsesMinOfLibraryAndServer() throws Exception {
        negotiate("3.71");
        respondWith(200, TYPES);

        osv3().blockStorage().volumes().listVolumeTypes();

        assertVersionHeader(takeRequest(), "3.71");
        BlockStorageVersion version = osv3().blockStorage().microVersions().get();
        Assert.assertEquals(version.getMicroVersion(), "3.71");
        Assert.assertEquals(version.getServerMinVersion(), "3.0");
        Assert.assertEquals(version.getServerMaxVersion(), "3.71");
        Assert.assertTrue(version.isEnabled());
        Assert.assertFalse(version.isPinned());
    }

    public void olderServerMaxIsUsedAsIs() throws Exception {
        negotiate("3.40");
        respondWith(200, TYPES);
        osv3().blockStorage().volumes().listVolumeTypes();
        assertVersionHeader(takeRequest(), "3.40");
    }

    public void newerServerIsCappedAtLibraryLatest() throws Exception {
        negotiate("3.80");
        Assert.assertEquals(osv3().blockStorage().microVersions().get().getMicroVersion(), "3.71");
    }

    public void useThenClear() throws Exception {
        negotiate("3.71");
        osv3().blockStorage().microVersions().use("3.50");
        respondWith(200, TYPES);
        respondWith(200, TYPES);

        osv3().blockStorage().volumes().listVolumeTypes();
        assertVersionHeader(takeRequest(), "3.50");
        osv3().blockStorage().microVersions().clear();
        osv3().blockStorage().volumes().listVolumeTypes();
        assertNoVersionHeader(takeRequest());
        Assert.assertFalse(osv3().blockStorage().microVersions().get().isEnabled());
    }

    public void useWithoutNegotiateDiscoversOnce() throws Exception {
        respondWithCinderVersions("3.71");

        BlockStorageVersion version = osv3().blockStorage().microVersions().use("3.27");

        Assert.assertEquals(takeRequest().getPath(), "/");
        Assert.assertEquals(version.getMicroVersion(), "3.27");
        Assert.assertTrue(version.isPinned());
        assertNoMoreRequests();
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.72.*")
    public void useAboveServerMaxIsRejected() throws Exception {
        negotiate("3.71");
        osv3().blockStorage().microVersions().use("3.72");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*microversion.*")
    public void serverWithoutV3CannotNegotiate() throws Exception {
        respondWith(300, "{\"versions\": [{\"id\": \"v2.0\", \"status\": \"DEPRECATED\", \"version\": \"\", \"min_version\": \"\"}]}");
        try {
            osv3().blockStorage().microVersions().negotiate();
        } finally {
            takeRequest();
        }
    }

    public void computeAndBlockStorageStatesAreIndependent() throws Exception {
        negotiate("3.71");
        respondWith(200, TYPES);

        osv3().blockStorage().volumes().listVolumeTypes();

        assertVersionHeader(takeRequest(), "3.71");
        Assert.assertFalse(osv3().compute().microVersions().get().isEnabled());   // compute was never turned on
        Assert.assertNull(osv3().compute().microVersions().get().getMicroVersion());
    }
}
