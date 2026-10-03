package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a service flavor create or update. */
public class ServiceFlavorOptions extends NeutronAttributes<ServiceFlavorOptions> {

    public static ServiceFlavorOptions create(String name, String serviceType) {
        return new ServiceFlavorOptions().put("name", Objects.requireNonNull(name)).put("service_type", Objects.requireNonNull(serviceType));
    }

    /** An update that sends only the fields set afterwards. */
    public static ServiceFlavorOptions update() {
        return new ServiceFlavorOptions();
    }

    @Override
    protected ServiceFlavorOptions self() {
        return this;
    }

    public ServiceFlavorOptions name(String value) { return put("name", value); }
    public ServiceFlavorOptions description(String value) { return put("description", value); }
    public ServiceFlavorOptions enabled(Boolean value) { return put("enabled", value); }
}
