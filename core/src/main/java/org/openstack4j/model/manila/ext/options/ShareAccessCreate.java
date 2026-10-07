package org.openstack4j.model.manila.ext.options;

import java.util.Map;
import java.util.Objects;

/** The {@code allow_access} action of a share. */
public final class ShareAccessCreate extends ManilaAttributes<ShareAccessCreate> {

    private ShareAccessCreate() {
    }

    /**
     * @param accessType {@code ip}, {@code cert}, {@code user} or {@code cephx}
     * @param accessTo   the client the rule allows, e.g. {@code 10.0.0.0/24}
     */
    public static ShareAccessCreate create(String accessType, String accessTo) {
        return new ShareAccessCreate().put("access_type", Objects.requireNonNull(accessType, "accessType"))
                .put("access_to", Objects.requireNonNull(accessTo, "accessTo"));
    }

    @Override
    protected ShareAccessCreate self() {
        return this;
    }

    /** {@code rw} (default) or {@code ro}. */
    public ShareAccessCreate accessLevel(String accessLevel) {
        return put("access_level", accessLevel);
    }

    /** Needs microversion 2.45. */
    public ShareAccessCreate metadata(Map<String, String> metadata) {
        return put("metadata", metadata);
    }

    /** Hides the access_to and access_key fields from other users (microversion 2.82). */
    public ShareAccessCreate lockVisibility(Boolean lockVisibility) {
        return put("lock_visibility", lockVisibility);
    }

    /** Prevents the rule from being deleted (microversion 2.82). */
    public ShareAccessCreate lockDeletion(Boolean lockDeletion) {
        return put("lock_deletion", lockDeletion);
    }

    /** Needs microversion 2.82. */
    public ShareAccessCreate lockReason(String lockReason) {
        return put("lock_reason", lockReason);
    }
}
