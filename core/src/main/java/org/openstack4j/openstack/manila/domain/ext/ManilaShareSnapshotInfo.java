package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ShareSnapshotInfo;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("snapshot")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaShareSnapshotInfo implements ShareSnapshotInfo {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("status") private String status;
    @JsonProperty("share_id") private String shareId;
    @JsonProperty("size") private Integer size;
    @JsonProperty("share_size") private Integer shareSize;
    @JsonProperty("share_proto") private String shareProto;
    @JsonProperty("provider_location") private String providerLocation;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("created_at") private String createdAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getStatus() { return status; }
    @Override public String getShareId() { return shareId; }
    @Override public Integer getSize() { return size; }
    @Override public Integer getShareSize() { return shareSize; }
    @Override public String getShareProto() { return shareProto; }
    @Override public String getProviderLocation() { return providerLocation; }
    @Override public String getProjectId() { return projectId; }
    @Override public String getCreatedAt() { return createdAt; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class ManilaShareSnapshotInfoList extends ListResult<ManilaShareSnapshotInfo> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("snapshots")
        private List<ManilaShareSnapshotInfo> list;

        @Override
        protected List<ManilaShareSnapshotInfo> value() {
            return list;
        }
    }
}
