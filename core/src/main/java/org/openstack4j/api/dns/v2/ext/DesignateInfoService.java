package org.openstack4j.api.dns.v2.ext;

import java.util.Map;

import org.openstack4j.common.RestService;

/** Designate service information. */
public interface DesignateInfoService extends RestService {

    /** @return the project's limits ({@code GET /v2/limits}), e.g. {@code max_zones}, {@code max_page_limit} */
    Map<String, Object> limits();
}
