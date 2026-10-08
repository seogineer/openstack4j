package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.TapFlow;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("tap_flow")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronTapFlow implements TapFlow {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("tap_service_id") private String tapServiceId;
    @JsonProperty("source_port") private String sourcePort;
    @JsonProperty("direction") private String direction;
    @JsonProperty("vlan_filter") private String vlanFilter;
    @JsonProperty("status") private String status;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getTapServiceId() { return tapServiceId; }
    @Override public String getSourcePort() { return sourcePort; }
    @Override public String getDirection() { return direction; }
    @Override public String getVlanFilter() { return vlanFilter; }
    @Override public String getStatus() { return status; }
    @Override public String getProjectId() { return projectId; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class NeutronTapFlowList extends ListResult<NeutronTapFlow> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("tap_flows")
        private List<NeutronTapFlow> list;

        @Override
        protected List<NeutronTapFlow> value() {
            return list;
        }
    }
}
