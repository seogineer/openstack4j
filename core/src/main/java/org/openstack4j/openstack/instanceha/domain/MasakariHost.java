package org.openstack4j.openstack.instanceha.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.instanceha.Host;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("host")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MasakariHost implements Host {

    private static final long serialVersionUID = 1L;

    @JsonProperty("uuid") private String uuid;
    @JsonProperty("name") private String name;
    @JsonProperty("type") private String type;
    @JsonProperty("control_attributes") private String controlAttributes;
    @JsonProperty("reserved") private Boolean reserved;
    @JsonProperty("on_maintenance") private Boolean onMaintenance;
    @JsonProperty("failover_segment_id") private String failoverSegmentId;
    @JsonProperty("failover_segment") private Map<String, Object> failoverSegment;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getUuid() { return uuid; }
    @Override public String getName() { return name; }
    @Override public String getType() { return type; }
    @Override public String getControlAttributes() { return controlAttributes; }
    @Override public Boolean isReserved() { return reserved; }
    @Override public Boolean isOnMaintenance() { return onMaintenance; }
    @Override public String getFailoverSegmentId() { return failoverSegmentId; }
    @Override public Map<String, Object> getFailoverSegment() { return failoverSegment; }
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

    public static class MasakariHostList extends ListResult<MasakariHost> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("hosts")
        private List<MasakariHost> list;

        @Override
        protected List<MasakariHost> value() {
            return list;
        }
    }
}
