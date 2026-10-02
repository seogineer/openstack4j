package org.openstack4j.model.storage.block.options;

/** Filters for {@code GET /backups/detail}. */
public class BackupListOptions extends BlockStorageListOptions<BackupListOptions> {

    public static BackupListOptions create() {
        return new BackupListOptions();
    }

    public BackupListOptions name(String name) { return put("name", name, 0); }
    /** 3.34+ */
    public BackupListOptions nameLike(String fragment) { return put("name~", fragment, 34); }
    public BackupListOptions status(String status) { return put("status", status, 0); }
    public BackupListOptions volumeId(String volumeId) { return put("volume_id", volumeId, 0); }
    /** 3.45+ */
    public BackupListOptions withCount(boolean withCount) { return put("with_count", withCount, 45); }
}
