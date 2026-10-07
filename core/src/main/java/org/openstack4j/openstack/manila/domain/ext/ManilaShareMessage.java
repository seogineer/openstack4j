package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ShareMessage;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("message")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaShareMessage implements ShareMessage {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("resource_type") private String resourceType;
    @JsonProperty("resource_id") private String resourceId;
    @JsonProperty("action_id") private String actionId;
    @JsonProperty("message_level") private String messageLevel;
    @JsonProperty("user_message") private String userMessage;
    @JsonProperty("detail_id") private String detailId;
    @JsonProperty("request_id") private String requestId;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("expires_at") private String expiresAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getResourceType() { return resourceType; }
    @Override public String getResourceId() { return resourceId; }
    @Override public String getActionId() { return actionId; }
    @Override public String getMessageLevel() { return messageLevel; }
    @Override public String getUserMessage() { return userMessage; }
    @Override public String getDetailId() { return detailId; }
    @Override public String getRequestId() { return requestId; }
    @Override public String getProjectId() { return projectId; }
    @Override public String getCreatedAt() { return createdAt; }
    @Override public String getExpiresAt() { return expiresAt; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class ManilaShareMessageList extends ListResult<ManilaShareMessage> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("messages")
        private List<ManilaShareMessage> list;

        @Override
        protected List<ManilaShareMessage> value() {
            return list;
        }
    }
}
