package org.openstack4j.model.storage.block.options;

import java.util.Map;

/** Filters for {@code GET /volumes/detail} and {@code GET /volumes/summary}. */
public class VolumeListOptions extends BlockStorageListOptions<VolumeListOptions> {

    public static VolumeListOptions create() {
        return new VolumeListOptions();
    }

    public VolumeListOptions name(String name) { return put("name", name, 0); }
    /** Substring match (3.34+). */
    public VolumeListOptions nameLike(String fragment) { return put("name~", fragment, 34); }
    public VolumeListOptions status(String status) { return put("status", status, 0); }
    public VolumeListOptions bootable(boolean bootable) { return put("bootable", bootable, 0); }
    public VolumeListOptions metadata(Map<String, String> metadata) { return put("metadata", dict(metadata), 0); }
    /** 3.4+ */
    public VolumeListOptions glanceMetadata(Map<String, String> metadata) { return put("glance_metadata", dict(metadata), 4); }
    /** 3.10+ */
    public VolumeListOptions groupId(String groupId) { return put("group_id", groupId, 10); }
    /** Adds {@code count} to the response (3.45+). */
    public VolumeListOptions withCount(boolean withCount) { return put("with_count", withCount, 45); }
    /** Time comparison filter (3.60+): {@code op} is gt, gte, eq, neq, lt or lte. */
    public VolumeListOptions createdAt(String op, String isoTime) { return put("created_at", op + ":" + isoTime, 60); }
    /** 3.60+ */
    public VolumeListOptions updatedAt(String op, String isoTime) { return put("updated_at", op + ":" + isoTime, 60); }
    /** 3.65+ */
    public VolumeListOptions consumesQuota(boolean consumesQuota) { return put("consumes_quota", consumesQuota, 65); }
}
