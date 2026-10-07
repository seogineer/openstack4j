package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ShareTransfer;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("transfer")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaShareTransfer implements ShareTransfer {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("resource_type") private String resourceType;
    @JsonProperty("resource_id") private String resourceId;
    @JsonProperty("auth_key") private String authKey;
    @JsonProperty("source_project_id") private String sourceProjectId;
    @JsonProperty("destination_project_id") private String destinationProjectId;
    @JsonProperty("accepted") private Boolean accepted;
    @JsonProperty("expires_at") private String expiresAt;
    @JsonProperty("created_at") private String createdAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getResourceType() { return resourceType; }
    @Override public String getResourceId() { return resourceId; }
    @Override public String getAuthKey() { return authKey; }
    @Override public String getSourceProjectId() { return sourceProjectId; }
    @Override public String getDestinationProjectId() { return destinationProjectId; }
    @Override public Boolean isAccepted() { return accepted; }
    @Override public String getExpiresAt() { return expiresAt; }
    @Override public String getCreatedAt() { return createdAt; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class ManilaShareTransferList extends ListResult<ManilaShareTransfer> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("transfers")
        private List<ManilaShareTransfer> list;

        @Override
        protected List<ManilaShareTransfer> value() {
            return list;
        }
    }
}
