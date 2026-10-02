package org.openstack4j.model.placement.v1;

import org.openstack4j.model.ModelEntity;

/**
 * Inventory of one resource class on a resource provider. Capacity available for allocation is
 * {@code (total - reserved) * allocationRatio}.
 */
public interface Inventory extends ModelEntity {

    long getTotal();

    Long getReserved();

    Long getMinUnit();

    Long getMaxUnit();

    Long getStepSize();

    Float getAllocationRatio();

    /** @return the provider generation after the change, present on responses only */
    Long getResourceProviderGeneration();

    static Builder builder() {
        return new org.openstack4j.openstack.placement.v1.domain.PlacementInventory.Builder();
    }

    interface Builder {
        Builder total(long total);

        Builder reserved(long reserved);

        Builder minUnit(long minUnit);

        Builder maxUnit(long maxUnit);

        Builder stepSize(long stepSize);

        Builder allocationRatio(float allocationRatio);

        Inventory build();
    }
}
