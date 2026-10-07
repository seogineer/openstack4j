package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ShareBackup;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("share_backup")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaShareBackup implements ShareBackup {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("share_id") private String shareId;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("status") private String status;
    @JsonProperty("size") private Integer size;
    @JsonProperty("availability_zone") private String availabilityZone;
    @JsonProperty("progress") private String progress;
    @JsonProperty("restore_progress") private String restoreProgress;
    @JsonProperty("backup_type") private String backupType;
    @JsonProperty("host") private String host;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getShareId() { return shareId; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getStatus() { return status; }
    @Override public Integer getSize() { return size; }
    @Override public String getAvailabilityZone() { return availabilityZone; }
    @Override public String getProgress() { return progress; }
    @Override public String getRestoreProgress() { return restoreProgress; }
    @Override public String getBackupType() { return backupType; }
    @Override public String getHost() { return host; }
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

    public static class ManilaShareBackupList extends ListResult<ManilaShareBackup> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("share_backups")
        private List<ManilaShareBackup> list;

        @Override
        protected List<ManilaShareBackup> value() {
            return list;
        }
    }
}
