package org.openstack4j.openstack.image.v2.domain.ext;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.image.v2.ext.MetadefObject;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceMetadefObject implements MetadefObject {

    private static final long serialVersionUID = 1L;

    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("required") private List<String> required;
    @JsonProperty("properties") private Map<String, Object> properties;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public List<String> getRequired() { return required; }
    @Override public Map<String, Object> getProperties() { return properties; }
    @Override public String getCreatedAt() { return createdAt; }
    @Override public String getUpdatedAt() { return updatedAt; }

    public static class MetadefObjects extends ListResult<GlanceMetadefObject> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("objects")
        private List<GlanceMetadefObject> list;

        @Override
        protected List<GlanceMetadefObject> value() {
            return list;
        }
    }
}
