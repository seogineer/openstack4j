package org.openstack4j.model.network.ext;

import java.util.List;

import org.openstack4j.model.ModelEntity;

/** A Neutron service flavor (for example a router flavor), not a compute flavor. */
public interface ServiceFlavor extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getServiceType();
    Boolean isEnabled();
    List<String> getServiceProfiles();
}
