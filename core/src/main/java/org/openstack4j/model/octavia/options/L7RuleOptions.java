package org.openstack4j.model.octavia.options;

import java.util.List;
import java.util.Objects;

/** Body of an L7 rule create or update. */
public class L7RuleOptions extends OctaviaAttributes<L7RuleOptions> {

    public static L7RuleOptions create(String type, String compareType, String value) {
        return new L7RuleOptions().put("type", Objects.requireNonNull(type)).put("compare_type", Objects.requireNonNull(compareType)).put("value", Objects.requireNonNull(value));
    }

    /** An update that sends only the fields set afterwards. */
    public static L7RuleOptions update() {
        return new L7RuleOptions();
    }

    @Override
    protected L7RuleOptions self() {
        return this;
    }

    public L7RuleOptions key(String value) { return put("key", value); }
    public L7RuleOptions invert(Boolean value) { return put("invert", value); }
    public L7RuleOptions adminStateUp(Boolean value) { return put("admin_state_up", value); }
    public L7RuleOptions tags(List<String> value) { return put("tags", value); }
}
