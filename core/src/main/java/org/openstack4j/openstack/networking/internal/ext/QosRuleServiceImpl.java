package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;

import org.openstack4j.api.networking.ext.QosRuleService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.NetQosPolicyBandwidthLimitRule;
import org.openstack4j.model.network.ext.QosDscpMarkingRule;
import org.openstack4j.model.network.ext.QosMinimumBandwidthRule;
import org.openstack4j.model.network.ext.QosMinimumPacketRateRule;
import org.openstack4j.model.network.ext.QosPacketRateLimitRule;
import org.openstack4j.model.network.ext.QosRuleType;
import org.openstack4j.model.network.options.QosRuleOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronNetQosPolicyBandwidthLimitRule;
import org.openstack4j.openstack.networking.domain.ext.NeutronQosDscpMarkingRule;
import org.openstack4j.openstack.networking.domain.ext.NeutronQosMinimumBandwidthRule;
import org.openstack4j.openstack.networking.domain.ext.NeutronQosMinimumPacketRateRule;
import org.openstack4j.openstack.networking.domain.ext.NeutronQosPacketRateLimitRule;
import org.openstack4j.openstack.networking.domain.ext.NeutronQosRuleType;

public class QosRuleServiceImpl extends BaseNeutronExtService implements QosRuleService {

    private static final String DSCP = "dscp_marking_rule";
    private static final String MIN_BW = "minimum_bandwidth_rule";
    private static final String MIN_PPS = "minimum_packet_rate_rule";
    private static final String PPS_LIMIT = "packet_rate_limit_rule";
    private static final String BW_LIMIT = "bandwidth_limit_rule";

    private static String rules(String policyId, String kind) {
        return "/qos/policies/" + id(policyId) + "/" + kind + "s";
    }

    private static String rule(String policyId, String kind, String ruleId) {
        return rules(policyId, kind) + "/" + id(ruleId);
    }

    private static String alias(String kind, String ruleId) {
        return "/qos/alias_" + kind + "s/" + id(ruleId);
    }

    @Override public List<? extends QosRuleType> ruleTypes() { return listOf(NeutronQosRuleType.RuleTypes.class, "/qos/rule-types", null); }
    @Override public QosRuleType ruleType(String type) { return show(NeutronQosRuleType.class, "/qos/rule-types/" + id(type)); }

    @Override public List<? extends QosDscpMarkingRule> listDscpMarkingRules(String p) { return listOf(NeutronQosDscpMarkingRule.Rules.class, rules(p, DSCP), null); }
    @Override public QosDscpMarkingRule getDscpMarkingRule(String p, String r) { return show(NeutronQosDscpMarkingRule.class, rule(p, DSCP, r)); }
    @Override public QosDscpMarkingRule createDscpMarkingRule(String p, QosRuleOptions o) { return create(NeutronQosDscpMarkingRule.class, rules(p, DSCP), DSCP, o); }
    @Override public QosDscpMarkingRule updateDscpMarkingRule(String p, String r, QosRuleOptions o) { return update(NeutronQosDscpMarkingRule.class, rule(p, DSCP, r), DSCP, o); }
    @Override public ActionResponse deleteDscpMarkingRule(String p, String r) { return remove(rule(p, DSCP, r)); }

    @Override public List<? extends QosMinimumBandwidthRule> listMinimumBandwidthRules(String p) { return listOf(NeutronQosMinimumBandwidthRule.Rules.class, rules(p, MIN_BW), null); }
    @Override public QosMinimumBandwidthRule getMinimumBandwidthRule(String p, String r) { return show(NeutronQosMinimumBandwidthRule.class, rule(p, MIN_BW, r)); }
    @Override public QosMinimumBandwidthRule createMinimumBandwidthRule(String p, QosRuleOptions o) { return create(NeutronQosMinimumBandwidthRule.class, rules(p, MIN_BW), MIN_BW, o); }
    @Override public QosMinimumBandwidthRule updateMinimumBandwidthRule(String p, String r, QosRuleOptions o) { return update(NeutronQosMinimumBandwidthRule.class, rule(p, MIN_BW, r), MIN_BW, o); }
    @Override public ActionResponse deleteMinimumBandwidthRule(String p, String r) { return remove(rule(p, MIN_BW, r)); }

