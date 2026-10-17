package org.openstack4j.api.network.ext2;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import org.openstack4j.api.Builders;
import org.openstack4j.model.gbp.ExternalSegment;
import org.openstack4j.model.gbp.L2Policy;
import org.openstack4j.model.gbp.L3Policy;
import org.openstack4j.model.gbp.NatPool;
import org.openstack4j.model.gbp.NetworkServicePolicy;
import org.openstack4j.model.gbp.PolicyRule;
import org.openstack4j.model.gbp.PolicyRuleSet;
import org.openstack4j.model.gbp.PolicyTarget;
import org.openstack4j.model.network.ext.FlowClassifier;
import org.openstack4j.model.network.ext.PortChain;
import org.openstack4j.model.network.ext.PortPair;
import org.openstack4j.model.network.ext.PortPairGroup;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Updating with an object read from a response must not send the attributes the server only returns (id, project/tenant id and,
 * for SFC, the immutable wiring). Covers the Service Function Chaining and Group Based Policy updates, which take the response model.
 */
@Test(suiteName = "Network/ReadOnlyUpdateBody")
public class ReadOnlyUpdateBodyTests extends AbstractNetworkingExtTest {

    private static final String PORT_PAIR = "{\"port_pair\":{\"id\":\"pp1\",\"name\":\"pp\",\"project_id\":\"t1\",\"tenant_id\":\"t1\","
            + "\"description\":\"d\",\"ingress\":\"in1\",\"egress\":\"out1\",\"service_function_parameters\":{\"correlation\":null}}}";
    private static final String PORT_PAIR_GROUP = "{\"port_pair_group\":{\"id\":\"ppg1\",\"name\":\"ppg\",\"project_id\":\"t1\","
            + "\"description\":\"d\",\"port_pairs\":[\"pp1\"],\"port_pair_group_parameters\":{\"lb_fields\":[]}}}";
    private static final String PORT_CHAIN = "{\"port_chain\":{\"id\":\"pc1\",\"name\":\"pc\",\"project_id\":\"t1\",\"description\":\"d\","
            + "\"chain_id\":1,\"flow_classifiers\":[\"fc1\"],\"port_pair_groups\":[\"ppg1\"],\"chain_parameters\":{\"symmetric\":\"false\"}}}";
    private static final String FLOW_CLASSIFIER = "{\"flow_classifier\":{\"id\":\"fc1\",\"name\":\"fc\",\"project_id\":\"t1\",\"description\":\"d\","
            + "\"protocol\":\"tcp\",\"source_ip_prefix\":\"10.0.0.0/24\"}}";

    private static final String L2 = "{\"l2_policy\":{\"id\":\"l2\",\"name\":\"l2n\",\"tenant_id\":\"t1\",\"description\":\"d\",\"shared\":false,"
            + "\"l3_policy_id\":\"l3\",\"network_id\":\"n1\",\"policy_target_groups\":[\"g1\"]}}";
    private static final String L3 = "{\"l3_policy\":{\"id\":\"l3\",\"name\":\"l3n\",\"tenant_id\":\"t1\",\"description\":\"d\",\"shared\":false,"
            + "\"ip_version\":4,\"ip_pool\":\"10.0.0.0/8\",\"l2_policies\":[\"l2\"]}}";
    private static final String EXTERNAL_SEGMENT = "{\"external_segment\":{\"id\":\"es\",\"name\":\"esn\",\"tenant_id\":\"t1\",\"description\":\"d\","
            + "\"shared\":false,\"ip_version\":4,\"cidr\":\"10.1.0.0/24\"}}";
    private static final String NAT_POOL = "{\"nat_pool\":{\"id\":\"np\",\"name\":\"npn\",\"tenant_id\":\"t1\",\"description\":\"d\",\"shared\":false,"
            + "\"ip_pool\":\"10.2.0.0/24\"}}";
    private static final String POLICY_TARGET = "{\"policy_target\":{\"id\":\"pt\",\"name\":\"ptn\",\"tenant_id\":\"t1\",\"description\":\"d\"}}";
    private static final String POLICY_RULE = "{\"policy_rule\":{\"id\":\"pr\",\"name\":\"prn\",\"tenant_id\":\"t1\",\"description\":\"d\",\"shared\":false,"
            + "\"enabled\":true}}";
    private static final String POLICY_RULE_SET = "{\"policy_rule_set\":{\"id\":\"prs\",\"name\":\"prsn\",\"tenant_id\":\"t1\",\"description\":\"d\","
            + "\"shared\":false,\"policy_rules\":[\"pr\"]}}";
    private static final String NETWORK_SERVICE_POLICY = "{\"network_service_policy\":{\"id\":\"nsp\",\"name\":\"nspn\",\"tenant_id\":\"t1\","
            + "\"description\":\"d\",\"shared\":false}}";

