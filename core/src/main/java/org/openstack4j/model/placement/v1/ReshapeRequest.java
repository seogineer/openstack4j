package org.openstack4j.model.placement.v1;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** Body of {@code POST /reshaper}: new inventories for several providers and the allocations that move with them. */
public final class ReshapeRequest implements ModelEntity {

    private static final long serialVersionUID = 1L;

    public static final class ProviderInventories implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final long generation;
        private final Map<String, Inventory> inventories;

        ProviderInventories(long generation, Map<String, ? extends Inventory> inventories) {
            this.generation = generation;
            this.inventories = Collections.unmodifiableMap(new LinkedHashMap<>(inventories));
        }

        public long getGeneration() {
            return generation;
        }

        public Map<String, Inventory> getInventories() {
            return inventories;
        }
    }

    private final Map<String, ProviderInventories> inventories;
    private final Map<String, AllocationRequest> allocations;

    private ReshapeRequest(Builder b) {
        this.inventories = Collections.unmodifiableMap(new LinkedHashMap<>(b.inventories));
        this.allocations = Collections.unmodifiableMap(new LinkedHashMap<>(b.allocations));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, ProviderInventories> getInventories() {
        return inventories;
    }

    public Map<String, AllocationRequest> getAllocations() {
        return allocations;
    }

    public static final class Builder {
        private final Map<String, ProviderInventories> inventories = new LinkedHashMap<>();
        private final Map<String, AllocationRequest> allocations = new LinkedHashMap<>();

        /** The complete new inventories of a provider, with its current generation. */
        public Builder inventories(String providerUuid, long generation, Map<String, ? extends Inventory> providerInventories) {
            inventories.put(providerUuid, new ProviderInventories(generation, providerInventories));
            return this;
        }

        public Builder allocation(String consumerUuid, AllocationRequest request) {
            allocations.put(consumerUuid, request);
            return this;
        }

        public ReshapeRequest build() {
            if (inventories.isEmpty())
                throw new IllegalArgumentException("a reshape needs inventories for at least one provider");
            return new ReshapeRequest(this);
        }
    }
}
