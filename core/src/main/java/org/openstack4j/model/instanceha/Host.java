package org.openstack4j.model.instanceha;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A host of a failover segment. Fields without a getter are in getAttributes(). */
public interface Host extends ModelEntity {
    String getUuid();
    String getName();
    String getType();
    String getControlAttributes();
    Boolean isReserved();
    Boolean isOnMaintenance();
    String getFailoverSegmentId();
    Map<String, Object> getFailoverSegment();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
