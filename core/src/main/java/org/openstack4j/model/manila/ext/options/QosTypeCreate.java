package org.openstack4j.model.manila.ext.options;

import java.util.Map;
import java.util.Objects;

/** The body of {@code POST /v2/qos-types} (microversion 2.94). */
public final class QosTypeCreate extends ManilaAttributes<QosTypeCreate> {

    private QosTypeCreate() {
    }

    public static QosTypeCreate create(String name) {
        return new QosTypeCreate().put("name", Objects.requireNonNull(name, "name"));
    }

    @Override
    protected QosTypeCreate self() {
        return this;
    }

    public QosTypeCreate description(String description) {
        return put("description", description);
    }

    /** e.g. {@code peak_iops}, {@code expected_iops}, {@code policy_type}. */
    public QosTypeCreate specs(Map<String, Object> specs) {
        return put("specs", specs);
    }
}