    /** get, then update twice with the object that came back; returns the first update body (the second must be identical). */
    private <T> JsonNode getThenUpdateTwice(String json, String path, Supplier<T> get, Consumer<T> update) throws Exception {
        respondWith(200, json);
        respondWith(200, json);
        respondWith(200, json);
        T model = get.get();
        expect("GET", path);
        update.accept(model);
        JsonNode first = body(expect("PUT", path));
        update.accept(model);
        JsonNode second = body(expect("PUT", path));
        Assert.assertEquals(second, first, "the second update with the same object must send the same body");
        return first;
    }

    private static void assertAbsent(JsonNode sent, String... keys) {
        for (String key : keys)
            Assert.assertFalse(sent.has(key), key + " must not be sent: " + sent);
    }

    // ---- SFC ----

    public void portPairUpdateDropsReadOnly() throws Exception {
        PortPair[] held = new PortPair[1];
        JsonNode sent = getThenUpdateTwice(PORT_PAIR, "/sfc/port_pairs/pp1", () -> held[0] = osv3().sfc().portpairs().get("pp1"),
                m -> osv3().sfc().portpairs().update("pp1", m)).get("port_pair");
        assertAbsent(sent, "id", "project_id", "tenant_id");
        Assert.assertEquals(sent.get("ingress").asText(), "in1");
        Assert.assertEquals(sent.get("egress").asText(), "out1");
        Assert.assertEquals(sent.get("name").asText(), "pp");
        Assert.assertEquals(sent.get("description").asText(), "d");
        Assert.assertTrue(sent.has("service_function_parameters"), sent.toString());
        Assert.assertEquals(held[0].getId(), "pp1");
        Assert.assertEquals(held[0].getIngressId(), "in1");
        Assert.assertEquals(held[0].getEgressId(), "out1");
    }

    public void portPairBuilderValuesAreSent() throws Exception {
        respondWith(200, PORT_PAIR);
        PortPair pair = Builders.portPair().name("x").description("y").serviceFunctionParameters(java.util.Collections.singletonMap("a", "b")).build();
        osv3().sfc().portpairs().update("pp9", pair);
        JsonNode sent = body(expect("PUT", "/sfc/port_pairs/pp9")).get("port_pair");
        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertEquals(sent.get("description").asText(), "y");
        Assert.assertEquals(sent.get("service_function_parameters").get("a").asText(), "b");
    }

    public void portPairBuilderIngressEgressAreSent() throws Exception {
        respondWith(200, PORT_PAIR);
        PortPair pair = Builders.portPair().name("x").ingressId("i9").egressId("e9").build();
        osv3().sfc().portpairs().update("pp8", pair);
        JsonNode sent = body(expect("PUT", "/sfc/port_pairs/pp8")).get("port_pair");
        Assert.assertEquals(sent.get("ingress").asText(), "i9");
        Assert.assertEquals(sent.get("egress").asText(), "e9");
    }

    public void portPairGroupUpdateDropsReadOnly() throws Exception {
        PortPairGroup[] held = new PortPairGroup[1];
        JsonNode sent = getThenUpdateTwice(PORT_PAIR_GROUP, "/sfc/port_pair_groups/ppg1",
                () -> held[0] = osv3().sfc().portpairgroups().get("ppg1"), m -> osv3().sfc().portpairgroups().update("ppg1", m))
                .get("port_pair_group");
        assertAbsent(sent, "id", "project_id", "tenant_id");
        Assert.assertEquals(sent.get("name").asText(), "ppg");
        Assert.assertEquals(sent.get("port_pairs").get(0).asText(), "pp1");
        Assert.assertEquals(held[0].getId(), "ppg1");
    }

