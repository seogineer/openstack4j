package org.openstack4j.openstack.placement.v1.internal;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.placement.v1.AllocationRequest;
import org.openstack4j.model.placement.v1.PlacementMicroVersions;
import org.openstack4j.openstack.internal.MicroVersion;

/** Serialises {@link AllocationRequest} the way Placement 1.28+ expects, applying the consumer type rules of 1.38. */
final class AllocationBodies {

    private AllocationBodies() {
    }

    static void write(ObjectNode target, AllocationRequest request, MicroVersion version) {
        boolean typeSupported = version.compareTo(PlacementMicroVersions.V1_38) >= 0;
        if (typeSupported && request.getConsumerType() == null)
            throw new IllegalArgumentException(
                    "A consumer type is required for allocations from placement " + PlacementMicroVersions.V1_38
                            + " (negotiated " + version + "); set AllocationRequest.consumerType, for example \"INSTANCE\"");
        if (!typeSupported && request.getConsumerType() != null)
            throw new PlacementMicroVersionException("Consumer types require placement microversion "
                    + PlacementMicroVersions.V1_38 + ", but the negotiated version is " + version);

        ObjectNode allocations = target.putObject("allocations");
        for (Map.Entry<String, Map<String, Long>> provider : request.getAllocations().entrySet()) {
            ObjectNode resources = allocations.putObject(provider.getKey()).putObject("resources");
            provider.getValue().forEach(resources::put);
        }
        target.put("project_id", request.getProjectId());
        target.put("user_id", request.getUserId());
        if (request.getConsumerGeneration() == null) target.putNull("consumer_generation");
        else target.put("consumer_generation", request.getConsumerGeneration());
        if (request.getConsumerType() != null) target.put("consumer_type", request.getConsumerType());
    }
}
