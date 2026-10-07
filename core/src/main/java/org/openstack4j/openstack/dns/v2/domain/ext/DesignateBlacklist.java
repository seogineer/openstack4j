package org.openstack4j.openstack.dns.v2.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.dns.v2.ext.Blacklist;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DesignateBlacklist implements Blacklist {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("pattern") private String pattern;
    @JsonProperty("description") private String description;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getPattern() { return pattern; }
    @Override public String getDescription() { return description; }
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

    public static class DesignateBlacklistList extends ListResult<DesignateBlacklist> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("blacklists")
        private List<DesignateBlacklist> list;

        @Override
        protected List<DesignateBlacklist> value() {
            return list;
        }
    }
}
