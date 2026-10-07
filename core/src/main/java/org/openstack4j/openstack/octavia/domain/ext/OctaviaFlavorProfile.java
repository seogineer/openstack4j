package org.openstack4j.openstack.octavia.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.octavia.ext.FlavorProfile;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("flavorprofile")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaviaFlavorProfile implements FlavorProfile {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("provider_name") private String providerName;
    @JsonProperty("flavor_data") private String flavorData;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getProviderName() { return providerName; }
    @Override public String getFlavorData() { return flavorData; }

    public static class FlavorProfiles extends ListResult<OctaviaFlavorProfile> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("flavorprofiles")
        private List<OctaviaFlavorProfile> list;

        @Override
        protected List<OctaviaFlavorProfile> value() {
            return list;
        }
    }
}
