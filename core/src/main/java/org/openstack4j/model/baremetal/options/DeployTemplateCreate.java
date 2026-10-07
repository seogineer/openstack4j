package org.openstack4j.model.baremetal.options;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The body of {@code POST /v1/deploy_templates}. */
public final class DeployTemplateCreate extends BaremetalAttributes<DeployTemplateCreate> {

    private DeployTemplateCreate() {
    }

    public static DeployTemplateCreate create(String name, List<Map<String, Object>> steps) {
        return new DeployTemplateCreate().put("name", Objects.requireNonNull(name, "name")).put("steps", Objects.requireNonNull(steps, "steps"));
    }

    @Override
    protected DeployTemplateCreate self() {
        return this;
    }

    public DeployTemplateCreate uuid(String uuid) {
        return put("uuid", uuid);
    }

    public DeployTemplateCreate extra(Map<String, ?> extra) {
        return put("extra", extra);
    }
}
