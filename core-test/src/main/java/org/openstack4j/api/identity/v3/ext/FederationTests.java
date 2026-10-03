package org.openstack4j.api.identity.v3.ext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.IdentityProvider;
import org.openstack4j.model.identity.v3.Mapping;
import org.openstack4j.model.identity.v3.ServiceProvider;
import org.openstack4j.model.identity.v3.Token;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/Federation")
public class FederationTests extends AbstractIdentityExtTest {

    private static final String IDP_JSON = "{\"id\": \"os4j-fixture\", \"domain_id\": \"f9381a82bc9a4805bc5c4370f3215857\", \"enabled\": false,"
            + " \"description\": \"fixture\", \"remote_ids\": [\"https://idp.example.com/os4j\"], \"authorization_ttl\": null, \"links\": {}}";
    private static final String MAPPING_JSON = "{\"id\": \"os4j-fixture\", \"rules\": [{\"local\": [{\"user\": {\"name\": \"{0}\"}}],"
            + " \"remote\": [{\"type\": \"REMOTE_USER\"}]}], \"schema_version\": \"1.0\", \"links\": {}}";
    private static final String PROTOCOL_JSON = "{\"id\": \"saml2\", \"mapping_id\": \"os4j-fixture\", \"links\": {}}";
    private static final String SP_JSON = "{\"id\": \"sp1\", \"auth_url\": \"https://sp/v3/OS-FEDERATION/identity_providers/acme/protocols/saml2/auth\","
            + " \"sp_url\": \"https://sp/Shibboleth.sso/SAML2/ECP\", \"description\": null, \"enabled\": true, \"relay_state_prefix\": \"ss:mem:\", \"links\": {}}";

    public void identityProvidersAndProtocols() throws Exception {
        respondWith(201, "{\"identity_provider\": " + IDP_JSON + "}");
        respondWith(200, "{\"identity_providers\": [" + IDP_JSON + "], \"links\": {}}");
        respondWith(200, "{\"identity_provider\": " + IDP_JSON + "}");
        respondWith(200, "{\"identity_provider\": " + IDP_JSON + "}");
        respondWith(201, "{\"protocol\": " + PROTOCOL_JSON + "}");
        respondWith(200, "{\"protocols\": [" + PROTOCOL_JSON + "], \"links\": {}}");
        respondWith(200, "{\"protocol\": " + PROTOCOL_JSON + "}");
        respondWith(200, "{\"protocol\": " + PROTOCOL_JSON + "}");
        respondWith(204);
        respondWith(204);

        var idps = osv3().identity().federation().identityProviders();
        IdentityProvider created = idps.create("os4j-fixture", Map.of("enabled", false, "remote_ids", List.of("https://idp.example.com/os4j")));
        List<? extends IdentityProvider> all = idps.list();
        idps.get("os4j-fixture");
        idps.update("os4j-fixture", Map.of("description", "changed"));
        idps.createProtocol("os4j-fixture", "saml2", "os4j-fixture");
        Assert.assertEquals(idps.protocols("os4j-fixture").get(0).getMappingId(), "os4j-fixture");
        idps.getProtocol("os4j-fixture", "saml2");
        idps.updateProtocol("os4j-fixture", "saml2", "other");
        idps.deleteProtocol("os4j-fixture", "saml2");
        idps.delete("os4j-fixture");

        String idp = "/v3/OS-FEDERATION/identity_providers/os4j-fixture";
        RecordedRequest create = takeRequest();
        Assert.assertEquals(create.getMethod(), "PUT");
        Assert.assertTrue(create.getPath().endsWith(idp));
        Assert.assertEquals(body(create).get("identity_provider").get("remote_ids").get(0).asText(), "https://idp.example.com/os4j");
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-FEDERATION/identity_providers"));
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertEquals(takeRequest().getMethod(), "PATCH");
        RecordedRequest protocol = takeRequest();
        Assert.assertEquals(protocol.getMethod(), "PUT");
        Assert.assertTrue(protocol.getPath().endsWith(idp + "/protocols/saml2"));
        Assert.assertEquals(body(protocol).get("protocol").get("mapping_id").asText(), "os4j-fixture");
        Assert.assertTrue(takeRequest().getPath().endsWith(idp + "/protocols"));
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertEquals(body(takeRequest()).get("protocol").get("mapping_id").asText(), "other");
        Assert.assertTrue(takeRequest().getPath().endsWith(idp + "/protocols/saml2"));
        Assert.assertTrue(takeRequest().getPath().endsWith(idp));
        Assert.assertFalse(created.isEnabled());
        Assert.assertEquals(created.getRemoteIds(), Arrays.asList("https://idp.example.com/os4j"));
        Assert.assertNull(all.get(0).getAuthorizationTtl());
    }

