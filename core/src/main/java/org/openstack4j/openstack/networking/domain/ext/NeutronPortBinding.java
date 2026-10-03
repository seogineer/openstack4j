package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.PortBinding;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("binding")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronPortBinding implements PortBinding {

    private static final long serialVersionUID = 1L;

    @JsonProperty("host") private String host;
    @JsonProperty("status") private String status;
    @JsonProperty("vif_type") private String vifType;
    @JsonProperty("vnic_type") private String vnicType;
    @JsonProperty("profile") private Map<String, Object> profile;
    @JsonProperty("vif_details") private Map<String, Object> vifDetails;

    @Override public String getHost() { return host; }
    @Override public String getStatus() { return status; }
    @Override public String getVifType() { return vifType; }
    @Override public String getVnicType() { return vnicType; }
    @Override public Map<String, Object> getProfile() { return profile; }
    @Override public Map<String, Object> getVifDetails() { return vifDetails; }

    public static class PortBindings extends ListResult<NeutronPortBinding> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("bindings")
        private List<NeutronPortBinding> list;

        @Override
        protected List<NeutronPortBinding> value() {
            return list;
        }
    }
}