    public void portPairGroupBuilderValuesAreSent() throws Exception {
        respondWith(200, PORT_PAIR_GROUP);
        PortPairGroup group = Builders.portPairGroup().name("x").portPairs(Arrays.asList("p1", "p2")).build();
        osv3().sfc().portpairgroups().update("ppg9", group);
        JsonNode sent = body(expect("PUT", "/sfc/port_pair_groups/ppg9")).get("port_pair_group");
        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertEquals(sent.get("port_pairs").size(), 2);
    }

    public void portChainUpdateDropsReadOnly() throws Exception {
        PortChain[] held = new PortChain[1];
        JsonNode sent = getThenUpdateTwice(PORT_CHAIN, "/sfc/port_chains/pc1", () -> held[0] = osv3().sfc().portchains().get("pc1"),
                m -> osv3().sfc().portchains().update("pc1", m)).get("port_chain");
        assertAbsent(sent, "id", "project_id", "tenant_id");
        Assert.assertEquals(sent.get("chain_id").asInt(), 1);
        Assert.assertEquals(sent.get("name").asText(), "pc");
        Assert.assertEquals(sent.get("flow_classifiers").get(0).asText(), "fc1");
        Assert.assertEquals(sent.get("port_pair_groups").get(0).asText(), "ppg1");
        Assert.assertEquals(held[0].getId(), "pc1");
    }

    public void portChainBuilderValuesAreSent() throws Exception {
        respondWith(200, PORT_CHAIN);
        PortChain chain = Builders.portChain().name("x").flowClassifiers(Arrays.asList("f1")).portPairGroups(Arrays.asList("g1")).build();
        osv3().sfc().portchains().update("pc9", chain);
        JsonNode sent = body(expect("PUT", "/sfc/port_chains/pc9")).get("port_chain");
        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertEquals(sent.get("flow_classifiers").get(0).asText(), "f1");
        Assert.assertEquals(sent.get("port_pair_groups").get(0).asText(), "g1");
    }

    public void portChainBuilderChainIdIsSent() throws Exception {
        respondWith(200, PORT_CHAIN);
        osv3().sfc().portchains().update("pc8", Builders.portChain().name("x").chainId("7").build());
        JsonNode sent = body(expect("PUT", "/sfc/port_chains/pc8")).get("port_chain");
        Assert.assertEquals(sent.get("chain_id").asText(), "7");
    }

    public void flowClassifierUpdateDropsReadOnly() throws Exception {
        FlowClassifier[] held = new FlowClassifier[1];
        JsonNode sent = getThenUpdateTwice(FLOW_CLASSIFIER, "/sfc/flow_classifiers/fc1",
                () -> held[0] = osv3().sfc().flowclassifiers().get("fc1"), m -> osv3().sfc().flowclassifiers().update("fc1", m))
                .get("flow_classifier");
        assertAbsent(sent, "id", "project_id", "tenant_id");
        Assert.assertEquals(sent.get("name").asText(), "fc");
        Assert.assertEquals(sent.get("description").asText(), "d");
        Assert.assertEquals(held[0].getId(), "fc1");
        Assert.assertEquals(held[0].getTenantId(), "t1");
    }

    public void flowClassifierBuilderValuesAreSent() throws Exception {
        respondWith(200, FLOW_CLASSIFIER);
        FlowClassifier fc = Builders.flowClassifier().name("x").description("y").build();
        osv3().sfc().flowclassifiers().update("fc9", fc);
        JsonNode sent = body(expect("PUT", "/sfc/flow_classifiers/fc9")).get("flow_classifier");
        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertEquals(sent.get("description").asText(), "y");
    }

    // ---- GBP ----

    public void l2PolicyUpdateDropsReadOnly() throws Exception {
        L2Policy[] held = new L2Policy[1];
        JsonNode sent = getThenUpdateTwice(L2, "/grouppolicy/l2_policies/l2", () -> held[0] = osv3().gbp().l2Policy().get("l2"),
                m -> osv3().gbp().l2Policy().update("l2", m)).get("l2_policy");
        assertAbsent(sent, "id", "tenant_id");
        Assert.assertEquals(sent.get("name").asText(), "l2n");
        Assert.assertEquals(sent.get("description").asText(), "d");
        Assert.assertEquals(held[0].getId(), "l2");
        Assert.assertEquals(held[0].getTenantId(), "t1");
    }

