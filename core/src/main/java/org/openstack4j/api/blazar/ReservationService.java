package org.openstack4j.api.blazar;

import org.openstack4j.common.RestService;

/** Reservation (Blazar v1): leases and the host and floating IP pools. */
public interface ReservationService extends RestService {

    LeaseService leases();

    ReservableHostService hosts();

    ReservableFloatingIpService floatingIps();
}
