package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A metering label counting router traffic. */
public interface MeteringLabel extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getProjectId();
    Boolean isShared();
}
