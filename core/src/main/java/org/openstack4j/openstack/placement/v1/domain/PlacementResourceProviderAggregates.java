package org.openstack4j.openstack.placement.v1.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.ResourceProviderAggregates;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementResourceProviderAggregates implements ResourceProviderAggregates {

    private static final long serialVersionUID = 1L;

    @JsonProperty("resource_provider_generation")
    private long resourceProviderGeneration;
    @JsonProperty("aggregates")
    private List<String> aggregates;

    public PlacementResourceProviderAggregates() {
    }

    public PlacementResourceProviderAggregates(long resourceProviderGeneration, Collection<String> aggregates) {
        this.resourceProviderGeneration = resourceProviderGeneration;
        this.aggregates = new ArrayList<>(aggregates);
    }

    @Override
    public long getResourceProviderGeneration() {
        return resourceProviderGeneration;
    }

    @Override
    public List<String> getAggregates() {
        return aggregates == null ? Collections.emptyList() : aggregates;
    }
}
