package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.BgpvpnNetworkAssociation;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("network_association")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronBgpvpnNetworkAssociation implements BgpvpnNetworkAssociation {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("network_id") private String networkId;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getNetworkId() { return networkId; }
    @Override public String getProjectId() { return projectId; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class NeutronBgpvpnNetworkAssociationList extends ListResult<NeutronBgpvpnNetworkAssociation> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("network_associations")
        private List<NeutronBgpvpnNetworkAssociation> list;

        @Override
        protected List<NeutronBgpvpnNetworkAssociation> value() {
            return list;
        }
    }
}
