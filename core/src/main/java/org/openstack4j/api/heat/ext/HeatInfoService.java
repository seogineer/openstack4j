package org.openstack4j.api.heat.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.heat.ext.HeatEngineService;

/**
 * Heat build information and engine services.
 */
public interface HeatInfoService extends RestService {

    /**
     * Returns the API and engine build revisions.
     *
     * @throws org.openstack4j.api.exceptions.ResponseException on an error status, including 404
     * @return the build information
     */
    Map<String, Object> buildInfo();

    /**
     * Lists the heat-engine services (admin).
     *
     * @throws org.openstack4j.api.exceptions.ResponseException for a non-admin (403) or another error status
     * @return the engine services
     */
    List<? extends HeatEngineService> services();
}
