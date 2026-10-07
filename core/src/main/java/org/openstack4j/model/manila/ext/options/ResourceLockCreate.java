package org.openstack4j.model.manila.ext.options;

import java.util.Objects;

/** The body of {@code POST /v2/resource-locks} (microversion 2.81). */
public final class ResourceLockCreate extends ManilaAttributes<ResourceLockCreate> {

    private ResourceLockCreate() {
    }

    public static ResourceLockCreate create(String resourceId) {
        return new ResourceLockCreate().put("resource_id", Objects.requireNonNull(resourceId, "resourceId"));
    }

    @Override
    protected ResourceLockCreate self() {
        return this;
    }

    /** {@code share} (default) or {@code access_rule}. */
    public ResourceLockCreate resourceType(String resourceType) {
        return put("resource_type", resourceType);
    }

    /** {@code delete} (default) or {@code show}. */
    public ResourceLockCreate resourceAction(String resourceAction) {
        return put("resource_action", resourceAction);
    }

    public ResourceLockCreate lockReason(String lockReason) {
        return put("lock_reason", lockReason);
    }
}
