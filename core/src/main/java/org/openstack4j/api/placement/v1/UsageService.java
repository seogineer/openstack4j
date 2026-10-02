package org.openstack4j.api.placement.v1;

import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.placement.v1.ProjectUsages;
import org.openstack4j.model.placement.v1.ResourceCapacity;
import org.openstack4j.model.placement.v1.ResourceProviderUsages;

public interface UsageService extends RestService {

    ResourceProviderUsages forProvider(String providerUuid);

    /**
     * @param userId optional user filter
     * @param consumerType optional consumer type filter (placement 1.38)
     */
    ProjectUsages forProject(String projectId, String userId, String consumerType);

    /**
     * Capacity per resource class of a provider, from its inventories and usages (two requests; a change between
     * them is not reflected).
     */
    Map<String, ResourceCapacity> capacity(String providerUuid);
}
