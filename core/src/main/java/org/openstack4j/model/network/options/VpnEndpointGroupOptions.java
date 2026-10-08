package org.openstack4j.model.network.options;

import java.util.List;
import java.util.Objects;

/** Body of a VPN endpoint group create or update; only the fields set are sent. */
public class VpnEndpointGroupOptions extends NeutronAttributes<VpnEndpointGroupOptions> {

    public static VpnEndpointGroupOptions create(String type, List<String> endpoints) {
        return new VpnEndpointGroupOptions().put("type", Objects.requireNonNull(type, "type")).put("endpoints", Objects.requireNonNull(endpoints, "endpoints"));
    }

    /** An update that sends only the fields set afterwards. */
    public static VpnEndpointGroupOptions update() {
        return new VpnEndpointGroupOptions();
    }

    @Override
    protected VpnEndpointGroupOptions self() {
        return this;
    }

    public VpnEndpointGroupOptions type(String value) {
        return put("type", value);
    }

    public VpnEndpointGroupOptions endpoints(List<String> value) {
        return put("endpoints", value);
    }

    public VpnEndpointGroupOptions name(String value) {
        return put("name", value);
    }

    public VpnEndpointGroupOptions description(String value) {
        return put("description", value);
    }
}
