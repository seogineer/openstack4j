package org.openstack4j.model.identity.v3;

import org.openstack4j.model.ModelEntity;

/** A Keystone-to-Keystone service provider. */
public interface ServiceProvider extends ModelEntity {
    String getId();
    String getDescription();
    Boolean isEnabled();
    String getAuthUrl();
    String getSpUrl();
    String getRelayStatePrefix();
}
