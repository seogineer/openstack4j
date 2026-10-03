package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of an RBAC policy create or update; the target project is sent as {@code target_tenant} ("*" = every project). */
public class RbacPolicyOptions extends NeutronAttributes<RbacPolicyOptions> {

    public static RbacPolicyOptions create(String objectType, String objectId, String action, String targetProject) {
        return new RbacPolicyOptions().put("object_type", Objects.requireNonNull(objectType)).put("object_id", Objects.requireNonNull(objectId))
                .put("action", Objects.requireNonNull(action)).put("target_tenant", Objects.requireNonNull(targetProject));
    }

    /** Only the target project of an RBAC policy can change. */
    public static RbacPolicyOptions update(String targetProject) {
        return new RbacPolicyOptions().put("target_tenant", Objects.requireNonNull(targetProject));
    }

    @Override
    protected RbacPolicyOptions self() {
        return this;
    }

    public RbacPolicyOptions projectId(String projectId) { return put("project_id", projectId); }
}
