package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ShareServerInfo;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("share_server")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaShareServerInfo implements ShareServerInfo {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("host") private String host;
    @JsonProperty("status") private String status;
    @JsonProperty("share_network_id") private String shareNetworkId;
    @JsonProperty("share_network_name") private String shareNetworkName;
    @JsonProperty("identifier") private String identifier;
    @JsonProperty("is_auto_deletable") private Boolean isAutoDeletable;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getHost() { return host; }
    @Override public String getStatus() { return status; }
    @Override public String getShareNetworkId() { return shareNetworkId; }
    @Override public String getShareNetworkName() { return shareNetworkName; }
    @Override public String getIdentifier() { return identifier; }
    @Override public Boolean isAutoDeletable() { return isAutoDeletable; }
    @Override public String getProjectId() { return projectId; }
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

    public static class ManilaShareServerInfoList extends ListResult<ManilaShareServerInfo> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("share_servers")
        private List<ManilaShareServerInfo> list;

        @Override
        protected List<ManilaShareServerInfo> value() {
            return list;
        }
    }
}
