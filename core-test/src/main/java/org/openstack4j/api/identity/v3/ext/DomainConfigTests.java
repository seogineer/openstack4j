package org.openstack4j.api.identity.v3.ext;

import java.util.Collections;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/DomainConfig")
public class DomainConfigTests extends AbstractIdentityExtTest {

    private static final String CONFIG = "{\"config\": {\"identity\": {\"driver\": \"ldap\"}, \"ldap\": {\"url\": \"ldap://ldap.example.com\", \"user_tree_dn\": \"ou=Users,dc=example,dc=com\"}}}";

    public void domainConfigLifecycle() throws Exception {
        respondWith(201, CONFIG);
        respondWith(200, CONFIG);
        respondWith(200, "{\"config\": {\"url\": \"ldap://ldap.example.com\", \"user_tree_dn\": \"ou=Users,dc=example,dc=com\"}}");
        respondWith(200, "{\"url\": \"ldap://ldap.example.com\"}");
        respondWith(200, CONFIG);
        respondWith(200, "{\"config\": {\"url\": \"ldap://ldap2.example.com\"}}");
        respondWith(200, "{\"config\": {\"url\": \"ldap://ldap3.example.com\"}}");
        respondWith(204);
        respondWith(204);
        respondWith(204);

        var domains = osv3().identity().domains();
        Map<String, Map<String, Object>> created = domains.createConfig("d1", Map.of("identity", Map.of("driver", "ldap")));
        Map<String, Map<String, Object>> all = domains.config("d1");
        Map<String, Object> ldap = domains.configGroup("d1", "ldap");
        Object url = domains.configOption("d1", "ldap", "url");
        domains.updateConfig("d1", Map.of("ldap", Map.of("url", "ldap://ldap.example.com")));
        Map<String, Object> group = domains.updateConfigGroup("d1", "ldap", Map.of("url", "ldap://ldap2.example.com"));
        Object option = domains.updateConfigOption("d1", "ldap", "url", "ldap://ldap3.example.com");
        domains.deleteConfigOption("d1", "ldap", "url");
        domains.deleteConfigGroup("d1", "ldap");
        domains.deleteConfig("d1");

        RecordedRequest create = takeRequest();
        Assert.assertEquals(create.getMethod(), "PUT");
        Assert.assertTrue(create.getPath().endsWith("/v3/domains/d1/config"));
        Assert.assertEquals(body(create).get("config").get("identity").get("driver").asText(), "ldap");
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertTrue(takeRequest().getPath().endsWith("/config/ldap"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/config/ldap/url"));
        Assert.assertEquals(takeRequest().getMethod(), "PATCH");
        RecordedRequest patchGroup = takeRequest();
        Assert.assertEquals(body(patchGroup).get("config").get("url").asText(), "ldap://ldap2.example.com");
        RecordedRequest patchOption = takeRequest();
        Assert.assertTrue(patchOption.getPath().endsWith("/config/ldap/url"));
        Assert.assertEquals(body(patchOption).get("config").get("url").asText(), "ldap://ldap3.example.com");
        for (String suffix : new String[] {"/config/ldap/url", "/config/ldap", "/config"}) {
            RecordedRequest r = takeRequest();
            Assert.assertEquals(r.getMethod(), "DELETE");
            Assert.assertTrue(r.getPath().endsWith("/v3/domains/d1" + suffix), r.getPath());
        }
        Assert.assertEquals(created.get("identity").get("driver"), "ldap");
        Assert.assertEquals(all.get("ldap").get("url"), "ldap://ldap.example.com");
        Assert.assertEquals(ldap.get("user_tree_dn"), "ou=Users,dc=example,dc=com");
        Assert.assertEquals(url, "ldap://ldap.example.com");
        Assert.assertEquals(group.get("url"), "ldap://ldap2.example.com");
        Assert.assertEquals(option, "ldap://ldap3.example.com");
    }

    public void defaultConfig() throws Exception {
        respondWith(200, "{\"config\": {\"identity\": {\"driver\": \"sql\", \"list_limit\": null}, \"ldap\": {\"url\": \"ldap://localhost\", \"user\": null}}}");
        respondWith(200, "{\"config\": {\"url\": \"ldap://localhost\", \"user\": null}}");
        respondWith(200, "{\"driver\": \"sql\"}");

        Map<String, Map<String, Object>> all = osv3().identity().domains().defaultConfig();
        Map<String, Object> ldap = osv3().identity().domains().defaultConfigGroup("ldap");
        Object driver = osv3().identity().domains().defaultConfigOption("identity", "driver");

        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/domains/config/default"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/domains/config/ldap/default"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/domains/config/identity/driver/default"));
        Assert.assertEquals(all.get("identity").get("driver"), "sql");
        Assert.assertTrue(all.get("identity").containsKey("list_limit"));
        Assert.assertEquals(ldap.get("url"), "ldap://localhost");
        Assert.assertEquals(driver, "sql");
    }
}
