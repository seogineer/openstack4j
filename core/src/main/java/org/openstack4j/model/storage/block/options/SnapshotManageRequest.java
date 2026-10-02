package org.openstack4j.model.storage.block.options;

import java.util.LinkedHashMap;
import java.util.Map;

/** Body of {@code POST /manageable_snapshots} (3.8+). */
public class SnapshotManageRequest {

    private final String volumeId;
    private final Map<String, String> ref;
    private final Map<String, Object> fields = new LinkedHashMap<>();

    private SnapshotManageRequest(String volumeId, Map<String, String> ref) {
        this.volumeId = volumeId;
        this.ref = ref;
    }

    /** @param ref the driver reference, such as {@code {"source-name": "lvol0-snap"}} */
    public static SnapshotManageRequest create(String volumeId, Map<String, String> ref) {
        return new SnapshotManageRequest(volumeId, ref);
    }

    public SnapshotManageRequest name(String name) { fields.put("name", name); return this; }
    public SnapshotManageRequest description(String description) { fields.put("description", description); return this; }
    public SnapshotManageRequest metadata(Map<String, String> metadata) { fields.put("metadata", metadata); return this; }

    public Map<String, Object> toMap() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("volume_id", volumeId);
        body.put("ref", ref);
        body.putAll(fields);
        return body;
    }
}
