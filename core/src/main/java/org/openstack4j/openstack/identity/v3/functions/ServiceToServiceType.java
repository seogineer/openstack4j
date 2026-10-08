package org.openstack4j.openstack.identity.v3.functions;

import java.util.function.Function;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.model.identity.v3.Service;

/**
 * A Function which takes a ServiceCatalog -> Service and returns the corresponding common ServiceType
 *
 * @author Jeremy Unruh
 */
public class ServiceToServiceType implements Function<Service, ServiceType> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceType apply(Service input) {
        return ServiceType.forCatalogEntry(input.getType(), input.getName());
    }

}
