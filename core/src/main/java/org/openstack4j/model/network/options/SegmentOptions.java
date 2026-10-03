package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a segment create or update. */
public class SegmentOptions extends NeutronAttributes<SegmentOptions> {

    public static SegmentOptions create(String networkId, String networkType) {
        return new SegmentOptions().put("network_id", Objects.requireNonNull(networkId)).put("network_type", Objects.requireNonNull(networkType));
    }

    public static SegmentOptions update() {
        return new SegmentOptions();
    }

    @Override
    protected SegmentOptions self() {
        return this;
    }

    public SegmentOptions name(String name) { return put("name", name); }
    public SegmentOptions description(String description) { return put("description", description); }
    public SegmentOptions physicalNetwork(String physicalNetwork) { return put("physical_network", physicalNetwork); }
    public SegmentOptions segmentationId(Integer segmentationId) { return put("segmentation_id", segmentationId); }
}
