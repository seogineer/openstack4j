package org.openstack4j.model.manila.ext.options;

import java.util.Objects;

/** The body of {@code POST /v2/share-transfers} (microversion 2.77). */
public final class ShareTransferCreate extends ManilaAttributes<ShareTransferCreate> {

    private ShareTransferCreate() {
    }

    public static ShareTransferCreate create(String shareId) {
        return new ShareTransferCreate().put("share_id", Objects.requireNonNull(shareId, "shareId"));
    }

    @Override
    protected ShareTransferCreate self() {
        return this;
    }

    public ShareTransferCreate name(String name) {
        return put("name", name);
    }
}
