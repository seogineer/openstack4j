package org.openstack4j.model.compute.ext;

import java.util.LinkedHashMap;
import java.util.Map;

/** Filters for {@code GET /os-hypervisors/detail}. */
public class HypervisorListOptions {

    private final Map<String, String> params = new LinkedHashMap<>();
    private int requiredMinor = 0;

    public static HypervisorListOptions create() {
        return new HypervisorListOptions();
    }

    private HypervisorListOptions put(String key, Object value, int minor) {
        params.put(key, String.valueOf(value));
        requiredMinor = Math.max(requiredMinor, minor);
        return this;
    }

    /** 2.53+ */
    public HypervisorListOptions hypervisorHostnamePattern(String pattern) { return put("hypervisor_hostname_pattern", pattern, 53); }
    /** 2.53+ */
    public HypervisorListOptions withServers(boolean withServers) { return put("with_servers", withServers, 53); }
    /** 2.33+ */
    public HypervisorListOptions limit(int limit) { return put("limit", limit, 33); }
    /** Hypervisor id (2.33+; a UUID from 2.53). */
    public HypervisorListOptions marker(String hypervisorId) { return put("marker", hypervisorId, 33); }

    public Map<String, String> toQueryParams() { return new LinkedHashMap<>(params); }

    public String getRequiredMicroVersion() { return requiredMinor == 0 ? null : "2." + requiredMinor; }
}
