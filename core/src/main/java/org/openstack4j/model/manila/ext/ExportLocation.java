package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An export location (mount path) of a share or share instance. Fields without a getter are in getAttributes(). */
public interface ExportLocation extends ModelEntity {
    String getId();
    String getPath();
    Boolean isPreferred();
    Boolean isAdminOnly();
    String getShareInstanceId();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
