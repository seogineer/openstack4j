package org.openstack4j.model.storage.block.options;

import java.util.Map;

/** Filters for {@code GET /snapshots/detail}. */
public class SnapshotListOptions extends BlockStorageListOptions<SnapshotListOptions> {

    public static SnapshotListOptions create() {
        return new SnapshotListOptions();
    }

    public SnapshotListOptions name(String name) { return put("name", name, 0); }
    /** 3.34+ */
    public SnapshotListOptions nameLike(String fragment) { return put("name~", fragment, 34); }
    public SnapshotListOptions status(String status) { return put("status", status, 0); }
    public SnapshotListOptions volumeId(String volumeId) { return put("volume_id", volumeId, 0); }
    /** 3.22+ */
    public SnapshotListOptions metadata(Map<String, String> metadata) { return put("metadata", dict(metadata), 22); }
    /** 3.45+ */
    public SnapshotListOptions withCount(boolean withCount) { return put("with_count", withCount, 45); }
    /** 3.65+ */
    public SnapshotListOptions consumesQuota(boolean consumesQuota) { return put("consumes_quota", consumesQuota, 65); }
}
