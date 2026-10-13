package org.openstack4j.api.network;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.network.ext2.AbstractNetworkingExtTest;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.network.Agent;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.openstack.networking.domain.NeutronAgent;
import org.openstack4j.openstack.networking.domain.NeutronNetwork;
import org.openstack4j.openstack.networking.domain.NeutronPort;
import org.openstack4j.openstack.networking.domain.NeutronRouter;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Neutron response-only fields: read from the response, never sent back on create/update. */
@Test(suiteName = "Network/ResponseFields")
public class NeutronResponseFieldTests extends AbstractNetworkingExtTest {

    private static final String PORT = "{\"port\":{\"id\":\"p1\",\"network_id\":\"n1\",\"name\":\"port\","
            + "\"qos_policy_id\":\"q1\",\"qos_network_policy_id\":\"q2\"}}";
    private static final String ROUTER = "{\"router\":{\"id\":\"r1\",\"name\":\"router\",\"project_id\":\"p1\",\"tenant_id\":\"p1\"}}";

    public void networkQosPolicyId() throws Exception {
        respondWith(200, "{\"network\":{\"id\":\"n1\",\"name\":\"net\",\"qos_policy_id\":\"q1\"}}");
        Network network = osv3().networking().network().get("n1");
        expect("GET", "/networks/n1");
        Assert.assertEquals(network.getQosPolicyId(), "q1");
    }

    public void networkWithoutQosPolicyIdIsNull() throws Exception {
        respondWith(200, "{\"network\":{\"id\":\"n1\",\"name\":\"net\"}}");
        Network network = osv3().networking().network().get("n1");
        expect("GET", "/networks/n1");
        Assert.assertNull(network.getQosPolicyId());
    }

    public void portQosFieldsNotSentOnUpdate() throws Exception {
        respondWith(200, PORT);
        respondWith(200, PORT);
        Port port = osv3().networking().port().get("p1");
        expect("GET", "/ports/p1");
        Assert.assertEquals(port.getQosPolicyId(), "q1");
        Assert.assertEquals(port.getQosNetworkPolicyId(), "q2");

        osv3().networking().port().update(port);
        JsonNode sent = body(expect("PUT", "/ports/p1")).get("port");
        Assert.assertFalse(sent.has("qos_policy_id"), sent.toString());
        Assert.assertFalse(sent.has("qos_network_policy_id"), sent.toString());
    }

    public void routerProjectIdNotSentOnCreate() throws Exception {
        respondWith(200, ROUTER);
        respondWith(200, ROUTER);
        Router router = osv3().networking().router().get("r1");
        expect("GET", "/routers/r1");
        Assert.assertEquals(router.getProjectId(), "p1");
        Assert.assertEquals(router.getTenantId(), "p1");

        osv3().networking().router().create(router);
        JsonNode sent = body(expect("POST", "/routers")).get("router");
        Assert.assertFalse(sent.has("project_id"), sent.toString());
    }

    public void agentAvailabilityZone() throws Exception {
        respondWith(200, "{\"agents\":[{\"id\":\"a1\",\"agent_type\":\"DHCP agent\",\"availability_zone\":\"nova\"}]}");
        List<? extends Agent> agents = osv3().networking().agent().list();
        expect("GET", "/agents");
        Assert.assertEquals(agents.get(0).getAvailabilityZone(), "nova");
    }

    public void responseOnlyFieldsNotSerialized() throws Exception {
        NeutronNetwork network = ObjectMapperSingleton.getContext(NeutronNetwork.class).readValue(
                "{\"network\":{\"id\":\"n1\",\"qos_policy_id\":\"q1\"}}", NeutronNetwork.class);
        Assert.assertEquals(network.getQosPolicyId(), "q1");
        assertNotSerialized(NeutronNetwork.class, network, "qos_policy_id");

        NeutronPort port = ObjectMapperSingleton.getContext(NeutronPort.class).readValue(PORT, NeutronPort.class);
        Assert.assertEquals(port.getQosPolicyId(), "q1");
        Assert.assertEquals(port.getQosNetworkPolicyId(), "q2");
        assertNotSerialized(NeutronPort.class, port, "qos_policy_id", "qos_network_policy_id");

        NeutronRouter router = ObjectMapperSingleton.getContext(NeutronRouter.class).readValue(ROUTER, NeutronRouter.class);
        Assert.assertEquals(router.getProjectId(), "p1");
        assertNotSerialized(NeutronRouter.class, router, "project_id");

        NeutronAgent agent = ObjectMapperSingleton.getContext(NeutronAgent.class).readValue(
                "{\"agent\":{\"id\":\"a1\",\"agent_type\":\"DHCP agent\",\"availability_zone\":\"nova\"}}", NeutronAgent.class);
        Assert.assertEquals(agent.getAvailabilityZone(), "nova");
        assertNotSerialized(NeutronAgent.class, agent, "availability_zone");
    }

    private static void assertNotSerialized(Class<?> type, Object value, String... keys) throws Exception {
        String json = ObjectMapperSingleton.getContext(type).writeValueAsString(value);
        JsonNode node = new ObjectMapper().readTree(json);
        for (String key : keys) {
            Assert.assertFalse(node.toString().contains("\"" + key + "\""), key + " was serialized: " + json);
        }
    }
}
