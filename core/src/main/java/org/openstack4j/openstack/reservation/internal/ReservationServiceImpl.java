package org.openstack4j.openstack.reservation.internal;

import org.openstack4j.api.Apis;
import org.openstack4j.api.reservation.LeaseService;
import org.openstack4j.api.reservation.ReservableFloatingIpService;
import org.openstack4j.api.reservation.ReservableHostService;
import org.openstack4j.api.reservation.ReservationService;

public class ReservationServiceImpl implements ReservationService {

    @Override
    public LeaseService leases() {
        return Apis.get(LeaseService.class);
    }

    @Override
    public ReservableHostService hosts() {
        return Apis.get(ReservableHostService.class);
    }

    @Override
    public ReservableFloatingIpService floatingIps() {
        return Apis.get(ReservableFloatingIpService.class);
    }
}
