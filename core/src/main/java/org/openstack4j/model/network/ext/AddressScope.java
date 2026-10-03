package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** An address scope: subnet pools whose prefixes must not overlap. */
public interface AddressScope extends ModelEntity {
    String getId();
    String getName();
    String getProjectId();
    Integer getIpVersion();
    Boolean isShared();
}
