package org.openstack4j.openstack.placement.v1.domain;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.ResourceProviderUsages;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementResourceProviderUsages implements ResourceProviderUsages {

    private static final long serialVersionUID = 1L;

    @JsonProperty("resource_provider_generation")
    private long resourceProviderGeneration;
    @JsonProperty("usages")
    private Map<String, Long> usages;

    @Override
    public long getResourceProviderGeneration() {
        return resourceProviderGeneration;
    }

    @Override
    public Map<String, Long> getUsages() {
        return usages == null ? Collections.emptyMap() : usages;
    }
}
