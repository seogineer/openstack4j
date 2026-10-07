package org.openstack4j.model.manila.ext.options;

import java.util.List;

/** The body of {@code POST /v2/share-groups} (microversion 2.55). */
public final class ShareGroupCreate extends ManilaAttributes<ShareGroupCreate> {

    private ShareGroupCreate() {
    }

    public static ShareGroupCreate create() {
        return new ShareGroupCreate();
    }

    @Override
    protected ShareGroupCreate self() {
        return this;
    }

    public ShareGroupCreate name(String name) {
        return put("name", name);
    }

    public ShareGroupCreate description(String description) {
        return put("description", description);
    }

    /** Share type ids of the group. */
    public ShareGroupCreate shareTypes(List<String> shareTypes) {
        return put("share_types", shareTypes);
    }

    public ShareGroupCreate shareGroupTypeId(String shareGroupTypeId) {
        return put("share_group_type_id", shareGroupTypeId);
    }

    public ShareGroupCreate availabilityZone(String availabilityZone) {
        return put("availability_zone", availabilityZone);
    }

    public ShareGroupCreate shareNetworkId(String shareNetworkId) {
        return put("share_network_id", shareNetworkId);
    }

    /** Creates the group from a group snapshot. */
    public ShareGroupCreate sourceShareGroupSnapshotId(String sourceShareGroupSnapshotId) {
        return put("source_share_group_snapshot_id", sourceShareGroupSnapshotId);
    }
}
