package org.openstack4j.model.identity.v3;

import org.openstack4j.model.ModelEntity;

/** An OAuth1 access token a user authorized. */
public interface OAuth1AccessToken extends ModelEntity {
    String getId();
    String getConsumerId();
    String getProjectId();
    String getAuthorizingUserId();
    String getExpiresAt();
}
