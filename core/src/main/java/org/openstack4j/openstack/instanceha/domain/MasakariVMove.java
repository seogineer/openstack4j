package org.openstack4j.openstack.instanceha.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.instanceha.VMove;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("vmove")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MasakariVMove implements VMove {

    private static final long serialVersionUID = 1L;

    @JsonProperty("uuid") private String uuid;
    @JsonProperty("notification_uuid") private String notificationUuid;
    @JsonProperty("instance_uuid") private String instanceUuid;
    @JsonProperty("instance_name") private String instanceName;
    @JsonProperty("source_host") private String sourceHost;
    @JsonProperty("dest_host") private String destHost;
    @JsonProperty("start_time") private String startTime;
    @JsonProperty("end_time") private String endTime;
    @JsonProperty("type") private String type;
    @JsonProperty("status") private String status;
    @JsonProperty("message") private String message;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getUuid() { return uuid; }
    @Override public String getNotificationUuid() { return notificationUuid; }
    @Override public String getInstanceUuid() { return instanceUuid; }
    @Override public String getInstanceName() { return instanceName; }
    @Override public String getSourceHost() { return sourceHost; }
    @Override public String getDestHost() { return destHost; }
    @Override public String getStartTime() { return startTime; }
    @Override public String getEndTime() { return endTime; }
    @Override public String getType() { return type; }
    @Override public String getStatus() { return status; }
    @Override public String getMessage() { return message; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class MasakariVMoveList extends ListResult<MasakariVMove> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("vmoves")
        private List<MasakariVMove> list;

        @Override
        protected List<MasakariVMove> value() {
            return list;
        }
    }
}
