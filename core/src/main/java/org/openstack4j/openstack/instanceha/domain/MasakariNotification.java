package org.openstack4j.openstack.instanceha.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.instanceha.Notification;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("notification")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MasakariNotification implements Notification {

    private static final long serialVersionUID = 1L;

    @JsonProperty("notification_uuid") private String notificationUuid;
    @JsonProperty("type") private String type;
    @JsonProperty("status") private String status;
    @JsonProperty("source_host_uuid") private String sourceHostUuid;
    @JsonProperty("generated_time") private String generatedTime;
    @JsonProperty("payload") private Map<String, Object> payload;
    @JsonProperty("recovery_workflow_details") private List<Map<String, Object>> recoveryWorkflowDetails;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getNotificationUuid() { return notificationUuid; }
    @Override public String getType() { return type; }
    @Override public String getStatus() { return status; }
    @Override public String getSourceHostUuid() { return sourceHostUuid; }
    @Override public String getGeneratedTime() { return generatedTime; }
    @Override public Map<String, Object> getPayload() { return payload; }
    @Override public List<Map<String, Object>> getRecoveryWorkflowDetails() { return recoveryWorkflowDetails; }
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

    public static class MasakariNotificationList extends ListResult<MasakariNotification> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("notifications")
        private List<MasakariNotification> list;

        @Override
        protected List<MasakariNotification> value() {
            return list;
        }
    }
}
