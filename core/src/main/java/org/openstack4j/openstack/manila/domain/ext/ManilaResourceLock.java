package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ResourceLock;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("resource_lock")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaResourceLock implements ResourceLock {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("user_id") private String userId;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("lock_context") private String lockContext;
    @JsonProperty("resource_type") private String resourceType;
    @JsonProperty("resource_id") private String resourceId;
    @JsonProperty("resource_action") private String resourceAction;
    @JsonProperty("lock_reason") private String lockReason;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getUserId() { return userId; }
    @Override public String getProjectId() { return projectId; }
    @Override public String getLockContext() { return lockContext; }
    @Override public String getResourceType() { return resourceType; }
    @Override public String getResourceId() { return resourceId; }
    @Override public String getResourceAction() { return resourceAction; }
    @Override public String getLockReason() { return lockReason; }
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

    public static class ManilaResourceLockList extends ListResult<ManilaResourceLock> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("resource_locks")
        private List<ManilaResourceLock> list;

        @Override
        protected List<ManilaResourceLock> value() {
            return list;
        }
    }
}
