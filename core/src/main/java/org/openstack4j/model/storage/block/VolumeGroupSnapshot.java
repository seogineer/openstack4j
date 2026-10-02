package org.openstack4j.model.storage.block;

import java.util.Date;

import org.openstack4j.model.ModelEntity;

/** A snapshot of a volume group (3.14+). */
public interface VolumeGroupSnapshot extends ModelEntity {
    String getId();
    String getGroupId();
    String getStatus();
    Date getCreatedAt();
    String getName();
    String getDescription();
    String getGroupTypeId();
    /** @return project_id (3.58+) */
    String getProjectId();
}
