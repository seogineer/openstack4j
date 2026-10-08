package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.BgpvpnPortAssociation;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("port_association")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronBgpvpnPortAssociation implements BgpvpnPortAssociation {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("port_id") private String portId;
    @JsonProperty("routes") private List<Map<String, Object>> routes;
    @JsonProperty("advertise_fixed_ips") private Boolean advertiseFixedIps;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getPortId() { return portId; }
    @Override public List<Map<String, Object>> getRoutes() { return routes; }
    @Override public Boolean isAdvertiseFixedIps() { return advertiseFixedIps; }
    @Override public String getProjectId() { return projectId; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class NeutronBgpvpnPortAssociationList extends ListResult<NeutronBgpvpnPortAssociation> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("port_associations")
        private List<NeutronBgpvpnPortAssociation> list;

        @Override
        protected List<NeutronBgpvpnPortAssociation> value() {
            return list;
        }
    }
}
