package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A share as the 2.7+ APIs return it (status kept as text; new statuses appear with new microversions). Fields without a getter are in getAttributes(). */
public interface ShareInfo extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getStatus();
    Integer getSize();
    String getShareProto();
    String getShareType();
    String getShareTypeName();
    String getAvailabilityZone();
    String getHost();
    String getShareNetworkId();
    String getShareServerId();
    String getShareGroupId();
    String getSnapshotId();
    Boolean isPublic();
    Map<String, Object> getMetadata();
    String getProjectId();
    String getCreatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
