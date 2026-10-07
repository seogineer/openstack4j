package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.Segment;
import org.openstack4j.model.network.options.SegmentOptions;

/**
 * Network segments ({@code /v2.0/segments}, segment): routed provider networks.
 */
public interface SegmentService extends RestService {

    /**
     * Lists segments.
     *
     * @return the result
     */
    List<? extends Segment> list();

    /**
     * Lists segments, optionally filtered (network_id, network_type, physical_network).
     *
     * @param filters the filters
     * @return the result
     */
    List<? extends Segment> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    Segment get(String id);

    /**
     * Creates a segment on a network.
     *
     * @param options the options
     * @return the result
     */
    Segment create(SegmentOptions options);

    /**
     * Updates the name or description of a segment.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    Segment update(String id, SegmentOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
