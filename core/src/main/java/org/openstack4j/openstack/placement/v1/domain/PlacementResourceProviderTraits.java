package org.openstack4j.openstack.placement.v1.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.ResourceProviderTraits;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementResourceProviderTraits implements ResourceProviderTraits {

    private static final long serialVersionUID = 1L;

    @JsonProperty("resource_provider_generation")
    private long resourceProviderGeneration;
    @JsonProperty("traits")
    private List<String> traits;

    public PlacementResourceProviderTraits() {
    }

    public PlacementResourceProviderTraits(long resourceProviderGeneration, Collection<String> traits) {
        this.resourceProviderGeneration = resourceProviderGeneration;
        this.traits = new ArrayList<>(traits);
    }

    @Override
    public long getResourceProviderGeneration() {
        return resourceProviderGeneration;
    }

    @Override
    public List<String> getTraits() {
        return traits == null ? Collections.emptyList() : traits;
    }
}
