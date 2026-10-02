package org.openstack4j.model.placement.v1;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** All allocations against one resource provider, keyed by consumer UUID. */
public interface ResourceProviderAllocations extends ModelEntity {

    long getResourceProviderGeneration();

    Map<String, ? extends ConsumerAllocation> getAllocations();

    interface ConsumerAllocation {
        Map<String, Long> getResources();

        Long getConsumerGeneration();
    }
}
