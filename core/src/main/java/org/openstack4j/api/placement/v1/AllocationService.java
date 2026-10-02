package org.openstack4j.api.placement.v1;

import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.AllocationRequest;
import org.openstack4j.model.placement.v1.ConsumerAllocations;
import org.openstack4j.model.placement.v1.ResourceProviderAllocations;

/**
 * Allocations ({@code /allocations}). Writing allocations by hand changes what the scheduler believes is in use;
 * normally Nova owns them.
 */
public interface AllocationService extends RestService {

    /** @return the consumer's allocations; empty (never {@code null}) for an unknown consumer */
    ConsumerAllocations get(String consumerUuid);

    /** Replaces the consumer's allocations. The request's consumer generation must match the server's. */
    void set(String consumerUuid, AllocationRequest request);

    /** Replaces the allocations of several consumers atomically ({@code POST /allocations}). */
    void setMany(Map<String, AllocationRequest> requestsByConsumer);

    ActionResponse delete(String consumerUuid);

    ResourceProviderAllocations listForProvider(String providerUuid);
}
