package org.openstack4j.api.instanceha;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.instanceha.Host;
import org.openstack4j.model.instanceha.options.HostOptions;

/** Hosts of a failover segment ({@code /segments/{segment_id}/hosts}). */
public interface HostService extends RestService {

    /** @param filters e.g. {@code failover_segment_id}, {@code type}, {@code on_maintenance}, {@code reserved}, {@code limit}, {@code marker} */
    List<? extends Host> list(String segmentId, Map<String, String> filters);

    /** @return the host, or {@code null} when it does not exist */
    Host get(String segmentId, String hostId);

    Host create(String segmentId, HostOptions options);

    /** Changes only the fields set in {@code options}. */
    Host update(String segmentId, String hostId, HostOptions options);

    ActionResponse delete(String segmentId, String hostId);
}
