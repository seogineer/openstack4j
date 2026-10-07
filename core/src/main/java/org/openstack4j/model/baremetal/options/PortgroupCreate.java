package org.openstack4j.model.baremetal.options;

import java.util.Map;
import java.util.Objects;

/** The body of {@code POST /v1/portgroups} (microversion 1.23). */
public final class PortgroupCreate extends BaremetalAttributes<PortgroupCreate> {

    private PortgroupCreate() {
    }

    /** @param nodeUuid the node the port group belongs to */
    public static PortgroupCreate create(String nodeUuid) {
        return new PortgroupCreate().put("node_uuid", Objects.requireNonNull(nodeUuid, "nodeUuid"));
    }

    @Override
    protected PortgroupCreate self() {
        return this;
    }

    public PortgroupCreate name(String name) {
        return put("name", name);
    }

    /** The MAC address. */
    public PortgroupCreate address(String address) {
        return put("address", address);
    }

    /** The bonding mode, e.g. {@code active-backup} or {@code 802.3ad} (microversion 1.26). */
    public PortgroupCreate mode(String mode) {
        return put("mode", mode);
    }

    /** Bonding properties, e.g. {@code miimon} (microversion 1.26). */
    public PortgroupCreate properties(Map<String, ?> properties) {
        return put("properties", properties);
    }

    public PortgroupCreate standalonePortsSupported(Boolean standalonePortsSupported) {
        return put("standalone_ports_supported", standalonePortsSupported);
    }

    public PortgroupCreate extra(Map<String, ?> extra) {
        return put("extra", extra);
    }
}
