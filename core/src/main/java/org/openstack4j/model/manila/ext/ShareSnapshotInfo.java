package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A share snapshot as the 2.12+ APIs return it (status kept as text). Fields without a getter are in getAttributes(). */
public interface ShareSnapshotInfo extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getStatus();
    String getShareId();
    Integer getSize();
    Integer getShareSize();
    String getShareProto();
    String getProviderLocation();
    String getProjectId();
    String getCreatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
