package org.openstack4j.api.placement.v1;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.placement.v1.PlacementVersion;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/Versions")
public class PlacementVersionTests extends AbstractPlacementTest {

    public void negotiatesLibraryLatestWhenServerIsNewer() throws Exception {
        respondWithVersions("1.45");

        PlacementVersion version = osv3().placement().versions().get();

        Assert.assertEquals(version.getMicroVersion(), "1.39");
        Assert.assertEquals(version.getServerMaxVersion(), "1.45");
        Assert.assertFalse(version.isPinned());
        Assert.assertEquals(takeRequest().getPath(), "/");
    }

    public void usesServerMaxWhenServerIsOlder() throws Exception {
        respondWithVersions("1.30");

        Assert.assertEquals(osv3().placement().versions().get().getMicroVersion(), "1.30");
        takeRequest();
    }

    public void serverOlderThanMinimumHasNoUsableVersion() throws Exception {
        respondWithVersions("1.27");

        PlacementVersion version = osv3().placement().versions().get();

        Assert.assertNull(version.getMicroVersion());
        Assert.assertEquals(version.getServerMaxVersion(), "1.27");
        takeRequest();
    }

    public void rootIsFetchedOncePerSession() throws Exception {
        respondWithVersions("1.39");

        osv3().placement().versions().get();
        osv3().placement().versions().get();

        takeRequest();
        assertNoMoreRequests();
    }

    public void pinWithinRangeIsUsedAndCanBeCleared() throws Exception {
        respondWithVersions("1.39");

        osv3().placement().useMicroVersion("1.30");
        PlacementVersion pinned = osv3().placement().versions().get();
        osv3().placement().useMicroVersion(null);
        PlacementVersion negotiated = osv3().placement().versions().get();

        Assert.assertEquals(pinned.getMicroVersion(), "1.30");
        Assert.assertTrue(pinned.isPinned());
        Assert.assertEquals(negotiated.getMicroVersion(), "1.39");
        Assert.assertFalse(negotiated.isPinned());
        takeRequest();
        assertNoMoreRequests();
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.40.*outside.*")
    public void pinAboveRangeIsRejected() throws Exception {
        respondWithVersions("1.39");
        try {
            osv3().placement().useMicroVersion("1.40");
        } finally {
            takeRequest();
        }
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class)
    public void pinBelowMinimumIsRejected() throws Exception {
        respondWithVersions("1.39");
        try {
            osv3().placement().useMicroVersion("1.27");
        } finally {
            takeRequest();
        }
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = "Invalid placement microversion 'latest'.*")
    public void pinWithInvalidFormatIsRejected() throws Exception {
        respondWithVersions("1.39");
        try {
            osv3().placement().useMicroVersion("latest");
        } finally {
            takeRequest();
        }
    }

    public void pinIsPerSessionAndEndpoint() throws Exception {
        respondWithVersions("1.39");
        osv3().placement().useMicroVersion("1.30");
        takeRequest();

        // a second, independent session (same token, new client) must negotiate on its own
        org.openstack4j.api.OSClient.OSClientV3 other = org.openstack4j.openstack.OSFactory.clientFromToken(osv3().getToken());
        respondWithVersions("1.39");
        PlacementVersion otherVersion = other.placement().versions().get();
        RecordedRequest root = takeRequest();

        Assert.assertEquals(root.getPath(), "/");
        Assert.assertEquals(otherVersion.getMicroVersion(), "1.39");
        Assert.assertFalse(otherVersion.isPinned());
    }
}
