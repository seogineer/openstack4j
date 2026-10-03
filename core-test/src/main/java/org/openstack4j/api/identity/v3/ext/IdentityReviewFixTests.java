package org.openstack4j.api.identity.v3.ext;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Regressions found in the final review of the identity extensions. */
@Test(suiteName = "Identity/V3/ReviewFixes")
public class IdentityReviewFixTests extends AbstractIdentityExtTest {

    private static final String UNAUTHORIZED = "{\"error\": {\"code\": 401, \"message\": \"Invalid signature\", \"title\": \"Unauthorized\"}}";

    private void assertFailsWithoutReauthentication(Runnable call) throws Exception {
        respondWith(401, UNAUTHORIZED);
        try {
            call.run();
            Assert.fail("expected the 401 to surface");
        } catch (RuntimeException expected) {
            Assert.assertFalse(expected instanceof NullPointerException, "re-authentication was attempted: " + expected);
        }
        takeRequest();
        assertNoMoreRequests();
    }

    public void oauth1RequestToken401IsNotRetried() throws Exception {
        assertFailsWithoutReauthentication(() -> osv3().identity().oauth1().requestToken("k", "s", PROJECT));
    }

    public void oauth1AccessToken401IsNotRetried() throws Exception {
        assertFailsWithoutReauthentication(() -> osv3().identity().oauth1().accessToken("k", "s", "rk", "rs", "v"));
    }

    public void oauth2Token401IsNotRetried() throws Exception {
        assertFailsWithoutReauthentication(() -> osv3().identity().oauth2().token("client", "bad"));
    }

    public void saml2Assertion401IsNotRetried() throws Exception {
        assertFailsWithoutReauthentication(() -> osv3().identity().federation().saml2Assertion("tok", "sp1"));
    }

    public void federatedToken401IsNotRetried() throws Exception {
        assertFailsWithoutReauthentication(() -> osv3().identity().federation().federatedToken("acme", "saml2", Map.of()));
    }

    /** Real Keystone (3.14) wraps option reads: {"config": {"driver": "sql"}}. */
    public void configOptionReadsConfigWrapper() throws Exception {
        respondWith(200, "{\"config\": {\"driver\": \"sql\"}}");
        respondWith(200, "{\"config\": {\"url\": \"ldap://ldap.example.com\"}}");
        Assert.assertEquals(osv3().identity().domains().defaultConfigOption("identity", "driver"), "sql");
        Assert.assertEquals(osv3().identity().domains().configOption("d1", "ldap", "url"), "ldap://ldap.example.com");
        takeRequest();
        takeRequest();
    }

    /** Real Keystone (3.14) nests group reads: {"config": {"ldap": {...}}}. */
    public void configGroupReadsNestedGroup() throws Exception {
        respondWith(200, "{\"config\": {\"ldap\": {\"url\": \"ldap://localhost\", \"user\": null}}}");
        respondWith(200, "{\"config\": {\"ldap\": {\"url\": \"ldap://ldap.example.com\"}}}");
        respondWith(200, "{\"config\": {\"ldap\": {\"url\": \"ldap://ldap2.example.com\"}}}");
        Map<String, Object> defaults = osv3().identity().domains().defaultConfigGroup("ldap");
        Assert.assertEquals(defaults.get("url"), "ldap://localhost");
        Assert.assertTrue(defaults.containsKey("user"));
        Assert.assertEquals(osv3().identity().domains().configGroup("d1", "ldap").get("url"), "ldap://ldap.example.com");
        Assert.assertEquals(osv3().identity().domains().updateConfigGroup("d1", "ldap", Map.of("url", "ldap://ldap2.example.com")).get("url"),
                "ldap://ldap2.example.com");
        takeRequest();
        takeRequest();
        takeRequest();
    }
}
