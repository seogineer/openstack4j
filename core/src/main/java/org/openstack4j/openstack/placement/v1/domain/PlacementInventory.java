package org.openstack4j.openstack.placement.v1.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.util.ToStringHelper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlacementInventory implements Inventory {

    private static final long serialVersionUID = 1L;

    @JsonProperty("total")
    private long total;
    @JsonProperty("reserved")
    private Long reserved;
    @JsonProperty("min_unit")
    private Long minUnit;
    @JsonProperty("max_unit")
    private Long maxUnit;
    @JsonProperty("step_size")
    private Long stepSize;
    @JsonProperty("allocation_ratio")
    private Float allocationRatio;
    @JsonProperty("resource_provider_generation")
    private Long resourceProviderGeneration;

    @Override
    public long getTotal() {
        return total;
    }

    @Override
    public Long getReserved() {
        return reserved;
    }

    @Override
    public Long getMinUnit() {
        return minUnit;
    }

    @Override
    public Long getMaxUnit() {
        return maxUnit;
    }

    @Override
    public Long getStepSize() {
        return stepSize;
    }

    @Override
    public Float getAllocationRatio() {
        return allocationRatio;
    }

    @Override
    public Long getResourceProviderGeneration() {
        return resourceProviderGeneration;
    }

    /** Copy without the generation, for use inside request bodies. */
    public PlacementInventory withoutGeneration() {
        PlacementInventory copy = new PlacementInventory();
        copy.total = total;
        copy.reserved = reserved;
        copy.minUnit = minUnit;
        copy.maxUnit = maxUnit;
        copy.stepSize = stepSize;
        copy.allocationRatio = allocationRatio;
        return copy;
    }

    public static PlacementInventory from(Inventory inventory) {
        if (inventory instanceof PlacementInventory)
            return ((PlacementInventory) inventory).withoutGeneration();
        PlacementInventory copy = new PlacementInventory();
        copy.total = inventory.getTotal();
        copy.reserved = inventory.getReserved();
        copy.minUnit = inventory.getMinUnit();
        copy.maxUnit = inventory.getMaxUnit();
        copy.stepSize = inventory.getStepSize();
        copy.allocationRatio = inventory.getAllocationRatio();
        return copy;
    }

    @Override
    public String toString() {
        return new ToStringHelper(this).add("total", total).add("reserved", reserved).add("minUnit", minUnit)
                .add("maxUnit", maxUnit).add("stepSize", stepSize).add("allocationRatio", allocationRatio)
                .add("resourceProviderGeneration", resourceProviderGeneration).toString();
    }

    public static class Builder implements Inventory.Builder {
        private final PlacementInventory inventory = new PlacementInventory();

        @Override
        public Builder total(long total) {
            inventory.total = total;
            return this;
        }

        @Override
        public Builder reserved(long reserved) {
            inventory.reserved = reserved;
            return this;
        }

        @Override
        public Builder minUnit(long minUnit) {
            inventory.minUnit = minUnit;
            return this;
        }

        @Override
        public Builder maxUnit(long maxUnit) {
            inventory.maxUnit = maxUnit;
            return this;
        }

        @Override
        public Builder stepSize(long stepSize) {
            inventory.stepSize = stepSize;
            return this;
        }

        @Override
        public Builder allocationRatio(float allocationRatio) {
            inventory.allocationRatio = allocationRatio;
            return this;
        }

        @Override
        public Inventory build() {
            return inventory;
        }
    }
}
