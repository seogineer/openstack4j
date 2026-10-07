package org.openstack4j.openstack.octavia.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.octavia.ext.AvailabilityZoneProfile;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("availability_zone_profile")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaviaAvailabilityZoneProfile implements AvailabilityZoneProfile {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("provider_name") private String providerName;
    @JsonProperty("availability_zone_data") private String availabilityZoneData;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getProviderName() { return providerName; }
    @Override public String getAvailabilityZoneData() { return availabilityZoneData; }

    public static class AvailabilityZoneProfiles extends ListResult<OctaviaAvailabilityZoneProfile> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("availability_zone_profiles")
        private List<OctaviaAvailabilityZoneProfile> list;

        @Override
        protected List<OctaviaAvailabilityZoneProfile> value() {
            return list;
        }
    }
}
