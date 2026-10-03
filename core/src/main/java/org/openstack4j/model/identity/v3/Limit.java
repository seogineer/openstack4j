package org.openstack4j.model.identity.v3;

import org.openstack4j.model.ModelEntity;

/** A project or domain limit overriding a registered limit (unified limits). */
public interface Limit extends ModelEntity {
    String getId();
    String getServiceId();
    String getRegionId();
    String getResourceName();
    Integer getResourceLimit();
    String getDescription();
    String getProjectId();
    String getDomainId();
}
