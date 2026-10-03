package org.openstack4j.openstack.identity.v3.internal;

import org.openstack4j.core.transport.ClientConstants;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openstack4j.api.identity.v3.OAuth1Service;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.OAuth1AccessToken;
import org.openstack4j.model.identity.v3.OAuth1Consumer;
import org.openstack4j.model.identity.v3.OAuth1Token;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.openstack.identity.v3.domain.KeystoneOAuth1AccessToken;
import org.openstack4j.openstack.identity.v3.domain.KeystoneOAuth1AccessToken.AccessTokens;
import org.openstack4j.openstack.identity.v3.domain.KeystoneOAuth1Consumer;
import org.openstack4j.openstack.identity.v3.domain.KeystoneOAuth1Consumer.Consumers;
import org.openstack4j.openstack.identity.v3.domain.KeystoneOAuth1Token;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRole;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRole.Roles;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class OAuth1ServiceImpl extends BaseIdentityServices implements OAuth1Service {

    private static final String CONSUMERS = "/OS-OAUTH1/consumers";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override public List<? extends OAuth1Consumer> listConsumers() { return get(Consumers.class, CONSUMERS).execute().getList(); }
    @Override public OAuth1Consumer getConsumer(String id) { return get(KeystoneOAuth1Consumer.class, CONSUMERS, "/", Objects.requireNonNull(id)).execute(); }

    @Override
    public OAuth1Consumer createConsumer(String description) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (description != null) fields.put("description", description);
        return post(KeystoneOAuth1Consumer.class, CONSUMERS).entity(JsonBody.of("consumer", fields)).execute();
    }

    @Override
    public OAuth1Consumer updateConsumer(String id, String description) {
        return patch(KeystoneOAuth1Consumer.class, CONSUMERS, "/", Objects.requireNonNull(id))
                .entity(JsonBody.of("consumer", Collections.singletonMap("description", description))).execute();
    }

    @Override public ActionResponse deleteConsumer(String id) { return deleteWithResponse(CONSUMERS, "/", Objects.requireNonNull(id)).execute(); }

    private static Map<String, String> oauthParams(String consumerKey) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("oauth_consumer_key", Objects.requireNonNull(consumerKey));
        params.put("oauth_nonce", Long.toHexString(RANDOM.nextLong()) + Long.toHexString(RANDOM.nextLong()));
        params.put("oauth_signature_method", "HMAC-SHA1");
        params.put("oauth_timestamp", Long.toString(System.currentTimeMillis() / 1000));
        params.put("oauth_version", "1.0");
        return params;
    }

    /** Signs against the exact URL the request goes to, then sends it with an empty body. */
    private OAuth1Token signedPost(String path, Map<String, String> params, String consumerSecret, String tokenSecret, String projectId) {
        BaseOpenStackService.Invocation<Void> invocation = post(Void.class, path).header(ClientConstants.HEADER_OS4J_AUTH, IdentityResponses.NO_REAUTH);
        String url = invocation.getRequest().getUrl();
        invocation.header("Authorization", OAuth1Signer.header(params, OAuth1Signer.signature("POST", url, params, consumerSecret, tokenSecret)));
        if (projectId != null)
            invocation.header("Requested-Project-Id", projectId);
        return KeystoneOAuth1Token.parse(IdentityResponses.text(invocation.executeWithResponse()));
    }

    @Override
    public OAuth1Token requestToken(String consumerKey, String consumerSecret, String projectId) {
        Map<String, String> params = oauthParams(consumerKey);
        params.put("oauth_callback", "oob");
        return signedPost("/OS-OAUTH1/request_token", params, Objects.requireNonNull(consumerSecret), "", Objects.requireNonNull(projectId));
    }

    @Override
    @SuppressWarnings("unchecked")
    public String authorize(String requestTokenKey, List<String> roleIds) {
        List<Map<String, String>> roles = roleIds.stream().map(id -> Collections.singletonMap("id", id)).collect(Collectors.toList());
        Map<String, Object> response = put(Map.class, "/OS-OAUTH1/authorize/", Objects.requireNonNull(requestTokenKey))
                .entity(JsonBody.of(Collections.singletonMap("roles", roles))).execute();
        Object token = response == null ? null : response.get("token");
        return token instanceof Map ? (String) ((Map<String, Object>) token).get("oauth_verifier") : null;
    }

    @Override
    public OAuth1Token accessToken(String consumerKey, String consumerSecret, String requestTokenKey, String requestTokenSecret, String verifier) {
        Map<String, String> params = oauthParams(consumerKey);
        params.put("oauth_token", Objects.requireNonNull(requestTokenKey));
        params.put("oauth_verifier", Objects.requireNonNull(verifier));
        return signedPost("/OS-OAUTH1/access_token", params, Objects.requireNonNull(consumerSecret), Objects.requireNonNull(requestTokenSecret), null);
    }

    private static String tokens(String userId) {
        return "/users/" + Objects.requireNonNull(userId) + "/OS-OAUTH1/access_tokens";
    }

    @Override public List<? extends OAuth1AccessToken> listAccessTokens(String userId) { return get(AccessTokens.class, tokens(userId)).execute().getList(); }
    @Override public OAuth1AccessToken getAccessToken(String userId, String tokenId) { return get(KeystoneOAuth1AccessToken.class, tokens(userId), "/", Objects.requireNonNull(tokenId)).execute(); }
    @Override public ActionResponse deleteAccessToken(String userId, String tokenId) { return deleteWithResponse(tokens(userId), "/", Objects.requireNonNull(tokenId)).execute(); }
    @Override public List<? extends Role> accessTokenRoles(String userId, String tokenId) { return get(Roles.class, tokens(userId), "/", Objects.requireNonNull(tokenId), "/roles").execute().getList(); }
    @Override public Role getAccessTokenRole(String userId, String tokenId, String roleId) { return get(KeystoneRole.class, tokens(userId), "/", Objects.requireNonNull(tokenId), "/roles/", Objects.requireNonNull(roleId)).execute(); }
}
