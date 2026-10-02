package org.openstack4j.model.placement.v1;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** All inventories of a resource provider together with the provider generation they belong to. */
public interface ResourceProviderInventories extends ModelEntity {

    long getResourceProviderGeneration();

    /** @return inventories keyed by resource class name */
    Map<String, ? extends Inventory> getInventories();
}
