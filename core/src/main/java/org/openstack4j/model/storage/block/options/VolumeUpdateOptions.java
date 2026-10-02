package org.openstack4j.model.storage.block.options;

import java.util.LinkedHashMap;
import java.util.Map;

/** Body of {@code PUT /volumes/{id}}; only the fields set are sent (3.53 requires at least one). */
public class VolumeUpdateOptions {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    public static VolumeUpdateOptions create() {
        return new VolumeUpdateOptions();
    }

    public VolumeUpdateOptions name(String name) { fields.put("name", name); return this; }
    public VolumeUpdateOptions description(String description) { fields.put("description", description); return this; }
    /** Replaces the volume metadata. */
    public VolumeUpdateOptions metadata(Map<String, String> metadata) { fields.put("metadata", metadata); return this; }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(fields);
    }
}
