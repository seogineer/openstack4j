package org.openstack4j.model.placement.v1;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** Allocations of one consumer (for example a server) across resource providers. */
public interface ConsumerAllocations extends ModelEntity {

    /** @return allocations keyed by resource provider UUID; empty for an unknown consumer */
    Map<String, ? extends ProviderAllocation> getAllocations();

    String getProjectId();

    String getUserId();

    /** @return the consumer generation to send with the next write, or {@code null} for an unknown consumer */
    Long getConsumerGeneration();

    /** @return the consumer type (placement 1.38), otherwise {@code null} */
    String getConsumerType();

    interface ProviderAllocation {
        /** @return amount per resource class */
        Map<String, Long> getResources();

        /** @return the provider generation at the time of the read */
        Long getGeneration();
    }
}
