package org.openstack4j.api.identity.v3.ext;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.identity.v3.domain.KeystoneAuth;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/AuthMethods")
public class IdentityAuthMethodTests extends AbstractIdentityExtTest {

    private static final String TOKEN = "gAAAAABqwMlkDR4imx9eF";
    /** Application credential token: project-scoped, methods [application_credential]. */
    private static final String APP_CRED_TOKEN = "{\"token\": {\"methods\": [\"application_credential\"], \"user\": {\"domain\": {\"id\": \"default\", \"name\": \"Default\"},"
            + " \"id\": \"" + USER + "\", \"name\": \"admin\", \"password_expires_at\": null}, \"audit_ids\": [\"9YW6EsOqSNuX0I2eleHe-Q\"],"
            + " \"expires_at\": \"2026-10-04T09:22:44.000000Z\", \"issued_at\": \"2026-10-03T09:22:44.000000Z\","
            + " \"project\": {\"domain\": {\"id\": \"default\", \"name\": \"Default\"}, \"id\": \"" + PROJECT + "\", \"name\": \"admin\"},"
            + " \"is_domain\": false, \"roles\": [{\"id\": \"1222de53b11f40e68497383c7636fbe0\", \"name\": \"admin\"}],"
            + " \"application_credential\": {\"id\": \"a05ec0a2f3ac4e8cbd1b3f1c467c0fc5\", \"name\": \"os4j-fixture\", \"restricted\": true},"
            + " \"catalog\": []}}";
    private static final String SYSTEM_TOKEN = "{\"token\": {\"methods\": [\"password\"], \"user\": {\"domain\": {\"id\": \"default\", \"name\": \"Default\"}, \"id\": \"" + USER + "\", \"name\": \"admin\"},"
            + " \"audit_ids\": [\"a\"], \"expires_at\": \"2026-10-04T09:22:44.000000Z\", \"issued_at\": \"2026-10-03T09:22:44.000000Z\","
            + " \"system\": {\"all\": true}, \"roles\": [{\"id\": \"1222de53b11f40e68497383c7636fbe0\", \"name\": \"admin\"}], \"catalog\": []}}";

    private JsonNode authBody() throws Exception {
        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/v3/auth/tokens"), request.getPath());
        return body(request).get("auth");
    }

