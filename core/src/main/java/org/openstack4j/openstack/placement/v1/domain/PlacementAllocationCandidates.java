package org.openstack4j.openstack.placement.v1.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.AllocationCandidates;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementAllocationCandidates implements AllocationCandidates {

    private static final long serialVersionUID = 1L;

    @JsonProperty("allocation_requests")
    private List<Request> allocationRequests;
    @JsonProperty("provider_summaries")
    private Map<String, Summary> providerSummaries;

    @Override
    public List<? extends Candidate> getAllocationRequests() {
        return allocationRequests == null ? Collections.emptyList() : allocationRequests;
    }

    @Override
    public Map<String, ? extends ProviderSummary> getProviderSummaries() {
        return providerSummaries == null ? Collections.emptyMap() : providerSummaries;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Request implements Candidate, Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("allocations")
        private Map<String, Resources> allocations;
        @JsonProperty("mappings")
        private Map<String, List<String>> mappings;

        @Override
        public Map<String, Map<String, Long>> getAllocations() {
            Map<String, Map<String, Long>> result = new LinkedHashMap<>();
            if (allocations != null)
                allocations.forEach((rp, r) -> result.put(rp, r.resources == null ? Collections.emptyMap() : r.resources));
            return result;
        }

        @Override
        public Map<String, List<String>> getMappings() {
            return mappings == null ? Collections.emptyMap() : mappings;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resources implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("resources")
        Map<String, Long> resources;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Summary implements ProviderSummary, Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("resources")
        private Map<String, Capacity> resources;
        @JsonProperty("traits")
        private List<String> traits;
        @JsonProperty("parent_provider_uuid")
        private String parentProviderUuid;
        @JsonProperty("root_provider_uuid")
        private String rootProviderUuid;

        @Override
        public Map<String, ? extends CapacityUsed> getResources() {
            return resources == null ? Collections.emptyMap() : resources;
        }

        @Override
        public List<String> getTraits() {
            return traits == null ? Collections.emptyList() : traits;
        }

        @Override
        public String getParentProviderUuid() {
            return parentProviderUuid;
        }

        @Override
        public String getRootProviderUuid() {
            return rootProviderUuid;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Capacity implements CapacityUsed, Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("capacity")
        private long capacity;
        @JsonProperty("used")
        private long used;

        @Override
        public long getCapacity() {
            return capacity;
        }

        @Override
        public long getUsed() {
            return used;
        }
    }
}
