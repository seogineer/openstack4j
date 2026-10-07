package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ShareGroupSnapshot;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("share_group_snapshot")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaShareGroupSnapshot implements ShareGroupSnapshot {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("status") private String status;
    @JsonProperty("share_group_id") private String shareGroupId;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("members") private List<Map<String, Object>> members;
    @JsonProperty("created_at") private String createdAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getStatus() { return status; }
    @Override public String getShareGroupId() { return shareGroupId; }
    @Override public String getProjectId() { return projectId; }
    @Override public List<Map<String, Object>> getMembers() { return members; }
    @Override public String getCreatedAt() { return createdAt; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class ManilaShareGroupSnapshotList extends ListResult<ManilaShareGroupSnapshot> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("share_group_snapshots")
        private List<ManilaShareGroupSnapshot> list;

        @Override
        protected List<ManilaShareGroupSnapshot> value() {
            return list;
        }
    }
}
