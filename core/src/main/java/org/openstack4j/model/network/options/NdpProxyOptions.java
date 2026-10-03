package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of an NDP proxy create or update. */
public class NdpProxyOptions extends NeutronAttributes<NdpProxyOptions> {

    public static NdpProxyOptions create(String routerId, String portId) {
        return new NdpProxyOptions().put("router_id", Objects.requireNonNull(routerId)).put("port_id", Objects.requireNonNull(portId));
    }

    /** An update that sends only the fields set afterwards. */
    public static NdpProxyOptions update() {
        return new NdpProxyOptions();
    }

    @Override
    protected NdpProxyOptions self() {
        return this;
    }

    public NdpProxyOptions name(String value) { return put("name", value); }
    public NdpProxyOptions description(String value) { return put("description", value); }
    public NdpProxyOptions ipAddress(String value) { return put("ip_address", value); }
}
