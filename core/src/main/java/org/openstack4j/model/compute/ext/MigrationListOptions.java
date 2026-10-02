package org.openstack4j.model.compute.ext;

import java.util.LinkedHashMap;
import java.util.Map;

/** Filters for {@code GET /os-migrations}. */
public class MigrationListOptions {

    private final Map<String, String> params = new LinkedHashMap<>();
    private int requiredMinor = 0;

    public static MigrationListOptions create() {
        return new MigrationListOptions();
    }

    private MigrationListOptions put(String key, Object value, int minor) {
        params.put(key, String.valueOf(value));
        requiredMinor = Math.max(requiredMinor, minor);
        return this;
    }

    public MigrationListOptions host(String host) { return put("host", host, 0); }
    public MigrationListOptions status(String status) { return put("status", status, 0); }
    public MigrationListOptions instanceUuid(String serverId) { return put("instance_uuid", serverId, 0); }
    public MigrationListOptions sourceCompute(String host) { return put("source_compute", host, 0); }
    /** {@code evacuation}, {@code live-migration}, {@code migration} or {@code resize} (2.23+). */
    public MigrationListOptions migrationType(String type) { return put("migration_type", type, 23); }
    public MigrationListOptions hidden(boolean hidden) { return put("hidden", hidden, 0); }
    /** 2.59+ */
    public MigrationListOptions changesSince(String isoTime) { return put("changes-since", isoTime, 59); }
    /** 2.59+ */
    public MigrationListOptions limit(int limit) { return put("limit", limit, 59); }
    /** Migration uuid (2.59+). */
    public MigrationListOptions marker(String migrationUuid) { return put("marker", migrationUuid, 59); }
    /** 2.66+ */
    public MigrationListOptions changesBefore(String isoTime) { return put("changes-before", isoTime, 66); }
    /** 2.80+ */
    public MigrationListOptions userId(String userId) { return put("user_id", userId, 80); }
    /** 2.80+ */
    public MigrationListOptions projectId(String projectId) { return put("project_id", projectId, 80); }

    public Map<String, String> toQueryParams() {
        return new LinkedHashMap<>(params);
    }

    public String getRequiredMicroVersion() {
        return requiredMinor == 0 ? null : "2." + requiredMinor;
    }
}
