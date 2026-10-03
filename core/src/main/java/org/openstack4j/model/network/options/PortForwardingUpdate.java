package org.openstack4j.model.network.options;

/** Body of {@code PUT /floatingips/{id}/port_forwardings/{id}}; only the fields set are sent. */
public class PortForwardingUpdate extends NeutronAttributes<PortForwardingUpdate> {

    public static PortForwardingUpdate create() {
        return new PortForwardingUpdate();
    }

    @Override
    protected PortForwardingUpdate self() {
        return this;
    }

    public PortForwardingUpdate internalPortId(String id) { return put("internal_port_id", id); }
    public PortForwardingUpdate internalIpAddress(String ip) { return put("internal_ip_address", ip); }
    public PortForwardingUpdate internalPort(Integer port) { return put("internal_port", port); }
    public PortForwardingUpdate externalPort(Integer port) { return put("external_port", port); }
    /** For example {@code "100:200"} (floating-ip-port-forwarding-port-ranges). */
    public PortForwardingUpdate internalPortRange(String range) { return put("internal_port_range", range); }
    public PortForwardingUpdate externalPortRange(String range) { return put("external_port_range", range); }
    public PortForwardingUpdate protocol(String protocol) { return put("protocol", protocol); }
    public PortForwardingUpdate description(String description) { return put("description", description); }
}
