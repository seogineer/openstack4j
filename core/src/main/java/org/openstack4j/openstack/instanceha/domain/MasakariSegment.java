package org.openstack4j.openstack.instanceha.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.instanceha.Segment;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("segment")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MasakariSegment implements Segment {

    private static final long serialVersionUID = 1L;

    @JsonProperty("uuid") private String uuid;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("service_type") private String serviceType;
    @JsonProperty("recovery_method") private String recoveryMethod;
    @JsonProperty("enabled") private Boolean enabled;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getUuid() { return uuid; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getServiceType() { return serviceType; }
    @Override public String getRecoveryMethod() { return recoveryMethod; }
    @Override public Boolean isEnabled() { return enabled; }
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

    public static class MasakariSegmentList extends ListResult<MasakariSegment> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("segments")
        private List<MasakariSegment> list;

        @Override
        protected List<MasakariSegment> value() {
            return list;
        }
    }
}
