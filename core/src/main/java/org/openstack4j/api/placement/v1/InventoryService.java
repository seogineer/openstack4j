package org.openstack4j.api.placement.v1;

import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.ResourceProviderInventories;

/**
 * Inventories of a resource provider ({@code /resource_providers/{uuid}/inventories}). Writes take the provider
 * generation read beforehand; a stale generation fails with
 * {@link org.openstack4j.api.placement.v1.exceptions.PlacementConcurrentUpdateException}.
 */
public interface InventoryService extends RestService {

    ResourceProviderInventories list(String providerUuid);

    /** @return the inventory, or {@code null} when the provider has none for the resource class */
    Inventory get(String providerUuid, String resourceClass);

    /** Replaces every inventory of the provider. An empty map removes them all. */
    ResourceProviderInventories replace(String providerUuid, long generation, Map<String, ? extends Inventory> inventories);

    Inventory create(String providerUuid, long generation, String resourceClass, Inventory inventory);

    Inventory update(String providerUuid, long generation, String resourceClass, Inventory inventory);

    /** Fails (409) while allocations exist against the inventory. */
    ActionResponse delete(String providerUuid, String resourceClass);

    ActionResponse deleteAll(String providerUuid);
}
