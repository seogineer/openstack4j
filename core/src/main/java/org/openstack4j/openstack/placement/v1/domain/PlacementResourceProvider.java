package org.openstack4j.openstack.placement.v1.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.placement.v1.ResourceProvider;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.util.ToStringHelper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementResourceProvider implements ResourceProvider {

    private static final long serialVersionUID = 1L;

    @JsonProperty("uuid")
    private String uuid;
    @JsonProperty("name")
    private String name;
    @JsonProperty("generation")
    private Long generation;
    @JsonProperty("parent_provider_uuid")
    private String parentProviderUuid;
    @JsonProperty("root_provider_uuid")
    private String rootProviderUuid;

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Long getGeneration() {
        return generation;
    }

    @Override
    public String getParentProviderUuid() {
        return parentProviderUuid;
    }

    @Override
    public String getRootProviderUuid() {
        return rootProviderUuid;
    }

    @Override
    public String toString() {
        return new ToStringHelper(this).add("uuid", uuid).add("name", name).add("generation", generation)
                .add("parentProviderUuid", parentProviderUuid).add("rootProviderUuid", rootProviderUuid).toString();
    }

    public static class ResourceProviders extends ListResult<PlacementResourceProvider> {

        private static final long serialVersionUID = 1L;

        @JsonProperty("resource_providers")
        private List<PlacementResourceProvider> resourceProviders;

        @Override
        protected List<PlacementResourceProvider> value() {
            return resourceProviders;
        }
    }
}
