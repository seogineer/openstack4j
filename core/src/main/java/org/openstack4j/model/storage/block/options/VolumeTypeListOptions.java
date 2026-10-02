package org.openstack4j.model.storage.block.options;

import java.util.Map;

/** Filters for {@code GET /types}. */
public class VolumeTypeListOptions extends BlockStorageListOptions<VolumeTypeListOptions> {

    public static VolumeTypeListOptions create() {
        return new VolumeTypeListOptions();
    }

    public VolumeTypeListOptions isPublic(boolean isPublic) { return put("is_public", isPublic, 0); }
    /** Filter by extra specs (3.52+). */
    public VolumeTypeListOptions extraSpecs(Map<String, String> extraSpecs) { return put("extra_specs", dict(extraSpecs), 52); }
}
