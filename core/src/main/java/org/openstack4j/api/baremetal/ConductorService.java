package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.baremetal.Conductor;

/** Bare metal conductors ({@code /v1/conductors}, microversion 1.49) and node shards ({@code /v1/shards}, 1.82). */
public interface ConductorService extends RestService {

    /** @return the conductors with all fields */
    List<? extends Conductor> list();

    /** @param filters query parameters such as {@code limit}, {@code marker} ({@code detail=true} is sent unless given) */
    List<? extends Conductor> list(Map<String, String> filters);

    /** @return the conductor, or {@code null} when it does not exist */
    Conductor get(String hostname);

    /** @return the node shards: shard name to its node count ({@code null} name = nodes without a shard) */
    Map<String, Integer> shards();
}
