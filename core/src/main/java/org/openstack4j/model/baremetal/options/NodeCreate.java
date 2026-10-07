package org.openstack4j.model.baremetal.options;

import java.util.Map;
import java.util.Objects;

/** The body of {@code POST /v1/nodes}. Only {@code driver} is required. */
public final class NodeCreate extends BaremetalAttributes<NodeCreate> {

    private NodeCreate() {
    }

    /** @param driver the hardware type, e.g. {@code ipmi} or {@code redfish} */
    public static NodeCreate create(String driver) {
        return new NodeCreate().put("driver", Objects.requireNonNull(driver, "driver"));
    }

    @Override
    protected NodeCreate self() {
        return this;
    }

    public NodeCreate name(String name) {
        return put("name", name);
    }

    public NodeCreate uuid(String uuid) {
        return put("uuid", uuid);
    }

    public NodeCreate description(String description) {
        return put("description", description);
    }

    /** Needs microversion 1.21. */
    public NodeCreate resourceClass(String resourceClass) {
        return put("resource_class", resourceClass);
    }

    public NodeCreate chassisUuid(String chassisUuid) {
        return put("chassis_uuid", chassisUuid);
    }

    public NodeCreate driverInfo(Map<String, ?> driverInfo) {
        return put("driver_info", driverInfo);
    }

    public NodeCreate properties(Map<String, ?> properties) {
        return put("properties", properties);
    }

    public NodeCreate extra(Map<String, ?> extra) {
        return put("extra", extra);
    }

    /** Needs microversion 1.46. */
    public NodeCreate conductorGroup(String conductorGroup) {
        return put("conductor_group", conductorGroup);
    }

    /** Needs microversion 1.50. */
    public NodeCreate owner(String owner) {
        return put("owner", owner);
    }

    /** Needs microversion 1.65. */
    public NodeCreate lessee(String lessee) {
        return put("lessee", lessee);
    }

    /** Needs microversion 1.31; e.g. {@code bootInterface} is {@code attribute("boot_interface", "pxe")}. */
    public NodeCreate networkInterface(String networkInterface) {
        return put("network_interface", networkInterface);
    }
}
