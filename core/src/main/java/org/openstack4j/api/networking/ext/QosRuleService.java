package org.openstack4j.api.networking.ext;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.NetQosPolicyBandwidthLimitRule;
import org.openstack4j.model.network.ext.QosDscpMarkingRule;
import org.openstack4j.model.network.ext.QosMinimumBandwidthRule;
import org.openstack4j.model.network.ext.QosMinimumPacketRateRule;
import org.openstack4j.model.network.ext.QosPacketRateLimitRule;
import org.openstack4j.model.network.ext.QosRuleType;
import org.openstack4j.model.network.options.QosRuleOptions;

/**
 * QoS rules beyond bandwidth limits: rule types, DSCP marking, minimum bandwidth, minimum packet rate (qos-pps-minimum), packet rate limit (qos-pps) and alias rules (qos-rules-alias, addressed by rule id alone).
 */
public interface QosRuleService extends RestService {

    /**
     * Lists the QoS rule types the deployment supports.
     *
     * @return the result
     */
    List<? extends QosRuleType> ruleTypes();

    /**
     * Returns a rule type with the drivers and parameters that support it.
     *
     * @param type the type
     * @return the result
     */
    QosRuleType ruleType(String type);

    /**
     * @param policyId the QoS policy
     * @return the result
     */
    List<? extends QosDscpMarkingRule> listDscpMarkingRules(String policyId);

    /**
     * @param policyId the QoS policy
     * @param ruleId the rule
     * @return the result
     */
    QosDscpMarkingRule getDscpMarkingRule(String policyId, String ruleId);

    /**
     * @param policyId the QoS policy
     * @param options the rule fields to send
     * @return the result
     */
    QosDscpMarkingRule createDscpMarkingRule(String policyId, QosRuleOptions options);

    /**
     * @param policyId the QoS policy
     * @param ruleId the rule
     * @param options the rule fields to send
     * @return the result
     */
    QosDscpMarkingRule updateDscpMarkingRule(String policyId, String ruleId, QosRuleOptions options);

    /**
     * @param policyId the QoS policy
     * @param ruleId the rule
     * @return the action response
     */
    ActionResponse deleteDscpMarkingRule(String policyId, String ruleId);

    /**
     * @param policyId the QoS policy
     * @return the result
     */
    List<? extends QosMinimumBandwidthRule> listMinimumBandwidthRules(String policyId);

    /**
     * @param policyId the QoS policy
     * @param ruleId the rule
     * @return the result
     */
    QosMinimumBandwidthRule getMinimumBandwidthRule(String policyId, String ruleId);

    /**
     * @param policyId the QoS policy
     * @param options the rule fields to send
     * @return the result
     */
    QosMinimumBandwidthRule createMinimumBandwidthRule(String policyId, QosRuleOptions options);

    /**
     * @param policyId the QoS policy
     * @param ruleId the rule
     * @param options the rule fields to send
     * @return the result
     */
    QosMinimumBandwidthRule updateMinimumBandwidthRule(String policyId, String ruleId, QosRuleOptions options);

    /**
     * @param policyId the QoS policy
     * @param ruleId the rule
     * @return the action response
     */
    ActionResponse deleteMinimumBandwidthRule(String policyId, String ruleId);

    /**
     * @param policyId the QoS policy
     * @return the result
     */
    List<? extends QosMinimumPacketRateRule> listMinimumPacketRateRules(String policyId);

    /**
     * @param policyId the QoS policy
     * @param ruleId the rule
     * @return the result
     */
    QosMinimumPacketRateRule getMinimumPacketRateRule(String policyId, String ruleId);

    /**
     * @param policyId the QoS policy
     * @param options the rule fields to send
     * @return the result
     */
    QosMinimumPacketRateRule createMinimumPacketRateRule(String policyId, QosRuleOptions options);

    /**
     * @param policyId the QoS policy
     * @param ruleId the rule
     * @param options the rule fields to send
     * @return the result
     */
    QosMinimumPacketRateRule updateMinimumPacketRateRule(String policyId, String ruleId, QosRuleOptions options);

    /**
     * @param policyId the QoS policy
     * @param ruleId the rule
     * @return the action response
     */
    ActionResponse deleteMinimumPacketRateRule(String policyId, String ruleId);

    /**
     * @param policyId the QoS policy
     * @return the result
     */
    List<? extends QosPacketRateLimitRule> listPacketRateLimitRules(String policyId);

    /**
     * @param policyId the QoS policy
     * @param ruleId the rule
     * @return the result
     */
    QosPacketRateLimitRule getPacketRateLimitRule(String policyId, String ruleId);

    /**
     * @param policyId the QoS policy
     * @param options the rule fields to send
     * @return the result
     */
    QosPacketRateLimitRule createPacketRateLimitRule(String policyId, QosRuleOptions options);

    /**
     * @param policyId the QoS policy
     * @param ruleId the rule
     * @param options the rule fields to send
     * @return the result
     */
    QosPacketRateLimitRule updatePacketRateLimitRule(String policyId, String ruleId, QosRuleOptions options);

    /**
     * @param policyId the QoS policy
     * @param ruleId the rule
     * @return the action response
     */
    ActionResponse deletePacketRateLimitRule(String policyId, String ruleId);

    /**
     * @param ruleId the rule
     * @return the result
     */
    NetQosPolicyBandwidthLimitRule getAliasBandwidthLimitRule(String ruleId);

    /**
     * @param ruleId the rule
     * @param options the rule fields to send
     * @return the result
     */
    NetQosPolicyBandwidthLimitRule updateAliasBandwidthLimitRule(String ruleId, QosRuleOptions options);

    /**
     * @param ruleId the rule
     * @return the action response
     */
    ActionResponse deleteAliasBandwidthLimitRule(String ruleId);

    /**
     * @param ruleId the rule
     * @return the result
     */
    QosDscpMarkingRule getAliasDscpMarkingRule(String ruleId);

    /**
     * @param ruleId the rule
     * @param options the rule fields to send
     * @return the result
     */
    QosDscpMarkingRule updateAliasDscpMarkingRule(String ruleId, QosRuleOptions options);

    /**
     * @param ruleId the rule
     * @return the action response
     */
    ActionResponse deleteAliasDscpMarkingRule(String ruleId);

    /**
     * @param ruleId the rule
     * @return the result
     */
    QosMinimumBandwidthRule getAliasMinimumBandwidthRule(String ruleId);

    /**
     * @param ruleId the rule
     * @param options the rule fields to send
     * @return the result
     */
    QosMinimumBandwidthRule updateAliasMinimumBandwidthRule(String ruleId, QosRuleOptions options);

    /**
     * @param ruleId the rule
     * @return the action response
     */
    ActionResponse deleteAliasMinimumBandwidthRule(String ruleId);

    /**
     * @param ruleId the rule
     * @return the result
     */
    QosMinimumPacketRateRule getAliasMinimumPacketRateRule(String ruleId);

    /**
     * @param ruleId the rule
     * @param options the rule fields to send
     * @return the result
     */
    QosMinimumPacketRateRule updateAliasMinimumPacketRateRule(String ruleId, QosRuleOptions options);

    /**
     * @param ruleId the rule
     * @return the action response
     */
    ActionResponse deleteAliasMinimumPacketRateRule(String ruleId);
}
