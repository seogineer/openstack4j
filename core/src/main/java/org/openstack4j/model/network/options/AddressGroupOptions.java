package org.openstack4j.model.network.options;

import java.util.List;
import java.util.Objects;

/** Body of an address group create or update. */
public class AddressGroupOptions extends NeutronAttributes<AddressGroupOptions> {

    public static AddressGroupOptions create(String name) { return new AddressGroupOptions().name(Objects.requireNonNull(name)); }
    public static AddressGroupOptions update() { return new AddressGroupOptions(); }

    @Override
    protected AddressGroupOptions self() {
        return this;
    }

    public AddressGroupOptions name(String name) { return put("name", name); }
    public AddressGroupOptions description(String description) { return put("description", description); }
    /** Initial addresses on create; use {@code addAddresses}/{@code removeAddresses} afterwards. */
    public AddressGroupOptions addresses(List<String> addresses) { return put("addresses", addresses); }
    public AddressGroupOptions projectId(String projectId) { return put("project_id", projectId); }
}
