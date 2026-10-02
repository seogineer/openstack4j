package org.openstack4j.model.placement.v1;

import org.openstack4j.model.ModelEntity;

/**
 * Capacity of one resource class on a provider, computed from its inventory and usage:
 * {@code capacity = floor((total - reserved) * allocationRatio)}, {@code free = capacity - used}.
 */
public interface ResourceCapacity extends ModelEntity {

    String getResourceClass();

    long getTotal();

    long getReserved();

    float getAllocationRatio();

    long getCapacity();

    long getUsed();

    long getFree();
}
