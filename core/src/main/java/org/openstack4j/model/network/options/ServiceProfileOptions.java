package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a service profile create or update. */
public class ServiceProfileOptions extends NeutronAttributes<ServiceProfileOptions> {

    public static ServiceProfileOptions create() {
        return new ServiceProfileOptions();
    }

    /** An update that sends only the fields set afterwards. */
    public static ServiceProfileOptions update() {
        return new ServiceProfileOptions();
    }

    @Override
    protected ServiceProfileOptions self() {
        return this;
    }

    public ServiceProfileOptions description(String value) { return put("description", value); }
    public ServiceProfileOptions driver(String value) { return put("driver", value); }
    public ServiceProfileOptions metainfo(String value) { return put("metainfo", value); }
    public ServiceProfileOptions enabled(Boolean value) { return put("enabled", value); }
}
