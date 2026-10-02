package org.openstack4j.model.compute;

import java.util.LinkedHashMap;
import java.util.Map;

/** Filters for {@code GET /os-keypairs}. */
public class KeypairListOptions {

    private final Map<String, String> params = new LinkedHashMap<>();
    private int requiredMinor = 0;

    public static KeypairListOptions create() {
        return new KeypairListOptions();
    }

    private KeypairListOptions put(String key, Object value, int minor) {
        params.put(key, String.valueOf(value));
        requiredMinor = Math.max(requiredMinor, minor);
        return this;
    }

    /** Admin: list another user's key pairs (2.10+). */
    public KeypairListOptions userId(String userId) { return put("user_id", userId, 10); }
    /** 2.35+ */
    public KeypairListOptions limit(int limit) { return put("limit", limit, 35); }
    /** Key pair name (2.35+). */
    public KeypairListOptions marker(String keypairName) { return put("marker", keypairName, 35); }

    public Map<String, String> toQueryParams() { return new LinkedHashMap<>(params); }

    public String getRequiredMicroVersion() { return requiredMinor == 0 ? null : "2." + requiredMinor; }
}
