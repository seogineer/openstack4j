package org.openstack4j.test.microversion;

import org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BlockStorageRootUrlTest {

    @Test
    public void rootUrlStripsVersionAndProject() {
        Assert.assertEquals(BlockStorageMicroVersions.rootUrl("http://10.0.0.1:8776/v3/2580a7b51d564c1d848ee27fda2db713"), "http://10.0.0.1:8776");
        Assert.assertEquals(BlockStorageMicroVersions.rootUrl("http://10.0.0.1:8776/v3"), "http://10.0.0.1:8776");
        Assert.assertEquals(BlockStorageMicroVersions.rootUrl("http://10.0.0.1:8776/v3/"), "http://10.0.0.1:8776");
        Assert.assertEquals(BlockStorageMicroVersions.rootUrl("http://127.0.0.1:8776/v2/123ac695d4db400a9001b91bb3b8aa46"), "http://127.0.0.1:8776");
        Assert.assertEquals(BlockStorageMicroVersions.rootUrl("https://cloud.example.com/volume/v3/abc"), "https://cloud.example.com/volume");
        Assert.assertEquals(BlockStorageMicroVersions.rootUrl("https://cloud.example.com/volume"), "https://cloud.example.com/volume");
    }
}
