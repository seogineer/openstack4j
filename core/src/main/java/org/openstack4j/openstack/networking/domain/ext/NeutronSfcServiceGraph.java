package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.SfcServiceGraph;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("service_graph")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronSfcServiceGraph implements SfcServiceGraph {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("port_chains") private Map<String, List<String>> portChains;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public Map<String, List<String>> getPortChains() { return portChains; }
    @Override public String getProjectId() { return projectId; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class NeutronSfcServiceGraphList extends ListResult<NeutronSfcServiceGraph> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("service_graphs")
        private List<NeutronSfcServiceGraph> list;

        @Override
        protected List<NeutronSfcServiceGraph> value() {
            return list;
        }
    }
}
