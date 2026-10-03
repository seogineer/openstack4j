package org.openstack4j.api.identity.v3;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.OAuth1AccessToken;
import org.openstack4j.model.identity.v3.OAuth1Consumer;
import org.openstack4j.model.identity.v3.OAuth1Token;
import org.openstack4j.model.identity.v3.Role;

/**
 * OS-OAUTH1 delegated authentication: consumers, the request/authorize/access token flow and users' access tokens.
 */
public interface OAuth1Service extends RestService {

    /**
     * Lists OAuth1 consumers.
     *
     * @return the result
     */
    List<? extends OAuth1Consumer> listConsumers();

    /**
     * @param id the id
     * @return the result
     */
    OAuth1Consumer getConsumer(String id);

    /**
     * Creates a consumer; its secret is returned only here.
     *
     * @param description the description
     * @return the result
     */
    OAuth1Consumer createConsumer(String description);

    /**
     * @param id the id
     * @param description the description
     * @return the result
     */
    OAuth1Consumer updateConsumer(String id, String description);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse deleteConsumer(String id);

    /**
     * Step 1: obtains a request token for the project, signed with the consumer key and secret (HMAC-SHA1).
     *
     * @param consumerKey the consumer key
     * @param consumerSecret the consumer secret
     * @param projectId the project id
     * @return the result
     */
    OAuth1Token requestToken(String consumerKey, String consumerSecret, String projectId);

    /**
     * Step 2: the user authorizes the request token for the roles; returns the verifier.
     *
     * @param requestTokenKey the request token key
     * @param roleIds the roles to delegate
     * @return the OAuth verifier
     */
    String authorize(String requestTokenKey, List<String> roleIds);

    /**
     * Step 3: exchanges the authorized request token and verifier for an access token.
     *
     * @param consumerKey the consumer key
     * @param consumerSecret the consumer secret
     * @param requestTokenKey the request token key
     * @param requestTokenSecret the request token secret
     * @param verifier the verifier
     * @return the result
     */
    OAuth1Token accessToken(String consumerKey, String consumerSecret, String requestTokenKey, String requestTokenSecret, String verifier);

    /**
     * Lists the OAuth1 access tokens the user authorized.
     *
     * @param userId the user id
     * @return the result
     */
    List<? extends OAuth1AccessToken> listAccessTokens(String userId);

    /**
     * @param userId the user id
     * @param tokenId the token id
     * @return the result
     */
    OAuth1AccessToken getAccessToken(String userId, String tokenId);

    /**
     * @param userId the user id
     * @param tokenId the token id
     * @return the action response
     */
    ActionResponse deleteAccessToken(String userId, String tokenId);

    /**
     * Lists the roles delegated through the access token.
     *
     * @param userId the user id
     * @param tokenId the token id
     * @return the result
     */
    List<? extends Role> accessTokenRoles(String userId, String tokenId);

    /**
     * @param userId the user id
     * @param tokenId the token id
     * @param roleId the role id
     * @return the result
     */
    Role getAccessTokenRole(String userId, String tokenId, String roleId);
}
