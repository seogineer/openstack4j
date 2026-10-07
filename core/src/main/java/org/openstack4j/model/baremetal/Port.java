package org.openstack4j.model.baremetal;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A bare metal port (a NIC of a node). Fields without a getter are in getAttributes(). */
public interface Port extends ModelEntity {
    String getUuid();
    String getName();
    String getAddress();
    String getNodeUuid();
    String getPortgroupUuid();
    Map<String, Object> getLocalLinkConnection();
    Boolean isPxeEnabled();
    String getPhysicalNetwork();
    Boolean isSmartnic();
    Map<String, Object> getExtra();
    Map<String, Object> getInternalInfo();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
