package org.openstack4j.openstack.baremetal.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.baremetal.Runbook;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IronicRunbook implements Runbook {

    private static final long serialVersionUID = 1L;

    @JsonProperty("uuid") private String uuid;
    @JsonProperty("name") private String name;
    @JsonProperty("steps") private List<Map<String, Object>> steps;
    @JsonProperty("public") private Boolean publicValue;
    @JsonProperty("owner") private String owner;
    @JsonProperty("extra") private Map<String, Object> extra;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getUuid() { return uuid; }
    @Override public String getName() { return name; }
    @Override public List<Map<String, Object>> getSteps() { return steps; }
    @Override public Boolean isPublic() { return publicValue; }
    @Override public String getOwner() { return owner; }
    @Override public Map<String, Object> getExtra() { return extra; }
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

    public static class IronicRunbookList extends ListResult<IronicRunbook> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("runbooks")
        private List<IronicRunbook> list;

        @Override
        protected List<IronicRunbook> value() {
            return list;
        }
    }
}
