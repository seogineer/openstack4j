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
     * @return the result
     */
    Map<String, Object> buildInfo();

    /**
     * Lists the heat-engine services (admin).
     *
     * @return the result
     */
    List<? extends HeatEngineService> services();
}
