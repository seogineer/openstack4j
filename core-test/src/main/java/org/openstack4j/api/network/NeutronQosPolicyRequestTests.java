package org.openstack4j.api.network;

import java.util.Arrays;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import org.openstack4j.api.Builders;
import org.openstack4j.api.network.ext2.AbstractNetworkingExtTest;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.network.NetworkUpdate;
import org.openstack4j.model.network.Port;
import org.openstack4j.openstack.networking.domain.NeutronNetwork;
import org.openstack4j.openstack.networking.domain.NeutronNetworkUpdate;
import org.openstack4j.openstack.networking.domain.NeutronPort;
import org.openstack4j.openstack.networking.domain.NeutronPortUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Neutron qos_policy_id: sent only when set through a builder, never echoed back from a response. */
@Test(suiteName = "Network/QosPolicyRequest")
public class NeutronQosPolicyRequestTests extends AbstractNetworkingExtTest {

    private static final String NETWORK = "{\"network\":{\"id\":\"n1\",\"name\":\"net\"}}";
    private static final String PORT = "{\"port\":{\"id\":\"p1\",\"network_id\":\"n1\",\"name\":\"port\",\"qos_policy_id\":\"q1\",\"qos_network_policy_id\":\"q9\"}}";
    private static final String PORT_PLAIN = "{\"port\":{\"id\":\"p1\",\"network_id\":\"n1\",\"name\":\"port\"}}";
    private static final String PORTS = "{\"ports\":[{\"id\":\"p1\",\"network_id\":\"n1\"}]}";

    public void networkCreateWithQos() throws Exception {
        respondWith(201, NETWORK);
        osv3().networking().network().create(Builders.network().name("n").qosPolicyId("q1").build());
        JsonNode sent = body(expect("POST", "/networks")).get("network");
        Assert.assertEquals(sent.get("qos_policy_id").asText(), "q1");
        Assert.assertFalse(sent.has("qosPolicyId"), sent.toString());
    }

    public void networkCreateWithoutQosUnchanged() throws Exception {
        respondWith(201, NETWORK);
        osv3().networking().network().create(Builders.network().name("n").build());
        JsonNode sent = body(expect("POST", "/networks")).get("network");
        Assert.assertFalse(sent.has("qos_policy_id"), sent.toString());
    }

    public void networkUpdateSetAndClear() throws Exception {
        respondWith(200, NETWORK);
        respondWith(200, NETWORK);
        osv3().networking().network().update("n1", Builders.networkUpdate().qosPolicyId("q1").build());
        osv3().networking().network().update("n1", Builders.networkUpdate().qosPolicyId(null).build());

        JsonNode set = body(expect("PUT", "/networks/n1")).get("network");
        Assert.assertEquals(set.get("qos_policy_id").asText(), "q1");
        JsonNode cleared = body(expect("PUT", "/networks/n1")).get("network");
        Assert.assertTrue(cleared.has("qos_policy_id"), cleared.toString());
        Assert.assertTrue(cleared.get("qos_policy_id").isNull(), cleared.toString());
    }

    public void networkUpdateWithoutQosUnchanged() throws Exception {
        respondWith(200, NETWORK);
        osv3().networking().network().update("n1", Builders.networkUpdate().name("x").build());
        JsonNode sent = body(expect("PUT", "/networks/n1")).get("network");
        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertFalse(sent.has("qos_policy_id"), sent.toString());
        Assert.assertFalse(sent.has("qosPolicyId"), sent.toString());
    }

    public void portCreateWithQos() throws Exception {
        respondWith(201, PORT_PLAIN);
        respondWith(201, PORTS);
        osv3().networking().port().create(Builders.port().networkId("n1").qosPolicyId("q1").build());
        osv3().networking().port().create(Arrays.asList(Builders.port().networkId("n1").qosPolicyId("q2").build()));

        JsonNode single = body(expect("POST", "/ports")).get("port");
        Assert.assertEquals(single.get("qos_policy_id").asText(), "q1");
        JsonNode batch = body(expect("POST", "/ports")).get("ports");
        Assert.assertEquals(batch.size(), 1);
        Assert.assertEquals(batch.get(0).get("qos_policy_id").asText(), "q2");
    }

