package org.openstack4j.openstack.baremetal.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.baremetal.InspectionRule;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IronicInspectionRule implements InspectionRule {

    private static final long serialVersionUID = 1L;

    @JsonProperty("uuid") private String uuid;
    @JsonProperty("description") private String description;
    @JsonProperty("phase") private String phase;
    @JsonProperty("priority") private Integer priority;
    @JsonProperty("sensitive") private Boolean sensitive;
    @JsonProperty("conditions") private List<Map<String, Object>> conditions;
    @JsonProperty("actions") private List<Map<String, Object>> actions;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getUuid() { return uuid; }
    @Override public String getDescription() { return description; }
    @Override public String getPhase() { return phase; }
    @Override public Integer getPriority() { return priority; }
    @Override public Boolean isSensitive() { return sensitive; }
    @Override public List<Map<String, Object>> getConditions() { return conditions; }
    @Override public List<Map<String, Object>> getActions() { return actions; }
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

    public static class IronicInspectionRuleList extends ListResult<IronicInspectionRule> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("inspection_rules")
        private List<IronicInspectionRule> list;

        @Override
        protected List<IronicInspectionRule> value() {
            return list;
        }
    }
}
