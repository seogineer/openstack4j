package org.openstack4j.model.instanceha;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A failover segment (hosts that protect each other's instances). Fields without a getter are in getAttributes(). */
public interface Segment extends ModelEntity {
    String getUuid();
    String getName();
    String getDescription();
    String getServiceType();
    String getRecoveryMethod();
    Boolean isEnabled();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
