package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A share instance as the 2.7+ APIs return it (status kept as text). Fields without a getter are in getAttributes(). */
public interface ShareInstanceInfo extends ModelEntity {
    String getId();
    String getShareId();
    String getStatus();
    String getReplicaState();
    String getAvailabilityZone();
    String getHost();
    String getShareNetworkId();
    String getShareServerId();
    String getProgress();
    Boolean isCastRulesToReadonly();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
