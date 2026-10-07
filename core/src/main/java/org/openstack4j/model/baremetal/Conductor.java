package org.openstack4j.model.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A bare metal conductor (microversion 1.49). Fields without a getter are in getAttributes(). */
public interface Conductor extends ModelEntity {
    String getHostname();
    String getConductorGroup();
    Boolean isAlive();
    List<String> getDrivers();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
