package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;

import org.openstack4j.api.networking.ext.ServiceProviderService;
import org.openstack4j.model.network.ext.ServiceProvider;
import org.openstack4j.openstack.networking.domain.ext.NeutronServiceProvider.ServiceProviders;

public class ServiceProviderServiceImpl extends BaseNeutronExtService implements ServiceProviderService {

    @Override
    public List<? extends ServiceProvider> list() {
        return listOf(ServiceProviders.class, "/service-providers", null);
    }
}
