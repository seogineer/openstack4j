package org.openstack4j.openstack.dns.v2.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.dns.v2.ext.ZoneExport;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DesignateZoneExport implements ZoneExport {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("zone_id") private String zoneId;
    @JsonProperty("status") private String status;
    @JsonProperty("message") private String message;
    @JsonProperty("location") private String location;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("version") private Integer version;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getZoneId() { return zoneId; }
    @Override public String getStatus() { return status; }
    @Override public String getMessage() { return message; }
    @Override public String getLocation() { return location; }
    @Override public String getProjectId() { return projectId; }
    @Override public Integer getVersion() { return version; }
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

    public static class DesignateZoneExportList extends ListResult<DesignateZoneExport> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("exports")
        private List<DesignateZoneExport> list;

        @Override
        protected List<DesignateZoneExport> value() {
            return list;
        }
    }
}
