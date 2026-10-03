package org.openstack4j.model.identity.v3;

import org.openstack4j.model.ModelEntity;

/** A federation protocol of an identity provider, bound to a mapping. */
public interface FederationProtocol extends ModelEntity {
    String getId();
    String getMappingId();
    String getRemoteIdAttribute();
}
