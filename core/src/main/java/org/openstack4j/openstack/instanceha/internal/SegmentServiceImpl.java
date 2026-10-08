package org.openstack4j.openstack.instanceha.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.instanceha.SegmentService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.instanceha.Segment;
import org.openstack4j.model.instanceha.options.SegmentOptions;
import org.openstack4j.openstack.instanceha.domain.MasakariSegment;
import org.openstack4j.openstack.instanceha.domain.MasakariSegment.MasakariSegmentList;

public class SegmentServiceImpl extends BaseMasakariService implements SegmentService {

    private static final String PATH = "/segments";
    private static final String ROOT = "segment";

    @Override
    public List<? extends Segment> list() {
        return list(null);
    }

    @Override
    public List<? extends Segment> list(Map<String, String> filters) {
        return listOf(MasakariSegmentList.class, PATH, filters);
    }

    @Override
    public Segment get(String id) {
        return show(MasakariSegment.class, PATH + "/" + id(id));
    }

    @Override
    public Segment create(SegmentOptions options) {
        return create(MasakariSegment.class, PATH, ROOT, options);
    }

    @Override
    public Segment update(String id, SegmentOptions options) {
        return update(MasakariSegment.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
