package org.openstack4j.model.baremetal.options;

import java.util.Map;
import java.util.Objects;

/** The body of {@code POST /v1/ports}. */
public final class PortCreate extends BaremetalAttributes<PortCreate> {

    private PortCreate() {
    }

    /**
     * @param nodeUuid the node the port belongs to
     * @param address  the MAC address
     */
    public static PortCreate create(String nodeUuid, String address) {
        return new PortCreate().put("node_uuid", Objects.requireNonNull(nodeUuid, "nodeUuid")).put("address", Objects.requireNonNull(address, "address"));
    }

    @Override
    protected PortCreate self() {
        return this;
    }

    /** Needs microversion 1.88. */
    public PortCreate name(String name) {
        return put("name", name);
    }

    /** Needs microversion 1.24. */
    public PortCreate portgroupUuid(String portgroupUuid) {
        return put("portgroup_uuid", portgroupUuid);
    }

    /** e.g. {@code switch_id}, {@code port_id}, {@code switch_info} (microversion 1.19). */
    public PortCreate localLinkConnection(Map<String, ?> localLinkConnection) {
        return put("local_link_connection", localLinkConnection);
    }

    /** Needs microversion 1.19. */
    public PortCreate pxeEnabled(Boolean pxeEnabled) {
        return put("pxe_enabled", pxeEnabled);
    }

    /** Needs microversion 1.34. */
    public PortCreate physicalNetwork(String physicalNetwork) {
        return put("physical_network", physicalNetwork);
    }

    /** Needs microversion 1.53. */
    public PortCreate smartnic(Boolean smartnic) {
        return put("is_smartnic", smartnic);
    }

    public PortCreate extra(Map<String, ?> extra) {
        return put("extra", extra);
    }
}
