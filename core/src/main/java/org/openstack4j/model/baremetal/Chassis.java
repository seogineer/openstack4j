package org.openstack4j.model.baremetal;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A bare metal chassis (a group of nodes). Fields without a getter are in getAttributes(). */
public interface Chassis extends ModelEntity {
    String getUuid();
    String getDescription();
    Map<String, Object> getExtra();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
