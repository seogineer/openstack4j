package org.openstack4j.model.storage.block.options;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Common query parameters of Cinder list APIs. Subclasses add their resource's filters; each parameter records the
 * lowest microversion it needs so the service can check the floor before sending.
 *
 * @param <T> the concrete options type, for fluent chaining
 */
public abstract class BlockStorageListOptions<T extends BlockStorageListOptions<T>> {

    private final Map<String, String> params = new LinkedHashMap<>();
    private int requiredMinor = 0;

    @SuppressWarnings("unchecked")
    protected T put(String key, Object value, int minor) {
        params.put(key, String.valueOf(value));
        requiredMinor = Math.max(requiredMinor, minor);
        return (T) this;
    }

    public T limit(int limit) { return put("limit", limit, 0); }
    public T marker(String marker) { return put("marker", marker, 0); }
    public T offset(int offset) { return put("offset", offset, 0); }
    public T sortKey(String key) { return put("sort_key", key, 0); }
    /** {@code asc} or {@code desc} */
    public T sortDir(String dir) { return put("sort_dir", dir, 0); }
    /** Combined form such as {@code name:asc,created_at:desc}. */
    public T sort(String sort) { return put("sort", sort, 0); }
    /** Admin: include all projects. */
    public T allTenants(boolean allTenants) { return put("all_tenants", allTenants, 0); }

    public Map<String, String> toQueryParams() {
        return new LinkedHashMap<>(params);
    }

    /** @return the lowest block storage microversion these parameters need, such as {@code "3.45"}, or {@code null} */
    public String getRequiredMicroVersion() {
        return requiredMinor == 0 ? null : "3." + requiredMinor;
    }

    /** Cinder expects dict-style metadata filters: {@code {'key': 'value'}}. */
    protected static String dict(Map<String, String> metadata) {
        StringBuilder sb = new StringBuilder("{");
        metadata.forEach((k, v) -> sb.append(sb.length() > 1 ? ", " : "").append('\'').append(k).append("': '").append(v).append('\''));
        return sb.append('}').toString();
    }
}
