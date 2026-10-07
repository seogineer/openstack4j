package org.openstack4j.api.network.ext2;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Deferred minor from the networking review: floating IP pools expose the subnet CIDR. */
@Test(suiteName = "Network/Ext2/QualityFixes")
public class NetworkQualityFixTests extends AbstractNetworkingExtTest {

    public void floatingIpPoolCidr() throws Exception {
        respondWith(200, "{\"floatingip_pools\": [{\"subnet_id\": \"s1\", \"subnet_name\": \"ext\", \"network_id\": \"n1\", \"project_id\": \"p1\", \"cidr\": \"192.0.2.0/24\"}]}");
        Assert.assertEquals(osv3().networking().floatingip().listPools().get(0).getCidr(), "192.0.2.0/24");
        takeRequest();
    }
}
