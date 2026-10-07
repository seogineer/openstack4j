package org.openstack4j.api.dns.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.ext.ReverseFloatingIp;

/** PTR records of floating IPs ({@code /v2/reverse/floatingips}). */
public interface ReverseFloatingIpService extends RestService {

    /** @return the PTR records of the project's floating IPs */
    List<? extends ReverseFloatingIp> list();

    /** @param filters query parameters such as {@code ptrdname}, {@code status}, {@code limit}, {@code marker} */
    List<? extends ReverseFloatingIp> list(Map<String, String> filters);

    /** @return the PTR record, or {@code null} when the floating IP does not exist */
    ReverseFloatingIp get(String region, String floatingIpId);

    /**
     * Sets the PTR record of a floating IP.
     *
     * @param description may be {@code null}
     * @param ttl         may be {@code null} for the default
     */
    ReverseFloatingIp set(String region, String floatingIpId, String ptrdname, String description, Integer ttl);

    /** Removes the PTR record of a floating IP ({@code ptrdname} set to null). */
    ActionResponse unset(String region, String floatingIpId);
}
