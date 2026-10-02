package org.openstack4j.model.storage.block;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A generic volume group (3.13+). */
public interface VolumeGroup extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getStatus();
    String getAvailabilityZone();
    Date getCreatedAt();
    String getGroupType();
    List<String> getVolumeTypes();
    /** @return member volume ids (3.25+) */
    List<String> getVolumes();
    String getGroupSnapshotId();
    String getSourceGroupId();
    /** @return project_id (3.58+) */
    String getProjectId();
    String getReplicationStatus();
}