    public void applicationCredentialById() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, APP_CRED_TOKEN);

        OSClientV3 os = OSFactory.builderV3().endpoint(authURL("/v3")).applicationCredential("a05ec0a2f3ac4e8cbd1b3f1c467c0fc5", "s3cret").authenticate();

        JsonNode auth = authBody();
        Assert.assertEquals(auth.get("identity").get("methods").get(0).asText(), "application_credential");
        JsonNode ac = auth.get("identity").get("application_credential");
        Assert.assertEquals(ac.get("id").asText(), "a05ec0a2f3ac4e8cbd1b3f1c467c0fc5");
        Assert.assertEquals(ac.get("secret").asText(), "s3cret");
        Assert.assertFalse(auth.has("scope"));
        Assert.assertEquals(os.getToken().getProject().getId(), PROJECT);
        Assert.assertEquals(os.getToken().getApplicationCredential().get("name"), "os4j-fixture");
    }

    public void applicationCredentialByNameAndUser() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, APP_CRED_TOKEN);

        OSFactory.builderV3().endpoint(authURL("/v3"))
                .applicationCredential("os4j-fixture", "s3cret", Identifier.byName("admin"), Identifier.byName("Default")).authenticate();

        JsonNode ac = authBody().get("identity").get("application_credential");
        Assert.assertEquals(ac.get("name").asText(), "os4j-fixture");
        Assert.assertEquals(ac.get("user").get("name").asText(), "admin");
        Assert.assertEquals(ac.get("user").get("domain").get("name").asText(), "Default");
        Assert.assertFalse(ac.has("id"));
    }

    public void applicationCredentialTokenKeepsCredentialsForReauth() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, APP_CRED_TOKEN);

        OSClientV3 os = OSFactory.builderV3().endpoint(authURL("/v3")).applicationCredential("a05ec0a2f3ac4e8cbd1b3f1c467c0fc5", "s3cret").authenticate();
        takeRequest();

        Assert.assertTrue(os.getToken().getCredentials() instanceof KeystoneAuth);
        KeystoneAuth stored = (KeystoneAuth) os.getToken().getCredentials();
        Assert.assertEquals(stored.getType(), org.openstack4j.openstack.common.Auth.Type.APPLICATION_CREDENTIAL);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*scope.*")
    public void applicationCredentialWithScopeIsRejected() throws Exception {
        try {
            OSFactory.builderV3().endpoint(authURL("/v3")).applicationCredential("a", "s").scopeToProject(Identifier.byId(PROJECT)).authenticate();
        } finally {
            assertNoMoreRequests();
        }
    }

    public void passwordWithTotpIsMultiFactor() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, SYSTEM_TOKEN.replace("\"system\": {\"all\": true}, ", ""));

        OSFactory.builderV3().endpoint(authURL("/v3")).credentials("admin", "pw", Identifier.byName("Default")).passcode("123456").authenticate();

        JsonNode identity = authBody().get("identity");
        Assert.assertEquals(identity.get("methods").size(), 2);
        Assert.assertEquals(identity.get("methods").get(0).asText(), "password");
        Assert.assertEquals(identity.get("methods").get(1).asText(), "totp");
        Assert.assertEquals(identity.get("password").get("user").get("password").asText(), "pw");
        JsonNode totp = identity.get("totp").get("user");
        Assert.assertEquals(totp.get("name").asText(), "admin");
        Assert.assertEquals(totp.get("domain").get("name").asText(), "Default");
        Assert.assertEquals(totp.get("passcode").asText(), "123456");
    }

    public void totpOnly() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, SYSTEM_TOKEN.replace("\"system\": {\"all\": true}, ", ""));

        OSFactory.builderV3().endpoint(authURL("/v3")).credentials(USER, null).passcode("654321").authenticate();

        JsonNode identity = authBody().get("identity");
        Assert.assertEquals(identity.get("methods").size(), 1);
        Assert.assertEquals(identity.get("methods").get(0).asText(), "totp");
        Assert.assertFalse(identity.has("password"));
        Assert.assertEquals(identity.get("totp").get("user").get("id").asText(), USER);
    }

    public void systemScope() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, SYSTEM_TOKEN);

        OSClientV3 os = OSFactory.builderV3().endpoint(authURL("/v3")).credentials(USER, "pw").scopeToSystem().authenticate();

        Assert.assertTrue(authBody().get("scope").get("system").get("all").asBoolean());
        Assert.assertEquals(os.getToken().getSystem().get("all"), Boolean.TRUE);
    }

    public void trustScope() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, APP_CRED_TOKEN.replace("\"application_credential\"", "\"password\""));

        OSFactory.builderV3().endpoint(authURL("/v3")).token("t0").scopeToTrust("c947c7f50eff44afaaa94782b90fd395").authenticate();

        Assert.assertEquals(authBody().get("scope").get("OS-TRUST:trust").get("id").asText(), "c947c7f50eff44afaaa94782b90fd395");
    }

    public void existingPasswordAuthBodyUnchanged() throws Exception {
        respondWith(tokenHeaders(TOKEN), 201, APP_CRED_TOKEN.replace("\"application_credential\"", "\"password\""));

        OSFactory.builderV3().endpoint(authURL("/v3")).credentials("admin", "pw", Identifier.byName("Default"))
                .scopeToProject(Identifier.byId(PROJECT)).authenticate();

        JsonNode auth = authBody();
        Assert.assertEquals(auth.toString(), "{\"identity\":{\"password\":{\"user\":{\"name\":\"admin\",\"domain\":{\"name\":\"Default\"},\"password\":\"pw\"}},"
                + "\"methods\":[\"password\"]},\"scope\":{\"project\":{\"id\":\"" + PROJECT + "\"}}}");
    }
}
