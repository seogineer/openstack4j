package org.openstack4j.api.identity.v3;

import org.openstack4j.common.RestService;
import org.openstack4j.model.identity.v3.OAuth2AccessToken;

/**
 * OS-OAUTH2 client credentials tokens.
 */
public interface OAuth2Service extends RestService {

    /**
     * Obtains an access token with the client credentials grant (POST /OS-OAUTH2/token, HTTP Basic).
     *
     * @param clientId the client id
     * @param clientSecret the client secret
     * @return the result
     */
    OAuth2AccessToken token(String clientId, String clientSecret);
}
