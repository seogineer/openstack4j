package org.openstack4j.openstack.trove.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.trove.ext.Backup;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("backup")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TroveBackup implements Backup {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("status") private String status;
    @JsonProperty("instance_id") private String instanceId;
    @JsonProperty("size") private Double size;
    @JsonProperty("parent_id") private String parentId;
    @JsonProperty("locationRef") private String locationRef;
    @JsonProperty("storage_driver") private String storageDriver;
    @JsonProperty("datastore") private Map<String, Object> datastore;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("created") private String created;
    @JsonProperty("updated") private String updated;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getStatus() { return status; }
    @Override public String getInstanceId() { return instanceId; }
    @Override public Double getSize() { return size; }
    @Override public String getParentId() { return parentId; }
    @Override public String getLocationRef() { return locationRef; }
    @Override public String getStorageDriver() { return storageDriver; }
    @Override public Map<String, Object> getDatastore() { return datastore; }
    @Override public String getProjectId() { return projectId; }
    @Override public String getCreated() { return created; }
    @Override public String getUpdated() { return updated; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class TroveBackupList extends ListResult<TroveBackup> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("backups")
        private List<TroveBackup> list;

        @Override
        protected List<TroveBackup> value() {
            return list;
        }
    }
}