    public void l2PolicyBuilderValuesAreSent() throws Exception {
        respondWith(200, L2);
        osv3().gbp().l2Policy().update("l29", Builders.l2Policy().name("x").description("y").l3PolicyId("l3x").build());
        JsonNode sent = body(expect("PUT", "/grouppolicy/l2_policies/l29")).get("l2_policy");
        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertEquals(sent.get("description").asText(), "y");
        Assert.assertEquals(sent.get("l3_policy_id").asText(), "l3x");
    }

    public void l3PolicyUpdateDropsReadOnly() throws Exception {
        L3Policy[] held = new L3Policy[1];
        JsonNode sent = getThenUpdateTwice(L3, "/grouppolicy/l3_policies/l3", () -> held[0] = osv3().gbp().l3Policy().get("l3"),
                m -> osv3().gbp().l3Policy().update("l3", m)).get("l3_policy");
        assertAbsent(sent, "id", "tenant_id");
        Assert.assertEquals(sent.get("name").asText(), "l3n");
        Assert.assertEquals(held[0].getId(), "l3");
        Assert.assertEquals(held[0].getTenantId(), "t1");
    }

    public void l3PolicyBuilderValuesAreSent() throws Exception {
        respondWith(200, L3);
        osv3().gbp().l3Policy().update("l39", Builders.l3Policy().name("x").description("y").build());
        JsonNode sent = body(expect("PUT", "/grouppolicy/l3_policies/l39")).get("l3_policy");
        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertEquals(sent.get("description").asText(), "y");
    }

    public void externalSegmentUpdateDropsReadOnly() throws Exception {
        ExternalSegment[] held = new ExternalSegment[1];
        JsonNode sent = getThenUpdateTwice(EXTERNAL_SEGMENT, "/grouppolicy/external_segments/es",
                () -> held[0] = osv3().gbp().externalSegment().get("es"), m -> osv3().gbp().externalSegment().update("es", m))
                .get("external_segment");
        assertAbsent(sent, "id", "tenant_id");
        Assert.assertEquals(sent.get("name").asText(), "esn");
        Assert.assertEquals(sent.get("cidr").asText(), "10.1.0.0/24");
        Assert.assertEquals(held[0].getId(), "es");
        Assert.assertEquals(held[0].getTenantId(), "t1");
    }

    public void externalSegmentBuilderValuesAreSent() throws Exception {
        respondWith(200, EXTERNAL_SEGMENT);
        osv3().gbp().externalSegment().update("es9", Builders.externalSegment().name("x").cidr("10.9.0.0/24").build());
        JsonNode sent = body(expect("PUT", "/grouppolicy/external_segments/es9")).get("external_segment");
        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertEquals(sent.get("cidr").asText(), "10.9.0.0/24");
    }

    public void natPoolUpdateDropsReadOnly() throws Exception {
        NatPool[] held = new NatPool[1];
        JsonNode sent = getThenUpdateTwice(NAT_POOL, "/grouppolicy/nat_pools/np", () -> held[0] = osv3().gbp().natPool().get("np"),
                m -> osv3().gbp().natPool().update("np", m)).get("nat_pool");
        assertAbsent(sent, "id", "tenant_id");
        Assert.assertEquals(sent.get("name").asText(), "npn");
        Assert.assertEquals(sent.get("ip_pool").asText(), "10.2.0.0/24");
        Assert.assertEquals(held[0].getId(), "np");
        Assert.assertEquals(held[0].getTenantId(), "t1");
    }

    public void natPoolBuilderValuesAreSent() throws Exception {
        respondWith(200, NAT_POOL);
        osv3().gbp().natPool().update("np9", Builders.natPool().name("x").cidr("10.8.0.0/24").build());
        JsonNode sent = body(expect("PUT", "/grouppolicy/nat_pools/np9")).get("nat_pool");
        Assert.assertEquals(sent.get("name").asText(), "x");
    }

    public void policyTargetUpdateDropsReadOnly() throws Exception {
        PolicyTarget[] held = new PolicyTarget[1];
        JsonNode sent = getThenUpdateTwice(POLICY_TARGET, "/grouppolicy/policy_targets/pt",
                () -> held[0] = osv3().gbp().policyTarget().get("pt"), m -> osv3().gbp().policyTarget().update("pt", m))
                .get("policy_target");
        assertAbsent(sent, "id", "tenant_id");
        Assert.assertEquals(sent.get("name").asText(), "ptn");
        Assert.assertEquals(held[0].getId(), "pt");
        Assert.assertEquals(held[0].getTenantId(), "t1");
    }

