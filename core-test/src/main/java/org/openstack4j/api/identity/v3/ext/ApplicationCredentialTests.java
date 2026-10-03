package org.openstack4j.api.identity.v3.ext;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.AccessRule;
import org.openstack4j.model.identity.v3.ApplicationCredential;
import org.openstack4j.model.identity.v3.options.ApplicationCredentialCreate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/ApplicationCredentials")
public class ApplicationCredentialTests extends AbstractIdentityExtTest {

    private static final String AC = "a05ec0a2f3ac4e8cbd1b3f1c467c0fc5";
    private static final String AC_JSON = "{\"id\": \"" + AC + "\", \"name\": \"os4j-fixture\", \"description\": \"fixture\", \"user_id\": \"" + USER + "\","
            + " \"project_id\": \"" + PROJECT + "\", \"system\": null, \"expires_at\": null, \"unrestricted\": null,"
            + " \"roles\": [{\"id\": \"1222de53b11f40e68497383c7636fbe0\", \"name\": \"admin\", \"domain_id\": null}],"
            + " \"access_rules\": [{\"id\": \"8bff75d3c41a426f9e29440584030920\", \"service\": \"compute\", \"path\": \"/v2.1/servers\", \"method\": \"GET\"}],"
            + " \"links\": {\"self\": \"http://127.0.0.1:5000/v3/users/" + USER + "/application_credentials/" + AC + "\"}";

    public void createListGetDelete() throws Exception {
        respondWith(201, "{\"application_credential\": " + AC_JSON + ", \"secret\": \"generated-secret\"}}");
        respondWith(200, "{\"application_credentials\": [" + AC_JSON + "}], \"links\": {}}");
        respondWith(200, "{\"application_credentials\": [], \"links\": {}}");
        respondWith(200, "{\"application_credential\": " + AC_JSON + "}}");
        respondWith(204);

        ApplicationCredential created = osv3().identity().applicationCredentials().create(USER, ApplicationCredentialCreate.create("os4j-fixture")
                .description("fixture").roleNames("admin").accessRule("compute", "GET", "/v2.1/servers"));
        List<? extends ApplicationCredential> all = osv3().identity().applicationCredentials().list(USER);
        osv3().identity().applicationCredentials().list(USER, "missing");
        ApplicationCredential one = osv3().identity().applicationCredentials().get(USER, AC);
        boolean deleted = osv3().identity().applicationCredentials().delete(USER, AC).isSuccess();

        RecordedRequest create = takeRequest();
        Assert.assertTrue(create.getPath().endsWith("/v3/users/" + USER + "/application_credentials"));
        JsonNode body = body(create).get("application_credential");
        Assert.assertEquals(body.get("name").asText(), "os4j-fixture");
        Assert.assertEquals(body.get("roles").get(0).get("name").asText(), "admin");
        Assert.assertEquals(body.get("access_rules").get(0).get("path").asText(), "/v2.1/servers");
        Assert.assertFalse(body.has("secret"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/application_credentials"));
        Assert.assertTrue(decodedPath(takeRequest()).endsWith("/application_credentials?name=missing"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/application_credentials/" + AC));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getSecret(), "generated-secret");
        Assert.assertEquals(created.getRoles().get(0).getName(), "admin");
        Assert.assertEquals(all.get(0).getAccessRules().get(0).getService(), "compute");
        Assert.assertNull(one.getSecret());
        Assert.assertEquals(one.getProjectId(), PROJECT);
        Assert.assertTrue(deleted);
    }

    public void accessRules() throws Exception {
        respondWith(200, "{\"access_rules\": [{\"id\": \"8bff75d3c41a426f9e29440584030920\", \"service\": \"compute\", \"path\": \"/v2.1/servers\", \"method\": \"GET\", \"links\": {}}], \"links\": {}}");
        respondWith(200, "{\"access_rule\": {\"id\": \"8bff75d3c41a426f9e29440584030920\", \"service\": \"compute\", \"path\": \"/v2.1/servers\", \"method\": \"GET\", \"links\": {}}}");
        respondWith(204);

        List<? extends AccessRule> rules = osv3().identity().users().accessRules(USER);
        AccessRule rule = osv3().identity().users().getAccessRule(USER, "8bff75d3c41a426f9e29440584030920");
        boolean deleted = osv3().identity().users().deleteAccessRule(USER, "8bff75d3c41a426f9e29440584030920").isSuccess();

        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/users/" + USER + "/access_rules"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/access_rules/8bff75d3c41a426f9e29440584030920"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(rules.get(0).getMethod(), "GET");
        Assert.assertEquals(rule.getPath(), "/v2.1/servers");
        Assert.assertTrue(deleted);
    }
}
