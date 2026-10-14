package org.openstack4j.openstack.masakari.internal;

import org.openstack4j.api.Apis;
import org.openstack4j.api.masakari.HostService;
import org.openstack4j.api.masakari.MasakariService;
import org.openstack4j.api.masakari.NotificationService;
import org.openstack4j.api.masakari.SegmentService;

public class MasakariServiceImpl implements MasakariService {

    @Override
    public SegmentService segments() {
        return Apis.get(SegmentService.class);
    }

    @Override
    public HostService hosts() {
        return Apis.get(HostService.class);
    }

    @Override
    public NotificationService notifications() {
        return Apis.get(NotificationService.class);
    }
}
