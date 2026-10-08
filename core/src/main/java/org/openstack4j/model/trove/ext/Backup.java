package org.openstack4j.model.trove.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A database backup. Fields without a getter are in getAttributes(). */
public interface Backup extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getStatus();
    String getInstanceId();
    Double getSize();
    String getParentId();
    String getLocationRef();
    String getStorageDriver();
    Map<String, Object> getDatastore();
    String getProjectId();
    String getCreated();
    String getUpdated();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
