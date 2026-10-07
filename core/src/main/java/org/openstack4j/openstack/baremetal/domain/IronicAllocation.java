package org.openstack4j.openstack.baremetal.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.baremetal.Allocation;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IronicAllocation implements Allocation {

    private static final long serialVersionUID = 1L;

    @JsonProperty("uuid") private String uuid;
    @JsonProperty("name") private String name;
    @JsonProperty("node_uuid") private String nodeUuid;
    @JsonProperty("state") private String state;
    @JsonProperty("last_error") private String lastError;
    @JsonProperty("resource_class") private String resourceClass;
    @JsonProperty("candidate_nodes") private List<String> candidateNodes;
    @JsonProperty("traits") private List<String> traits;
    @JsonProperty("extra") private Map<String, Object> extra;
    @JsonProperty("owner") private String owner;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getUuid() { return uuid; }
    @Override public String getName() { return name; }
    @Override public String getNodeUuid() { return nodeUuid; }
    @Override public String getState() { return state; }
    @Override public String getLastError() { return lastError; }
    @Override public String getResourceClass() { return resourceClass; }
    @Override public List<String> getCandidateNodes() { return candidateNodes; }
    @Override public List<String> getTraits() { return traits; }
    @Override public Map<String, Object> getExtra() { return extra; }
    @Override public String getOwner() { return owner; }
    @Override public String getCreatedAt() { return createdAt; }
    @Override public String getUpdatedAt() { return updatedAt; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class IronicAllocationList extends ListResult<IronicAllocation> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("allocations")
        private List<IronicAllocation> list;

        @Override
        protected List<IronicAllocation> value() {
            return list;
        }
    }
}
