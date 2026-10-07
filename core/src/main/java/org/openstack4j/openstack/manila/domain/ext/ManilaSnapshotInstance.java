package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.SnapshotInstance;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("snapshot_instance")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaSnapshotInstance implements SnapshotInstance {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("snapshot_id") private String snapshotId;
    @JsonProperty("share_id") private String shareId;
    @JsonProperty("share_instance_id") private String shareInstanceId;
    @JsonProperty("status") private String status;
    @JsonProperty("progress") private String progress;
    @JsonProperty("provider_location") private String providerLocation;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getSnapshotId() { return snapshotId; }
    @Override public String getShareId() { return shareId; }
    @Override public String getShareInstanceId() { return shareInstanceId; }
    @Override public String getStatus() { return status; }
    @Override public String getProgress() { return progress; }
    @Override public String getProviderLocation() { return providerLocation; }
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

    public static class ManilaSnapshotInstanceList extends ListResult<ManilaSnapshotInstance> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("snapshot_instances")
        private List<ManilaSnapshotInstance> list;

        @Override
        protected List<ManilaSnapshotInstance> value() {
            return list;
        }
    }
}
