package org.openstack4j.api.identity.v3.ext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.OAuth1Consumer;
import org.openstack4j.model.identity.v3.OAuth1Token;
import org.openstack4j.model.identity.v3.OAuth2AccessToken;
import org.openstack4j.model.identity.v3.RevocationEvent;
import org.openstack4j.openstack.identity.v3.internal.OAuth1Signer;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/OAuth")
public class OAuthTests extends AbstractIdentityExtTest {

    private static Map<String, String> baseParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("oauth_consumer_key", "7fea2d");
        params.put("oauth_nonce", "abc123");
        params.put("oauth_signature_method", "HMAC-SHA1");
        params.put("oauth_timestamp", "1700000000");
        params.put("oauth_version", "1.0");
        return params;
    }

    /** Vectors computed independently with Python hmac/hashlib (RFC 5849 base string). */
    public void signatureMatchesIndependentVector() {
        Map<String, String> request = baseParams();
        request.put("oauth_callback", "oob");
        Assert.assertEquals(OAuth1Signer.signature("POST", "http://127.0.0.1:5000/v3/OS-OAUTH1/request_token", request, "secret1", ""),
                "xd9vbzZ5lHemn1aq0L+A42gPi8w=");

        Map<String, String> access = baseParams();
        access.put("oauth_token", "reqkey");
        access.put("oauth_verifier", "8171");
        Assert.assertEquals(OAuth1Signer.signature("POST", "http://127.0.0.1:5000/v3/OS-OAUTH1/access_token", access, "secret1", "reqsecret"),
                "kPRcl1Abl9xTcBkQWXpKOtuqWuA=");
    }

    public void headerEncodesValues() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("oauth_consumer_key", "a b");
        String header = OAuth1Signer.header(params, "x+y/=");
        Assert.assertEquals(header, "OAuth oauth_consumer_key=\"a%20b\", oauth_signature=\"x%2By%2F%3D\"");
    }

    public void requestTokenParsesFormResponse() throws Exception {
        respondWith(java.util.Collections.singletonMap("Content-Type", "application/x-www-form-urlencoded"), 201,
                "oauth_token=29971f&oauth_token_secret=238eb8&oauth_expires_at=2026-10-03T12%3A00%3A00.000000Z");

        OAuth1Token token = osv3().identity().oauth1().requestToken("7fea2d", "secret1", PROJECT);

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "POST");
        Assert.assertTrue(request.getPath().endsWith("/v3/OS-OAUTH1/request_token"));
        Assert.assertEquals(request.getHeader("Requested-Project-Id"), PROJECT);
        String authorization = request.getHeader("Authorization");
        Assert.assertTrue(authorization.startsWith("OAuth "), authorization);
        Assert.assertTrue(authorization.contains("oauth_consumer_key=\"7fea2d\""), authorization);
        Assert.assertTrue(authorization.contains("oauth_callback=\"oob\""), authorization);
        Assert.assertTrue(authorization.contains("oauth_signature=\""), authorization);
        Assert.assertEquals(token.getKey(), "29971f");
        Assert.assertEquals(token.getSecret(), "238eb8");
        Assert.assertEquals(token.getExpiresAt(), "2026-10-03T12:00:00.000000Z");
    }

    public void authorizeAndAccessToken() throws Exception {
        respondWith(200, "{\"token\": {\"oauth_verifier\": \"8171\"}}");
        respondWith(java.util.Collections.singletonMap("Content-Type", "application/x-www-form-urlencoded"), 201,
                "oauth_token=accesskey&oauth_token_secret=accesssecret&oauth_expires_at=");

        String verifier = osv3().identity().oauth1().authorize("29971f", List.of("r1"));
        OAuth1Token access = osv3().identity().oauth1().accessToken("7fea2d", "secret1", "29971f", "238eb8", verifier);

        RecordedRequest authorize = takeRequest();
        Assert.assertEquals(authorize.getMethod(), "PUT");
        Assert.assertTrue(authorize.getPath().endsWith("/v3/OS-OAUTH1/authorize/29971f"));
        Assert.assertEquals(body(authorize).get("roles").get(0).get("id").asText(), "r1");
        RecordedRequest token = takeRequest();
        Assert.assertTrue(token.getPath().endsWith("/v3/OS-OAUTH1/access_token"));
        String authorization = token.getHeader("Authorization");
        Assert.assertTrue(authorization.contains("oauth_token=\"29971f\"") && authorization.contains("oauth_verifier=\"8171\""), authorization);
        Assert.assertEquals(verifier, "8171");
        Assert.assertEquals(access.getKey(), "accesskey");
        Assert.assertEquals(access.getSecret(), "accesssecret");
    }

    public void consumersAndUserAccessTokens() throws Exception {
        String consumer = "{\"id\": \"9c467b\", \"description\": \"os4j fixture\", \"secret\": \"2fc164\", \"links\": {}}";
        String accessToken = "{\"id\": \"at1\", \"consumer_id\": \"9c467b\", \"project_id\": \"" + PROJECT + "\", \"authorizing_user_id\": \"" + USER + "\", \"expires_at\": null, \"links\": {}}";
        String role = "{\"id\": \"r1\", \"name\": \"member\", \"links\": {}}";
        respondWith(201, "{\"consumer\": " + consumer + "}");
        respondWith(200, "{\"consumers\": [" + consumer + "], \"links\": {}}");
        respondWith(200, "{\"consumer\": " + consumer + "}");
        respondWith(200, "{\"consumer\": " + consumer + "}");
        respondWith(204);
        respondWith(200, "{\"access_tokens\": [" + accessToken + "], \"links\": {}}");
        respondWith(200, "{\"access_token\": " + accessToken + "}");
        respondWith(200, "{\"roles\": [" + role + "], \"links\": {}}");
        respondWith(200, "{\"role\": " + role + "}");
        respondWith(204);

        var oauth1 = osv3().identity().oauth1();
        OAuth1Consumer created = oauth1.createConsumer("os4j fixture");
        Assert.assertEquals(oauth1.listConsumers().size(), 1);
        oauth1.getConsumer("9c467b");
        oauth1.updateConsumer("9c467b", "changed");
        oauth1.deleteConsumer("9c467b");
        Assert.assertEquals(oauth1.listAccessTokens(USER).get(0).getConsumerId(), "9c467b");
        Assert.assertEquals(oauth1.getAccessToken(USER, "at1").getAuthorizingUserId(), USER);
        Assert.assertEquals(oauth1.accessTokenRoles(USER, "at1").get(0).getName(), "member");
        Assert.assertEquals(oauth1.getAccessTokenRole(USER, "at1", "r1").getId(), "r1");
        oauth1.deleteAccessToken(USER, "at1");

        RecordedRequest create = takeRequest();
        Assert.assertTrue(create.getPath().endsWith("/v3/OS-OAUTH1/consumers"));
        Assert.assertEquals(body(create).get("consumer").get("description").asText(), "os4j fixture");
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertTrue(takeRequest().getPath().endsWith("/consumers/9c467b"));
        Assert.assertEquals(body(takeRequest()).get("consumer").get("description").asText(), "changed");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        String tokens = "/v3/users/" + USER + "/OS-OAUTH1/access_tokens";
        Assert.assertTrue(takeRequest().getPath().endsWith(tokens));
        Assert.assertTrue(takeRequest().getPath().endsWith(tokens + "/at1"));
        Assert.assertTrue(takeRequest().getPath().endsWith(tokens + "/at1/roles"));
        Assert.assertTrue(takeRequest().getPath().endsWith(tokens + "/at1/roles/r1"));
        RecordedRequest delete = takeRequest();
        Assert.assertEquals(delete.getMethod(), "DELETE");
        Assert.assertTrue(delete.getPath().endsWith(tokens + "/at1"));
        Assert.assertEquals(created.getSecret(), "2fc164");
    }

    public void oauth2ClientCredentials() throws Exception {
        respondWith(200, "{\"access_token\": \"tok2\", \"token_type\": \"Bearer\", \"expires_in\": 3600}");

        OAuth2AccessToken token = osv3().identity().oauth2().token("client1", "s3cret");

        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/v3/OS-OAUTH2/token"));
        Assert.assertEquals(request.getHeader("Authorization"),
                "Basic " + Base64.getEncoder().encodeToString("client1:s3cret".getBytes(StandardCharsets.UTF_8)));
        Assert.assertTrue(request.getHeader("Content-Type").startsWith("application/x-www-form-urlencoded"), request.getHeader("Content-Type"));
        Assert.assertEquals(request.getBody().readUtf8(), "grant_type=client_credentials");
        Assert.assertEquals(token.getAccessToken(), "tok2");
        Assert.assertEquals(token.getTokenType(), "Bearer");
        Assert.assertEquals(token.getExpiresIn(), Integer.valueOf(3600));
    }

    public void revocationEvents() throws Exception {
        String events = "{\"events\": [{\"project_id\": \"ed6351\", \"issued_before\": \"2026-10-02T00:07:59.000000Z\", \"revoked_at\": \"2026-10-02T00:07:59.000000Z\"},"
                + " {\"audit_id\": \"7OpmWuy8QQKA_K38Sio9sQ\", \"issued_before\": \"2026-10-02T03:43:48.000000Z\", \"revoked_at\": \"2026-10-02T03:43:48.000000Z\"}], \"links\": {}}";
        respondWith(200, events);
        respondWith(200, "{\"events\": [], \"links\": {}}");

        List<? extends RevocationEvent> all = osv3().identity().revocationEvents().list();
        osv3().identity().revocationEvents().list(new Date(1759363200000L));

        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-REVOKE/events"));
        Assert.assertTrue(decodedPath(takeRequest()).endsWith("/v3/OS-REVOKE/events?since=2025-10-02T00:00:00Z"));
        Assert.assertEquals(all.get(0).getProjectId(), "ed6351");
        Assert.assertEquals(all.get(1).getAuditId(), "7OpmWuy8QQKA_K38Sio9sQ");
        Assert.assertNotNull(all.get(1).getRevokedAt());
    }
}
