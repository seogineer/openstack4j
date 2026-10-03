package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A Neutron service provider (driver) of a service type. */
public interface ServiceProvider extends ModelEntity {
    String getServiceType();
    String getName();
    Boolean isDefault();
}
