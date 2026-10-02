package org.openstack4j.openstack.storage.block.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.storage.block.VolumeAttachmentRecord;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("attachment")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderAttachment implements VolumeAttachmentRecord {

    private static final long serialVersionUID = 1L;

    private String id;
    private String status;
    private String instance;
    @JsonProperty("volume_id") private String volumeId;
    @JsonProperty("attached_at") private Date attachedAt;
    @JsonProperty("detached_at") private Date detachedAt;
    @JsonProperty("attach_mode") private String attachMode;
    @JsonProperty("connection_info") private Map<String, Object> connectionInfo;

    @Override public String getId() { return id; }
    @Override public String getStatus() { return status; }
    @Override public String getInstance() { return instance; }
    @Override public String getVolumeId() { return volumeId; }
    @Override public Date getAttachedAt() { return attachedAt; }
    @Override public Date getDetachedAt() { return detachedAt; }
    @Override public String getAttachMode() { return attachMode; }
    @Override public Map<String, Object> getConnectionInfo() { return connectionInfo; }

    public static class Attachments extends ListResult<CinderAttachment> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("attachments")
        private List<CinderAttachment> attachments;

        @Override
        protected List<CinderAttachment> value() {
            return attachments;
        }
    }
}
