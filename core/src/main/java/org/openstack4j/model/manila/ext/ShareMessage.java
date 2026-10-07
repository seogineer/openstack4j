package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A user message about an asynchronous failure. Fields without a getter are in getAttributes(). */
public interface ShareMessage extends ModelEntity {
    String getId();
    String getResourceType();
    String getResourceId();
    String getActionId();
    String getMessageLevel();
    String getUserMessage();
    String getDetailId();
    String getRequestId();
    String getProjectId();
    String getCreatedAt();
    String getExpiresAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
