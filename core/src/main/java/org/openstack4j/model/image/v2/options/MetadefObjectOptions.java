package org.openstack4j.model.image.v2.options;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Body of a metadef object create or (full) update. */
public class MetadefObjectOptions extends ImageAttributes<MetadefObjectOptions> {

    public static MetadefObjectOptions create(String name) {
        return new MetadefObjectOptions().put("name", Objects.requireNonNull(name));
    }

    @Override
    protected MetadefObjectOptions self() {
        return this;
    }

    public MetadefObjectOptions description(String value) { return put("description", value); }
    public MetadefObjectOptions required(List<String> value) { return put("required", value); }
    public MetadefObjectOptions properties(Map<String, Object> value) { return put("properties", value); }
}
