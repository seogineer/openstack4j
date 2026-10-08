package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a tap flow create or update; only the fields set are sent. */
public class TapFlowOptions extends NeutronAttributes<TapFlowOptions> {

    public static TapFlowOptions create(String tapServiceId, String sourcePort, String direction) {
        return new TapFlowOptions().put("tap_service_id", Objects.requireNonNull(tapServiceId, "tapServiceId")).put("source_port", Objects.requireNonNull(sourcePort, "sourcePort")).put("direction", Objects.requireNonNull(direction, "direction"));
    }

    /** An update that sends only the fields set afterwards. */
    public static TapFlowOptions update() {
        return new TapFlowOptions();
    }

    @Override
    protected TapFlowOptions self() {
        return this;
    }

    public TapFlowOptions tapServiceId(String value) {
        return put("tap_service_id", value);
    }

    public TapFlowOptions sourcePort(String value) {
        return put("source_port", value);
    }

    public TapFlowOptions direction(String value) {
        return put("direction", value);
    }

    public TapFlowOptions name(String value) {
        return put("name", value);
    }

    public TapFlowOptions description(String value) {
        return put("description", value);
    }

    /** e.g. {@code 9,18,27-36}. */
    public TapFlowOptions vlanFilter(String value) {
        return put("vlan_filter", value);
    }
}
