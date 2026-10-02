package org.openstack4j.openstack.placement.v1.internal;

import java.util.Collection;
import java.util.Objects;

import org.openstack4j.api.placement.v1.AggregateService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.placement.v1.ResourceProviderAggregates;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceProviderAggregates;

public class AggregateServiceImpl extends BasePlacementV1Service implements AggregateService {

    @Override
    public ResourceProviderAggregates listForProvider(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeOrThrow(placement(HttpMethod.GET, PlacementResourceProviderAggregates.class,
                uri("/resource_providers/%s/aggregates", providerUuid)));
    }

    @Override
    public ResourceProviderAggregates replaceForProvider(String providerUuid, long generation, Collection<String> aggregateUuids) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        Objects.requireNonNull(aggregateUuids, "aggregateUuids");
        return executeOrThrow(placement(HttpMethod.PUT, PlacementResourceProviderAggregates.class,
                uri("/resource_providers/%s/aggregates", providerUuid))
                .entity(new PlacementResourceProviderAggregates(generation, aggregateUuids)));
    }
}
