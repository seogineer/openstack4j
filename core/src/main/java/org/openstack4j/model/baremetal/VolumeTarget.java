package org.openstack4j.model.baremetal;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A bare metal volume target (a volume a node boots from or attaches). Fields without a getter are in getAttributes(). */
public interface VolumeTarget extends ModelEntity {
    String getUuid();
    String getNodeUuid();
    String getVolumeType();
    Integer getBootIndex();
    String getVolumeId();
    Map<String, Object> getProperties();
    Map<String, Object> getExtra();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
