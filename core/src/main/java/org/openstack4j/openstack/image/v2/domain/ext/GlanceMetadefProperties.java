package org.openstack4j.openstack.image.v2.domain.ext;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"properties": {"<name>": {...}}}} of a namespace property list. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceMetadefProperties implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("properties")
    private Map<String, GlanceMetadefProperty> properties;

    public Map<String, GlanceMetadefProperty> getProperties() {
        return properties;
    }
}
