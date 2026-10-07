package org.openstack4j.openstack.dns.v2.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.dns.v2.ext.Pool;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DesignatePool implements Pool {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("attributes") private Map<String, Object> attributesValue;
    @JsonProperty("ns_records") private List<Map<String, Object>> nsRecords;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public Map<String, Object> getPoolAttributes() { return attributesValue; }
    @Override public List<Map<String, Object>> getNsRecords() { return nsRecords; }
    @Override public String getProjectId() { return projectId; }
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

    public static class DesignatePoolList extends ListResult<DesignatePool> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("pools")
        private List<DesignatePool> list;

        @Override
        protected List<DesignatePool> value() {
            return list;
        }
    }
}
