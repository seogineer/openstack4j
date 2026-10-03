package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of an address scope create or update. */
public class AddressScopeOptions extends NeutronAttributes<AddressScopeOptions> {

    public static AddressScopeOptions create(String name, int ipVersion) {
        return new AddressScopeOptions().name(Objects.requireNonNull(name)).put("ip_version", ipVersion);
    }

    /** An update that sends only the fields set afterwards. */
    public static AddressScopeOptions update() {
        return new AddressScopeOptions();
    }

    @Override
    protected AddressScopeOptions self() {
        return this;
    }

    public AddressScopeOptions name(String value) { return put("name", value); }
    public AddressScopeOptions shared(Boolean value) { return put("shared", value); }
    public AddressScopeOptions projectId(String value) { return put("project_id", value); }
}
