package org.openstack4j.api.network.ext2;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.QosDscpMarkingRule;
import org.openstack4j.model.network.ext.QosMinimumBandwidthRule;
import org.openstack4j.model.network.ext.QosMinimumPacketRateRule;
import org.openstack4j.model.network.ext.QosPacketRateLimitRule;
import org.openstack4j.model.network.ext.QosRuleType;
import org.openstack4j.model.network.options.QosRuleOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/QosRules")
public class QosRuleTests extends AbstractNetworkingExtTest {

    private static final String POLICY = "23f44e76-cb35-42fe-99fe-e73de56721f1";
    private static final String P = "/v2.0/qos/policies/" + POLICY;

    public void ruleTypes() throws Exception {
        respondWith(200, "{\"rule_types\": [{\"type\": \"bandwidth_limit\"}, {\"type\": \"minimum_bandwidth\"}, {\"type\": \"dscp_marking\"}]}");
        List<? extends QosRuleType> types = osv3().networking().qosRules().ruleTypes();
        expect("GET", "/v2.0/qos/rule-types");
        Assert.assertEquals(types.size(), 3);
        Assert.assertEquals(types.get(2).getType(), "dscp_marking");
    }

    public void ruleTypeDetailsReadDrivers() throws Exception {
        respondWith(200, "{\"rule_type\": {\"type\": \"dscp_marking\", \"drivers\": [{\"name\": \"OVNQosDriver\", \"supported_parameters\": "
                + "[{\"parameter_name\": \"dscp_mark\", \"parameter_values\": [0, 8, 10], \"parameter_type\": \"choices\"}]}]}}");
        QosRuleType type = osv3().networking().qosRules().ruleType("dscp_marking");
        expect("GET", "/v2.0/qos/rule-types/dscp_marking");
        Assert.assertEquals(type.getType(), "dscp_marking");
        Assert.assertEquals(type.getDrivers().get(0).get("name"), "OVNQosDriver");
    }

    public void dscpMarkingRules() throws Exception {
        String rule = "{\"id\": \"5f126d84-551a-4dcf-bb01-0e9c0df0c794\", \"dscp_mark\": 26}";
        respondWith(201, "{\"dscp_marking_rule\": " + rule + "}");
        respondWith(200, "{\"dscp_marking_rules\": [" + rule + "]}");
        respondWith(200, "{\"dscp_marking_rule\": " + rule + "}");
        respondWith(200, "{\"dscp_marking_rule\": {\"id\": \"5f126d84-551a-4dcf-bb01-0e9c0df0c794\", \"dscp_mark\": 16}}");
        respondWith(204);

        var qos = osv3().networking().qosRules();
        QosDscpMarkingRule created = qos.createDscpMarkingRule(POLICY, QosRuleOptions.dscpMarking(26));
        List<? extends QosDscpMarkingRule> all = qos.listDscpMarkingRules(POLICY);
        qos.getDscpMarkingRule(POLICY, created.getId());
        QosDscpMarkingRule updated = qos.updateDscpMarkingRule(POLICY, created.getId(), QosRuleOptions.update().dscpMark(16));
        boolean deleted = qos.deleteDscpMarkingRule(POLICY, created.getId()).isSuccess();

        RecordedRequest create = expect("POST", P + "/dscp_marking_rules");
        Assert.assertEquals(body(create).get("dscp_marking_rule").get("dscp_mark").asInt(), 26);
        Assert.assertEquals(body(create).get("dscp_marking_rule").size(), 1);
        expect("GET", P + "/dscp_marking_rules");
        expect("GET", P + "/dscp_marking_rules/" + created.getId());
        RecordedRequest update = expect("PUT", P + "/dscp_marking_rules/" + created.getId());
        Assert.assertEquals(body(update).get("dscp_marking_rule").get("dscp_mark").asInt(), 16);
        expect("DELETE", P + "/dscp_marking_rules/" + created.getId());
        Assert.assertEquals(created.getDscpMark(), Integer.valueOf(26));
        Assert.assertEquals(all.size(), 1);
        Assert.assertEquals(updated.getDscpMark(), Integer.valueOf(16));
        Assert.assertTrue(deleted);
    }

