package org.openstack4j.api.identity.v3.ext;

import java.util.Arrays;
import java.util.List;

import org.openstack4j.model.identity.v3.Domain;
import org.openstack4j.model.identity.v3.Project;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.model.identity.v3.User;
import org.openstack4j.model.identity.v3.options.ProjectListOptions;
import org.openstack4j.model.identity.v3.options.UserListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/Models")
public class IdentityModelTests extends AbstractIdentityExtTest {

    public void userReadsOptionsAndPasswordExpiry() throws Exception {
        respondWith(200, "{\"user\": {\"id\": \"" + USER + "\", \"name\": \"admin\", \"domain_id\": \"default\", \"enabled\": true,"
                + " \"password_expires_at\": \"2027-01-01T00:00:00.000000\", \"options\": {\"ignore_password_expiry\": true},"
                + " \"federated\": [{\"idp_id\": \"idp1\", \"protocols\": [{\"protocol_id\": \"saml2\", \"unique_id\": \"u1\"}]}], \"links\": {}}}");

        User user = osv3().identity().users().get(USER);
        takeRequest();

        Assert.assertNotNull(user.getPasswordExpiresAt());
        Assert.assertEquals(user.getOptions().get("ignore_password_expiry"), Boolean.TRUE);
        Assert.assertEquals(user.getFederated().get(0).get("idp_id"), "idp1");
    }

    public void projectReadsIsDomainOptionsTags() throws Exception {
        respondWith(200, "{\"project\": {\"id\": \"" + PROJECT + "\", \"name\": \"admin\", \"domain_id\": \"default\", \"enabled\": true,"
                + " \"is_domain\": false, \"parent_id\": \"default\", \"options\": {\"immutable\": false}, \"tags\": [\"prod\", \"web\"], \"links\": {}}}");

        Project project = osv3().identity().projects().get(PROJECT);
        takeRequest();

        Assert.assertEquals(project.getIsDomain(), Boolean.FALSE);
        Assert.assertEquals(project.getOptions().get("immutable"), "false");
        Assert.assertEquals(project.getTags(), Arrays.asList("prod", "web"));
    }

    public void domainAndRoleReadOptions() throws Exception {
        respondWith(200, "{\"domain\": {\"id\": \"default\", \"name\": \"Default\", \"description\": \"The default domain\", \"enabled\": true, \"tags\": [\"t\"], \"options\": {\"immutable\": true}, \"links\": {}}}");
        respondWith(200, "{\"roles\": [{\"id\": \"1222de53b11f40e68497383c7636fbe0\", \"name\": \"admin\", \"domain_id\": null, \"description\": \"Administrator\", \"options\": {\"immutable\": true}, \"links\": {}}]}");

        Domain domain = osv3().identity().domains().get("default");
        List<? extends Role> roles = osv3().identity().roles().list();
        takeRequest();
        takeRequest();

        Assert.assertEquals(domain.getTags(), Arrays.asList("t"));
        Assert.assertEquals(domain.getOptions().get("immutable"), "true");
        Assert.assertEquals(roles.get(0).getDescription(), "Administrator");
        Assert.assertEquals(roles.get(0).getOptions().get("immutable"), "true");
    }

    public void listOptionsBecomeQuery() throws Exception {
        respondWith(200, "{\"users\": [], \"links\": {}}");
        respondWith(200, "{\"projects\": [], \"links\": {}}");

        osv3().identity().users().list(UserListOptions.create().domainId("default").enabled(true).idpId("idp1").passwordExpiresAt("lt", "2027-01-01T00:00:00Z"));
        osv3().identity().projects().list(ProjectListOptions.create().tags("prod", "web").notTagsAny("old").isDomain(false).parentId("default"));

        String users = decodedPath(takeRequest());
        for (String part : new String[] {"/v3/users?", "domain_id=default", "enabled=true", "idp_id=idp1", "password_expires_at=lt:2027-01-01T00:00:00Z"})
            Assert.assertTrue(users.contains(part), users + " lacks " + part);
        String projects = decodedPath(takeRequest());
        for (String part : new String[] {"/v3/projects?", "tags=prod,web", "not-tags-any=old", "is_domain=false", "parent_id=default"})
            Assert.assertTrue(projects.contains(part), projects + " lacks " + part);
    }

    public void existingProjectCreateBodyHasNoResponseOnlyFields() throws Exception {
        respondWith(201, "{\"project\": {\"id\": \"p1\", \"name\": \"n\", \"domain_id\": \"default\", \"enabled\": true, \"links\": {}}}");

        osv3().identity().projects().create(org.openstack4j.api.Builders.project().name("n").domainId("default").build());

        com.fasterxml.jackson.databind.JsonNode body = body(takeRequest()).get("project");
        Assert.assertFalse(body.has("is_domain"));
    }
}
