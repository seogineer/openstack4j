package org.openstack4j.model.manila.ext.options;


/** The body of {@code PUT /v2/share-groups/{id}}: only the fields set are changed. */
public final class ShareGroupUpdate extends ManilaAttributes<ShareGroupUpdate> {

    private ShareGroupUpdate() {
    }

    public static ShareGroupUpdate create() {
        return new ShareGroupUpdate();
    }

    @Override
    protected ShareGroupUpdate self() {
        return this;
    }

    public ShareGroupUpdate name(String name) {
        return put("name", name);
    }

    public ShareGroupUpdate description(String description) {
        return put("description", description);
    }
}
