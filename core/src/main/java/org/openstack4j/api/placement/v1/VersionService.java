package org.openstack4j.api.placement.v1;

import org.openstack4j.common.RestService;
import org.openstack4j.model.placement.v1.PlacementVersion;

public interface VersionService extends RestService {

    /** @return the server's microversion range and the version this session uses */
    PlacementVersion get();
}
