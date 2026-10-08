package org.openstack4j.model.instanceha.options;

import java.util.Objects;

/** Body of a segment create or update; only the fields set are sent. */
public class SegmentOptions extends MasakariAttributes<SegmentOptions> {

    public static SegmentOptions create(String name, String serviceType, String recoveryMethod) {
        return new SegmentOptions().put("name", Objects.requireNonNull(name, "name")).put("service_type", Objects.requireNonNull(serviceType, "serviceType")).put("recovery_method", Objects.requireNonNull(recoveryMethod, "recoveryMethod"));
    }

    /** An update that sends only the fields set afterwards. */
    public static SegmentOptions update() {
        return new SegmentOptions();
    }

    @Override
    protected SegmentOptions self() {
        return this;
    }

    public SegmentOptions name(String value) {
        return put("name", value);
    }

    public SegmentOptions serviceType(String value) {
        return put("service_type", value);
    }

    public SegmentOptions recoveryMethod(String value) {
        return put("recovery_method", value);
    }

    public SegmentOptions description(String value) {
        return put("description", value);
    }

    /** Needs instance-ha 1.2 (sent for you). */
    public SegmentOptions enabled(Boolean value) {
        return put("enabled", value);
    }
}
