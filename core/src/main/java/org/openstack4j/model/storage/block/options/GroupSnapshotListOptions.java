package org.openstack4j.model.storage.block.options;

/** Filters for {@code GET /group_snapshots[/detail]}; filtering, sorting and paging need 3.29. */
public class GroupSnapshotListOptions extends BlockStorageListOptions<GroupSnapshotListOptions> {

    public static GroupSnapshotListOptions create() {
        return new GroupSnapshotListOptions();
    }

    @Override public GroupSnapshotListOptions limit(int limit) { return put("limit", limit, 29); }
    @Override public GroupSnapshotListOptions marker(String marker) { return put("marker", marker, 29); }
    @Override public GroupSnapshotListOptions offset(int offset) { return put("offset", offset, 29); }
    @Override public GroupSnapshotListOptions sortKey(String key) { return put("sort_key", key, 29); }
    @Override public GroupSnapshotListOptions sortDir(String dir) { return put("sort_dir", dir, 29); }
    @Override public GroupSnapshotListOptions sort(String sort) { return put("sort", sort, 29); }
    @Override public GroupSnapshotListOptions allTenants(boolean allTenants) { return put("all_tenants", allTenants, 29); }
    public GroupSnapshotListOptions name(String name) { return put("name", name, 29); }
    public GroupSnapshotListOptions status(String status) { return put("status", status, 29); }
    public GroupSnapshotListOptions groupId(String groupId) { return put("group_id", groupId, 29); }
}
