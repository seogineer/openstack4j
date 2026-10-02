package org.openstack4j.openstack.placement.v1.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.ResourceProviderAllocations;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementResourceProviderAllocations implements ResourceProviderAllocations {

    private static final long serialVersionUID = 1L;

    @JsonProperty("resource_provider_generation")
    private long resourceProviderGeneration;
    @JsonProperty("allocations")
    private Map<String, Allocation> allocations;

    @Override
    public long getResourceProviderGeneration() {
        return resourceProviderGeneration;
    }

    @Override
    public Map<String, ? extends ConsumerAllocation> getAllocations() {
        return allocations == null ? Collections.emptyMap() : allocations;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Allocation implements ConsumerAllocation, Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("resources")
        private Map<String, Long> resources;
        @JsonProperty("consumer_generation")
        private Long consumerGeneration;

        @Override
        public Map<String, Long> getResources() {
            return resources == null ? Collections.emptyMap() : resources;
        }

        @Override
        public Long getConsumerGeneration() {
            return consumerGeneration;
        }
    }
}
