package org.openstack4j.api.ext.small;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.openstack.storage.object.internal.ext.SwiftInfoService;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "SmallGaps/ReviewFixes")
public class SmallGapsReviewFixTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.MAGNUM;
    }

    public void swiftEndpointsPathHasNoTrailingSlash() {
        Assert.assertEquals(SwiftInfoService.endpointsPath("http://h:8080/v1/AUTH_x", null, null), "/AUTH_x");
        Assert.assertEquals(SwiftInfoService.endpointsPath("http://h:8080/v1/AUTH_x/", "c", null), "/AUTH_x/c");
        Assert.assertEquals(SwiftInfoService.endpointsPath("http://h/swift/v1/AUTH_x", "c", "o"), "/AUTH_x/c/o");
    }

    public void resizeToZeroNodesUses110() throws Exception {
        respondWith(202, "{\"uuid\": \"c1\"}");
        osv3().magnum().extensions().resizeCluster("c1", 0, null, "workers");
        var request = takeRequest();
        Assert.assertEquals(request.getHeader("OpenStack-API-Version"), "container-infra 1.10");
        Assert.assertEquals(new ObjectMapper().readTree(request.getBody().readUtf8()).get("node_count").asInt(), 0);
    }
}
