package org.openstack4j.model.manila.ext.options;


/** The body of {@code PUT /v2/share-backups/{id}}: only the fields set are changed. */
public final class ShareBackupUpdate extends ManilaAttributes<ShareBackupUpdate> {

    private ShareBackupUpdate() {
    }

    public static ShareBackupUpdate create() {
        return new ShareBackupUpdate();
    }

    @Override
    protected ShareBackupUpdate self() {
        return this;
    }

    public ShareBackupUpdate name(String name) {
        return put("name", name);
    }

    public ShareBackupUpdate description(String description) {
        return put("description", description);
    }
}
