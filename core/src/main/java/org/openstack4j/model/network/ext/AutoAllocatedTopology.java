package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** The auto-allocated (get-me-a-network) topology of a project. */
public interface AutoAllocatedTopology extends ModelEntity {
    String getId();
    String getProjectId();
    String getDryRun();
}
