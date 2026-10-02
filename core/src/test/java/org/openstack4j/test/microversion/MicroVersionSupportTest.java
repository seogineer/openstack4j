package org.openstack4j.test.microversion;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionStore;
import org.openstack4j.openstack.internal.microversion.MicroVersionSupport;
import org.openstack4j.openstack.internal.microversion.VersionRange;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Session-free parts of MicroVersionSupport; negotiation against a session is covered by the connector tests. */
public class MicroVersionSupportTest {

    private final MicroVersionSupport volume = new MicroVersionSupport(ServiceType.BLOCK_STORAGE, "volume",
            new MicroVersion(3, 0), new MicroVersion(3, 71), "volume", Collections.emptyList(),
            "os.blockStorage().microVersions().negotiate()");
    private final MicroVersionSupport compute = new MicroVersionSupport(ServiceType.COMPUTE, "compute",
            new MicroVersion(2, 1), new MicroVersion(2, 104), "compute", List.of("X-OpenStack-Nova-API-Version"),
            "os.compute().microVersions().negotiate()");

    @BeforeMethod
    public void clear() {
        MicroVersionStore.clearAll();
    }

    @Test
    public void headersFollowTheServiceShape() {
        Map<String, String> v = volume.headers(new MicroVersion(3, 50));
        Assert.assertEquals(v, Map.of("OpenStack-API-Version", "volume 3.50"));
        Map<String, String> c = compute.headers(new MicroVersion(2, 60));
        Assert.assertEquals(c.get("OpenStack-API-Version"), "compute 2.60");
        Assert.assertEquals(c.get("X-OpenStack-Nova-API-Version"), "2.60");
    }

    @Test
    public void effectiveIsNullWithoutASession() {
        Assert.assertNull(volume.effective(null, null));
        Assert.assertNull(volume.currentState());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*os\\.blockStorage\\(\\)\\.microVersions\\(\\)\\.negotiate\\(\\).*")
    public void requireWhileDisabledNamesTheEnableCall() {
        volume.require("Attachments", new MicroVersion(3, 27), null);
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.27.*3\\.20.*")
    public void requireBelowFloorNamesBothVersions() {
        volume.require("Attachments", new MicroVersion(3, 27), new MicroVersion(3, 20));
    }

    @Test
    public void requireAtOrAboveFloorPasses() {
        volume.require("Attachments", new MicroVersion(3, 27), new MicroVersion(3, 27));
        volume.require("Attachments", new MicroVersion(3, 27), new MicroVersion(3, 71));
    }

    @Test
    public void versionRangeKeepsBothEnds() {
        VersionRange range = new VersionRange(new MicroVersion(3, 0), new MicroVersion(3, 71));
        Assert.assertEquals(range.getMin().toString(), "3.0");
        Assert.assertEquals(range.getMax().toString(), "3.71");
    }

    @Test
    public void stateWithoutMinUsesTheLibraryMinimum() {
        MicroVersionState state = new MicroVersionState(volume.getMinimum(), new MicroVersion(3, 71));
        Assert.assertEquals(state.getServerMin().toString(), "3.0");
    }
}
