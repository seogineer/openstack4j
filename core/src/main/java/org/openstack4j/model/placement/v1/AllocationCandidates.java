package org.openstack4j.model.placement.v1;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** Response of {@code GET /allocation_candidates}. */
public interface AllocationCandidates extends ModelEntity {

    List<? extends Candidate> getAllocationRequests();

    /** @return summaries keyed by provider UUID */
    Map<String, ? extends ProviderSummary> getProviderSummaries();

    interface Candidate {
        /** @return provider UUID → (resource class → amount), ready to be sent as an allocation */
        Map<String, Map<String, Long>> getAllocations();

        /** @return request group suffix → provider UUIDs (placement 1.34), otherwise empty */
        Map<String, List<String>> getMappings();
    }

    interface ProviderSummary {
        Map<String, ? extends CapacityUsed> getResources();

        List<String> getTraits();

        String getParentProviderUuid();

        String getRootProviderUuid();
    }

    interface CapacityUsed {
        long getCapacity();

        long getUsed();
    }
}
