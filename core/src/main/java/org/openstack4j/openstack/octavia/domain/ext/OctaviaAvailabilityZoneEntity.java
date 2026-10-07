package org.openstack4j.openstack.octavia.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.octavia.ext.OctaviaAvailabilityZone;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("availability_zone")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaviaAvailabilityZoneEntity implements OctaviaAvailabilityZone {

    private static final long serialVersionUID = 1L;

    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("enabled") private Boolean enabled;
    @JsonProperty("availability_zone_profile_id") private String availabilityZoneProfileId;

    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public Boolean isEnabled() { return enabled; }
    @Override public String getAvailabilityZoneProfileId() { return availabilityZoneProfileId; }

    public static class AvailabilityZones extends ListResult<OctaviaAvailabilityZoneEntity> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("availability_zones")
        private List<OctaviaAvailabilityZoneEntity> list;

        @Override
        protected List<OctaviaAvailabilityZoneEntity> value() {
            return list;
        }
    }
}
