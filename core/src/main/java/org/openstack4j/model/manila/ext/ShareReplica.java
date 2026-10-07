package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A replica of a share. Fields without a getter are in getAttributes(). */
public interface ShareReplica extends ModelEntity {
    String getId();
    String getShareId();
    String getStatus();
    String getReplicaState();
    String getAvailabilityZone();
    String getShareNetworkId();
    String getShareServerId();
    String getHost();
    Boolean isCastRulesToReadonly();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
