package org.openstack4j.model.identity.v3.options;

import java.util.LinkedHashMap;
import java.util.Map;

/** Filters of {@code GET /registered_limits}. */
public class RegisteredLimitListOptions {

    private final Map<String, Object> options = new LinkedHashMap<>();

    public static RegisteredLimitListOptions create() {
        return new RegisteredLimitListOptions();
    }

    public RegisteredLimitListOptions serviceId(String serviceId) { options.put("service_id", serviceId); return this; }
    public RegisteredLimitListOptions regionId(String regionId) { options.put("region_id", regionId); return this; }
    public RegisteredLimitListOptions resourceName(String resourceName) { options.put("resource_name", resourceName); return this; }

    public Map<String, Object> getOptions() {
        return options;
    }
}
