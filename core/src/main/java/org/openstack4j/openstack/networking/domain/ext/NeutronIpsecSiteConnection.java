package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.IpsecSiteConnection;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("ipsec_site_connection")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronIpsecSiteConnection implements IpsecSiteConnection {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("status") private String status;
    @JsonProperty("vpnservice_id") private String vpnserviceId;
    @JsonProperty("ikepolicy_id") private String ikepolicyId;
    @JsonProperty("ipsecpolicy_id") private String ipsecpolicyId;
    @JsonProperty("local_ep_group_id") private String localEpGroupId;
    @JsonProperty("peer_ep_group_id") private String peerEpGroupId;
    @JsonProperty("peer_address") private String peerAddress;
    @JsonProperty("peer_id") private String peerId;
    @JsonProperty("peer_cidrs") private List<String> peerCidrs;
    @JsonProperty("local_id") private String localId;
    @JsonProperty("psk") private String psk;
    @JsonProperty("initiator") private String initiator;
    @JsonProperty("auth_mode") private String authMode;
    @JsonProperty("route_mode") private String routeMode;
    @JsonProperty("mtu") private Integer mtu;
    @JsonProperty("dpd") private Map<String, Object> dpd;
    @JsonProperty("admin_state_up") private Boolean adminStateUp;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getStatus() { return status; }
    @Override public String getVpnserviceId() { return vpnserviceId; }
    @Override public String getIkepolicyId() { return ikepolicyId; }
    @Override public String getIpsecpolicyId() { return ipsecpolicyId; }
    @Override public String getLocalEpGroupId() { return localEpGroupId; }
    @Override public String getPeerEpGroupId() { return peerEpGroupId; }
    @Override public String getPeerAddress() { return peerAddress; }
    @Override public String getPeerId() { return peerId; }
    @Override public List<String> getPeerCidrs() { return peerCidrs; }
    @Override public String getLocalId() { return localId; }
    @Override public String getPsk() { return psk; }
    @Override public String getInitiator() { return initiator; }
    @Override public String getAuthMode() { return authMode; }
    @Override public String getRouteMode() { return routeMode; }
    @Override public Integer getMtu() { return mtu; }
    @Override public Map<String, Object> getDpd() { return dpd; }
    @Override public Boolean isAdminStateUp() { return adminStateUp; }
    @Override public String getProjectId() { return projectId; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class NeutronIpsecSiteConnectionList extends ListResult<NeutronIpsecSiteConnection> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("ipsec_site_connections")
        private List<NeutronIpsecSiteConnection> list;

        @Override
        protected List<NeutronIpsecSiteConnection> value() {
            return list;
        }
    }
}
