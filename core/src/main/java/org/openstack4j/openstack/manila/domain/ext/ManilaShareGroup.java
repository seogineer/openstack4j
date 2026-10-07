package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ShareGroup;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("share_group")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaShareGroup implements ShareGroup {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("status") private String status;
    @JsonProperty("availability_zone") private String availabilityZone;
    @JsonProperty("share_group_type_id") private String shareGroupTypeId;
    @JsonProperty("share_types") private List<String> shareTypes;
    @JsonProperty("share_network_id") private String shareNetworkId;
    @JsonProperty("share_server_id") private String shareServerId;
    @JsonProperty("source_share_group_snapshot_id") private String sourceShareGroupSnapshotId;
    @JsonProperty("host") private String host;
    @JsonProperty("consistent_snapshot_support") private String consistentSnapshotSupport;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("created_at") private String createdAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getStatus() { return status; }
    @Override public String getAvailabilityZone() { return availabilityZone; }
    @Override public String getShareGroupTypeId() { return shareGroupTypeId; }
    @Override public List<String> getShareTypes() { return shareTypes; }
    @Override public String getShareNetworkId() { return shareNetworkId; }
    @Override public String getShareServerId() { return shareServerId; }
    @Override public String getSourceShareGroupSnapshotId() { return sourceShareGroupSnapshotId; }
    @Override public String getHost() { return host; }
    @Override public String getConsistentSnapshotSupport() { return consistentSnapshotSupport; }
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

    public static class ManilaShareGroupList extends ListResult<ManilaShareGroup> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("share_groups")
        private List<ManilaShareGroup> list;

        @Override
        protected List<ManilaShareGroup> value() {
            return list;
        }
    }
}
