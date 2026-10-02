package org.openstack4j.test.microversion;

import org.openstack4j.openstack.compute.internal.ComputeMicroVersions;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ComputeRootUrlTest {

    @Test
    public void discoveryStripsVersionAndTenantFromEndpoint() {
        Assert.assertEquals(ComputeMicroVersions.rootUrl("http://10.0.0.1:8774/v2.1"), "http://10.0.0.1:8774");
        Assert.assertEquals(ComputeMicroVersions.rootUrl("http://10.0.0.1:8774/v2.1/"), "http://10.0.0.1:8774");
        Assert.assertEquals(ComputeMicroVersions.rootUrl("http://127.0.0.1:8774/v2/123ac695d4db400a9001b91bb3b8aa46"), "http://127.0.0.1:8774");
        Assert.assertEquals(ComputeMicroVersions.rootUrl("https://cloud.example.com/compute/v2.1/abc"), "https://cloud.example.com/compute");
        Assert.assertEquals(ComputeMicroVersions.rootUrl("https://cloud.example.com/compute"), "https://cloud.example.com/compute");
    }
}
