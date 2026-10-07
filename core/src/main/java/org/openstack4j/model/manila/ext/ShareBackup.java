package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A backup of a share (experimental API; the header is sent for you). Fields without a getter are in getAttributes(). */
public interface ShareBackup extends ModelEntity {
    String getId();
    String getShareId();
    String getName();
    String getDescription();
    String getStatus();
    Integer getSize();
    String getAvailabilityZone();
    String getProgress();
    String getRestoreProgress();
    String getBackupType();
    String getHost();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
