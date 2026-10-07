package org.openstack4j.model.baremetal;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A bare metal port group (bonded ports of a node). Fields without a getter are in getAttributes(). */
public interface Portgroup extends ModelEntity {
    String getUuid();
    String getName();
    String getAddress();
    String getNodeUuid();
    String getMode();
    Map<String, Object> getProperties();
    Boolean isStandalonePortsSupported();
    Map<String, Object> getExtra();
    Map<String, Object> getInternalInfo();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
