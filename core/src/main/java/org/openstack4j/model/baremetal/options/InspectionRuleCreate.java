package org.openstack4j.model.baremetal.options;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The body of {@code POST /v1/inspection_rules}. */
public final class InspectionRuleCreate extends BaremetalAttributes<InspectionRuleCreate> {

    private InspectionRuleCreate() {
    }

    public static InspectionRuleCreate create(List<Map<String, Object>> actions) {
        return new InspectionRuleCreate().put("actions", Objects.requireNonNull(actions, "actions"));
    }

    @Override
    protected InspectionRuleCreate self() {
        return this;
    }

    public InspectionRuleCreate uuid(String uuid) {
        return put("uuid", uuid);
    }

    public InspectionRuleCreate description(String description) {
        return put("description", description);
    }

    /** e.g. {@code main}. */
    public InspectionRuleCreate phase(String phase) {
        return put("phase", phase);
    }

    public InspectionRuleCreate priority(Integer priority) {
        return put("priority", priority);
    }

    /** Hides the rule's details from non-admins. */
    public InspectionRuleCreate sensitive(Boolean sensitive) {
        return put("sensitive", sensitive);
    }

    public InspectionRuleCreate conditions(List<Map<String, Object>> conditions) {
        return put("conditions", conditions);
    }
}
