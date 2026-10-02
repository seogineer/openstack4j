package org.openstack4j.openstack.placement.v1.internal;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openstack4j.api.placement.v1.ReshaperService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.placement.v1.AllocationRequest;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.PlacementMicroVersions;
import org.openstack4j.model.placement.v1.ReshapeRequest;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.placement.v1.domain.PlacementInventory;

public class ReshaperServiceImpl extends BasePlacementV1Service implements ReshaperService {

    private static final ObjectMapper MAPPER = ObjectMapperSingleton.getContext(Object.class);

    @Override
    public void reshape(ReshapeRequest request) {
        Objects.requireNonNull(request, "request");
        requireMicroVersion("reshaper", PlacementMicroVersions.V1_30);
        MicroVersion version = microVersion();
        ObjectNode body = MAPPER.createObjectNode();
        ObjectNode inventories = body.putObject("inventories");
        for (Map.Entry<String, ReshapeRequest.ProviderInventories> provider : request.getInventories().entrySet()) {
            ObjectNode node = inventories.putObject(provider.getKey());
            node.put("resource_provider_generation", provider.getValue().getGeneration());
            ObjectNode byClass = node.putObject("inventories");
            for (Map.Entry<String, Inventory> inventory : provider.getValue().getInventories().entrySet())
                byClass.set(requireName("resource class", inventory.getKey()), MAPPER.valueToTree(PlacementInventory.from(inventory.getValue())));
        }
        ObjectNode allocations = body.putObject("allocations");
        for (Map.Entry<String, AllocationRequest> consumer : request.getAllocations().entrySet())
            AllocationBodies.write(allocations.putObject(consumer.getKey()), consumer.getValue(), version);
        executeOrThrow(placement(HttpMethod.POST, Void.class, "/reshaper").json(body.toString()));
    }
}
