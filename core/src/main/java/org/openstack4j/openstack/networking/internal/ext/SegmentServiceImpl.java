package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.SegmentService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.Segment;
import org.openstack4j.model.network.options.SegmentOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronSegment;
import org.openstack4j.openstack.networking.domain.ext.NeutronSegment.Segments;

public class SegmentServiceImpl extends BaseNeutronExtService implements SegmentService {

    private static final String PATH = "/segments";
    private static final String ROOT = "segment";

    @Override public List<? extends Segment> list() { return listOf(Segments.class, PATH, null); }
    @Override public List<? extends Segment> list(Map<String, String> filters) { return listOf(Segments.class, PATH, filters); }
    @Override public Segment get(String id) { return show(NeutronSegment.class, PATH + "/" + id(id)); }
    @Override public Segment create(SegmentOptions options) { return create(NeutronSegment.class, PATH, ROOT, options); }
    @Override public Segment update(String id, SegmentOptions options) { return update(NeutronSegment.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }
}
