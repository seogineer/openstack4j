package org.openstack4j.model.placement.v1;

import org.openstack4j.model.ModelEntity;

/** A Placement resource provider (for example a compute node or one of its child providers). */
public interface ResourceProvider extends ModelEntity {

    String getUuid();

    String getName();

    /** Incremented by Placement on every change to the provider's inventories, traits or aggregates. */
    Long getGeneration();

    /** @return the parent provider UUID, or {@code null} for a root provider */
    String getParentProviderUuid();

    String getRootProviderUuid();
}
