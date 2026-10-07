package org.openstack4j.model.baremetal;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A bare metal volume connector (an initiator of a node, e.g. an iSCSI IQN). Fields without a getter are in getAttributes(). */
public interface VolumeConnector extends ModelEntity {
    String getUuid();
    String getNodeUuid();
    String getType();
    String getConnectorId();
    Map<String, Object> getExtra();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
