package org.openstack4j.api.reservation;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.reservation.ReservableHost;

/** Hosts of the reservation pool ({@code /v1/os-hosts}, admin). */
public interface ReservableHostService extends RestService {

    List<? extends ReservableHost> list();

    /** @return the host, or {@code null} when it does not exist */
    ReservableHost get(String hostId);

    /** @param host {@code name} (the compute host) and extra capabilities, e.g. {@code {"name": "compute-1", "gpu": "a100"}} */
    ReservableHost create(Map<String, ?> host);

    /** @param capabilities the extra capabilities to set */
    ReservableHost update(String hostId, Map<String, ?> capabilities);

    ActionResponse delete(String hostId);

    /** @param filters e.g. {@code lease_id}, {@code reservation_id} @return per host its reservations */
    List<Map<String, Object>> listAllocations(Map<String, String> filters);

    /** @return the reservations of a host; a missing host raises */
    Map<String, Object> getAllocation(String hostId);

    /** @param detail whether to include private properties and values @return the host properties */
    List<Map<String, Object>> listProperties(boolean detail);

    /** Makes a host property private (hidden from non-admins) or public; @return the property */
    Map<String, Object> updateProperty(String propertyName, boolean isPrivate);
}
