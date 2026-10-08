package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.FirewallPolicyV2Service;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.FirewallPolicyV2;
import org.openstack4j.model.network.options.FirewallPolicyV2Options;
import org.openstack4j.openstack.networking.domain.ext.NeutronFirewallPolicyV2;
import org.openstack4j.openstack.networking.domain.ext.NeutronFirewallPolicyV2.NeutronFirewallPolicyV2List;

public class FirewallPolicyV2ServiceImpl extends BaseNeutronExtService implements FirewallPolicyV2Service {

    private static final String PATH = "/fwaas/firewall_policies";
    private static final String ROOT = "firewall_policy";

    @Override
    public List<? extends FirewallPolicyV2> list() {
        return list(null);
    }

    @Override
    public List<? extends FirewallPolicyV2> list(Map<String, String> filters) {
        return listOf(NeutronFirewallPolicyV2List.class, PATH, filters);
    }

    @Override
    public FirewallPolicyV2 get(String id) {
        return show(NeutronFirewallPolicyV2.class, PATH + "/" + id(id));
    }

    @Override
    public FirewallPolicyV2 create(FirewallPolicyV2Options options) {
        return create(NeutronFirewallPolicyV2.class, PATH, ROOT, options);
    }

    @Override
    public FirewallPolicyV2 update(String id, FirewallPolicyV2Options options) {
        return update(NeutronFirewallPolicyV2.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public FirewallPolicyV2 insertRule(String id, String ruleId, String insertBefore, String insertAfter) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("firewall_rule_id", id(ruleId));
        body.put("insert_before", insertBefore == null ? "" : insertBefore);
        body.put("insert_after", insertAfter == null ? "" : insertAfter);
        return ruleAction(id, "insert_rule", body);
    }

    @Override
    public FirewallPolicyV2 removeRule(String id, String ruleId) {
        return ruleAction(id, "remove_rule", Map.of("firewall_rule_id", id(ruleId)));
    }

    /** These actions answer with the policy itself, without the {@code firewall_policy} root. */
    @SuppressWarnings("unchecked")
    private FirewallPolicyV2 ruleAction(String id, String action, Map<String, ?> body) {
        Map<String, Object> policy = put(Map.class, PATH + "/" + id(id) + "/" + action)
                .entity(org.openstack4j.openstack.internal.microversion.JsonBody.of(body)).execute(NeutronExecution.propagate404());
        return policy == null ? null : PLAIN.convertValue(policy, NeutronFirewallPolicyV2.class);
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper PLAIN = new com.fasterxml.jackson.databind.ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
