package org.openstack4j.model.manila.ext.options;


/** The body of {@code PUT /v2/qos-types/{id}}: only the fields set are changed. */
public final class QosTypeUpdate extends ManilaAttributes<QosTypeUpdate> {

    private QosTypeUpdate() {
    }

    public static QosTypeUpdate create() {
        return new QosTypeUpdate();
    }

    @Override
    protected QosTypeUpdate self() {
        return this;
    }

    public QosTypeUpdate description(String description) {
        return put("description", description);
    }
}
