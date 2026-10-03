package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.NetworkSegmentRangeService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.NetworkSegmentRange;
import org.openstack4j.model.network.options.NetworkSegmentRangeOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronNetworkSegmentRange;
import org.openstack4j.openstack.networking.domain.ext.NeutronNetworkSegmentRange.NetworkSegmentRanges;

public class NetworkSegmentRangeServiceImpl extends BaseNeutronExtService implements NetworkSegmentRangeService {

    private static final String PATH = "/network_segment_ranges";
    private static final String ROOT = "network_segment_range";

    @Override public List<? extends NetworkSegmentRange> list() { return listOf(NetworkSegmentRanges.class, PATH, null); }
    @Override public List<? extends NetworkSegmentRange> list(Map<String, String> filters) { return listOf(NetworkSegmentRanges.class, PATH, filters); }
    @Override public NetworkSegmentRange get(String id) { return show(NeutronNetworkSegmentRange.class, PATH + "/" + id(id)); }
    @Override public NetworkSegmentRange create(NetworkSegmentRangeOptions options) { return create(NeutronNetworkSegmentRange.class, PATH, ROOT, options); }
    @Override public NetworkSegmentRange update(String id, NetworkSegmentRangeOptions options) { return update(NeutronNetworkSegmentRange.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }
}
