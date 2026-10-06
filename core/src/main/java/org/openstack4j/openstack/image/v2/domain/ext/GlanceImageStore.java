package org.openstack4j.openstack.image.v2.domain.ext;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.image.v2.ext.ImageStore;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceImageStore implements ImageStore {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("description") private String description;
    @JsonProperty("default") private Boolean defaultValue;
    @JsonProperty("read-only") private Boolean readOnly;
    @JsonProperty("type") private String type;
    @JsonProperty("weight") private Integer weight;
    @JsonProperty("properties") private Map<String, Object> properties;

    @Override public String getId() { return id; }
    @Override public String getDescription() { return description; }
    @Override public Boolean isDefault() { return defaultValue; }
    @Override public Boolean isReadOnly() { return readOnly; }
    @Override public String getType() { return type; }
    @Override public Integer getWeight() { return weight; }
    @Override public Map<String, Object> getProperties() { return properties; }

    public static class Stores extends ListResult<GlanceImageStore> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("stores")
        private List<GlanceImageStore> list;

        @Override
        protected List<GlanceImageStore> value() {
            return list;
        }
    }
}
