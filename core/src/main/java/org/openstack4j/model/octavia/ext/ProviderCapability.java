package org.openstack4j.model.octavia.ext;

import org.openstack4j.model.ModelEntity;

/** A flavor or availability zone capability of a provider. */
public interface ProviderCapability extends ModelEntity {
    String getName();
    String getDescription();
}
