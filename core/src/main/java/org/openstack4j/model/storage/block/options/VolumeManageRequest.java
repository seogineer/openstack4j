package org.openstack4j.model.storage.block.options;

import java.util.LinkedHashMap;
import java.util.Map;

/** Body of {@code POST /manageable_volumes} (3.8+): brings an existing backend volume under Cinder's control. */
public class VolumeManageRequest {

    private final Map<String, String> ref;
    private final Map<String, Object> fields = new LinkedHashMap<>();

    private VolumeManageRequest(Map<String, String> ref) {
        this.ref = ref;
    }

    /** @param ref the driver reference, such as {@code {"source-name": "lvol0"}} */
    public static VolumeManageRequest create(Map<String, String> ref) {
        return new VolumeManageRequest(ref);
    }

    public VolumeManageRequest host(String host) { fields.put("host", host); return this; }
    /** 3.16+ */
    public VolumeManageRequest cluster(String cluster) { fields.put("cluster", cluster); return this; }
    public VolumeManageRequest name(String name) { fields.put("name", name); return this; }
    public VolumeManageRequest description(String description) { fields.put("description", description); return this; }
    public VolumeManageRequest volumeType(String volumeType) { fields.put("volume_type", volumeType); return this; }
    public VolumeManageRequest availabilityZone(String zone) { fields.put("availability_zone", zone); return this; }
    public VolumeManageRequest bootable(boolean bootable) { fields.put("bootable", bootable); return this; }
    public VolumeManageRequest metadata(Map<String, String> metadata) { fields.put("metadata", metadata); return this; }

    public String getCluster() { return (String) fields.get("cluster"); }

    public Map<String, Object> toMap() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ref", ref);
        body.putAll(fields);
        return body;
    }
}
