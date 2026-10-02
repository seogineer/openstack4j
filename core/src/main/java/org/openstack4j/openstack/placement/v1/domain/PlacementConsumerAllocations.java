package org.openstack4j.openstack.placement.v1.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.ConsumerAllocations;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementConsumerAllocations implements ConsumerAllocations {

    private static final long serialVersionUID = 1L;

    @JsonProperty("allocations")
    private Map<String, Allocation> allocations;
    @JsonProperty("project_id")
    private String projectId;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("consumer_generation")
    private Long consumerGeneration;
    @JsonProperty("consumer_type")
    private String consumerType;

    @Override
    public Map<String, ? extends ProviderAllocation> getAllocations() {
        return allocations == null ? Collections.emptyMap() : allocations;
    }

    @Override
    public String getProjectId() {
        return projectId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public Long getConsumerGeneration() {
        return consumerGeneration;
    }

    @Override
    public String getConsumerType() {
        return consumerType;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Allocation implements ProviderAllocation, Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("resources")
        private Map<String, Long> resources;
        @JsonProperty("generation")
        private Long generation;

        @Override
        public Map<String, Long> getResources() {
            return resources == null ? Collections.emptyMap() : resources;
        }

        @Override
        public Long getGeneration() {
            return generation;
        }
    }
}
