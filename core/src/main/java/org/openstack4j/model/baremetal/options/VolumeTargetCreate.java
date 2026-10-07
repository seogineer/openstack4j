package org.openstack4j.model.baremetal.options;

import java.util.Map;
import java.util.Objects;

/** The body of {@code POST /v1/volume/targets}. */
public final class VolumeTargetCreate extends BaremetalAttributes<VolumeTargetCreate> {

    private VolumeTargetCreate() {
    }

    public static VolumeTargetCreate create(String nodeUuid, String volumeType, Integer bootIndex, String volumeId) {
        return new VolumeTargetCreate().put("node_uuid", Objects.requireNonNull(nodeUuid, "nodeUuid")).put("volume_type", Objects.requireNonNull(volumeType, "volumeType")).put("boot_index", Objects.requireNonNull(bootIndex, "bootIndex")).put("volume_id", Objects.requireNonNull(volumeId, "volumeId"));
    }

    @Override
    protected VolumeTargetCreate self() {
        return this;
    }

    public VolumeTargetCreate uuid(String uuid) {
        return put("uuid", uuid);
    }

    public VolumeTargetCreate properties(Map<String, ?> properties) {
        return put("properties", properties);
    }

    public VolumeTargetCreate extra(Map<String, ?> extra) {
        return put("extra", extra);
    }
}
