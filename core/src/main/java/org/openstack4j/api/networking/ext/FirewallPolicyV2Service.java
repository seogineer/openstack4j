package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.FirewallPolicyV2;
import org.openstack4j.model.network.options.FirewallPolicyV2Options;

/** FWaaS v2 firewall policies ({@code /v2.0/fwaas/firewall_policies}). */
public interface FirewallPolicyV2Service extends RestService {

    /** @return the FWaaS v2 firewall policies */
    List<? extends FirewallPolicyV2> list();

    /** @param filters query parameters such as {@code name}, {@code shared}, {@code audited} */
    List<? extends FirewallPolicyV2> list(Map<String, String> filters);

    /** @return the firewall policy, or {@code null} when it does not exist */
    FirewallPolicyV2 get(String id);

    FirewallPolicyV2 create(FirewallPolicyV2Options options);

    /** Changes only the fields set in {@code options}. */
    FirewallPolicyV2 update(String id, FirewallPolicyV2Options options);

    /**
     * Inserts a rule into the policy.
     *
     * @param insertBefore a rule id, or {@code null}
     * @param insertAfter  a rule id, or {@code null}
     * @return the policy with its new rule order
     */
    FirewallPolicyV2 insertRule(String id, String ruleId, String insertBefore, String insertAfter);

    /** @return the policy without the rule */
    FirewallPolicyV2 removeRule(String id, String ruleId);

    ActionResponse delete(String id);
}
