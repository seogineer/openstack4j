package org.openstack4j.model.manila.ext.options;

import java.util.Map;
import java.util.Objects;

/** The body of {@code POST /v2/share-replicas} (microversion 2.56). */
public final class ShareReplicaCreate extends ManilaAttributes<ShareReplicaCreate> {

    private ShareReplicaCreate() {
    }

    public static ShareReplicaCreate create(String shareId) {
        return new ShareReplicaCreate().put("share_id", Objects.requireNonNull(shareId, "shareId"));
    }

    @Override
    protected ShareReplicaCreate self() {
        return this;
    }

    public ShareReplicaCreate availabilityZone(String availabilityZone) {
        return put("availability_zone", availabilityZone);
    }

    /** Needs microversion 2.72. */
    public ShareReplicaCreate shareNetworkId(String shareNetworkId) {
        return put("share_network_id", shareNetworkId);
    }

    /** e.g. {@code only_host}; needs microversion 2.67. */
    public ShareReplicaCreate schedulerHints(Map<String, String> schedulerHints) {
        return put("scheduler_hints", schedulerHints);
    }
}
