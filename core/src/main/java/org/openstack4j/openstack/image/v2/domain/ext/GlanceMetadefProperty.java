package org.openstack4j.openstack.image.v2.domain.ext;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.openstack4j.model.image.v2.ext.MetadefProperty;

/** A metadef property: a JSON schema fragment; the common fields have getters, everything is in {@link #getSchema()}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceMetadefProperty implements MetadefProperty {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> schema = new LinkedHashMap<>();

    @JsonAnySetter
    public void set(String key, Object value) {
        schema.put(key, value);
    }

    /** Fills the name from the map key of a list response. */
    public GlanceMetadefProperty named(String name) {
        schema.putIfAbsent("name", name);
        return this;
    }

    @Override public String getName() { return (String) schema.get("name"); }
    @Override public String getTitle() { return (String) schema.get("title"); }
    @Override public String getDescription() { return (String) schema.get("description"); }
    @Override public String getType() { return (String) schema.get("type"); }
    @Override public Map<String, Object> getSchema() { return schema; }
}
