package org.openstack4j.model.baremetal.options;

import java.util.Map;
import java.util.Objects;

/** The body of {@code POST /v1/volume/connectors}. */
public final class VolumeConnectorCreate extends BaremetalAttributes<VolumeConnectorCreate> {

    private VolumeConnectorCreate() {
    }

    public static VolumeConnectorCreate create(String nodeUuid, String type, String connectorId) {
        return new VolumeConnectorCreate().put("node_uuid", Objects.requireNonNull(nodeUuid, "nodeUuid")).put("type", Objects.requireNonNull(type, "type")).put("connector_id", Objects.requireNonNull(connectorId, "connectorId"));
    }

    @Override
    protected VolumeConnectorCreate self() {
        return this;
    }

    public VolumeConnectorCreate uuid(String uuid) {
        return put("uuid", uuid);
    }

    public VolumeConnectorCreate extra(Map<String, ?> extra) {
        return put("extra", extra);
    }
}
