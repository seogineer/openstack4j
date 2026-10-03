package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.ServiceFlavor;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("flavor")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronServiceFlavor implements ServiceFlavor {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("service_type") private String serviceType;
    @JsonProperty("enabled") private Boolean enabled;
    @JsonProperty("service_profiles") private List<String> serviceProfiles;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getServiceType() { return serviceType; }
    @Override public Boolean isEnabled() { return enabled; }
    @Override public List<String> getServiceProfiles() { return serviceProfiles; }

    public static class Flavors extends ListResult<NeutronServiceFlavor> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("flavors")
        private List<NeutronServiceFlavor> list;

        @Override
        protected List<NeutronServiceFlavor> value() {
            return list;
        }
    }
}
