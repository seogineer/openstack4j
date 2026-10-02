package org.openstack4j.model.compute;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filters for {@code GET /servers/detail}. Filters that need a compute microversion are checked before the request.
 */
public class ServerListOptions {

    private final Map<String, String> params = new LinkedHashMap<>();
    private int requiredMinor = 0;

    public static ServerListOptions create() {
        return new ServerListOptions();
    }

    private ServerListOptions put(String key, Object value, int minor) {
        params.put(key, String.valueOf(value));
        requiredMinor = Math.max(requiredMinor, minor);
        return this;
    }

    public ServerListOptions name(String regex) { return put("name", regex, 0); }
    public ServerListOptions status(String status) { return put("status", status, 0); }
    public ServerListOptions changesSince(String isoTime) { return put("changes-since", isoTime, 0); }
    /** 2.66+ */
    public ServerListOptions changesBefore(String isoTime) { return put("changes-before", isoTime, 66); }
    public ServerListOptions flavor(String flavorId) { return put("flavor", flavorId, 0); }
    public ServerListOptions image(String imageId) { return put("image", imageId, 0); }
    public ServerListOptions host(String host) { return put("host", host, 0); }
    public ServerListOptions allTenants(boolean allTenants) { return put("all_tenants", allTenants, 0); }
    public ServerListOptions limit(int limit) { return put("limit", limit, 0); }
    public ServerListOptions marker(String serverId) { return put("marker", serverId, 0); }
    public ServerListOptions sortKey(String key) { return put("sort_key", key, 0); }
    public ServerListOptions sortDir(String dir) { return put("sort_dir", dir, 0); }
    /** Servers with all of these tags (2.26+). */
    public ServerListOptions tags(String... tags) { return put("tags", String.join(",", tags), 26); }
    /** Servers with any of these tags (2.26+). */
    public ServerListOptions tagsAny(String... tags) { return put("tags-any", String.join(",", tags), 26); }
    /** 2.26+ */
    public ServerListOptions notTags(String... tags) { return put("not-tags", String.join(",", tags), 26); }
    /** 2.26+ */
    public ServerListOptions notTagsAny(String... tags) { return put("not-tags-any", String.join(",", tags), 26); }
    /** 2.73+ */
    public ServerListOptions locked(boolean locked) { return put("locked", locked, 73); }
    // the following were admin-only before 2.83 and open to all users from 2.83, so no floor is enforced
    public ServerListOptions availabilityZone(String zone) { return put("availability_zone", zone, 0); }
    public ServerListOptions configDrive(boolean configDrive) { return put("config_drive", configDrive, 0); }
    public ServerListOptions keyName(String keyName) { return put("key_name", keyName, 0); }
    public ServerListOptions createdAt(String isoTime) { return put("created_at", isoTime, 0); }
    public ServerListOptions launchedAt(String isoTime) { return put("launched_at", isoTime, 0); }
    public ServerListOptions terminatedAt(String isoTime) { return put("terminated_at", isoTime, 0); }
    public ServerListOptions powerState(int powerState) { return put("power_state", powerState, 0); }
    public ServerListOptions taskState(String taskState) { return put("task_state", taskState, 0); }
    public ServerListOptions vmState(String vmState) { return put("vm_state", vmState, 0); }
    public ServerListOptions progress(int progress) { return put("progress", progress, 0); }
    public ServerListOptions userId(String userId) { return put("user_id", userId, 0); }

    public Map<String, String> toQueryParams() {
        return new LinkedHashMap<>(params);
    }

    /** @return the lowest compute microversion these filters need, such as {@code "2.73"}, or {@code null} */
    public String getRequiredMicroVersion() {
        return requiredMinor == 0 ? null : "2." + requiredMinor;
    }
}
