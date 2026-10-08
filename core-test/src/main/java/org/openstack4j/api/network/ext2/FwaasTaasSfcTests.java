package org.openstack4j.api.network.ext2;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.FirewallGroup;
import org.openstack4j.model.network.ext.FirewallPolicyV2;
import org.openstack4j.model.network.ext.FirewallRuleV2;
import org.openstack4j.model.network.ext.SfcServiceGraph;
import org.openstack4j.model.network.ext.TapFlow;
import org.openstack4j.model.network.ext.TapMirror;
import org.openstack4j.model.network.options.FirewallGroupOptions;
import org.openstack4j.model.network.options.FirewallPolicyV2Options;
import org.openstack4j.model.network.options.FirewallRuleV2Options;
import org.openstack4j.model.network.options.SfcServiceGraphOptions;
import org.openstack4j.model.network.options.TapFlowOptions;
import org.openstack4j.model.network.options.TapMirrorOptions;
import org.openstack4j.model.network.options.TapServiceOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext/FwaasTaasSfc")
public class FwaasTaasSfcTests extends AbstractNetworkingExtTest {

    private static final String POLICY = "{\"audited\": false, \"description\": \"\", \"firewall_rules\": [\"r1\"], \"id\": \"p1\", \"name\": \"test-policy\", \"shared\": false}";

    public void firewallV2() throws Exception {
        respondWith(201, "{\"firewall_rule\": {\"action\": \"allow\", \"destination_port\": \"80\", \"enabled\": true, \"id\": \"r1\", \"name\": \"ALLOW_HTTP\", \"protocol\": \"tcp\", \"ip_version\": 4}}");
        respondWith(201, "{\"firewall_policy\": " + POLICY + "}");
        respondWith(201, "{\"firewall_group\": {\"id\": \"g1\", \"ingress_firewall_policy_id\": \"p1\", \"ports\": [\"port1\"], \"status\": \"PENDING_CREATE\", \"admin_state_up\": true}}");
        respondWith(200, POLICY.replace("[\"r1\"]", "[\"r2\", \"r1\"]"));
        respondWith(200, POLICY);
        respondWith(200, "{\"firewall_groups\": []}");
        respondWith(204);

        var networking = osv3().networking();
        FirewallRuleV2 rule = networking.firewallRulesV2().create(FirewallRuleV2Options.create().name("ALLOW_HTTP").action("allow").protocol("tcp").destinationPort("80"));
        FirewallPolicyV2 policy = networking.firewallPoliciesV2().create(FirewallPolicyV2Options.create("test-policy").firewallRules(List.of("r1")));
        FirewallGroup group = networking.firewallGroups().create(FirewallGroupOptions.create().ingressFirewallPolicyId("p1").ports(List.of("port1")));
        FirewallPolicyV2 inserted = networking.firewallPoliciesV2().insertRule("p1", "r2", "r1", null);
        FirewallPolicyV2 removed = networking.firewallPoliciesV2().removeRule("p1", "r2");
        networking.firewallGroups().list(Map.of("status", "ACTIVE"));
        Assert.assertTrue(networking.firewallRulesV2().delete("r1").isSuccess());

        Assert.assertEquals(body(expect("POST", "/v2.0/fwaas/firewall_rules")).toString(),
                "{\"firewall_rule\":{\"name\":\"ALLOW_HTTP\",\"action\":\"allow\",\"protocol\":\"tcp\",\"destination_port\":\"80\"}}");
        Assert.assertEquals(body(expect("POST", "/v2.0/fwaas/firewall_policies")).toString(), "{\"firewall_policy\":{\"name\":\"test-policy\",\"firewall_rules\":[\"r1\"]}}");
        Assert.assertEquals(body(expect("POST", "/v2.0/fwaas/firewall_groups")).toString(), "{\"firewall_group\":{\"ingress_firewall_policy_id\":\"p1\",\"ports\":[\"port1\"]}}");
        Assert.assertEquals(body(expect("PUT", "/v2.0/fwaas/firewall_policies/p1/insert_rule")).toString(),
                "{\"firewall_rule_id\":\"r2\",\"insert_before\":\"r1\",\"insert_after\":\"\"}");
        Assert.assertEquals(body(expect("PUT", "/v2.0/fwaas/firewall_policies/p1/remove_rule")).toString(), "{\"firewall_rule_id\":\"r2\"}");
        expect("GET", "/v2.0/fwaas/firewall_groups?status=ACTIVE");
        expect("DELETE", "/v2.0/fwaas/firewall_rules/r1");
        Assert.assertEquals(rule.getDestinationPort(), "80");
        Assert.assertEquals(policy.getFirewallRules(), List.of("r1"));
        Assert.assertEquals(group.getPorts(), List.of("port1"));
        Assert.assertEquals(inserted.getFirewallRules(), List.of("r2", "r1"));
        Assert.assertEquals(removed.getName(), "test-policy");
    }

