package org.openstack4j.model.identity.v3.options;

import java.util.LinkedHashMap;
import java.util.Map;

/** Filters of {@code GET /services}. */
public class ServiceListOptions {

    private final Map<String, Object> options = new LinkedHashMap<>();

    public static ServiceListOptions create() {
        return new ServiceListOptions();
    }

    public ServiceListOptions type(String type) { options.put("type", type); return this; }
    public ServiceListOptions name(String name) { options.put("name", name); return this; }

    public Map<String, Object> getOptions() {
        return options;
    }
}
