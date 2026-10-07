package org.openstack4j.model.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A group of shares that are snapshotted together. Fields without a getter are in getAttributes(). */
public interface ShareGroup extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getStatus();
    String getAvailabilityZone();
    String getShareGroupTypeId();
    List<String> getShareTypes();
    String getShareNetworkId();
    String getShareServerId();
    String getSourceShareGroupSnapshotId();
    String getHost();
    String getConsistentSnapshotSupport();
    String getProjectId();
    String getCreatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
