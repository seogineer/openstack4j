package org.openstack4j.api.dns.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.dns.v2.ext.ServiceStatus;

/** Service statuses ({@code /v2/service_statuses}). */
public interface DesignateServiceStatusService extends RestService {

    /** @return the service statuses */
    List<? extends ServiceStatus> list();

    /** @param filters query parameters such as {@code hostname}, {@code service_name}, {@code status} */
    List<? extends ServiceStatus> list(Map<String, String> filters);

    /** @return the service status, or {@code null} when it does not exist */
    ServiceStatus get(String id);
}
