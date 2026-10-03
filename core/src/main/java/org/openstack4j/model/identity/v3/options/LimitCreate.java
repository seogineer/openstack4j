package org.openstack4j.model.identity.v3.options;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** One entry of {@code POST /limits}: a project or domain override of a registered limit. */
public class LimitCreate {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    private LimitCreate(String ownerKey, String ownerId, String serviceId, String resourceName, int resourceLimit) {
        fields.put(ownerKey, Objects.requireNonNull(ownerId));
        fields.put("service_id", Objects.requireNonNull(serviceId));
        fields.put("resource_name", Objects.requireNonNull(resourceName));
        fields.put("resource_limit", resourceLimit);
    }

    public static LimitCreate forProject(String projectId, String serviceId, String resourceName, int resourceLimit) {
        return new LimitCreate("project_id", projectId, serviceId, resourceName, resourceLimit);
    }

    public static LimitCreate forDomain(String domainId, String serviceId, String resourceName, int resourceLimit) {
        return new LimitCreate("domain_id", domainId, serviceId, resourceName, resourceLimit);
    }

    public LimitCreate regionId(String regionId) { fields.put("region_id", regionId); return this; }
    public LimitCreate description(String description) { fields.put("description", description); return this; }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(fields);
    }
}
