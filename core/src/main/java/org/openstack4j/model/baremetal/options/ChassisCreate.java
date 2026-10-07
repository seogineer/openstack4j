package org.openstack4j.model.baremetal.options;

import java.util.Map;

/** The body of {@code POST /v1/chassis}. */
public final class ChassisCreate extends BaremetalAttributes<ChassisCreate> {

    private ChassisCreate() {
    }

    public static ChassisCreate create() {
        return new ChassisCreate();
    }

    @Override
    protected ChassisCreate self() {
        return this;
    }

    public ChassisCreate description(String description) {
        return put("description", description);
    }

    public ChassisCreate extra(Map<String, ?> extra) {
        return put("extra", extra);
    }
}
