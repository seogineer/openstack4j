package org.openstack4j.api.baremetal;

import org.openstack4j.common.RestService;

/** Bare metal (Ironic v1). Turn microversions on with {@code microVersions().negotiate()} for fields and APIs newer than 1.1. */
public interface BaremetalService extends RestService {

    /** @return the opt-in microversion controls of this session */
    BaremetalMicroVersionService microVersions();

    /** @return the bare metal nodes */
    NodeService nodes();

    /** @return the bare metal ports */
    PortService ports();

    /** @return the bare metal port groups (microversion 1.23) */
    PortgroupService portgroups();

    /** @return the bare metal chassis */
    ChassisService chassis();

    /** @return the bare metal drivers */
    DriverService drivers();

    /** @return the bare metal allocations (microversion 1.52) */
    AllocationService allocations();

    /** @return the deploy templates (microversion 1.55) */
    DeployTemplateService deployTemplates();

    /** @return the runbooks (microversion 1.92) */
    RunbookService runbooks();

    /** @return the inspection rules (microversion 1.96) */
    InspectionRuleService inspectionRules();

    /** @return the volume connectors (microversion 1.32) */
    VolumeConnectorService volumeConnectors();

    /** @return the volume targets (microversion 1.32) */
    VolumeTargetService volumeTargets();

    /** @return the conductors (microversion 1.49) and node shards (1.82) */
    ConductorService conductors();
}
