package org.openstack4j.api.placement.v1;

import java.util.Collection;

import org.openstack4j.common.RestService;
import org.openstack4j.model.placement.v1.ResourceProviderAggregates;

/** Aggregate membership of a resource provider ({@code /resource_providers/{uuid}/aggregates}). */
public interface AggregateService extends RestService {

    ResourceProviderAggregates listForProvider(String providerUuid);

    /** Replaces the provider's aggregates. Aggregates are plain UUIDs; Placement does not know Nova aggregates. */
    ResourceProviderAggregates replaceForProvider(String providerUuid, long generation, Collection<String> aggregateUuids);
}
