package org.openstack4j.openstack.placement.v1.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openstack4j.api.placement.v1.InventoryService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.ResourceProviderInventories;
import org.openstack4j.openstack.placement.v1.domain.PlacementInventory;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceProviderInventories;

public class InventoryServiceImpl extends BasePlacementV1Service implements InventoryService {

    private static final ObjectMapper MAPPER = ObjectMapperSingleton.getContext(Object.class);

    @Override
    public ResourceProviderInventories list(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeOrThrow(placement(HttpMethod.GET, PlacementResourceProviderInventories.class,
                uri("/resource_providers/%s/inventories", providerUuid)));
    }

    @Override
    public Inventory get(String providerUuid, String resourceClass) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        requireName("resource class", resourceClass);
        return executeOrNull(placement(HttpMethod.GET, PlacementInventory.class,
                uri("/resource_providers/%s/inventories/%s", providerUuid, resourceClass)));
    }

    @Override
    public ResourceProviderInventories replace(String providerUuid, long generation, Map<String, ? extends Inventory> inventories) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        Objects.requireNonNull(inventories, "inventories");
        Map<String, PlacementInventory> body = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends Inventory> entry : inventories.entrySet())
            body.put(requireName("resource class", entry.getKey()), PlacementInventory.from(entry.getValue()));
        return executeOrThrow(placement(HttpMethod.PUT, PlacementResourceProviderInventories.class,
                uri("/resource_providers/%s/inventories", providerUuid))
                .entity(new PlacementResourceProviderInventories(generation, body)));
    }

    @Override
    public Inventory create(String providerUuid, long generation, String resourceClass, Inventory inventory) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        requireName("resource class", resourceClass);
        ObjectNode body = inventoryBody(generation, inventory);
        body.put("resource_class", resourceClass);
        return executeOrThrow(placement(HttpMethod.POST, PlacementInventory.class,
                uri("/resource_providers/%s/inventories", providerUuid)).json(body.toString()));
    }

    @Override
    public Inventory update(String providerUuid, long generation, String resourceClass, Inventory inventory) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        requireName("resource class", resourceClass);
        return executeOrThrow(placement(HttpMethod.PUT, PlacementInventory.class,
                uri("/resource_providers/%s/inventories/%s", providerUuid, resourceClass))
                .json(inventoryBody(generation, inventory).toString()));
    }

    @Override
    public ActionResponse delete(String providerUuid, String resourceClass) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        requireName("resource class", resourceClass);
        return executeAction(placement(HttpMethod.DELETE, ActionResponse.class,
                uri("/resource_providers/%s/inventories/%s", providerUuid, resourceClass)));
    }

    @Override
    public ActionResponse deleteAll(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeAction(placement(HttpMethod.DELETE, ActionResponse.class,
                uri("/resource_providers/%s/inventories", providerUuid)));
    }

    private static ObjectNode inventoryBody(long generation, Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        ObjectNode body = MAPPER.valueToTree(PlacementInventory.from(inventory));
        body.put("resource_provider_generation", generation);
        return body;
    }
}
