package org.openstack4j.openstack.trove.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.trove.ext.Configuration;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("configuration")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TroveConfiguration implements Configuration {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("datastore_name") private String datastoreName;
    @JsonProperty("datastore_version_id") private String datastoreVersionId;
    @JsonProperty("datastore_version_name") private String datastoreVersionName;
    @JsonProperty("datastore_version_number") private String datastoreVersionNumber;
    @JsonProperty("instance_count") private Integer instanceCount;
    @JsonProperty("values") private Map<String, Object> values;
    @JsonProperty("created") private String created;
    @JsonProperty("updated") private String updated;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getDatastoreName() { return datastoreName; }
    @Override public String getDatastoreVersionId() { return datastoreVersionId; }
    @Override public String getDatastoreVersionName() { return datastoreVersionName; }
    @Override public String getDatastoreVersionNumber() { return datastoreVersionNumber; }
    @Override public Integer getInstanceCount() { return instanceCount; }
    @Override public Map<String, Object> getValues() { return values; }
    @Override public String getCreated() { return created; }
    @Override public String getUpdated() { return updated; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class TroveConfigurationList extends ListResult<TroveConfiguration> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("configurations")
        private List<TroveConfiguration> list;

        @Override
        protected List<TroveConfiguration> value() {
            return list;
        }
    }
}
