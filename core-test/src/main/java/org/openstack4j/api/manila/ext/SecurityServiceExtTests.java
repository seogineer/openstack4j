package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.model.manila.SecurityService;
import org.openstack4j.model.manila.ext.options.SecurityServiceOptions;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@Test(suiteName = "Manila/SecurityServiceExt")
public class SecurityServiceExtTests extends AbstractManilaExtTest {

    private static final String P = "/v2/b80f8d4e28b74188858b654cb1fccf7d";
    private static final String SS = "{\"id\": \"ss1\", \"type\": \"active_directory\", \"name\": \"ad\", \"ou\": \"OU=Shares,DC=example,DC=com\","
            + " \"dns_ip\": \"10.0.0.2\", \"status\": \"new\"}";

    @AfterMethod
    public void clearMicroVersions() {
        osv3().share().microVersions().clear();
    }

    public void createWithOu() throws Exception {
        respondWith(200, "{\"security_service\": " + SS + "}");

        SecurityService created = osv3().share().securityServicesExt().create(SecurityServiceOptions.create()
                .type(SecurityService.Type.ACTIVE_DIRECTORY).name("ad").dnsIp("10.0.0.2").ou("OU=Shares,DC=example,DC=com"));

        RecordedRequest request = expect("POST", P + "/security-services");
        Assert.assertEquals(request.getHeader("X-OpenStack-Manila-API-Version"), "2.44");
        Assert.assertEquals(body(request).toString(),
                "{\"security_service\":{\"type\":\"active_directory\",\"name\":\"ad\",\"dns_ip\":\"10.0.0.2\",\"ou\":\"OU=Shares,DC=example,DC=com\"}}");
        Assert.assertEquals(created.getOu(), "OU=Shares,DC=example,DC=com");
        Assert.assertEquals(created.getType(), SecurityService.Type.ACTIVE_DIRECTORY);
    }

    public void updateOu() throws Exception {
        respondWith(200, "{\"security_service\": " + SS + "}");

        SecurityService updated = osv3().share().securityServicesExt().update("ss1", SecurityServiceOptions.create().ou("OU=Shares,DC=example,DC=com"));

        RecordedRequest request = expect("PUT", P + "/security-services/ss1");
        Assert.assertEquals(request.getHeader("X-OpenStack-Manila-API-Version"), "2.44");
        Assert.assertEquals(body(request).toString(), "{\"security_service\":{\"ou\":\"OU=Shares,DC=example,DC=com\"}}");
        Assert.assertEquals(updated.getOu(), "OU=Shares,DC=example,DC=com");
    }

    public void getAndGet404Null() throws Exception {
        respondWith(200, "{\"security_service\": " + SS + "}");
        respondWith(404, "{\"itemNotFound\": {\"message\": \"not found\", \"code\": 404}}");

        var ext = osv3().share().securityServicesExt();
        SecurityService one = ext.get("ss1");
        SecurityService missing = ext.get("nope");

        Assert.assertEquals(expect("GET", P + "/security-services/ss1").getHeader("X-OpenStack-Manila-API-Version"), "2.44");
        expect("GET", P + "/security-services/nope");
        Assert.assertEquals(one.getName(), "ad");
        Assert.assertNull(missing);
    }

    public void listDetailsFilters() throws Exception {
        respondWith(200, "{\"security_services\": [" + SS + "]}");

        List<? extends SecurityService> list = osv3().share().securityServicesExt().listDetails(Map.of("name", "x"));

        Assert.assertEquals(expect("GET", P + "/security-services/detail?name=x").getHeader("X-OpenStack-Manila-API-Version"), "2.44");
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0).getOu(), "OU=Shares,DC=example,DC=com");
    }

    public void legacyCreateStaysAt26WithoutOu() throws Exception {
        respondWith(200, "{\"security_service\": {\"id\": \"ss2\", \"type\": \"ldap\", \"name\": \"l\"}}");

        SecurityService created = osv3().share().securityServices().create(
                Builders.securityService().name("l").type(SecurityService.Type.LDAP).build());

        RecordedRequest request = expect("POST", "/security-services");
        Assert.assertEquals(request.getHeader("X-Openstack-Manila-Api-Version"), "2.6");
        Assert.assertFalse(request.getBody().clone().readUtf8().contains("\"ou\""));
        Assert.assertNull(created.getOu());
    }
}
