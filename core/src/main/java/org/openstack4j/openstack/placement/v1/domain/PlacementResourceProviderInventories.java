package org.openstack4j.openstack.placement.v1.domain;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.ResourceProviderInventories;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementResourceProviderInventories implements ResourceProviderInventories {

    private static final long serialVersionUID = 1L;

    @JsonProperty("resource_provider_generation")
    private long resourceProviderGeneration;
    @JsonProperty("inventories")
    private Map<String, PlacementInventory> inventories;

    public PlacementResourceProviderInventories() {
    }

    public PlacementResourceProviderInventories(long resourceProviderGeneration, Map<String, PlacementInventory> inventories) {
        this.resourceProviderGeneration = resourceProviderGeneration;
        this.inventories = inventories;
    }

    @Override
    public long getResourceProviderGeneration() {
        return resourceProviderGeneration;
    }

    @Override
    public Map<String, ? extends Inventory> getInventories() {
        return inventories;
    }
}
