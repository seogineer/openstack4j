package org.openstack4j.model.network.options;

import java.util.List;

/** Body of a firewall group create or update; only the fields set are sent. */
public class FirewallGroupOptions extends NeutronAttributes<FirewallGroupOptions> {

    public static FirewallGroupOptions create() {
        return new FirewallGroupOptions();
    }

    /** An update that sends only the fields set afterwards. */
    public static FirewallGroupOptions update() {
        return new FirewallGroupOptions();
    }

    @Override
    protected FirewallGroupOptions self() {
        return this;
    }

    public FirewallGroupOptions name(String value) {
        return put("name", value);
    }

    public FirewallGroupOptions description(String value) {
        return put("description", value);
    }

    public FirewallGroupOptions ingressFirewallPolicyId(String value) {
        return put("ingress_firewall_policy_id", value);
    }

    public FirewallGroupOptions egressFirewallPolicyId(String value) {
        return put("egress_firewall_policy_id", value);
    }

    /** Router ports or VM ports the group applies to. */
    public FirewallGroupOptions ports(List<String> value) {
        return put("ports", value);
    }

    public FirewallGroupOptions adminStateUp(Boolean value) {
        return put("admin_state_up", value);
    }

    public FirewallGroupOptions shared(Boolean value) {
        return put("shared", value);
    }
}
