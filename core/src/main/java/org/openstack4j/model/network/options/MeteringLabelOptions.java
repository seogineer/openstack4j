package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a metering label create. */
public class MeteringLabelOptions extends NeutronAttributes<MeteringLabelOptions> {

    public static MeteringLabelOptions create(String name) {
        return new MeteringLabelOptions().put("name", Objects.requireNonNull(name));
    }

    @Override
    protected MeteringLabelOptions self() {
        return this;
    }

    public MeteringLabelOptions description(String value) { return put("description", value); }
    public MeteringLabelOptions shared(Boolean value) { return put("shared", value); }
    public MeteringLabelOptions projectId(String value) { return put("project_id", value); }
}
