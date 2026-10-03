package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.ServiceProfile;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("service_profile")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronServiceProfile implements ServiceProfile {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("description") private String description;
    @JsonProperty("driver") private String driver;
    @JsonProperty("metainfo") private String metainfo;
    @JsonProperty("enabled") private Boolean enabled;

    @Override public String getId() { return id; }
    @Override public String getDescription() { return description; }
    @Override public String getDriver() { return driver; }
    @Override public String getMetainfo() { return metainfo; }
    @Override public Boolean isEnabled() { return enabled; }

    public static class ServiceProfiles extends ListResult<NeutronServiceProfile> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("service_profiles")
        private List<NeutronServiceProfile> list;

        @Override
        protected List<NeutronServiceProfile> value() {
            return list;
        }
    }
}
