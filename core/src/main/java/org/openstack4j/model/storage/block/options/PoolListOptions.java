package org.openstack4j.model.storage.block.options;

/** Filters for {@code GET /scheduler-stats/get_pools} (capability filters 3.28+, volume type 3.35+). */
public class PoolListOptions extends BlockStorageListOptions<PoolListOptions> {

    public static PoolListOptions create() {
        return new PoolListOptions();
    }

    /** Filters on a backend capability such as {@code QoS_support}. */
    public PoolListOptions capability(String key, String value) { return put(key, value, 28); }
    /** Only pools that can host the given volume type (3.35+). */
    public PoolListOptions volumeType(String volumeType) { return put("volume_type", volumeType, 35); }
}
