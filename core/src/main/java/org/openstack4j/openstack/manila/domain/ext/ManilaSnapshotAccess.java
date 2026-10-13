package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.SnapshotAccess;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("snapshot_access")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaSnapshotAccess implements SnapshotAccess {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("access_type") private String accessType;
    @JsonProperty("access_to") private String accessTo;
    @JsonProperty("state") private String state;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getAccessType() { return accessType; }
    @Override public String getAccessTo() { return accessTo; }
    @Override public String getState() { return state; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class ManilaSnapshotAccessList extends ListResult<ManilaSnapshotAccess> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("snapshot_access_list")
        private List<ManilaSnapshotAccess> list;

        @Override
        protected List<ManilaSnapshotAccess> value() {
            return list;
        }
    }
}
