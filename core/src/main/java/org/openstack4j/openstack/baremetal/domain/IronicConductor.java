package org.openstack4j.openstack.baremetal.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.baremetal.Conductor;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IronicConductor implements Conductor {

    private static final long serialVersionUID = 1L;

    @JsonProperty("hostname") private String hostname;
    @JsonProperty("conductor_group") private String conductorGroup;
    @JsonProperty("alive") private Boolean alive;
    @JsonProperty("drivers") private List<String> drivers;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getHostname() { return hostname; }
    @Override public String getConductorGroup() { return conductorGroup; }
    @Override public Boolean isAlive() { return alive; }
    @Override public List<String> getDrivers() { return drivers; }
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

    public static class IronicConductorList extends ListResult<IronicConductor> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("conductors")
        private List<IronicConductor> list;

        @Override
        protected List<IronicConductor> value() {
            return list;
        }
    }
}
