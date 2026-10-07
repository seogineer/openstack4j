package org.openstack4j.openstack.octavia.internal;

import org.openstack4j.api.Apis;
import org.openstack4j.api.octavia.*;

/**
 * OpenStack Networking Operations API
 *
 * @author wei
 */
public class OctaviaServiceImpl implements OctaviaService {

    /**
     * {@inheritDoc}
     */
    @Override
    public LoadBalancerV2Service loadBalancerV2() {
        return Apis.get(LoadBalancerV2Service.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListenerV2Service listenerV2() {
        return Apis.get(ListenerV2Service.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LbPoolV2Service lbPoolV2() {
        return Apis.get(LbPoolV2Service.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HealthMonitorV2Service healthMonitorV2() {
        return Apis.get(HealthMonitorV2Service.class);
    }


    @Override
    public org.openstack4j.api.octavia.ext.ProviderService providers() {
        return Apis.get(org.openstack4j.api.octavia.ext.ProviderService.class);
    }

    @Override
    public org.openstack4j.api.octavia.ext.QuotaService quotas() {
        return Apis.get(org.openstack4j.api.octavia.ext.QuotaService.class);
    }

    @Override
    public org.openstack4j.api.octavia.ext.L7PolicyService l7Policies() {
        return Apis.get(org.openstack4j.api.octavia.ext.L7PolicyService.class);
    }

    @Override
    public org.openstack4j.api.octavia.ext.OctaviaFlavorService flavors() {
        return Apis.get(org.openstack4j.api.octavia.ext.OctaviaFlavorService.class);
    }

    @Override
    public org.openstack4j.api.octavia.ext.FlavorProfileService flavorProfiles() {
        return Apis.get(org.openstack4j.api.octavia.ext.FlavorProfileService.class);
    }

    @Override
    public org.openstack4j.api.octavia.ext.OctaviaAvailabilityZoneService availabilityZones() {
        return Apis.get(org.openstack4j.api.octavia.ext.OctaviaAvailabilityZoneService.class);
    }

    @Override
    public org.openstack4j.api.octavia.ext.AvailabilityZoneProfileService availabilityZoneProfiles() {
        return Apis.get(org.openstack4j.api.octavia.ext.AvailabilityZoneProfileService.class);
    }

    @Override
    public org.openstack4j.api.octavia.ext.AmphoraService amphorae() {
        return Apis.get(org.openstack4j.api.octavia.ext.AmphoraService.class);
    }
}
