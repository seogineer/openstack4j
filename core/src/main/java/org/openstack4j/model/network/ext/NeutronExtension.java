package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A Neutron API extension. */
public interface NeutronExtension extends ModelEntity {
    String getAlias();
    String getName();
    String getDescription();
    String getUpdated();
}
