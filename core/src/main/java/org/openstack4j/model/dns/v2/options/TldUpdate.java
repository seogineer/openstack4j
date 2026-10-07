package org.openstack4j.model.dns.v2.options;


/** The body of {@code PATCH /v2/tlds/{id}}: only the fields set are changed. */
public final class TldUpdate extends DesignateAttributes<TldUpdate> {

    private TldUpdate() {
    }

    public static TldUpdate create() {
        return new TldUpdate();
    }

    @Override
    protected TldUpdate self() {
        return this;
    }

    public TldUpdate name(String name) {
        return put("name", name);
    }

    public TldUpdate description(String description) {
        return put("description", description);
    }
}
