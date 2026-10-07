package org.openstack4j.model.baremetal.options;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The body of {@code POST /v1/runbooks}. */
public final class RunbookCreate extends BaremetalAttributes<RunbookCreate> {

    private RunbookCreate() {
    }

    public static RunbookCreate create(String name, List<Map<String, Object>> steps) {
        return new RunbookCreate().put("name", Objects.requireNonNull(name, "name")).put("steps", Objects.requireNonNull(steps, "steps"));
    }

    @Override
    protected RunbookCreate self() {
        return this;
    }

    public RunbookCreate uuid(String uuid) {
        return put("uuid", uuid);
    }

    /** Whether every project can use the runbook. */
    public RunbookCreate publicRunbook(Boolean publicRunbook) {
        return put("public", publicRunbook);
    }

    public RunbookCreate owner(String owner) {
        return put("owner", owner);
    }

    public RunbookCreate extra(Map<String, ?> extra) {
        return put("extra", extra);
    }
}