    public void tapAsAService() throws Exception {
        respondWith(201, "{\"tap_service\": {\"id\": \"ts1\", \"name\": \"ts1\", \"port_id\": \"port1\", \"status\": \"DOWN\"}}");
        respondWith(201, "{\"tap_flow\": {\"id\": \"tf1\", \"tap_service_id\": \"ts1\", \"source_port\": \"port2\", \"direction\": \"BOTH\", \"vlan_filter\": null}}");
        respondWith(201, "{\"tap_mirror\": {\"id\": \"tm1\", \"port_id\": \"port3\", \"directions\": {\"IN\": 99, \"OUT\": 100}, \"remote_ip\": \"100.109.0.142\", \"mirror_type\": \"erspanv1\"}}");
        respondWith(200, "{\"tap_flows\": []}");
        respondWith(204);

        var networking = osv3().networking();
        networking.tapServices().create(TapServiceOptions.create("port1").name("ts1"));
        TapFlow flow = networking.tapFlows().create(TapFlowOptions.create("ts1", "port2", "BOTH"));
        TapMirror mirror = networking.tapMirrors().create(TapMirrorOptions.create("port3", Map.of("IN", 99), "100.109.0.142", "erspanv1"));
        networking.tapFlows().list();
        Assert.assertTrue(networking.tapServices().delete("ts1").isSuccess());

        Assert.assertEquals(body(expect("POST", "/v2.0/taas/tap_services")).toString(), "{\"tap_service\":{\"port_id\":\"port1\",\"name\":\"ts1\"}}");
        Assert.assertEquals(body(expect("POST", "/v2.0/taas/tap_flows")).toString(), "{\"tap_flow\":{\"tap_service_id\":\"ts1\",\"source_port\":\"port2\",\"direction\":\"BOTH\"}}");
        RecordedRequest mirrorRequest = expect("POST", "/v2.0/taas/tap_mirrors");
        Assert.assertEquals(body(mirrorRequest).get("tap_mirror").get("directions").get("IN").asInt(), 99);
        expect("GET", "/v2.0/taas/tap_flows");
        expect("DELETE", "/v2.0/taas/tap_services/ts1");
        Assert.assertEquals(flow.getDirection(), "BOTH");
        Assert.assertEquals(mirror.getDirections().get("OUT"), 100);
    }

    public void sfcServiceGraphs() throws Exception {
        String graph = "{\"id\": \"sg1\", \"name\": \"graph\", \"port_chains\": {\"pc1\": [\"pc2\"]}}";
        respondWith(201, "{\"service_graph\": " + graph + "}");
        respondWith(200, "{\"service_graph\": " + graph + "}");
        respondWith(204);

        SfcServiceGraph created = osv3().networking().sfcServiceGraphs().create(SfcServiceGraphOptions.create(Map.of("pc1", List.of("pc2"))).name("graph"));
        osv3().networking().sfcServiceGraphs().update("sg1", SfcServiceGraphOptions.update().description("d"));
        Assert.assertTrue(osv3().networking().sfcServiceGraphs().delete("sg1").isSuccess());

        Assert.assertEquals(body(expect("POST", "/v2.0/sfc/service_graphs")).toString(), "{\"service_graph\":{\"port_chains\":{\"pc1\":[\"pc2\"]},\"name\":\"graph\"}}");
        Assert.assertEquals(body(expect("PUT", "/v2.0/sfc/service_graphs/sg1")).toString(), "{\"service_graph\":{\"description\":\"d\"}}");
        expect("DELETE", "/v2.0/sfc/service_graphs/sg1");
        Assert.assertEquals(created.getPortChains().get("pc1"), List.of("pc2"));
    }
}
