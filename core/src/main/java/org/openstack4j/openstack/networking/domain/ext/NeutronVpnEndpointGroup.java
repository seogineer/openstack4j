package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.VpnEndpointGroup;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("endpoint_group")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronVpnEndpointGroup implements VpnEndpointGroup {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("type") private String type;
    @JsonProperty("endpoints") private List<String> endpoints;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getType() { return type; }
    @Override public List<String> getEndpoints() { return endpoints; }
    @Override public String getProjectId() { return projectId; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class NeutronVpnEndpointGroupList extends ListResult<NeutronVpnEndpointGroup> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("endpoint_groups")
        private List<NeutronVpnEndpointGroup> list;

        @Override
        protected List<NeutronVpnEndpointGroup> value() {
            return list;
        }
    }
}
