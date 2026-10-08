package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.FirewallRuleV2Service;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.FirewallRuleV2;
import org.openstack4j.model.network.options.FirewallRuleV2Options;
import org.openstack4j.openstack.networking.domain.ext.NeutronFirewallRuleV2;
import org.openstack4j.openstack.networking.domain.ext.NeutronFirewallRuleV2.NeutronFirewallRuleV2List;

public class FirewallRuleV2ServiceImpl extends BaseNeutronExtService implements FirewallRuleV2Service {

    private static final String PATH = "/fwaas/firewall_rules";
    private static final String ROOT = "firewall_rule";

    @Override
    public List<? extends FirewallRuleV2> list() {
        return list(null);
    }

    @Override
    public List<? extends FirewallRuleV2> list(Map<String, String> filters) {
        return listOf(NeutronFirewallRuleV2List.class, PATH, filters);
    }

    @Override
    public FirewallRuleV2 get(String id) {
        return show(NeutronFirewallRuleV2.class, PATH + "/" + id(id));
    }

    @Override
    public FirewallRuleV2 create(FirewallRuleV2Options options) {
        return create(NeutronFirewallRuleV2.class, PATH, ROOT, options);
    }

    @Override
    public FirewallRuleV2 update(String id, FirewallRuleV2Options options) {
        return update(NeutronFirewallRuleV2.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
