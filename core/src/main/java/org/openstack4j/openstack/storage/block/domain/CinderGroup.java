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
import org.openstack4j.model.storage.block.VolumeGroup;

@JsonRootName("group")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderGroup implements VolumeGroup {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private String status;
    @JsonProperty("availability_zone") private String availabilityZone;
    @JsonProperty("created_at") private Date createdAt;
    @JsonProperty("group_type") private String groupType;
    @JsonProperty("volume_types") private List<String> volumeTypes;
    private List<String> volumes;
    @JsonProperty("group_snapshot_id") private String groupSnapshotId;
    @JsonProperty("source_group_id") private String sourceGroupId;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("replication_status") private String replicationStatus;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getStatus() { return status; }
    @Override public String getAvailabilityZone() { return availabilityZone; }
    @Override public Date getCreatedAt() { return createdAt; }
    @Override public String getGroupType() { return groupType; }
    @Override public List<String> getVolumeTypes() { return volumeTypes; }
    @Override public List<String> getVolumes() { return volumes; }
    @Override public String getGroupSnapshotId() { return groupSnapshotId; }
    @Override public String getSourceGroupId() { return sourceGroupId; }
    @Override public String getProjectId() { return projectId; }
    @Override public String getReplicationStatus() { return replicationStatus; }

    public static class Groups extends ListResult<CinderGroup> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("groups")
        private List<CinderGroup> items;

        @Override
        protected List<CinderGroup> value() {
            return items;
        }
    }
}
