package org.openstack4j.openstack.identity.v3.domain;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.openstack4j.model.identity.v3.OAuth1Token;

/** An OAuth1 request or access token from Keystone's form-encoded response. */
public class KeystoneOAuth1Token implements OAuth1Token {

    private static final long serialVersionUID = 1L;

    private String key;
    private String secret;
    private String expiresAt;

    /** Parses {@code oauth_token=..&oauth_token_secret=..&oauth_expires_at=..}. */
    public static KeystoneOAuth1Token parse(String formBody) {
        KeystoneOAuth1Token token = new KeystoneOAuth1Token();
        for (String pair : formBody.trim().split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0)
                continue;
            String name = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            switch (name) {
                case "oauth_token": token.key = value; break;
                case "oauth_token_secret": token.secret = value; break;
                case "oauth_expires_at": token.expiresAt = value.isEmpty() ? null : value; break;
                default: break;
            }
        }
        return token;
    }

    @Override public String getKey() { return key; }
    @Override public String getSecret() { return secret; }
    @Override public String getExpiresAt() { return expiresAt; }
}
