package org.openstack4j.openstack.instanceha.internal;

import org.openstack4j.api.Apis;
import org.openstack4j.api.instanceha.HostService;
import org.openstack4j.api.instanceha.InstanceHaService;
import org.openstack4j.api.instanceha.NotificationService;
import org.openstack4j.api.instanceha.SegmentService;

public class InstanceHaServiceImpl implements InstanceHaService {

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
