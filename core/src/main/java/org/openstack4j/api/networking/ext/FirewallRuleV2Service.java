package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.FirewallRuleV2;
import org.openstack4j.model.network.options.FirewallRuleV2Options;

/** FWaaS v2 firewall rules ({@code /v2.0/fwaas/firewall_rules}). */
public interface FirewallRuleV2Service extends RestService {

    /** @return the FWaaS v2 firewall rules */
    List<? extends FirewallRuleV2> list();

    /** @param filters query parameters such as {@code name}, {@code protocol}, {@code action}, {@code enabled} */
    List<? extends FirewallRuleV2> list(Map<String, String> filters);

    /** @return the firewall rule, or {@code null} when it does not exist */
    FirewallRuleV2 get(String id);

    FirewallRuleV2 create(FirewallRuleV2Options options);

    /** Changes only the fields set in {@code options}. */
    FirewallRuleV2 update(String id, FirewallRuleV2Options options);

    ActionResponse delete(String id);
}
