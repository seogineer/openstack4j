package org.openstack4j.openstack.image.v2.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.image.v2.ext.MetadefResourceTypeAssociation;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceMetadefResourceTypeAssociation implements MetadefResourceTypeAssociation {

    private static final long serialVersionUID = 1L;

    @JsonProperty("name") private String name;
    @JsonProperty("prefix") private String prefix;
    @JsonProperty("properties_target") private String propertiesTarget;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @Override public String getName() { return name; }
    @Override public String getPrefix() { return prefix; }
    @Override public String getPropertiesTarget() { return propertiesTarget; }
    @Override public String getCreatedAt() { return createdAt; }
    @Override public String getUpdatedAt() { return updatedAt; }

    public static class Associations extends ListResult<GlanceMetadefResourceTypeAssociation> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("resource_type_associations")
        private List<GlanceMetadefResourceTypeAssociation> list;

        @Override
        protected List<GlanceMetadefResourceTypeAssociation> value() {
            return list;
        }
    }
}
