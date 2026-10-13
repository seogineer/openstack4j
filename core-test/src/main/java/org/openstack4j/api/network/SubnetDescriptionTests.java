package org.openstack4j.api.network;

import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import org.openstack4j.api.Builders;
import org.openstack4j.api.network.ext2.AbstractNetworkingExtTest;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.networking.domain.NeutronSubnet;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Neutron subnet description: sent only when set through the builder, read from responses. */
@Test(suiteName = "Network/SubnetDescription")
public class SubnetDescriptionTests extends AbstractNetworkingExtTest {

    private static final String SUBNET = "{\"subnet\":{\"id\":\"s1\",\"name\":\"sub\",\"network_id\":\"n1\",\"cidr\":\"10.0.0.0/24\","
            + "\"ip_version\":4,\"gateway_ip\":\"10.0.0.1\",\"enable_dhcp\":true,\"description\":\"d\"}}";

    public void subnetDescriptionRead() throws Exception {
        respondWith(200, SUBNET);
        Subnet subnet = osv3().networking().subnet().get("s1");
        expect("GET", "/subnets/s1");
        Assert.assertEquals(subnet.getDescription(), "d");
    }

    public void subnetUpdateSendsDescription() throws Exception {
        respondWith(200, SUBNET);
        osv3().networking().subnet().update("s1", Builders.subnet().name("s").description("new").build());
        JsonNode sent = body(expect("PUT", "/subnets/s1")).get("subnet");
        Assert.assertEquals(sent.get("description").asText(), "new");
        Assert.assertTrue(sent.has("gateway_ip"), sent.toString());
        Assert.assertTrue(sent.get("gateway_ip").isNull(), sent.toString());
    }

    public void subnetCreateSendsDescription() throws Exception {
        respondWith(201, SUBNET);
        osv3().networking().subnet().create(Builders.subnet().networkId("n1").cidr("10.0.0.0/24").description("d").build());
        JsonNode sent = body(expect("POST", "/subnets")).get("subnet");
        Assert.assertEquals(sent.get("description").asText(), "d");
    }

    public void subnetNoGatewayKeepsDescription() throws Exception {
        respondWith(201, SUBNET);
        osv3().networking().subnet().create(Builders.subnet().networkId("n1").cidr("10.0.0.0/24").noGateway().description("d").build());
        JsonNode sent = body(expect("POST", "/subnets")).get("subnet");
        Assert.assertEquals(sent.get("description").asText(), "d");
        Assert.assertTrue(sent.has("gateway_ip"), sent.toString());
        Assert.assertTrue(sent.get("gateway_ip").isNull(), sent.toString());
    }

    public void descriptionFromResponseIsNotSentBack() throws Exception {
        respondWith(200, SUBNET);
        respondWith(200, SUBNET);
        respondWith(201, SUBNET);
        Subnet subnet = osv3().networking().subnet().get("s1");
        expect("GET", "/subnets/s1");
        osv3().networking().subnet().update(subnet);
        JsonNode updated = body(expect("PUT", "/subnets/s1")).get("subnet");
        Assert.assertFalse(updated.has("description"), updated.toString());

        osv3().networking().subnet().create(Builders.subnet().networkId("n1").cidr("10.0.0.0/24").build());
        JsonNode created = body(expect("POST", "/subnets")).get("subnet");
        Assert.assertFalse(created.has("description"), created.toString());
    }

    public void noInternalNamesLeak() throws Exception {
        NeutronSubnet fromResponse = ObjectMapperSingleton.getContext(NeutronSubnet.class).readValue(SUBNET, NeutronSubnet.class);
        assertClean(fromResponse, false);
        assertClean(Builders.subnet().networkId("n1").description("d").build(), true);
        assertClean(Builders.subnet().networkId("n1").noGateway().description("d").build(), true);
        assertClean(Builders.subnet().networkId("n1").description(null).build(), false);
    }

    private static void assertClean(Object value, boolean sent) throws Exception {
        String json = ObjectMapperSingleton.getContext(NeutronSubnet.class).writeValueAsString(value);
        String flat = json.toLowerCase(Locale.ROOT);
        for (String leaked : new String[]{"descriptionset", "descriptionrequested", "requestonlyfields"}) {
            Assert.assertFalse(flat.contains("\"" + leaked + "\""), leaked + " leaked: " + json);
        }
        if (sent) {
            Assert.assertEquals(json.indexOf("\"description\""), json.lastIndexOf("\"description\""), json);
            Assert.assertTrue(json.contains("\"description\""), json);
        } else {
            Assert.assertFalse(json.contains("\"description\""), json);
        }
    }
}
