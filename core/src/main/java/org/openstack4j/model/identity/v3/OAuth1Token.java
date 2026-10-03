package org.openstack4j.model.identity.v3;

import org.openstack4j.model.ModelEntity;

/** An OAuth1 request or access token key and secret. */
public interface OAuth1Token extends ModelEntity {
    String getKey();
    String getSecret();
    String getExpiresAt();
}
