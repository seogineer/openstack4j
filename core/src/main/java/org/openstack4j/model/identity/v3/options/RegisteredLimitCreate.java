package org.openstack4j.model.identity.v3.options;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** One entry of {@code POST /registered_limits}. */
public class RegisteredLimitCreate {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    private RegisteredLimitCreate(String serviceId, String resourceName, int defaultLimit) {
        fields.put("service_id", Objects.requireNonNull(serviceId));
        fields.put("resource_name", Objects.requireNonNull(resourceName));
        fields.put("default_limit", defaultLimit);
    }

    public static RegisteredLimitCreate create(String serviceId, String resourceName, int defaultLimit) {
        return new RegisteredLimitCreate(serviceId, resourceName, defaultLimit);
    }

    public RegisteredLimitCreate regionId(String regionId) { if (regionId != null) fields.put("region_id", regionId); return this; }
    public RegisteredLimitCreate description(String description) { if (description != null) fields.put("description", description); return this; }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(fields);
    }
}
