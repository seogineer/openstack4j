package org.openstack4j.model.instanceha;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An instance move made while recovering from a host failure (instance-ha 1.3). Fields without a getter are in getAttributes(). */
public interface VMove extends ModelEntity {
    String getUuid();
    String getNotificationUuid();
    String getInstanceUuid();
    String getInstanceName();
    String getSourceHost();
    String getDestHost();
    String getStartTime();
    String getEndTime();
    String getType();
    String getStatus();
    String getMessage();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
