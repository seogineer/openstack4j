package org.openstack4j.openstack.image.v2.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.image.v2.ext.MetadefResourceType;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceMetadefResourceType implements MetadefResourceType {

    private static final long serialVersionUID = 1L;

    @JsonProperty("name") private String name;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @Override public String getName() { return name; }
    @Override public String getCreatedAt() { return createdAt; }
    @Override public String getUpdatedAt() { return updatedAt; }

    public static class ResourceTypes extends ListResult<GlanceMetadefResourceType> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("resource_types")
        private List<GlanceMetadefResourceType> list;

        @Override
        protected List<GlanceMetadefResourceType> value() {
            return list;
        }
    }
}