    public void mappingsAndServiceProviders() throws Exception {
        respondWith(201, "{\"mapping\": " + MAPPING_JSON + "}");
        respondWith(200, "{\"mappings\": [" + MAPPING_JSON + "], \"links\": {}}");
        respondWith(200, "{\"mapping\": " + MAPPING_JSON + "}");
        respondWith(200, "{\"mapping\": " + MAPPING_JSON + "}");
        respondWith(204);
        respondWith(201, "{\"service_provider\": " + SP_JSON + "}");
        respondWith(200, "{\"service_providers\": [" + SP_JSON + "], \"links\": {}}");
        respondWith(200, "{\"service_provider\": " + SP_JSON + "}");
        respondWith(200, "{\"service_provider\": " + SP_JSON + "}");
        respondWith(204);

        List<Map<String, Object>> rules = List.of(Map.of("local", List.of(Map.of("user", Map.of("name", "{0}"))), "remote", List.of(Map.of("type", "REMOTE_USER"))));
        var mappings = osv3().identity().federation().mappings();
        Mapping mapping = mappings.create("os4j-fixture", rules);
        Assert.assertEquals(mappings.list().size(), 1);
        mappings.get("os4j-fixture");
        mappings.update("os4j-fixture", rules);
        mappings.delete("os4j-fixture");
        var sps = osv3().identity().federation().serviceProviders();
        ServiceProvider sp = sps.create("sp1", Map.of("auth_url", "https://sp/auth", "sp_url", "https://sp/ecp"));
        Assert.assertEquals(sps.list().size(), 1);
        sps.get("sp1");
        sps.update("sp1", Map.of("enabled", false));
        sps.delete("sp1");

        RecordedRequest create = takeRequest();
        Assert.assertEquals(create.getMethod(), "PUT");
        Assert.assertTrue(create.getPath().endsWith("/v3/OS-FEDERATION/mappings/os4j-fixture"));
        JsonNode rule = body(create).get("mapping").get("rules").get(0);
        Assert.assertEquals(rule.get("remote").get(0).get("type").asText(), "REMOTE_USER");
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-FEDERATION/mappings"));
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertEquals(takeRequest().getMethod(), "PATCH");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        RecordedRequest spCreate = takeRequest();
        Assert.assertTrue(spCreate.getPath().endsWith("/v3/OS-FEDERATION/service_providers/sp1"));
        Assert.assertEquals(body(spCreate).get("service_provider").get("sp_url").asText(), "https://sp/ecp");
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-FEDERATION/service_providers"));
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertFalse(body(takeRequest()).get("service_provider").get("enabled").asBoolean());
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(mapping.getRules().get(0).get("remote"), List.of(Map.of("type", "REMOTE_USER")));
        Assert.assertEquals(mapping.getSchemaVersion(), "1.0");
        Assert.assertEquals(sp.getRelayStatePrefix(), "ss:mem:");
        Assert.assertTrue(sp.isEnabled());
    }

    public void saml2MetadataIsReturnedAsXml() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>\n<ns0:EntityDescriptor xmlns:ns0=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"k2k\"/>";
        respondWith(Collections.singletonMap("Content-Type", "text/xml"), 200, xml);
        respondWith(Collections.singletonMap("Content-Type", "text/xml"), 200, "<samlp:Response/>");
        respondWith(Collections.singletonMap("Content-Type", "text/xml"), 200, "<soap11:Envelope/>");

        Assert.assertEquals(osv3().identity().federation().saml2Metadata(), xml);
        Assert.assertEquals(osv3().identity().federation().saml2Assertion("tok", "sp1"), "<samlp:Response/>");
        Assert.assertEquals(osv3().identity().federation().ecpAssertion("tok", "sp1"), "<soap11:Envelope/>");

        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-FEDERATION/saml2/metadata"));
        RecordedRequest saml = takeRequest();
        Assert.assertTrue(saml.getPath().endsWith("/v3/auth/OS-FEDERATION/saml2"));
        JsonNode auth = body(saml).get("auth");
        Assert.assertEquals(auth.get("identity").get("methods").get(0).asText(), "token");
        Assert.assertEquals(auth.get("identity").get("token").get("id").asText(), "tok");
        Assert.assertEquals(auth.get("scope").get("service_provider").get("id").asText(), "sp1");
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/auth/OS-FEDERATION/saml2/ecp"));
    }

    public void saml2ErrorIsRaised() throws Exception {
        respondWith(404, "{\"error\": {\"code\": 404, \"message\": \"not found\", \"title\": \"Not Found\"}}");
        try {
            osv3().identity().federation().saml2Metadata();
            Assert.fail("expected an exception");
        } catch (RuntimeException expected) {
            Assert.assertTrue(expected.getMessage() != null);
        }
        takeRequest();
    }

    public void projectsDomainsAndFederatedToken() throws Exception {
        respondWith(200, "{\"projects\": [{\"id\": \"" + PROJECT + "\", \"name\": \"admin\", \"links\": {}}], \"links\": {}}");
        respondWith(200, "{\"domains\": [{\"id\": \"default\", \"name\": \"Default\", \"links\": {}}], \"links\": {}}");
        respondWith(Collections.singletonMap("X-Subject-Token", "fedtok"), 201,
                "{\"token\": {\"methods\": [\"saml2\"], \"user\": {\"id\": \"u1\", \"name\": \"fed\", \"OS-FEDERATION\": {\"identity_provider\": {\"id\": \"acme\"}, \"protocol\": {\"id\": \"saml2\"}, \"groups\": []}},"
                        + " \"expires_at\": \"2026-10-03T12:00:00.000000Z\", \"issued_at\": \"2026-10-03T11:00:00.000000Z\", \"audit_ids\": [\"a\"]}}");

        Assert.assertEquals(osv3().identity().federation().projects().get(0).getId(), PROJECT);
        Assert.assertEquals(osv3().identity().federation().domains().get(0).getName(), "Default");
        Token token = osv3().identity().federation().federatedToken("acme", "saml2", Map.of("REMOTE_USER", "fed"));

        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-FEDERATION/projects"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-FEDERATION/domains"));
        RecordedRequest auth = takeRequest();
        Assert.assertTrue(auth.getPath().endsWith("/v3/OS-FEDERATION/identity_providers/acme/protocols/saml2/auth"));
        Assert.assertEquals(auth.getHeader("REMOTE_USER"), "fed");
        Assert.assertEquals(token.getId(), "fedtok");
        Assert.assertEquals(token.getUser().getName(), "fed");
    }
}
