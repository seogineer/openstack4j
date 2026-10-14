package org.openstack4j.openstack.blazar.internal;

import org.openstack4j.api.Apis;
import org.openstack4j.api.blazar.LeaseService;
import org.openstack4j.api.blazar.ReservableFloatingIpService;
import org.openstack4j.api.blazar.ReservableHostService;
import org.openstack4j.api.blazar.ReservationService;

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
