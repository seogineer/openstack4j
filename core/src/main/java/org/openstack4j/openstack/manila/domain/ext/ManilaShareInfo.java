package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ShareInfo;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("share")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaShareInfo implements ShareInfo {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("status") private String status;
    @JsonProperty("size") private Integer size;
    @JsonProperty("share_proto") private String shareProto;
    @JsonProperty("share_type") private String shareType;
    @JsonProperty("share_type_name") private String shareTypeName;
    @JsonProperty("availability_zone") private String availabilityZone;
    @JsonProperty("host") private String host;
    @JsonProperty("share_network_id") private String shareNetworkId;
    @JsonProperty("share_server_id") private String shareServerId;
    @JsonProperty("share_group_id") private String shareGroupId;
    @JsonProperty("snapshot_id") private String snapshotId;
    @JsonProperty("is_public") private Boolean isPublic;
    @JsonProperty("metadata") private Map<String, Object> metadata;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("created_at") private String createdAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getStatus() { return status; }
    @Override public Integer getSize() { return size; }
    @Override public String getShareProto() { return shareProto; }
    @Override public String getShareType() { return shareType; }
    @Override public String getShareTypeName() { return shareTypeName; }
    @Override public String getAvailabilityZone() { return availabilityZone; }
    @Override public String getHost() { return host; }
    @Override public String getShareNetworkId() { return shareNetworkId; }
    @Override public String getShareServerId() { return shareServerId; }
    @Override public String getShareGroupId() { return shareGroupId; }
    @Override public String getSnapshotId() { return snapshotId; }
    @Override public Boolean isPublic() { return isPublic; }
    @Override public Map<String, Object> getMetadata() { return metadata; }
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

    public static class ManilaShareInfoList extends ListResult<ManilaShareInfo> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("shares")
        private List<ManilaShareInfo> list;

        @Override
        protected List<ManilaShareInfo> value() {
            return list;
        }
    }
}
