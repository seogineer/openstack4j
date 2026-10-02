package org.openstack4j.openstack.placement.v1.domain;

import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.ResourceCapacity;
import org.openstack4j.util.ToStringHelper;

public class PlacementResourceCapacity implements ResourceCapacity {

    private static final long serialVersionUID = 1L;

    private final String resourceClass;
    private final long total;
    private final long reserved;
    private final float allocationRatio;
    private final long capacity;
    private final long used;

    public PlacementResourceCapacity(String resourceClass, Inventory inventory, long used) {
        this.resourceClass = resourceClass;
        this.total = inventory.getTotal();
        this.reserved = inventory.getReserved() == null ? 0 : inventory.getReserved();
        this.allocationRatio = inventory.getAllocationRatio() == null ? 1.0f : inventory.getAllocationRatio();
        this.capacity = (long) Math.floor((total - reserved) * (double) allocationRatio);
        this.used = used;
    }

    @Override
    public String getResourceClass() {
        return resourceClass;
    }

    @Override
    public long getTotal() {
        return total;
    }

    @Override
    public long getReserved() {
        return reserved;
    }

    @Override
    public float getAllocationRatio() {
        return allocationRatio;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    @Override
    public long getUsed() {
        return used;
    }

    @Override
    public long getFree() {
        return capacity - used;
    }

    @Override
    public String toString() {
        return new ToStringHelper(this).add("resourceClass", resourceClass).add("capacity", capacity).add("used", used)
                .add("free", getFree()).toString();
    }
}
