package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** The default statefulness of new security groups in a project. */
public interface SecurityGroupDefaultStatefulness extends ModelEntity {
    String getId();
    String getProjectId();
    Boolean isStateful();
}
