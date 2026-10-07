package org.openstack4j.model.dns.v2.options;

import java.util.Objects;

/** The body of {@code POST /v2/tlds}. */
public final class TldCreate extends DesignateAttributes<TldCreate> {

    private TldCreate() {
    }

    public static TldCreate create(String name) {
        return new TldCreate().put("name", Objects.requireNonNull(name, "name"));
    }

    @Override
    protected TldCreate self() {
        return this;
    }

    public TldCreate description(String description) {
        return put("description", description);
    }
}
