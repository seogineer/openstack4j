package org.openstack4j.test.microversion;

import org.openstack4j.openstack.manila.internal.ManilaMicroVersions;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ManilaV2UrlTest {

    @Test
    public void v2UrlReplacesV1AndKeepsTheRest() {
        Assert.assertEquals(ManilaMicroVersions.v2Url("http://m:8786/v1"), "http://m:8786/v2");
        Assert.assertEquals(ManilaMicroVersions.v2Url("http://m:8786/v1/p1/"), "http://m:8786/v2/p1");
        Assert.assertEquals(ManilaMicroVersions.v2Url("http://m:8786/v1/p1/extra"), "http://m:8786/v2/p1/extra");
        Assert.assertEquals(ManilaMicroVersions.v2Url("http://m:8786/v2/p1"), "http://m:8786/v2/p1");
        Assert.assertEquals(ManilaMicroVersions.v2Url("http://m/share"), "http://m/share/v2");
        Assert.assertEquals(ManilaMicroVersions.v2Url("http://m/v10/p1"), "http://m/v10/p1/v2");
    }
}
