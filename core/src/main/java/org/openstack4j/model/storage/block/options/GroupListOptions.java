package org.openstack4j.model.storage.block.options;

/** Filters for {@code GET /groups[/detail]} (3.13+). */
public class GroupListOptions extends BlockStorageListOptions<GroupListOptions> {

    public static GroupListOptions create() {
        return new GroupListOptions();
    }

    public GroupListOptions name(String name) { return put("name", name, 0); }
    public GroupListOptions status(String status) { return put("status", status, 0); }
}
