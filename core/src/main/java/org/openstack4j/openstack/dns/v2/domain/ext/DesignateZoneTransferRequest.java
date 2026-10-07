package org.openstack4j.openstack.dns.v2.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.dns.v2.ext.ZoneTransferRequest;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DesignateZoneTransferRequest implements ZoneTransferRequest {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("key") private String key;
    @JsonProperty("zone_id") private String zoneId;
    @JsonProperty("zone_name") private String zoneName;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("target_project_id") private String targetProjectId;
    @JsonProperty("description") private String description;
    @JsonProperty("status") private String status;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getKey() { return key; }
    @Override public String getZoneId() { return zoneId; }
    @Override public String getZoneName() { return zoneName; }
    @Override public String getProjectId() { return projectId; }
    @Override public String getTargetProjectId() { return targetProjectId; }
    @Override public String getDescription() { return description; }
    @Override public String getStatus() { return status; }
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

    public static class DesignateZoneTransferRequestList extends ListResult<DesignateZoneTransferRequest> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("transfer_requests")
        private List<DesignateZoneTransferRequest> list;

        @Override
        protected List<DesignateZoneTransferRequest> value() {
            return list;
        }
    }
}
