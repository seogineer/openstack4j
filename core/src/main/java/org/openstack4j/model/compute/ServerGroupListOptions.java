package org.openstack4j.model.compute;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Query options for listing server groups.
 */
public class ServerGroupListOptions {

    private final Map<String, String> params = new LinkedHashMap<>();

    public static ServerGroupListOptions create() {
        return new ServerGroupListOptions();
    }

    /**
     * All projects' groups (admin). Nova checks only that the key is present; a non-admin silently gets its own groups.
     */
    public ServerGroupListOptions allProjects() {
        params.put("all_projects", "true");
        return this;
    }

    public ServerGroupListOptions limit(int limit) {
        params.put("limit", String.valueOf(limit));
        return this;
    }

    public ServerGroupListOptions offset(int offset) {
        params.put("offset", String.valueOf(offset));
        return this;
    }

    public Map<String, String> toQueryParams() {
        return new LinkedHashMap<>(params);
    }
}
