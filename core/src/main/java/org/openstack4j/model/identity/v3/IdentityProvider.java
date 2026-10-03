package org.openstack4j.model.identity.v3;

import java.util.List;

import org.openstack4j.model.ModelEntity;

/** A federated identity provider (OS-FEDERATION). */
public interface IdentityProvider extends ModelEntity {
    String getId();
    String getDescription();
    Boolean isEnabled();
    String getDomainId();
    List<String> getRemoteIds();
    Integer getAuthorizationTtl();
}
