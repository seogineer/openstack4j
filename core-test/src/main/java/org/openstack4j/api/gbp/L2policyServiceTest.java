package org.openstack4j.api.gbp;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.api.Builders;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.gbp.L2Policy;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Test cases for l2 policies on GBP
 *
 * @author vinod borole
 */
@Test(suiteName = "grouppolicy/l2_policies")
public class L2policyServiceTest extends AbstractTest {

    private static final String L2_POLICIES = "/network/gbp/l2_policies.json";
    private static final String L2_POLICY = "/network/gbp/l2_policy.json";
    private static final String L2_POLICY_UPDATE = "/network/gbp/l2_policy_update.json";

    @Override
    protected Service service() {
        return Service.NETWORK;
    }

    @Test
    public void testListl2Policy() throws Exception {
        respondWith(L2_POLICIES);
        List<? extends L2Policy> l2policyList = osv2().gbp().l2Policy().list();
        assertEquals(10, l2policyList.size());
        Objects.requireNonNull(l2policyList.get(0));
        Logger.getLogger(getClass().getName()).info(getClass().getName() + " : L2 Policy from List : " + l2policyList.get(0));
        assertEquals(l2policyList.get(0).getId(), "08c1c093-6337-4383-938e-2d9c6cac531a");
    }

    @Test
    public void testGetl2Policy() throws Exception {
        respondWith(L2_POLICY);
        String id = "08c1c093-6337-4383-938e-2d9c6cac531a";
        L2Policy l2policy = osv2().gbp().l2Policy().get(id);
        Logger.getLogger(getClass().getName()).info(getClass().getName() + " : L2 Policy by ID : " + l2policy);
        assertNotNull(l2policy);
        assertEquals(id, l2policy.getId());
    }

    @Test
    public void testCreatel2Policy() throws Exception {
        respondWith(L2_POLICY);
        L2Policy l2PolicyCreate = Builders.l2Policy().name("test-policy-target-group").description("Implicitly created L2 policy").build();
        L2Policy l2Policy = osv2().gbp().l2Policy().create(l2PolicyCreate);
        Logger.getLogger(getClass().getName()).info(getClass().getName() + " : Created L2 Policy : " + l2Policy);
        assertEquals("test-policy-target-group", l2Policy.getName());
        assertEquals("f9c1f545-6ea6-4b05-99d5-50f02ed3c640", l2Policy.getNetworkId());
    }

    @Test
    public void testBuilderNetworkIdDoesNotSetId() {
        L2Policy l2Policy = Builders.l2Policy().networkId("n1").build();
        assertEquals(l2Policy.getNetworkId(), "n1");
        assertNull(l2Policy.getId());
    }

    @Test
    public void testCreatel2PolicySendsNetworkIdWithoutId() throws Exception {
        respondWith(L2_POLICY);
        L2Policy l2PolicyCreate = Builders.l2Policy().name("network-id-request").networkId("n1").build();
        osv2().gbp().l2Policy().create(l2PolicyCreate);
        // the mock server queue also holds requests of earlier tests; skip to ours
        JsonNode body = null;
        RecordedRequest request;
        while ((request = server.takeRequest(100, TimeUnit.MILLISECONDS)) != null) {
            JsonNode candidate = new ObjectMapper().readTree(request.getBody().clone().readUtf8());
            if (candidate.path("l2_policy").path("name").asText().equals("network-id-request"))
                body = candidate;
        }
        assertNotNull(body, "create request not recorded");
        assertEquals(body.path("l2_policy").path("network_id").asText(), "n1");
        assertFalse(body.get("l2_policy").has("id"));
    }

    @Test
    public void testUpdatel2Policy() throws Exception {
        respondWith(L2_POLICY_UPDATE);
        String id = "08c1c093-6337-4383-938e-2d9c6cac531a";
        L2Policy l2PolicyUpdate = Builders.l2Policy().name("test-policy-target-group-update").description("Implicitly created L2 policy-update").build();
        L2Policy l2Policy = osv2().gbp().l2Policy().update(id, l2PolicyUpdate);
        Logger.getLogger(getClass().getName()).info(getClass().getName() + " : Updated L2 Policy : " + l2Policy);
        assertEquals("Implicitly created L2 policy-update", l2Policy.getDescription());

    }

    @Test
    public void testDeletel2Policy() {
        respondWith(200);
        String id = "08c1c093-6337-4383-938e-2d9c6cac531a";
        ActionResponse result = osv2().gbp().l2Policy().delete(id);
        assertTrue(result.isSuccess());
    }


}
