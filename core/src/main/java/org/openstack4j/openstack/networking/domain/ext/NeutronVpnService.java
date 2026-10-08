package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.VpnService;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("vpnservice")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronVpnService implements VpnService {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("router_id") private String routerId;
    @JsonProperty("subnet_id") private String subnetId;
    @JsonProperty("flavor_id") private String flavorId;
    @JsonProperty("status") private String status;
    @JsonProperty("admin_state_up") private Boolean adminStateUp;
    @JsonProperty("external_v4_ip") private String externalV4Ip;
    @JsonProperty("external_v6_ip") private String externalV6Ip;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getRouterId() { return routerId; }
    @Override public String getSubnetId() { return subnetId; }
    @Override public String getFlavorId() { return flavorId; }
    @Override public String getStatus() { return status; }
    @Override public Boolean isAdminStateUp() { return adminStateUp; }
    @Override public String getExternalV4Ip() { return externalV4Ip; }
    @Override public String getExternalV6Ip() { return externalV6Ip; }
    @Override public String getProjectId() { return projectId; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class NeutronVpnServiceList extends ListResult<NeutronVpnService> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("vpnservices")
        private List<NeutronVpnService> list;

        @Override
        protected List<NeutronVpnService> value() {
            return list;
        }
    }
}
