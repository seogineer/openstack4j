package org.openstack4j.api.identity.v3.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.identity.v3.ApplicationCredential;
import org.openstack4j.model.identity.v3.EndpointGroup;
import org.openstack4j.model.identity.v3.RegisteredLimit;
import org.openstack4j.model.identity.v3.Service;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.model.identity.v3.Trust;
import org.openstack4j.model.identity.v3.options.ApplicationCredentialCreate;
import org.openstack4j.model.identity.v3.options.RegisteredLimitCreate;
import org.openstack4j.model.identity.v3.options.TrustCreate;
import org.openstack4j.openstack.OSFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Runs against a real Keystone when OS_AUTH_URL is set; every temporary resource is removed in finally. */
@Test(suiteName = "Identity/V3/Live", groups = "identity-live", singleThreaded = true)
public class IdentityExtensionsLiveTests {

    private String authUrl;
    private String user;
    private String password;
    private String project;
    private String domain;
    private String token;
    private OSClientV3 os;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live identity tests");
        authUrl = url.replaceAll("/+$", "").endsWith("/v3") ? url.replaceAll("/+$", "") : url.replaceAll("/+$", "") + "/v3";
        token = System.getenv("OS_TOKEN");
        project = env("OS_PROJECT_NAME", null);
        domain = env("OS_USER_DOMAIN_NAME", "Default");
        if (token != null && !token.isEmpty()) {
            // an existing token instead of a password (keeps passwords out of the environment)
            os = OSFactory.builderV3().endpoint(authUrl).token(token)
                    .scopeToProject(Identifier.byName(project), Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default"))).authenticate();
            return;
        }
        user = env("OS_USERNAME", null);
        password = env("OS_PASSWORD", null);
        os = OSFactory.builderV3().endpoint(authUrl).credentials(user, password, Identifier.byName(domain))
                .scopeToProject(Identifier.byName(project), Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default"))).authenticate();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live identity tests");
        }
        return value;
    }

    private OSClientV3 session() {
        return OSFactory.clientFromToken(os.getToken());
    }

    public void applicationCredentialAuthenticatesAndCallsApi() {
        OSClientV3 client = session();
        String userId = client.getToken().getUser().getId();
        ApplicationCredential credential = client.identity().applicationCredentials().create(userId,
                ApplicationCredentialCreate.create("os4j-live-" + System.nanoTime()).description("openstack4j live test"));
        final String credentialId = credential.getId();
        try {
            Assert.assertNotNull(credential.getSecret());
            OSClientV3 viaCredential = OSFactory.builderV3().endpoint(authUrl).applicationCredential(credentialId, credential.getSecret()).authenticate();
            Assert.assertEquals(viaCredential.getToken().getApplicationCredential().get("id"), credentialId);
            Assert.assertNotNull(viaCredential.getToken().getProject());
            Assert.assertFalse(viaCredential.identity().serviceEndpoints().list().isEmpty());
        } finally {
            // authenticate() made the credential's client current; switch back so an app-credential token does not delete itself
            Assert.assertTrue(session().identity().applicationCredentials().delete(userId, credentialId).isSuccess(), "application credential not deleted");
        }
    }

    public void systemScope() {
        OSClientV3 system = token != null && !token.isEmpty()
                ? OSFactory.builderV3().endpoint(authUrl).token(token).scopeToSystem().authenticate()
                : OSFactory.builderV3().endpoint(authUrl).credentials(user, password, Identifier.byName(domain)).scopeToSystem().authenticate();
        Assert.assertEquals(system.getToken().getSystem().get("all"), Boolean.TRUE);
        Assert.assertFalse(session().identity().tokens().getSystemScopes(os.getToken().getId()).isEmpty());
    }

    public void projectTags() {
        OSClientV3 client = session();
        String projectId = client.getToken().getProject().getId();
        String tag = "os4j live " + System.nanoTime();
        try {
            Assert.assertTrue(client.identity().projects().addTag(projectId, tag).isSuccess());
            Assert.assertTrue(client.identity().projects().tags(projectId).contains(tag));
            Assert.assertTrue(client.identity().projects().hasTag(projectId, tag).isSuccess());
        } finally {
            client.identity().projects().removeTag(projectId, tag);
        }
        Assert.assertFalse(client.identity().projects().hasTag(projectId, tag).isSuccess());
    }

    public void endpointGroup() {
        OSClientV3 client = session();
        EndpointGroup group = client.identity().endpointFilter().createEndpointGroup("os4j-live", "openstack4j live test", Map.of("interface", "public"));
        try {
            Assert.assertEquals(client.identity().endpointFilter().getEndpointGroup(group.getId()).getFilters().get("interface"), "public");
        } finally {
            client.identity().endpointFilter().deleteEndpointGroup(group.getId());
        }
    }

    public void registeredLimit() {
        OSClientV3 client = session();
        String computeId = client.identity().serviceEndpoints().list().stream().filter(s -> "compute".equals(s.getType()))
                .map(Service::getId).findFirst().orElseThrow(() -> new SkipException("no compute service"));
        List<? extends RegisteredLimit> created = client.identity().registeredLimits().create(List.of(
                RegisteredLimitCreate.create(computeId, "os4j_live_" + System.nanoTime(), 5).description("openstack4j live test")));
        try {
            Assert.assertEquals(client.identity().registeredLimits().get(created.get(0).getId()).getDefaultLimit(), Integer.valueOf(5));
        } finally {
            client.identity().registeredLimits().delete(created.get(0).getId());
        }
    }

    public void trustToSelf() {
        OSClientV3 client = session();
        Token token = client.getToken();
        Trust trust = client.identity().trusts().create(TrustCreate.create(token.getUser().getId(), token.getUser().getId(), false)
                .projectId(token.getProject().getId()).roleNames(token.getRoles().get(0).getName()));
        try {
            Assert.assertEquals(client.identity().trusts().get(trust.getId()).getTrusteeUserId(), token.getUser().getId());
            Assert.assertFalse(client.identity().trusts().roles(trust.getId()).isEmpty());
        } finally {
            client.identity().trusts().delete(trust.getId());
        }
    }

    public void federationObjects() {
        OSClientV3 client = session();
        var federation = client.identity().federation();
        String id = "os4j-live-" + System.nanoTime();
        federation.mappings().create(id, List.of(Map.of("local", List.of(Map.of("user", Map.of("name", "{0}"))), "remote", List.of(Map.of("type", "REMOTE_USER")))));
        try {
            federation.identityProviders().create(id, Map.of("enabled", false));
            try {
                federation.identityProviders().createProtocol(id, "saml2", id);
                Assert.assertEquals(federation.identityProviders().getProtocol(id, "saml2").getMappingId(), id);
                federation.identityProviders().deleteProtocol(id, "saml2");
            } finally {
                federation.identityProviders().delete(id);
            }
        } finally {
            federation.mappings().delete(id);
        }
    }

    public void oauth1Consumer() {
        OSClientV3 client = session();
        var consumer = client.identity().oauth1().createConsumer("openstack4j live test");
        try {
            Assert.assertNotNull(consumer.getSecret());
            Assert.assertEquals(client.identity().oauth1().getConsumer(consumer.getId()).getDescription(), "openstack4j live test");
        } finally {
            client.identity().oauth1().deleteConsumer(consumer.getId());
        }
    }

    public void readOnlyListings() {
        OSClientV3 client = session();
        Assert.assertNotNull(client.identity().roles().listRoleInferences());
        Assert.assertNotNull(client.identity().limits().model().getName());
        Assert.assertNotNull(client.identity().revocationEvents().list());
        Assert.assertFalse(client.identity().domains().defaultConfig().isEmpty());
        Assert.assertNotNull(client.identity().domains().defaultConfigOption("identity", "driver"));
        Assert.assertTrue(client.identity().domains().defaultConfigGroup("ldap").containsKey("url"));
    }
}
