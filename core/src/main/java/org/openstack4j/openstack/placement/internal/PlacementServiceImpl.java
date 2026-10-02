package org.openstack4j.openstack.placement.internal;

import org.openstack4j.api.Apis;
import org.openstack4j.api.placement.PlacementService;
import org.openstack4j.api.placement.ext.ResourceProviderService;

/**
 * Placement Operations API implementation
 *
 * @author Jyothi Saroja
 */
public class PlacementServiceImpl extends BasePlacementServices implements PlacementService {

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceProviderService resourceProviders() {
        return Apis.get(ResourceProviderService.class);
    }

    @Override
    public org.openstack4j.api.placement.v1.VersionService versions() {
        return Apis.get(org.openstack4j.api.placement.v1.VersionService.class);
    }

    @Override
    public void useMicroVersion(String version) {
        new org.openstack4j.openstack.placement.v1.internal.VersionServiceImpl().pin(version);
    }

    @Override
    public org.openstack4j.api.placement.v1.ResourceProviderService providers() {
        return Apis.get(org.openstack4j.api.placement.v1.ResourceProviderService.class);
    }

    @Override
    public org.openstack4j.api.placement.v1.InventoryService inventories() {
        return Apis.get(org.openstack4j.api.placement.v1.InventoryService.class);
    }

    @Override
    public org.openstack4j.api.placement.v1.ResourceClassService resourceClasses() {
        return Apis.get(org.openstack4j.api.placement.v1.ResourceClassService.class);
    }

}
