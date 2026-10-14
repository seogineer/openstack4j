package org.openstack4j.api.masakari;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.masakari.Segment;
import org.openstack4j.model.masakari.options.SegmentOptions;

/** Failover segments ({@code /segments}). */
public interface SegmentService extends RestService {

    /** @return the failover segments */
    List<? extends Segment> list();

    /** @param filters query parameters such as {@code recovery_method}, {@code service_type}, {@code enabled} (1.2), {@code limit}, {@code marker}, {@code sort_key} */
    List<? extends Segment> list(Map<String, String> filters);

    /** @return the segment, or {@code null} when it does not exist */
    Segment get(String id);

    Segment create(SegmentOptions options);

    /** Changes only the fields set in {@code options}. */
    Segment update(String id, SegmentOptions options);

    ActionResponse delete(String id);
}
