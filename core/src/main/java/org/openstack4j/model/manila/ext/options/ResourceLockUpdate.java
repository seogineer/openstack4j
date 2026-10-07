package org.openstack4j.model.manila.ext.options;


/** The body of {@code PUT /v2/resource-locks/{id}}: only the fields set are changed. */
public final class ResourceLockUpdate extends ManilaAttributes<ResourceLockUpdate> {

    private ResourceLockUpdate() {
    }

    public static ResourceLockUpdate create() {
        return new ResourceLockUpdate();
    }

    @Override
    protected ResourceLockUpdate self() {
        return this;
    }

    public ResourceLockUpdate resourceAction(String resourceAction) {
        return put("resource_action", resourceAction);
    }

    public ResourceLockUpdate lockReason(String lockReason) {
        return put("lock_reason", lockReason);
    }
}
