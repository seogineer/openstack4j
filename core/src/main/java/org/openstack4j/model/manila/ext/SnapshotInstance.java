package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An instance of a share snapshot (admin). Fields without a getter are in getAttributes(). */
public interface SnapshotInstance extends ModelEntity {
    String getId();
    String getSnapshotId();
    String getShareId();
    String getShareInstanceId();
    String getStatus();
    String getProgress();
    String getProviderLocation();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
