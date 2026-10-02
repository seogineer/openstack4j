package org.openstack4j.api.storage.microversion;

import org.openstack4j.api.AbstractTest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.openstack.internal.microversion.MicroVersionStore;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/** The default test token resolves block storage to a v2 endpoint; microversions need v3. */
@Test(suiteName = "BlockStorage/V2Endpoint")
public class BlockStorageV2EndpointTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.BLOCK_STORAGE;
    }

    @BeforeMethod
    public void forgetMicroVersions() {
        MicroVersionStore.clearAll();
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*v3.*")
    public void negotiateRefusesAV2Endpoint() throws Exception {
        try {
            osv3().blockStorage().microVersions().negotiate();
        } finally {
            Assert.assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS), "no root request must be sent for a v2 endpoint");
        }
    }

    public void legacyCallsOnV2StayUnversioned() throws Exception {
        respondWith(200, "{\"volume_types\": []}");
        osv3().blockStorage().volumes().listVolumeTypes();
        Assert.assertNull(takeRequest().getHeader("OpenStack-API-Version"));
    }
}
