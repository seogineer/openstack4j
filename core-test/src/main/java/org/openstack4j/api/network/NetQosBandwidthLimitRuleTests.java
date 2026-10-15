package org.openstack4j.api.network;

import com.fasterxml.jackson.databind.JsonNode;
import org.openstack4j.api.network.ext2.AbstractNetworkingExtTest;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.NetQosPolicyBandwidthLimitRule;
import org.openstack4j.openstack.networking.domain.ext.NeutronNetQosPolicyBandwidthLimitRule;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test cases for (Neutron) qos policy bandwidth limit rule extension based Services
 *
 * @author bboyHan
 * @author slankka
 */
@Test(suiteName = "Network/qosPolicyBandwidthLimitRule")
public class NetQosBandwidthLimitRuleTests extends AbstractNetworkingExtTest {

    private static final String QOS_POLICY_BANDWIDTH_LIMIT_RULE_JSON = "/network/qos_bandwidth_limit_rule.json";
    private static final String QOS_POLICY_BANDWIDTH_LIMIT_RULES_JSON = "/network/qos_bandwidth_limit_rules.json";

    public void list() throws Exception {
        respondWith(QOS_POLICY_BANDWIDTH_LIMIT_RULES_JSON);
        List<? extends NetQosPolicyBandwidthLimitRule> qosPolicyBandwidthLimitRules = osv3().networking().netQosPolicyBandwidthLimitRule().list("policyId");
        assertEquals(1, qosPolicyBandwidthLimitRules.size());
        assertEquals(10000, (int)qosPolicyBandwidthLimitRules.get(0).getMaxKbps());
        assertNotNull(qosPolicyBandwidthLimitRules.get(0).getId());
        expect("GET", "/qos/policies/policyId/bandwidth_limit_rules");
    }

    public void get() throws Exception {
        respondWith(QOS_POLICY_BANDWIDTH_LIMIT_RULE_JSON);
        NetQosPolicyBandwidthLimitRule netQosPolicyBandwidthLimitRule = osv3().networking().netQosPolicyBandwidthLimitRule().get("networkId", "ruleId");
        assertEquals("5f126d84-551a-4dcf-bb01-0e9c0df0c793", netQosPolicyBandwidthLimitRule.getId());
        assertEquals("egress", netQosPolicyBandwidthLimitRule.getDirectionValue());
        assertEquals(NetQosPolicyBandwidthLimitRule.Direction.egress, netQosPolicyBandwidthLimitRule.getDirection());
        expect("GET", "/qos/policies/networkId/bandwidth_limit_rules/ruleId");
    }

    public void delete() throws Exception {
        respondWith(204);
        ActionResponse response = osv3().networking().netQosPolicyBandwidthLimitRule().delete("policyId", "ruleId");
        assertTrue(response.isSuccess());
        expect("DELETE", "/qos/policies/policyId/bandwidth_limit_rules/ruleId");
    }

    public void create() throws Exception {
        respondWith(QOS_POLICY_BANDWIDTH_LIMIT_RULE_JSON);
        NetQosPolicyBandwidthLimitRule netQosPolicyBandwidthLimitRule = osv3().networking().netQosPolicyBandwidthLimitRule()
                .create("policyId", NeutronNetQosPolicyBandwidthLimitRule.builder().maxKbps(10000).build());
        assertEquals("5f126d84-551a-4dcf-bb01-0e9c0df0c793", netQosPolicyBandwidthLimitRule.getId());
        assertEquals("egress", netQosPolicyBandwidthLimitRule.getDirectionValue());
        assertEquals(NetQosPolicyBandwidthLimitRule.Direction.egress, netQosPolicyBandwidthLimitRule.getDirection());
        expect("POST", "/qos/policies/policyId/bandwidth_limit_rules");
    }

    public void createBodyHasNoId() throws Exception {
        respondWith(QOS_POLICY_BANDWIDTH_LIMIT_RULE_JSON);
        NetQosPolicyBandwidthLimitRule rule = NeutronNetQosPolicyBandwidthLimitRule.builder().maxKbps(10000).build();
        rule.setId("r1");

        osv3().networking().netQosPolicyBandwidthLimitRule().create("p1", rule);

        String raw = expect("POST", "/qos/policies/p1/bandwidth_limit_rules").getBody().readUtf8();
        assertFalse(raw.contains("\"id\""), raw);
        assertEquals(rule.getId(), "r1");
    }

    public void updateKeepsIdAndOmitsItFromBody() throws Exception {
        NetQosPolicyBandwidthLimitRule rule = NeutronNetQosPolicyBandwidthLimitRule.builder().maxKbps(2000).build();
        rule.setId("r1");
        respondWith(QOS_POLICY_BANDWIDTH_LIMIT_RULE_JSON);
        respondWith(QOS_POLICY_BANDWIDTH_LIMIT_RULE_JSON);

        osv3().networking().netQosPolicyBandwidthLimitRule().update("p1", rule);
        osv3().networking().netQosPolicyBandwidthLimitRule().update("p1", rule);

        for (int i = 0; i < 2; i++) {
            String raw = expect("PUT", "/qos/policies/p1/bandwidth_limit_rules/r1").getBody().readUtf8();
            assertFalse(raw.contains("\"id\""), raw);
            JsonNode body = new com.fasterxml.jackson.databind.ObjectMapper().readTree(raw);
            assertEquals(body.get("bandwidth_limit_rule").get("max_kbps").asInt(), 2000);
        }
        assertEquals(rule.getId(), "r1");
    }
}
