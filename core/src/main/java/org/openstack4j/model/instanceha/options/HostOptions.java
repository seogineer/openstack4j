package org.openstack4j.model.instanceha.options;

import java.util.Objects;

/** Body of a segment host create or update; only the fields set are sent. */
public class HostOptions extends MasakariAttributes<HostOptions> {

    /**
     * @param name              the compute host name
     * @param type              e.g. {@code COMPUTE}
     * @param controlAttributes e.g. {@code SSH}
     */
    public static HostOptions create(String name, String type, String controlAttributes) {
        return new HostOptions().put("name", Objects.requireNonNull(name, "name")).put("type", Objects.requireNonNull(type, "type"))
                .put("control_attributes", Objects.requireNonNull(controlAttributes, "controlAttributes"));
    }

    /** An update that sends only the fields set afterwards. */
    public static HostOptions update() {
        return new HostOptions();
    }

    @Override
    protected HostOptions self() {
        return this;
    }

    public HostOptions name(String value) {
        return put("name", value);
    }

    /** A reserved host takes over the instances of a failed host. */
    public HostOptions reserved(Boolean value) {
        return put("reserved", value);
    }

    public HostOptions onMaintenance(Boolean value) {
        return put("on_maintenance", value);
    }
}
