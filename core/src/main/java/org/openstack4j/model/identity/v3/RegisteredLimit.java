package org.openstack4j.model.identity.v3;

import org.openstack4j.model.ModelEntity;

/** A registered limit: the default limit of a resource for a service (unified limits). */
public interface RegisteredLimit extends ModelEntity {
    String getId();
    String getServiceId();
    String getRegionId();
    String getResourceName();
    Integer getDefaultLimit();
    String getDescription();
}
