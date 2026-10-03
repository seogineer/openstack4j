package org.openstack4j.model.network.options;

import java.util.Map;
import java.util.Objects;

/** Body of a port binding create (binding-extended). */
public class PortBindingOptions extends NeutronAttributes<PortBindingOptions> {

    public static PortBindingOptions create(String host) {
        return new PortBindingOptions().put("host", Objects.requireNonNull(host));
    }

    @Override
    protected PortBindingOptions self() {
        return this;
    }

    public PortBindingOptions vnicType(String value) { return put("vnic_type", value); }
    public PortBindingOptions profile(Map<String, Object> value) { return put("profile", value); }
}
