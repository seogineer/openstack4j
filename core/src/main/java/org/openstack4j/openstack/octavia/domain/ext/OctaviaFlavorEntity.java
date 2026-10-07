package org.openstack4j.openstack.octavia.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.octavia.ext.OctaviaFlavor;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("flavor")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaviaFlavorEntity implements OctaviaFlavor {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("enabled") private Boolean enabled;
    @JsonProperty("flavor_profile_id") private String flavorProfileId;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public Boolean isEnabled() { return enabled; }
    @Override public String getFlavorProfileId() { return flavorProfileId; }

    public static class Flavors extends ListResult<OctaviaFlavorEntity> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("flavors")
        private List<OctaviaFlavorEntity> list;

        @Override
        protected List<OctaviaFlavorEntity> value() {
            return list;
        }
    }
}
