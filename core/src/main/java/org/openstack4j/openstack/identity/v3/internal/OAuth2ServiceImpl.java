package org.openstack4j.openstack.identity.v3.internal;

import org.openstack4j.core.transport.ClientConstants;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

import org.openstack4j.api.identity.v3.OAuth2Service;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.identity.v3.OAuth2AccessToken;
import org.openstack4j.openstack.identity.v3.domain.KeystoneOAuth2AccessToken;

public class OAuth2ServiceImpl extends BaseIdentityServices implements OAuth2Service {

    @Override
    public OAuth2AccessToken token(String clientId, String clientSecret) {
        String basic = Base64.getEncoder().encodeToString((Objects.requireNonNull(clientId) + ":" + Objects.requireNonNull(clientSecret))
                .getBytes(StandardCharsets.UTF_8));
        byte[] form = "grant_type=client_credentials".getBytes(StandardCharsets.UTF_8);
        return post(KeystoneOAuth2AccessToken.class, "/OS-OAUTH2/token")
                .header(ClientConstants.HEADER_OS4J_AUTH, IdentityResponses.NO_REAUTH)
                .header("Authorization", "Basic " + basic)
                // entity(Payload) resets the content type to octet-stream, so set the form type after it
                .entity(Payloads.create(new ByteArrayInputStream(form)))
                .contentType("application/x-www-form-urlencoded")
                .execute();
    }
}
