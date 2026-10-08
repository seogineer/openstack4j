package org.openstack4j.model.network.options;

import java.util.List;
import java.util.Objects;

/** Body of a firewall policy create or update; only the fields set are sent. */
public class FirewallPolicyV2Options extends NeutronAttributes<FirewallPolicyV2Options> {

    public static FirewallPolicyV2Options create(String name) {
        return new FirewallPolicyV2Options().put("name", Objects.requireNonNull(name, "name"));
    }

    /** An update that sends only the fields set afterwards. */
    public static FirewallPolicyV2Options update() {
        return new FirewallPolicyV2Options();
    }

    @Override
    protected FirewallPolicyV2Options self() {
        return this;
    }

    public FirewallPolicyV2Options name(String value) {
        return put("name", value);
    }

    public FirewallPolicyV2Options description(String value) {
        return put("description", value);
    }

    /** Rule ids in order. */
    public FirewallPolicyV2Options firewallRules(List<String> value) {
        return put("firewall_rules", value);
    }

    public FirewallPolicyV2Options audited(Boolean value) {
        return put("audited", value);
    }

    public FirewallPolicyV2Options shared(Boolean value) {
        return put("shared", value);
    }
}
