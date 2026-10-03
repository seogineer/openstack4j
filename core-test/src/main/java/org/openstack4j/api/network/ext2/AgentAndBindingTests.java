package org.openstack4j.api.network.ext2;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.PortBinding;
import org.openstack4j.model.network.options.PortBindingOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/AgentsBindings")
public class AgentAndBindingTests extends AbstractNetworkingExtTest {

    public void agentScheduling() throws Exception {
        respondWith(204);
        respondWith(200, "{\"networks\": [{\"id\": \"n1\", \"name\": \"net1\", \"status\": \"ACTIVE\"}]}");
        respondWith(200, "{\"routers\": [{\"id\": \"r1\", \"name\": \"router1\"}]}");
        respondWith(201);
        respondWith(204);
        respondWith(200, "{\"agents\": [{\"id\": \"a2\", \"agent_type\": \"DHCP agent\", \"host\": \"net1\", \"alive\": true}]}");

        var agents = osv3().networking().agent();
        boolean deleted = agents.delete("a1").isSuccess();
        Assert.assertEquals(agents.listDhcpNetworks("a1").get(0).getName(), "net1");
        Assert.assertEquals(agents.listL3Routers("a1").get(0).getId(), "r1");
        Assert.assertTrue(agents.addRouterToL3Agent("a1", "r1").isSuccess());
        Assert.assertTrue(agents.removeRouterFromL3Agent("a1", "r1").isSuccess());
        Assert.assertEquals(agents.listDhcpAgentsHostingNetwork("n1").get(0).getId(), "a2");

        expect("DELETE", "/v2.0/agents/a1");
        expect("GET", "/v2.0/agents/a1/dhcp-networks");
        expect("GET", "/v2.0/agents/a1/l3-routers");
        RecordedRequest add = expect("POST", "/v2.0/agents/a1/l3-routers");
        Assert.assertEquals(body(add).get("router_id").asText(), "r1");
        expect("DELETE", "/v2.0/agents/a1/l3-routers/r1");
        expect("GET", "/v2.0/networks/n1/dhcp-agents");
        Assert.assertTrue(deleted);
    }

    public void portBindings() throws Exception {
        String binding = "{\"host\": \"compute\", \"vif_type\": \"ovs\", \"vnic_type\": \"normal\", \"status\": \"%s\", \"profile\": {},"
                + " \"vif_details\": {\"connectivity\": \"l2\", \"port_filter\": true, \"ovs_hybrid_plug\": false, \"datapath_type\": \"system\", \"bridge_name\": \"br-int\"}}";
        respondWith(200, "{\"bindings\": [" + String.format(binding, "ACTIVE") + "]}");
        respondWith(201, "{\"binding\": " + String.format(binding, "INACTIVE") + "}");
        respondWith(200, "{\"binding\": " + String.format(binding, "ACTIVE") + "}");
        respondWith(204);

        var ports = osv3().networking().port();
        List<? extends PortBinding> all = ports.listBindings("p1");
        PortBinding created = ports.createBinding("p1", PortBindingOptions.create("compute").vnicType("normal"));
        PortBinding active = ports.activateBinding("p1", "compute");
        boolean deleted = ports.deleteBinding("p1", "compute").isSuccess();

        expect("GET", "/v2.0/ports/p1/bindings");
        RecordedRequest create = expect("POST", "/v2.0/ports/p1/bindings/");
        Assert.assertEquals(body(create).get("binding").get("host").asText(), "compute");
        expect("PUT", "/v2.0/ports/p1/bindings/compute/activate");
        expect("DELETE", "/v2.0/ports/p1/bindings/compute");
        Assert.assertEquals(all.get(0).getVifDetails().get("bridge_name"), "br-int");
        Assert.assertEquals(created.getStatus(), "INACTIVE");
        Assert.assertEquals(active.getStatus(), "ACTIVE");
        Assert.assertEquals(active.getProfile(), Map.of());
        Assert.assertTrue(deleted);
    }
}
