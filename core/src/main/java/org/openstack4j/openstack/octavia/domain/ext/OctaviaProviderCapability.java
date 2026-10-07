package org.openstack4j.openstack.octavia.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.octavia.ext.ProviderCapability;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaviaProviderCapability implements ProviderCapability {

    private static final long serialVersionUID = 1L;

    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;

    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }

    public static class FlavorCapabilities extends ListResult<OctaviaProviderCapability> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("flavor_capabilities")
        private List<OctaviaProviderCapability> list;

        @Override
        protected List<OctaviaProviderCapability> value() {
            return list;
        }
    }

    public static class AvailabilityZoneCapabilities extends ListResult<OctaviaProviderCapability> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("availability_zone_capabilities")
        private List<OctaviaProviderCapability> list;

        @Override
        protected List<OctaviaProviderCapability> value() {
            return list;
        }
    }
}
