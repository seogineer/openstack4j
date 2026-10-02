package org.openstack4j.openstack.storage.block.domain;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.model.storage.block.VolumeGroupSnapshot;

@JsonRootName("group_snapshot")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderGroupSnapshot implements VolumeGroupSnapshot {

    private static final long serialVersionUID = 1L;

    private String id;
    @JsonProperty("group_id") private String groupId;
    private String status;
    @JsonProperty("created_at") private Date createdAt;
    private String name;
    private String description;
    @JsonProperty("group_type_id") private String groupTypeId;
    @JsonProperty("project_id") private String projectId;

    @Override public String getId() { return id; }
    @Override public String getGroupId() { return groupId; }
    @Override public String getStatus() { return status; }
    @Override public Date getCreatedAt() { return createdAt; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getGroupTypeId() { return groupTypeId; }
    @Override public String getProjectId() { return projectId; }

    public static class GroupSnapshots extends ListResult<CinderGroupSnapshot> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("group_snapshots")
        private List<CinderGroupSnapshot> items;

        @Override
        protected List<CinderGroupSnapshot> value() {
            return items;
        }
    }
}
