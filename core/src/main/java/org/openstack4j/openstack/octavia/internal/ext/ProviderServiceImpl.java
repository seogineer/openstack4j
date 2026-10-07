package org.openstack4j.openstack.octavia.internal.ext;

import java.util.List;

import org.openstack4j.api.octavia.ext.ProviderService;
import org.openstack4j.model.octavia.ext.OctaviaProvider;
import org.openstack4j.model.octavia.ext.ProviderCapability;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaProviderCapability.AvailabilityZoneCapabilities;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaProviderCapability.FlavorCapabilities;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaProviderEntity.Providers;

public class ProviderServiceImpl extends BaseOctaviaExtService implements ProviderService {

    @Override public List<? extends OctaviaProvider> list() { return listOf(Providers.class, "/lbaas/providers", null); }
    @Override public List<? extends ProviderCapability> flavorCapabilities(String provider) { return listOf(FlavorCapabilities.class, "/lbaas/providers/" + id(provider) + "/flavor_capabilities", null); }
    @Override public List<? extends ProviderCapability> availabilityZoneCapabilities(String provider) { return listOf(AvailabilityZoneCapabilities.class, "/lbaas/providers/" + id(provider) + "/availability_zone_capabilities", null); }
}
