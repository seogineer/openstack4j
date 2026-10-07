package org.openstack4j.model.manila.ext.options;

import java.util.Map;
import java.util.Objects;

/** The body of {@code POST /v2/share-backups} (microversion 2.80). */
public final class ShareBackupCreate extends ManilaAttributes<ShareBackupCreate> {

    private ShareBackupCreate() {
    }

    public static ShareBackupCreate create(String shareId) {
        return new ShareBackupCreate().put("share_id", Objects.requireNonNull(shareId, "shareId"));
    }

    @Override
    protected ShareBackupCreate self() {
        return this;
    }

    public ShareBackupCreate name(String name) {
        return put("name", name);
    }

    public ShareBackupCreate description(String description) {
        return put("description", description);
    }

    /** Back-end options, e.g. a backup type (2.85). */
    public ShareBackupCreate backupOptions(Map<String, Object> backupOptions) {
        return put("backup_options", backupOptions);
    }
}
