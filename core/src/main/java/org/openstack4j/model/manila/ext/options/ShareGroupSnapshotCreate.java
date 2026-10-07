package org.openstack4j.model.manila.ext.options;

import java.util.Objects;

/** The body of {@code POST /v2/share-group-snapshots} (microversion 2.55). */
public final class ShareGroupSnapshotCreate extends ManilaAttributes<ShareGroupSnapshotCreate> {

    private ShareGroupSnapshotCreate() {
    }

    public static ShareGroupSnapshotCreate create(String shareGroupId) {
        return new ShareGroupSnapshotCreate().put("share_group_id", Objects.requireNonNull(shareGroupId, "shareGroupId"));
    }

    @Override
    protected ShareGroupSnapshotCreate self() {
        return this;
    }

    public ShareGroupSnapshotCreate name(String name) {
        return put("name", name);
    }

    public ShareGroupSnapshotCreate description(String description) {
        return put("description", description);
    }
}
