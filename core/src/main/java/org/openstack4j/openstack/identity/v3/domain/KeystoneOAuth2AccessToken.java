package org.openstack4j.openstack.identity.v3.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.identity.v3.OAuth2AccessToken;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneOAuth2AccessToken implements OAuth2AccessToken {

    private static final long serialVersionUID = 1L;

    @JsonProperty("access_token") private String accessToken;
    @JsonProperty("token_type") private String tokenType;
    @JsonProperty("expires_in") private Integer expiresIn;

    @Override public String getAccessToken() { return accessToken; }
    @Override public String getTokenType() { return tokenType; }
    @Override public Integer getExpiresIn() { return expiresIn; }
}
