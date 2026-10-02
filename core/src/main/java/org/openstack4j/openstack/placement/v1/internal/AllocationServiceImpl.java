package org.openstack4j.openstack.placement.v1.internal;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openstack4j.api.placement.v1.AllocationService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.AllocationRequest;
import org.openstack4j.model.placement.v1.ConsumerAllocations;
import org.openstack4j.model.placement.v1.ResourceProviderAllocations;
import org.openstack4j.openstack.placement.v1.domain.PlacementConsumerAllocations;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceProviderAllocations;

public class AllocationServiceImpl extends BasePlacementV1Service implements AllocationService {

    @Override
    public ConsumerAllocations get(String consumerUuid) {
        Objects.requireNonNull(consumerUuid, "consumerUuid");
        return executeOrThrow(placement(HttpMethod.GET, PlacementConsumerAllocations.class, uri("/allocations/%s", consumerUuid)));
    }

    @Override
    public void set(String consumerUuid, AllocationRequest request) {
        Objects.requireNonNull(consumerUuid, "consumerUuid");
        Objects.requireNonNull(request, "request");
        ObjectNode body = ObjectMapperSingleton.getContext(Object.class).createObjectNode();
        AllocationBodies.write(body, request, microVersion());
        executeOrThrow(placement(HttpMethod.PUT, Void.class, uri("/allocations/%s", consumerUuid)).json(body.toString()));
    }

    @Override
    public void setMany(Map<String, AllocationRequest> requestsByConsumer) {
        Objects.requireNonNull(requestsByConsumer, "requestsByConsumer");
        ObjectNode body = ObjectMapperSingleton.getContext(Object.class).createObjectNode();
        for (Map.Entry<String, AllocationRequest> entry : requestsByConsumer.entrySet())
            AllocationBodies.write(body.putObject(entry.getKey()), entry.getValue(), microVersion());
        executeOrThrow(placement(HttpMethod.POST, Void.class, "/allocations").json(body.toString()));
    }

    @Override
    public ActionResponse delete(String consumerUuid) {
        Objects.requireNonNull(consumerUuid, "consumerUuid");
        return executeAction(placement(HttpMethod.DELETE, ActionResponse.class, uri("/allocations/%s", consumerUuid)));
    }

    @Override
    public ResourceProviderAllocations listForProvider(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeOrThrow(placement(HttpMethod.GET, PlacementResourceProviderAllocations.class, uri("/resource_providers/%s/allocations", providerUuid)));
    }
}
