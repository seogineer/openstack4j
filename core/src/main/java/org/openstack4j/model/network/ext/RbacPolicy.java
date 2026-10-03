package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** An RBAC policy sharing a network, QoS policy, security group, address scope, address group or subnet pool. */
public interface RbacPolicy extends ModelEntity {
    String getId();
    String getObjectType();
    String getObjectId();
    String getAction();
    String getTargetTenant();
    String getProjectId();
}
