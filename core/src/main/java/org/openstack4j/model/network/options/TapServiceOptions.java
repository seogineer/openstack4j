package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a tap service create or update; only the fields set are sent. */
public class TapServiceOptions extends NeutronAttributes<TapServiceOptions> {

    public static TapServiceOptions create(String portId) {
        return new TapServiceOptions().put("port_id", Objects.requireNonNull(portId, "portId"));
    }

    /** An update that sends only the fields set afterwards. */
    public static TapServiceOptions update() {
        return new TapServiceOptions();
    }

    @Override
    protected TapServiceOptions self() {
        return this;
    }

    public TapServiceOptions portId(String value) {
        return put("port_id", value);
    }

    public TapServiceOptions name(String value) {
        return put("name", value);
    }

    public TapServiceOptions description(String value) {
        return put("description", value);
    }
}
