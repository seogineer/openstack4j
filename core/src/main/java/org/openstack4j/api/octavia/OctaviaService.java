package org.openstack4j.api.octavia;

import org.openstack4j.common.RestService;

/**
 * OpenStack Octavia Operations API
 *
 * @author wei
 */
public interface OctaviaService extends RestService {

    /**
     * @return the LoadBalancerV2 Service API
     */
    LoadBalancerV2Service loadBalancerV2();

    /**
     * @return the ListenerV2 Service API
     */
    ListenerV2Service listenerV2();

    /**
     * @return the LbPoolV2 Service API
     */
    LbPoolV2Service lbPoolV2();

    /**
     * @return the healthMonitorV2 Service API
     */
    HealthMonitorV2Service healthMonitorV2();

    /**
     * Octavia provider drivers and their capabilities.
     *
     * @return the ProviderService
     */
    org.openstack4j.api.octavia.ext.ProviderService providers();

    /**
     * Octavia quotas.
     *
     * @return the QuotaService
     */
    org.openstack4j.api.octavia.ext.QuotaService quotas();

    /**
     * Octavia L7 policies and rules.
     *
     * @return the L7PolicyService
     */
    org.openstack4j.api.octavia.ext.L7PolicyService l7Policies();

    /**
     * Octavia flavors.
     *
     * @return the OctaviaFlavorService
     */
    org.openstack4j.api.octavia.ext.OctaviaFlavorService flavors();

    /**
     * Octavia flavor profiles.
     *
     * @return the FlavorProfileService
     */
    org.openstack4j.api.octavia.ext.FlavorProfileService flavorProfiles();

    /**
     * Octavia availability zones.
     *
     * @return the OctaviaAvailabilityZoneService
     */
    org.openstack4j.api.octavia.ext.OctaviaAvailabilityZoneService availabilityZones();

    /**
     * Octavia availability zone profiles.
     *
     * @return the AvailabilityZoneProfileService
     */
    org.openstack4j.api.octavia.ext.AvailabilityZoneProfileService availabilityZoneProfiles();
}
