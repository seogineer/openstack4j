package org.openstack4j.model.manila.ext.options;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The body of {@code POST /v2/share-group-types} (microversion 2.55). */
public final class ShareGroupTypeCreate extends ManilaAttributes<ShareGroupTypeCreate> {

    private ShareGroupTypeCreate() {
    }

    public static ShareGroupTypeCreate create(String name, List<String> shareTypes) {
        return new ShareGroupTypeCreate().put("name", Objects.requireNonNull(name, "name")).put("share_types", Objects.requireNonNull(shareTypes, "shareTypes"));
    }

    @Override
    protected ShareGroupTypeCreate self() {
        return this;
    }

    /** Private types need project access. */
    public ShareGroupTypeCreate isPublic(Boolean isPublic) {
        return put("is_public", isPublic);
    }

    public ShareGroupTypeCreate groupSpecs(Map<String, String> groupSpecs) {
        return put("group_specs", groupSpecs);
    }
}
