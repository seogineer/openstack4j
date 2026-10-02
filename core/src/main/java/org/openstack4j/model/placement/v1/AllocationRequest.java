package org.openstack4j.model.placement.v1;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.model.ModelEntity;

/** Body of {@code PUT /allocations/{consumer}} (and one entry of {@code POST /allocations}). */
public final class AllocationRequest implements ModelEntity {

    private static final long serialVersionUID = 1L;

    private final Map<String, Map<String, Long>> allocations;
    private final String projectId;
    private final String userId;
    private final Long consumerGeneration;
    private final String consumerType;

    private AllocationRequest(Builder b) {
        this.allocations = Collections.unmodifiableMap(b.allocations);
        this.projectId = Objects.requireNonNull(b.projectId, "projectId");
        this.userId = Objects.requireNonNull(b.userId, "userId");
        this.consumerGeneration = b.consumerGeneration;
        this.consumerType = b.consumerType;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @return resource provider UUID → (resource class → amount) */
    public Map<String, Map<String, Long>> getAllocations() {
        return allocations;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getUserId() {
        return userId;
    }

    /** @return the generation read before, or {@code null} for a consumer that has no allocations yet */
    public Long getConsumerGeneration() {
        return consumerGeneration;
    }

    /** @return the consumer type (placement 1.38, where it is required), or {@code null} */
    public String getConsumerType() {
        return consumerType;
    }

    public static final class Builder {
        private final Map<String, Map<String, Long>> allocations = new LinkedHashMap<>();
        private String projectId;
        private String userId;
        private Long consumerGeneration;
        private String consumerType;

        public Builder allocation(String providerUuid, String resourceClass, long amount) {
            allocations.computeIfAbsent(providerUuid, k -> new LinkedHashMap<>()).put(resourceClass, amount);
            return this;
        }

        public Builder allocations(String providerUuid, Map<String, Long> resources) {
            allocations.computeIfAbsent(providerUuid, k -> new LinkedHashMap<>()).putAll(resources);
            return this;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder consumerGeneration(Long consumerGeneration) {
            this.consumerGeneration = consumerGeneration;
            return this;
        }

        public Builder consumerType(String consumerType) {
            this.consumerType = consumerType;
            return this;
        }

        public AllocationRequest build() {
            return new AllocationRequest(this);
        }
    }
}
