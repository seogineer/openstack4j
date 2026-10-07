package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A lock that prevents an action (e.g. delete) on a resource. Fields without a getter are in getAttributes(). */
public interface ResourceLock extends ModelEntity {
    String getId();
    String getUserId();
    String getProjectId();
    String getLockContext();
    String getResourceType();
    String getResourceId();
    String getResourceAction();
    String getLockReason();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