    public void minimumBandwidthAndPacketRules() throws Exception {
        respondWith(201, "{\"minimum_bandwidth_rule\": {\"id\": \"mb1\", \"min_kbps\": 10000, \"direction\": \"egress\"}}");
        respondWith(200, "{\"minimum_bandwidth_rules\": [{\"id\": \"mb1\", \"min_kbps\": 10000, \"direction\": \"egress\"}]}");
        respondWith(201, "{\"minimum_packet_rate_rule\": {\"id\": \"mp1\", \"min_kpps\": 1000, \"direction\": \"any\"}}");
        respondWith(200, "{\"minimum_packet_rate_rule\": {\"id\": \"mp1\", \"min_kpps\": 2000, \"direction\": \"any\"}}");
        respondWith(201, "{\"packet_rate_limit_rule\": {\"id\": \"pl1\", \"max_kpps\": 10000, \"max_burst_kpps\": 500, \"direction\": \"egress\"}}");
        respondWith(204);

        var qos = osv3().networking().qosRules();
        QosMinimumBandwidthRule mb = qos.createMinimumBandwidthRule(POLICY, QosRuleOptions.minimumBandwidth(10000).direction("egress"));
        qos.listMinimumBandwidthRules(POLICY);
        QosMinimumPacketRateRule mp = qos.createMinimumPacketRateRule(POLICY, QosRuleOptions.minimumPacketRate(1000).direction("any"));
        QosMinimumPacketRateRule mp2 = qos.updateMinimumPacketRateRule(POLICY, "mp1", QosRuleOptions.update().minKpps(2000L));
        QosPacketRateLimitRule pl = qos.createPacketRateLimitRule(POLICY, QosRuleOptions.packetRateLimit(10000).maxBurstKpps(500L));
        qos.deletePacketRateLimitRule(POLICY, "pl1");

        RecordedRequest mbCreate = expect("POST", P + "/minimum_bandwidth_rules");
        Assert.assertEquals(body(mbCreate).get("minimum_bandwidth_rule").get("min_kbps").asLong(), 10000L);
        Assert.assertEquals(body(mbCreate).get("minimum_bandwidth_rule").get("direction").asText(), "egress");
        expect("GET", P + "/minimum_bandwidth_rules");
        expect("POST", P + "/minimum_packet_rate_rules");
        expect("PUT", P + "/minimum_packet_rate_rules/mp1");
        RecordedRequest plCreate = expect("POST", P + "/packet_rate_limit_rules");
        Assert.assertEquals(body(plCreate).get("packet_rate_limit_rule").get("max_burst_kpps").asLong(), 500L);
        expect("DELETE", P + "/packet_rate_limit_rules/pl1");
        Assert.assertEquals(mb.getMinKbps(), Long.valueOf(10000));
        Assert.assertEquals(mp.getDirection(), "any");
        Assert.assertEquals(mp2.getMinKpps(), Long.valueOf(2000));
        Assert.assertEquals(pl.getMaxBurstKpps(), Long.valueOf(500));
    }

    public void aliasRules() throws Exception {
        respondWith(200, "{\"bandwidth_limit_rule\": {\"id\": \"bl1\", \"max_kbps\": 1000, \"max_burst_kbps\": 100, \"direction\": \"egress\", \"qos_policy_id\": \"" + POLICY + "\"}}");
        respondWith(200, "{\"dscp_marking_rule\": {\"id\": \"d1\", \"dscp_mark\": 8, \"qos_policy_id\": \"" + POLICY + "\"}}");
        respondWith(200, "{\"minimum_bandwidth_rule\": {\"id\": \"mb1\", \"min_kbps\": 20000, \"direction\": \"egress\"}}");
        respondWith(204);
        respondWith(200, "{\"minimum_packet_rate_rule\": {\"id\": \"mp1\", \"min_kpps\": 1000, \"direction\": \"any\"}}");
        respondWith(204);

        var qos = osv3().networking().qosRules();
        Assert.assertEquals(qos.getAliasBandwidthLimitRule("bl1").getMaxKbps(), Integer.valueOf(1000));
        Assert.assertEquals(qos.getAliasDscpMarkingRule("d1").getQosPolicyId(), POLICY);
        Assert.assertEquals(qos.updateAliasMinimumBandwidthRule("mb1", QosRuleOptions.update().minKbps(20000L)).getMinKbps(), Long.valueOf(20000));
        Assert.assertTrue(qos.deleteAliasBandwidthLimitRule("bl1").isSuccess());
        qos.getAliasMinimumPacketRateRule("mp1");
        qos.deleteAliasDscpMarkingRule("d1");

        expect("GET", "/v2.0/qos/alias_bandwidth_limit_rules/bl1");
        expect("GET", "/v2.0/qos/alias_dscp_marking_rules/d1");
        RecordedRequest update = expect("PUT", "/v2.0/qos/alias_minimum_bandwidth_rules/mb1");
        Assert.assertEquals(body(update).get("minimum_bandwidth_rule").get("min_kbps").asLong(), 20000L);
        expect("DELETE", "/v2.0/qos/alias_bandwidth_limit_rules/bl1");
        expect("GET", "/v2.0/qos/alias_minimum_packet_rate_rules/mp1");
        expect("DELETE", "/v2.0/qos/alias_dscp_marking_rules/d1");
    }
}