    public void portCreateWithoutQos() throws Exception {
        respondWith(201, PORT_PLAIN);
        osv3().networking().port().create(Builders.port().networkId("n1").build());
        JsonNode sent = body(expect("POST", "/ports")).get("port");
        Assert.assertFalse(sent.has("qos_policy_id"), sent.toString());
    }

    public void portUpdateSetAndClear() throws Exception {
        respondWith(200, PORT);
        respondWith(200, PORT);
        respondWith(200, PORT);
        respondWith(200, PORT);
        Port first = osv3().networking().port().get("p1");
        expect("GET", "/ports/p1");
        osv3().networking().port().update(first.toBuilder().qosPolicyId("q2").build());
        JsonNode set = body(expect("PUT", "/ports/p1")).get("port");
        Assert.assertEquals(set.get("qos_policy_id").asText(), "q2");
        Assert.assertFalse(set.has("qos_network_policy_id"), set.toString());

        Port second = osv3().networking().port().get("p1");
        expect("GET", "/ports/p1");
        osv3().networking().port().update(second.toBuilder().qosPolicyId(null).build());
        JsonNode cleared = body(expect("PUT", "/ports/p1")).get("port");
        Assert.assertTrue(cleared.has("qos_policy_id"), cleared.toString());
        Assert.assertTrue(cleared.get("qos_policy_id").isNull(), cleared.toString());
    }

    public void portFromResponseRenamedOnly() throws Exception {
        respondWith(200, PORT);
        respondWith(200, PORT);
        Port port = osv3().networking().port().get("p1");
        expect("GET", "/ports/p1");
        Assert.assertEquals(port.getQosPolicyId(), "q1");
        osv3().networking().port().update(port.toBuilder().name("x").build());
        JsonNode sent = body(expect("PUT", "/ports/p1")).get("port");
        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertFalse(sent.has("qos_policy_id"), sent.toString());
        Assert.assertFalse(sent.has("qos_network_policy_id"), sent.toString());
    }

    public void noInternalNamesLeak() throws Exception {
        NeutronPort fromResponse = ObjectMapperSingleton.getContext(NeutronPort.class).readValue(PORT, NeutronPort.class);
        assertClean(NeutronPort.class, fromResponse, false);
        assertClean(NeutronPort.class, Builders.port().networkId("n1").qosPolicyId("q1").build(), true);
        assertClean(NeutronPort.class, Builders.port().networkId("n1").qosPolicyId(null).build(), true);

        assertClean(NeutronPortUpdate.class, NeutronPortUpdate.fromPort(Builders.port().qosPolicyId("q1").build()), true);

        NeutronNetwork networkFromResponse = ObjectMapperSingleton.getContext(NeutronNetwork.class).readValue(
                "{\"network\":{\"id\":\"n1\",\"qos_policy_id\":\"q1\"}}", NeutronNetwork.class);
        assertClean(NeutronNetwork.class, networkFromResponse, false);
        assertClean(NeutronNetwork.class, Builders.network().name("n").qosPolicyId("q1").build(), true);

        NetworkUpdate off = Builders.networkUpdate().name("x").build();
        NetworkUpdate on = Builders.networkUpdate().qosPolicyId("q1").build();
        Assert.assertNull(off.getQosPolicyId());
        Assert.assertEquals(on.getQosPolicyId(), "q1");
        assertClean(NeutronNetworkUpdate.class, off, false);
        assertClean(NeutronNetworkUpdate.class, on, true);
    }

    private static void assertClean(Class<?> type, Object value, boolean sent) throws Exception {
        String json = ObjectMapperSingleton.getContext(type).writeValueAsString(value);
        String flat = json.toLowerCase(Locale.ROOT);
        for (String leaked : new String[]{"qospolicyidset", "qospolicyidrequested", "requestonlyfields", "extradhcpopts", "qosPolicyId".toLowerCase(Locale.ROOT)}) {
            Assert.assertFalse(flat.contains("\"" + leaked + "\""), leaked + " leaked: " + json);
        }
        if (sent) {
            Assert.assertTrue(json.contains("\"qos_policy_id\""), json);
            Assert.assertEquals(json.indexOf("\"qos_policy_id\""), json.lastIndexOf("\"qos_policy_id\""), json);
        } else {
            Assert.assertFalse(json.contains("qos_policy_id"), json);
        }
    }
}
