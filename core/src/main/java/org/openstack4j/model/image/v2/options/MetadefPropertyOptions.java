package org.openstack4j.model.image.v2.options;

import java.util.List;
import java.util.Objects;

/** Body of a metadef property create or (full) update. */
public class MetadefPropertyOptions extends ImageAttributes<MetadefPropertyOptions> {

    public static MetadefPropertyOptions create(String name, String title, String type) {
        return new MetadefPropertyOptions().put("name", Objects.requireNonNull(name)).put("title", Objects.requireNonNull(title)).put("type", Objects.requireNonNull(type));
    }

    @Override
    protected MetadefPropertyOptions self() {
        return this;
    }

    public MetadefPropertyOptions description(String value) { return put("description", value); }
    public MetadefPropertyOptions enumValues(List<?> value) { return put("enum", value); }
    public MetadefPropertyOptions minimum(Number value) { return put("minimum", value); }
    public MetadefPropertyOptions maximum(Number value) { return put("maximum", value); }
    public MetadefPropertyOptions defaultValue(Object value) { return put("default", value); }
    public MetadefPropertyOptions readonly(Boolean value) { return put("readonly", value); }
}
