package org.openstack4j.model.manila.ext.options;


/** The body of {@code PUT /v2/share-group-snapshots/{id}}: only the fields set are changed. */
public final class ShareGroupSnapshotUpdate extends ManilaAttributes<ShareGroupSnapshotUpdate> {

    private ShareGroupSnapshotUpdate() {
    }

    public static ShareGroupSnapshotUpdate create() {
        return new ShareGroupSnapshotUpdate();
    }

    @Override
    protected ShareGroupSnapshotUpdate self() {
        return this;
    }

    public ShareGroupSnapshotUpdate name(String name) {
        return put("name", name);
    }

    public ShareGroupSnapshotUpdate description(String description) {
        return put("description", description);
    }
}
