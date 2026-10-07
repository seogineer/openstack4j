package org.openstack4j.model.baremetal;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** The state summary of a node ({@code GET /v1/nodes/{node}/states}). */
public interface NodeStates extends ModelEntity {
    String getPowerState();
    String getTargetPowerState();
    String getProvisionState();
    String getTargetProvisionState();
    String getLastError();
    Boolean getConsoleEnabled();
    /** @return {@code bios} or {@code uefi} (microversion 1.75) */
    String getBootMode();
    /** @return whether UEFI secure boot is on (microversion 1.75) */
    Boolean getSecureBoot();
    Map<String, Object> getRaidConfig();
    Map<String, Object> getTargetRaidConfig();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
