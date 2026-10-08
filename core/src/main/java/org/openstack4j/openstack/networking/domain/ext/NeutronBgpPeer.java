package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.BgpPeer;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("bgp_peer")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronBgpPeer implements BgpPeer {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("peer_ip") private String peerIp;
    @JsonProperty("remote_as") private Integer remoteAs;
    @JsonProperty("auth_type") private String authType;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getPeerIp() { return peerIp; }
    @Override public Integer getRemoteAs() { return remoteAs; }
    @Override public String getAuthType() { return authType; }
    @Override public String getProjectId() { return projectId; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class NeutronBgpPeerList extends ListResult<NeutronBgpPeer> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("bgp_peers")
        private List<NeutronBgpPeer> list;

        @Override
        protected List<NeutronBgpPeer> value() {
            return list;
        }
    }
}
