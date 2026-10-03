package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.NetworkSegmentRange;
import org.openstack4j.model.network.options.NetworkSegmentRangeOptions;

/**
 * Network segment ranges ({@code /v2.0/network_segment_ranges}, network-segment-range).
 */
public interface NetworkSegmentRangeService extends RestService {

    /**
     * Lists network segment ranges, optionally filtered.
     *
     * @return the result
     */
    List<? extends NetworkSegmentRange> list();

    /**
     * Lists network segment ranges, optionally filtered.
     *
     * @param Map<String the map< string
     * @param filters the filters
     * @return the result
     */
    List<? extends NetworkSegmentRange> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    NetworkSegmentRange get(String id);

    /**
     * Creates a segment range (admin).
     *
     * @param options the options
     * @return the result
     */
    NetworkSegmentRange create(NetworkSegmentRangeOptions options);

    /**
     * Updates a segment range; only the fields set are sent.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    NetworkSegmentRange update(String id, NetworkSegmentRangeOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