    @Override public List<? extends QosMinimumPacketRateRule> listMinimumPacketRateRules(String p) { return listOf(NeutronQosMinimumPacketRateRule.Rules.class, rules(p, MIN_PPS), null); }
    @Override public QosMinimumPacketRateRule getMinimumPacketRateRule(String p, String r) { return show(NeutronQosMinimumPacketRateRule.class, rule(p, MIN_PPS, r)); }
    @Override public QosMinimumPacketRateRule createMinimumPacketRateRule(String p, QosRuleOptions o) { return create(NeutronQosMinimumPacketRateRule.class, rules(p, MIN_PPS), MIN_PPS, o); }
    @Override public QosMinimumPacketRateRule updateMinimumPacketRateRule(String p, String r, QosRuleOptions o) { return update(NeutronQosMinimumPacketRateRule.class, rule(p, MIN_PPS, r), MIN_PPS, o); }
    @Override public ActionResponse deleteMinimumPacketRateRule(String p, String r) { return remove(rule(p, MIN_PPS, r)); }

    @Override public List<? extends QosPacketRateLimitRule> listPacketRateLimitRules(String p) { return listOf(NeutronQosPacketRateLimitRule.Rules.class, rules(p, PPS_LIMIT), null); }
    @Override public QosPacketRateLimitRule getPacketRateLimitRule(String p, String r) { return show(NeutronQosPacketRateLimitRule.class, rule(p, PPS_LIMIT, r)); }
    @Override public QosPacketRateLimitRule createPacketRateLimitRule(String p, QosRuleOptions o) { return create(NeutronQosPacketRateLimitRule.class, rules(p, PPS_LIMIT), PPS_LIMIT, o); }
    @Override public QosPacketRateLimitRule updatePacketRateLimitRule(String p, String r, QosRuleOptions o) { return update(NeutronQosPacketRateLimitRule.class, rule(p, PPS_LIMIT, r), PPS_LIMIT, o); }
    @Override public ActionResponse deletePacketRateLimitRule(String p, String r) { return remove(rule(p, PPS_LIMIT, r)); }

    @Override public NetQosPolicyBandwidthLimitRule getAliasBandwidthLimitRule(String r) { return show(NeutronNetQosPolicyBandwidthLimitRule.class, alias(BW_LIMIT, r)); }
    @Override public NetQosPolicyBandwidthLimitRule updateAliasBandwidthLimitRule(String r, QosRuleOptions o) { return update(NeutronNetQosPolicyBandwidthLimitRule.class, alias(BW_LIMIT, r), BW_LIMIT, o); }
    @Override public ActionResponse deleteAliasBandwidthLimitRule(String r) { return remove(alias(BW_LIMIT, r)); }
    @Override public QosDscpMarkingRule getAliasDscpMarkingRule(String r) { return show(NeutronQosDscpMarkingRule.class, alias(DSCP, r)); }
    @Override public QosDscpMarkingRule updateAliasDscpMarkingRule(String r, QosRuleOptions o) { return update(NeutronQosDscpMarkingRule.class, alias(DSCP, r), DSCP, o); }
    @Override public ActionResponse deleteAliasDscpMarkingRule(String r) { return remove(alias(DSCP, r)); }
    @Override public QosMinimumBandwidthRule getAliasMinimumBandwidthRule(String r) { return show(NeutronQosMinimumBandwidthRule.class, alias(MIN_BW, r)); }
    @Override public QosMinimumBandwidthRule updateAliasMinimumBandwidthRule(String r, QosRuleOptions o) { return update(NeutronQosMinimumBandwidthRule.class, alias(MIN_BW, r), MIN_BW, o); }
    @Override public ActionResponse deleteAliasMinimumBandwidthRule(String r) { return remove(alias(MIN_BW, r)); }
    @Override public QosMinimumPacketRateRule getAliasMinimumPacketRateRule(String r) { return show(NeutronQosMinimumPacketRateRule.class, alias(MIN_PPS, r)); }
    @Override public QosMinimumPacketRateRule updateAliasMinimumPacketRateRule(String r, QosRuleOptions o) { return update(NeutronQosMinimumPacketRateRule.class, alias(MIN_PPS, r), MIN_PPS, o); }
    @Override public ActionResponse deleteAliasMinimumPacketRateRule(String r) { return remove(alias(MIN_PPS, r)); }
}
