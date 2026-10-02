package org.openstack4j.openstack.storage.block.domain;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.common.Link;
import org.openstack4j.model.storage.block.VolumeMessage;
import org.openstack4j.openstack.common.GenericLink;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("message")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderMessage implements VolumeMessage {

    private static final long serialVersionUID = 1L;

    private String id;
    @JsonProperty("event_id") private String eventId;
    @JsonProperty("user_message") private String userMessage;
    @JsonProperty("message_level") private String messageLevel;
    @JsonProperty("resource_type") private String resourceType;
    @JsonProperty("resource_uuid") private String resourceUuid;
    @JsonProperty("request_id") private String requestId;
    @JsonProperty("created_at") private Date createdAt;
    @JsonProperty("guaranteed_until") private Date guaranteedUntil;
    private List<GenericLink> links;

    @Override public String getId() { return id; }
    @Override public String getEventId() { return eventId; }
    @Override public String getUserMessage() { return userMessage; }
    @Override public String getMessageLevel() { return messageLevel; }
    @Override public String getResourceType() { return resourceType; }
    @Override public String getResourceUuid() { return resourceUuid; }
    @Override public String getRequestId() { return requestId; }
    @Override public Date getCreatedAt() { return createdAt; }
    @Override public Date getGuaranteedUntil() { return guaranteedUntil; }
    @Override public List<? extends Link> getLinks() { return links; }

    public static class Messages extends ListResult<CinderMessage> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("messages")
        private List<CinderMessage> messages;

        @Override
        protected List<CinderMessage> value() {
            return messages;
        }
    }
}
