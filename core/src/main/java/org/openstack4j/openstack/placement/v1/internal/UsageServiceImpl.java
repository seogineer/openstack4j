package org.openstack4j.openstack.placement.v1.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.placement.v1.UsageService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.PlacementMicroVersions;
import org.openstack4j.model.placement.v1.ProjectUsages;
import org.openstack4j.model.placement.v1.ResourceCapacity;
import org.openstack4j.model.placement.v1.ResourceProviderInventories;
import org.openstack4j.model.placement.v1.ResourceProviderUsages;
import org.openstack4j.openstack.placement.v1.domain.PlacementProjectUsages;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceCapacity;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceProviderUsages;

public class UsageServiceImpl extends BasePlacementV1Service implements UsageService {

    @Override
    public ResourceProviderUsages forProvider(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeOrThrow(placement(HttpMethod.GET, PlacementResourceProviderUsages.class, uri("/resource_providers/%s/usages", providerUuid)));
    }

    @Override
    public ProjectUsages forProject(String projectId, String userId, String consumerType) {
        Objects.requireNonNull(projectId, "projectId");
        if (consumerType != null)
            requireMicroVersion("filtering usages by consumer type", PlacementMicroVersions.V1_38);
        return executeOrThrow(placement(HttpMethod.GET, PlacementProjectUsages.class, "/usages")
                .param("project_id", projectId)
                .param("user_id", userId)
                .param("consumer_type", consumerType));
    }

    @Override
    public Map<String, ResourceCapacity> capacity(String providerUuid) {
        ResourceProviderInventories inventories = new InventoryServiceImpl().list(providerUuid);
        Map<String, Long> used = forProvider(providerUuid).getUsages();
        Map<String, ResourceCapacity> result = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends Inventory> entry : inventories.getInventories().entrySet())
            result.put(entry.getKey(), new PlacementResourceCapacity(entry.getKey(), entry.getValue(), used.getOrDefault(entry.getKey(), 0L)));
        return result;
    }
}
