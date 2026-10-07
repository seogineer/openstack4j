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
}
