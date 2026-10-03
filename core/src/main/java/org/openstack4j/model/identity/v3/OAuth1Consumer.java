package org.openstack4j.model.identity.v3;

import org.openstack4j.model.ModelEntity;

/** An OAuth1 consumer (OS-OAUTH1); the secret is only present in the create response. */
public interface OAuth1Consumer extends ModelEntity {
    String getId();
    String getDescription();
    String getSecret();
}
