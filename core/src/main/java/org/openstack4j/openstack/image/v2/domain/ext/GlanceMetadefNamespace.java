package org.openstack4j.openstack.image.v2.domain.ext;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.image.v2.ext.MetadefNamespace;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceMetadefNamespace implements MetadefNamespace {

    private static final long serialVersionUID = 1L;

    @JsonProperty("namespace") private String namespace;
    @JsonProperty("display_name") private String displayName;
    @JsonProperty("description") private String description;
    @JsonProperty("visibility") private String visibility;
    @JsonProperty("protected") private Boolean protectedValue;
    @JsonProperty("owner") private String owner;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;
    @JsonProperty("properties") private Map<String, Object> properties;
    @JsonProperty("objects") private List<Map<String, Object>> objects;
    @JsonProperty("resource_type_associations") private List<Map<String, Object>> resourceTypeAssociations;
    @JsonProperty("tags") private List<Map<String, Object>> tags;

    @Override public String getNamespace() { return namespace; }
    @Override public String getDisplayName() { return displayName; }
    @Override public String getDescription() { return description; }
    @Override public String getVisibility() { return visibility; }
    @Override public Boolean isProtected() { return protectedValue; }
    @Override public String getOwner() { return owner; }
    @Override public String getCreatedAt() { return createdAt; }
    @Override public String getUpdatedAt() { return updatedAt; }
    @Override public Map<String, Object> getProperties() { return properties; }
    @Override public List<Map<String, Object>> getObjects() { return objects; }
    @Override public List<Map<String, Object>> getResourceTypeAssociations() { return resourceTypeAssociations; }
    @Override public List<Map<String, Object>> getTags() { return tags; }

    public static class Namespaces extends ListResult<GlanceMetadefNamespace> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("namespaces")
        private List<GlanceMetadefNamespace> list;

        @Override
        protected List<GlanceMetadefNamespace> value() {
            return list;
        }
    }
}
