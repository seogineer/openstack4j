package org.openstack4j.api.network;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;
import org.openstack4j.api.Builders;
import org.openstack4j.api.network.ext2.AbstractNetworkingExtTest;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.State;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Port update sends a copy of only the writable attributes and leaves the caller's port untouched. */
@Test(suiteName = "Network/PortUpdate")
public class NeutronPortUpdateTests extends AbstractNetworkingExtTest {

    private static final String FULL_PORT = "{\"port\":{\"id\":\"p1\",\"name\":\"port\",\"network_id\":\"n1\",\"status\":\"ACTIVE\","
            + "\"tenant_id\":\"t1\",\"mac_address\":\"fa:16:3e:00:00:01\",\"admin_state_up\":true,\"device_id\":\"d1\","
            + "\"device_owner\":\"compute:nova\",\"binding:vif_type\":\"ovs\",\"binding:vif_details\":{\"port_filter\":true},"
            + "\"created_at\":\"2026-01-01T00:00:00Z\",\"updated_at\":\"2026-01-02T00:00:00Z\",\"description\":\"desc\","
            + "\"qos_policy_id\":\"q1\",\"qos_network_policy_id\":\"q2\","
            + "\"fixed_ips\":[{\"subnet_id\":\"s1\",\"ip_address\":\"10.0.0.5\"}],"
            + "\"allowed_address_pairs\":[{\"ip_address\":\"10.0.0.9\",\"mac_address\":\"fa:16:3e:00:00:09\"}],"
            + "\"security_groups\":[\"sg1\"],\"extra_dhcp_opts\":[{\"opt_name\":\"tftp-server\",\"opt_value\":\"10.0.0.1\"}],"
            + "\"binding:host_id\":\"h1\",\"binding:vnic_type\":\"normal\",\"binding:profile\":{\"k\":\"v\"},"
            + "\"port_security_enabled\":true}}";

    public void sameObjectCanBeUpdatedTwice() throws Exception {
        respondWith(200, FULL_PORT);
        respondWith(200, FULL_PORT);
        respondWith(200, FULL_PORT);
        Port port = osv3().networking().port().get("p1");
        expect("GET", "/ports/p1");

        osv3().networking().port().update(port);
        osv3().networking().port().update(port);
        expect("PUT", "/ports/p1");
        expect("PUT", "/ports/p1");

        Assert.assertEquals(port.getId(), "p1");
        Assert.assertEquals(port.getNetworkId(), "n1");
        Assert.assertEquals(port.getState(), State.ACTIVE);
        Assert.assertEquals(port.getTenantId(), "t1");
        Assert.assertEquals(port.getMacAddress(), "fa:16:3e:00:00:01");
        Assert.assertEquals(port.getVifType(), "ovs");
        Assert.assertNotNull(port.getVifDetails());
        Assert.assertEquals(port.getVifDetails().get("port_filter"), true);
    }

    public void updateBodyHasOnlyWritableKeys() throws Exception {
        respondWith(200, FULL_PORT);
        respondWith(200, FULL_PORT);
        Port port = osv3().networking().port().get("p1");
        expect("GET", "/ports/p1");

        osv3().networking().port().update(port);
        JsonNode sent = body(expect("PUT", "/ports/p1")).get("port");

        Set<String> keys = new TreeSet<>();
        for (Iterator<String> it = sent.fieldNames(); it.hasNext(); ) keys.add(it.next());
        Set<String> expected = new TreeSet<>(Arrays.asList("name", "admin_state_up", "device_id", "device_owner", "fixed_ips",
                "allowed_address_pairs", "security_groups", "extra_dhcp_opts", "port_security_enabled", "binding:host_id",
                "binding:vnic_type", "binding:profile"));
        Assert.assertEquals(keys, expected, sent.toString());
        for (String absent : new String[] {"id", "network_id", "status", "tenant_id", "mac_address", "binding:vif_type",
                "binding:vif_details", "description", "qos_policy_id", "qos_network_policy_id", "created_at", "updated_at"}) {
            Assert.assertFalse(sent.has(absent), absent + " must not be sent: " + sent);
        }
        Assert.assertEquals(sent.get("name").asText(), "port");
        Assert.assertEquals(sent.get("fixed_ips").get(0).get("ip_address").asText(), "10.0.0.5");
        Assert.assertEquals(new HashSet<>(Arrays.asList(sent.get("security_groups").get(0).asText())), new HashSet<>(Arrays.asList("sg1")));
    }

    public void builderPortUpdateUnchanged() throws Exception {
        respondWith(200, FULL_PORT);
        Port port = Builders.port().name("x").trunkPortType("t").build();
        port.setId("p2");

        osv3().networking().port().update(port);
        JsonNode sent = body(expect("PUT", "/ports/p2")).get("port");

        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertEquals(sent.get("trunkport:type").asText(), "t");
        Assert.assertTrue(sent.get("admin_state_up").asBoolean());
        Assert.assertFalse(sent.has("id"), sent.toString());
        Assert.assertEquals(port.getId(), "p2");
    }
}
