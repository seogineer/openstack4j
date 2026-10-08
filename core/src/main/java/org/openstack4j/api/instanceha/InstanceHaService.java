package org.openstack4j.api.instanceha;

import org.openstack4j.common.RestService;

/** Instance HA (Masakari v1): failover segments, their hosts and failure notifications. */
public interface InstanceHaService extends RestService {

    SegmentService segments();

    HostService hosts();

    NotificationService notifications();
}
