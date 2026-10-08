package org.openstack4j.openstack.reservation.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.reservation.ReservableFloatingIpService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.reservation.ReservableFloatingIp;
import org.openstack4j.openstack.reservation.domain.BlazarReservableFloatingIp;
import org.openstack4j.openstack.reservation.domain.BlazarReservableFloatingIp.BlazarReservableFloatingIpList;

public class ReservableFloatingIpServiceImpl extends BaseBlazarService implements ReservableFloatingIpService {

    @Override
    public List<? extends ReservableFloatingIp> list() {
        return listOf(BlazarReservableFloatingIpList.class, "/floatingips", null);
    }

    @Override
    public ReservableFloatingIp get(String floatingIpId) {
        return show(BlazarReservableFloatingIp.class, "/floatingips/" + id(floatingIpId));
    }

    @Override
    public ReservableFloatingIp create(String floatingNetworkId, String floatingIpAddress) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("floating_network_id", Objects.requireNonNull(floatingNetworkId, "floatingNetworkId"));
        body.put("floating_ip_address", Objects.requireNonNull(floatingIpAddress, "floatingIpAddress"));
        return create(BlazarReservableFloatingIp.class, "/floatingips", body);
    }

    @Override
    public ActionResponse delete(String floatingIpId) {
        return remove("/floatingips/" + id(floatingIpId));
    }
}