    public void policyTargetBuilderValuesAreSent() throws Exception {
        respondWith(200, POLICY_TARGET);
        osv3().gbp().policyTarget().update("pt9", Builders.policyTarget().name("x").description("y").build());
        JsonNode sent = body(expect("PUT", "/grouppolicy/policy_targets/pt9")).get("policy_target");
        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertEquals(sent.get("description").asText(), "y");
    }

    public void policyRuleUpdateDropsReadOnly() throws Exception {
        PolicyRule[] held = new PolicyRule[1];
        JsonNode sent = getThenUpdateTwice(POLICY_RULE, "/grouppolicy/policy_rules/pr", () -> held[0] = osv3().gbp().policyRule().get("pr"),
                m -> osv3().gbp().policyRule().update("pr", m)).get("policy_rule");
        assertAbsent(sent, "id", "tenant_id");
        Assert.assertEquals(sent.get("name").asText(), "prn");
        Assert.assertEquals(held[0].getId(), "pr");
        Assert.assertEquals(held[0].getTenantId(), "t1");
    }

    public void policyRuleBuilderValuesAreSent() throws Exception {
        respondWith(200, POLICY_RULE);
        osv3().gbp().policyRule().update("pr9", Builders.policyRule().name("x").description("y").build());
        JsonNode sent = body(expect("PUT", "/grouppolicy/policy_rules/pr9")).get("policy_rule");
        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertEquals(sent.get("description").asText(), "y");
    }

    public void policyRuleSetUpdateDropsReadOnly() throws Exception {
        PolicyRuleSet[] held = new PolicyRuleSet[1];
        JsonNode sent = getThenUpdateTwice(POLICY_RULE_SET, "/grouppolicy/policy_rule_sets/prs",
                () -> held[0] = osv3().gbp().policyRuleSet().get("prs"), m -> osv3().gbp().policyRuleSet().update("prs", m))
                .get("policy_rule_set");
        assertAbsent(sent, "id", "tenant_id");
        Assert.assertEquals(sent.get("name").asText(), "prsn");
        Assert.assertEquals(sent.get("policy_rules").get(0).asText(), "pr");
        Assert.assertEquals(held[0].getId(), "prs");
        Assert.assertEquals(held[0].getTenantId(), "t1");
    }

    public void policyRuleSetBuilderValuesAreSent() throws Exception {
        respondWith(200, POLICY_RULE_SET);
        osv3().gbp().policyRuleSet().update("prs9", Builders.policyRuleSet().name("x").rules(Arrays.asList("r1")).build());
        JsonNode sent = body(expect("PUT", "/grouppolicy/policy_rule_sets/prs9")).get("policy_rule_set");
        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertEquals(sent.get("policy_rules").get(0).asText(), "r1");
    }

    public void networkServicePolicyUpdateDropsReadOnly() throws Exception {
        NetworkServicePolicy[] held = new NetworkServicePolicy[1];
        JsonNode sent = getThenUpdateTwice(NETWORK_SERVICE_POLICY, "/grouppolicy/network_service_policies/nsp",
                () -> held[0] = osv3().gbp().networkPolicyService().get("nsp"),
                m -> osv3().gbp().networkPolicyService().update("nsp", m)).get("network_service_policy");
        assertAbsent(sent, "id", "tenant_id");
        Assert.assertEquals(sent.get("name").asText(), "nspn");
        Assert.assertEquals(held[0].getId(), "nsp");
        Assert.assertEquals(held[0].getTenantId(), "t1");
    }

    public void networkServicePolicyBuilderValuesAreSent() throws Exception {
        respondWith(200, NETWORK_SERVICE_POLICY);
        osv3().gbp().networkPolicyService().update("nsp9", Builders.networkServicePolicy().name("x").description("y").build());
        JsonNode sent = body(expect("PUT", "/grouppolicy/network_service_policies/nsp9")).get("network_service_policy");
        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertEquals(sent.get("description").asText(), "y");
    }
}
