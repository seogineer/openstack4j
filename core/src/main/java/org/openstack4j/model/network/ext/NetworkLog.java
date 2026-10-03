package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A network log (security group or firewall group logging). */
public interface NetworkLog extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getProjectId();
    Boolean isEnabled();
    String getResourceType();
    String getResourceId();
    String getTargetId();
    String getEvent();
    Integer getRevisionNumber();
}
