package org.openstack4j.openstack.barbican.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.barbican.ext.BarbicanAcl;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BarbicanReadAcl implements BarbicanAcl {

    private static final long serialVersionUID = 1L;

    @JsonProperty("users") private List<String> users;
    @JsonProperty("project-access") private Boolean projectAccess;
    @JsonProperty("created") private String created;
    @JsonProperty("updated") private String updated;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public List<String> getUsers() { return users; }
    @Override public Boolean isProjectAccess() { return projectAccess; }
    @Override public String getCreated() { return created; }
    @Override public String getUpdated() { return updated; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
