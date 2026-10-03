package org.openstack4j.api.identity.v3.ext;

import okhttp3.mockwebserver.RecordedRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/EndpointPolicy")
public class EndpointPolicyTests extends AbstractIdentityExtTest {

    public void associations() throws Exception {
        for (int i = 0; i < 9; i++)
            respondWith(204);
        respondWith(200, "{\"endpoints\": [{\"id\": \"e1\", \"interface\": \"public\", \"url\": \"http://x\", \"links\": {}}], \"links\": {}}");
        respondWith(200, "{\"policy\": {\"id\": \"p1\", \"type\": \"application/json\", \"blob\": \"{}\", \"links\": {}}}");
        respondWith(200);

        var policies = osv3().identity().endpointPolicies();
        policies.associateWithEndpoint("p1", "e1");
        policies.checkEndpointAssociation("p1", "e1");
        policies.disassociateFromEndpoint("p1", "e1");
        policies.associateWithService("p1", "s1");
        policies.checkServiceAssociation("p1", "s1");
        policies.disassociateFromService("p1", "s1");
        policies.associateWithServiceInRegion("p1", "s1", "RegionOne");
        policies.checkServiceInRegionAssociation("p1", "s1", "RegionOne");
        policies.disassociateFromServiceInRegion("p1", "s1", "RegionOne");
        Assert.assertEquals(policies.endpointsForPolicy("p1").get(0).getId(), "e1");
        Assert.assertEquals(policies.policyForEndpoint("e1").getId(), "p1");
        Assert.assertTrue(policies.checkPolicyAssociations("p1").isSuccess());

        String base = "/v3/policies/p1/OS-ENDPOINT-POLICY";
        String[][] expected = {{"PUT", base + "/endpoints/e1"}, {"GET", base + "/endpoints/e1"}, {"DELETE", base + "/endpoints/e1"},
                {"PUT", base + "/services/s1"}, {"GET", base + "/services/s1"}, {"DELETE", base + "/services/s1"},
                {"PUT", base + "/services/s1/regions/RegionOne"}, {"GET", base + "/services/s1/regions/RegionOne"},
                {"DELETE", base + "/services/s1/regions/RegionOne"}, {"GET", base + "/endpoints"},
                {"GET", "/v3/endpoints/e1/OS-ENDPOINT-POLICY/policy"}, {"HEAD", base + "/policy"}};
        for (String[] e : expected) {
            RecordedRequest r = takeRequest();
            Assert.assertEquals(r.getMethod(), e[0], e[1]);
            Assert.assertTrue(r.getPath().endsWith(e[1]), r.getPath() + " vs " + e[1]);
        }
    }
}
