package org.openstack4j.model.identity.v3;

import org.openstack4j.model.ModelEntity;

/** An OAuth2 client-credentials access token (OS-OAUTH2). */
public interface OAuth2AccessToken extends ModelEntity {
    String getAccessToken();
    String getTokenType();
    Integer getExpiresIn();
}
