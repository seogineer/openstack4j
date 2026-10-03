package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a network segment range create or update. */
public class NetworkSegmentRangeOptions extends NeutronAttributes<NetworkSegmentRangeOptions> {

    public static NetworkSegmentRangeOptions create(String networkType, int minimum, int maximum) {
        return new NetworkSegmentRangeOptions().put("network_type", Objects.requireNonNull(networkType)).put("minimum", minimum).put("maximum", maximum);
    }

    /** An update that sends only the fields set afterwards. */
    public static NetworkSegmentRangeOptions update() {
        return new NetworkSegmentRangeOptions();
    }

    @Override
    protected NetworkSegmentRangeOptions self() {
        return this;
    }

    public NetworkSegmentRangeOptions name(String value) { return put("name", value); }
    public NetworkSegmentRangeOptions description(String value) { return put("description", value); }
    public NetworkSegmentRangeOptions shared(Boolean value) { return put("shared", value); }
    public NetworkSegmentRangeOptions projectId(String value) { return put("project_id", value); }
    public NetworkSegmentRangeOptions physicalNetwork(String value) { return put("physical_network", value); }
    public NetworkSegmentRangeOptions minimum(Integer value) { return put("minimum", value); }
    public NetworkSegmentRangeOptions maximum(Integer value) { return put("maximum", value); }
}
