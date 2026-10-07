package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A share transfer to another project. Fields without a getter are in getAttributes(). */
public interface ShareTransfer extends ModelEntity {
    String getId();
    String getName();
    String getResourceType();
    String getResourceId();
    String getAuthKey();
    String getSourceProjectId();
    String getDestinationProjectId();
    Boolean isAccepted();
    String getExpiresAt();
    String getCreatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
