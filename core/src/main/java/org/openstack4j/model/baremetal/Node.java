package org.openstack4j.model.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A bare metal node. Fields without a getter (they grow with each microversion) are in getAttributes(). */
public interface Node extends ModelEntity {
    String getUuid();
    String getName();
    String getDriver();
    String getProvisionState();
    String getTargetProvisionState();
    String getPowerState();
    String getTargetPowerState();
    Boolean isMaintenance();
    String getMaintenanceReason();
    String getInstanceUuid();
    String getResourceClass();
    String getConductor();
    String getConductorGroup();
    String getOwner();
    String getLessee();
    String getDescription();
    String getLastError();
    Boolean isConsoleEnabled();
    Boolean getAutomatedClean();
    Boolean isProtected();
    Boolean isRetired();
    String getChassisUuid();
    Map<String, Object> getProperties();
    Map<String, Object> getDriverInfo();
    Map<String, Object> getInstanceInfo();
    Map<String, Object> getExtra();
    List<String> getTraits();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter (interfaces, steps, timestamps of later microversions ...) */
    Map<String, Object> getAttributes();
}
