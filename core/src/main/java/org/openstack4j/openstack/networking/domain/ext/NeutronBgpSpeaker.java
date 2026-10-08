package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.BgpSpeaker;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("bgp_speaker")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronBgpSpeaker implements BgpSpeaker {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("local_as") private Long localAs;
    @JsonProperty("ip_version") private Integer ipVersion;
    @JsonProperty("peers") private List<String> peers;
    @JsonProperty("networks") private List<String> networks;
    @JsonProperty("advertise_floating_ip_host_routes") private Boolean advertiseFloatingIpHostRoutes;
    @JsonProperty("advertise_tenant_networks") private Boolean advertiseTenantNetworks;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public Long getLocalAs() { return localAs; }
    @Override public Integer getIpVersion() { return ipVersion; }
    @Override public List<String> getPeers() { return peers; }
    @Override public List<String> getNetworks() { return networks; }
    @Override public Boolean isAdvertiseFloatingIpHostRoutes() { return advertiseFloatingIpHostRoutes; }
    @Override public Boolean isAdvertiseTenantNetworks() { return advertiseTenantNetworks; }
    @Override public String getProjectId() { return projectId; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class NeutronBgpSpeakerList extends ListResult<NeutronBgpSpeaker> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("bgp_speakers")
        private List<NeutronBgpSpeaker> list;

        @Override
        protected List<NeutronBgpSpeaker> value() {
            return list;
        }
    }
}
