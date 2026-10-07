package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ShareAccessRule;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("access")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaShareAccessRule implements ShareAccessRule {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("share_id") private String shareId;
    @JsonProperty("access_type") private String accessType;
    @JsonProperty("access_to") private String accessTo;
    @JsonProperty("access_level") private String accessLevel;
    @JsonProperty("access_key") private String accessKey;
    @JsonProperty("state") private String state;
    @JsonProperty("metadata") private Map<String, Object> metadata;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getShareId() { return shareId; }
    @Override public String getAccessType() { return accessType; }
    @Override public String getAccessTo() { return accessTo; }
    @Override public String getAccessLevel() { return accessLevel; }
    @Override public String getAccessKey() { return accessKey; }
    @Override public String getState() { return state; }
    @Override public Map<String, Object> getMetadata() { return metadata; }
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

    public static class ManilaShareAccessRuleList extends ListResult<ManilaShareAccessRule> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("access_list")
        private List<ManilaShareAccessRule> list;

        @Override
        protected List<ManilaShareAccessRule> value() {
            return list;
        }
    }
}
