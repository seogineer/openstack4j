package org.openstack4j.api.reservation;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.reservation.ReservableFloatingIp;

/** Floating IPs of the reservation pool ({@code /v1/floatingips}, admin). */
public interface ReservableFloatingIpService extends RestService {

    List<? extends ReservableFloatingIp> list();

    /** @return the floating IP, or {@code null} when it does not exist */
    ReservableFloatingIp get(String floatingIpId);

    ReservableFloatingIp create(String floatingNetworkId, String floatingIpAddress);

    ActionResponse delete(String floatingIpId);
}
