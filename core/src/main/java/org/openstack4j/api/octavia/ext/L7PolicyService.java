package org.openstack4j.api.octavia.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.octavia.ext.L7Policy;
import org.openstack4j.model.octavia.ext.L7Rule;
import org.openstack4j.model.octavia.options.L7PolicyOptions;
import org.openstack4j.model.octavia.options.L7RuleOptions;

/**
 * Octavia L7 policies and their rules ({@code /v2/lbaas/l7policies}).
 */
public interface L7PolicyService extends RestService {

    /**
     * Lists L7 policies, optionally filtered (for example listener_id).
     *
     * @return the result
     */
    List<? extends L7Policy> list();

    /**
     * Lists L7 policies, optionally filtered (for example listener_id).
     *
     * @param Map<String the map< string
     * @param filters the filters
     * @return the result
     */
    List<? extends L7Policy> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    L7Policy get(String id);

    /**
     * Creates an L7 policy on a listener.
     *
     * @param options the options
     * @return the result
     */
    L7Policy create(L7PolicyOptions options);

    /**
     * Updates an L7 policy; only the fields set are sent.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    L7Policy update(String id, L7PolicyOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);

    /**
     * Lists the rules of an L7 policy.
     *
     * @param policyId the policy id
     * @return the result
     */
    List<? extends L7Rule> listRules(String policyId);

    /**
     * Lists the rules of an L7 policy.
     *
     * @param policyId the policy id
     * @param Map<String the map< string
     * @param filters the filters
     * @return the result
     */
    List<? extends L7Rule> listRules(String policyId, Map<String, String> filters);

    /**
     * @param policyId the policy id
     * @param ruleId the rule id
     * @return the result
     */
    L7Rule getRule(String policyId, String ruleId);

    /**
     * Adds a rule to an L7 policy.
     *
     * @param policyId the policy id
     * @param options the options
     * @return the result
     */
    L7Rule createRule(String policyId, L7RuleOptions options);

    /**
     * Updates a rule; only the fields set are sent.
     *
     * @param policyId the policy id
     * @param ruleId the rule id
     * @param options the options
     * @return the result
     */
    L7Rule updateRule(String policyId, String ruleId, L7RuleOptions options);

    /**
     * @param policyId the policy id
     * @param ruleId the rule id
     * @return the action response
     */
    ActionResponse deleteRule(String policyId, String ruleId);
}
