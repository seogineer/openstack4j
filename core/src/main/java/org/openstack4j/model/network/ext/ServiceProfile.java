package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A service profile: the driver and metadata behind a service flavor. */
public interface ServiceProfile extends ModelEntity {
    String getId();
    String getDescription();
    String getDriver();
    String getMetainfo();
    Boolean isEnabled();
}
