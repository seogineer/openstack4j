package org.openstack4j.model.identity.v3.options;

import java.util.LinkedHashMap;
import java.util.Map;

/** Filters of {@code GET /limits}. */
public class LimitListOptions {

    private final Map<String, Object> options = new LinkedHashMap<>();

    public static LimitListOptions create() {
        return new LimitListOptions();
    }

    public LimitListOptions serviceId(String serviceId) { options.put("service_id", serviceId); return this; }
    public LimitListOptions regionId(String regionId) { options.put("region_id", regionId); return this; }
    public LimitListOptions resourceName(String resourceName) { options.put("resource_name", resourceName); return this; }
    public LimitListOptions projectId(String projectId) { options.put("project_id", projectId); return this; }
    public LimitListOptions domainId(String domainId) { options.put("domain_id", domainId); return this; }

    public Map<String, Object> getOptions() {
        return options;
    }
}
