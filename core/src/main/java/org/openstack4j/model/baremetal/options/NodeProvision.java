package org.openstack4j.model.baremetal.options;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The body of {@code PUT /v1/nodes/{node}/states/provision}. */
public final class NodeProvision extends BaremetalAttributes<NodeProvision> {

    private NodeProvision() {
    }

    /**
     * @param target the provision action: {@code manage}, {@code provide}, {@code inspect}, {@code active},
     *               {@code deleted}, {@code rebuild}, {@code clean}, {@code rescue}, {@code unrescue}, {@code adopt},
     *               {@code abort}, {@code service}, {@code unhold} ...
     */
    public static NodeProvision target(String target) {
        return new NodeProvision().put("target", Objects.requireNonNull(target, "target"));
    }

    @Override
    protected NodeProvision self() {
        return this;
    }

    /** A config drive URL or gzipped base64 ISO (microversion 1.56 also takes a JSON object as {@code Map}). */
    public NodeProvision configDrive(Object configDrive) {
        return put("configdrive", configDrive);
    }

    /** Steps of a manual {@code clean} (microversion 1.15). */
    public NodeProvision cleanSteps(List<Map<String, Object>> cleanSteps) {
        return put("clean_steps", cleanSteps);
    }

    /** Steps of {@code active} or {@code rebuild} (microversion 1.69). */
    public NodeProvision deploySteps(List<Map<String, Object>> deploySteps) {
        return put("deploy_steps", deploySteps);
    }

    /** Steps of {@code service} (microversion 1.87). */
    public NodeProvision serviceSteps(List<Map<String, Object>> serviceSteps) {
        return put("service_steps", serviceSteps);
    }

    /** Password of {@code rescue} (microversion 1.38). */
    public NodeProvision rescuePassword(String rescuePassword) {
        return put("rescue_password", rescuePassword);
    }

    /** Skips booting the ramdisk for {@code clean} (microversion 1.70). */
    public NodeProvision disableRamdisk(Boolean disableRamdisk) {
        return put("disable_ramdisk", disableRamdisk);
    }

    /** A runbook name or UUID for {@code clean} or {@code service} (microversion 1.92). */
    public NodeProvision runbook(String runbook) {
        return put("runbook", runbook);
    }
}
