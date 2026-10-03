package org.openstack4j.api.networking.ext;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.network.ext.ServiceProvider;

/**
 * Neutron service providers ({@code GET /v2.0/service-providers}).
 */
public interface ServiceProviderService extends RestService {

    /**
     * Lists the service providers (drivers) per service type.
     *
     * @return the result
     */
    List<? extends ServiceProvider> list();
}
