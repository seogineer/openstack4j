package org.openstack4j.model.manila.ext.options;

import java.util.Objects;

/** The {@code migration_start} action of a share (microversion 2.29). */
public final class ShareMigration extends ManilaAttributes<ShareMigration> {

    private ShareMigration() {
    }

    /** @param host the destination pool, e.g. {@code ubuntu@generic2#GENERIC2} */
    public static ShareMigration to(String host, boolean writable, boolean preserveMetadata, boolean preserveSnapshots, boolean nondisruptive) {
        return new ShareMigration().put("host", Objects.requireNonNull(host, "host")).put("writable", writable)
                .put("preserve_metadata", preserveMetadata).put("preserve_snapshots", preserveSnapshots).put("nondisruptive", nondisruptive);
    }

    @Override
    protected ShareMigration self() {
        return this;
    }

    public ShareMigration newShareNetworkId(String newShareNetworkId) {
        return put("new_share_network_id", newShareNetworkId);
    }

    public ShareMigration newShareTypeId(String newShareTypeId) {
        return put("new_share_type_id", newShareTypeId);
    }

    public ShareMigration forceHostAssistedMigration(Boolean forceHostAssistedMigration) {
        return put("force_host_assisted_migration", forceHostAssistedMigration);
    }
}
