package org.openstack4j.api.identity.v3.ext;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.model.identity.v3.RoleInference;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/RoleExtensions")
public class RoleExtensionTests extends AbstractIdentityExtTest {

    private static final String ROLE = "1222de53b11f40e68497383c7636fbe0";
    private static final String MEMBER = "f7ab1b9e582440e0b93aceec359aeaef";
    private static final String GROUP = "g1";
    private static final String ROLES = "{\"roles\": [{\"id\": \"" + ROLE + "\", \"name\": \"admin\", \"links\": {}}], \"links\": {}}";

    public void inheritedAssignments() throws Exception {
        for (int i = 0; i < 14; i++)
            respondWith(i == 3 || i == 7 ? 200 : 204, i == 3 || i == 7 ? ROLES : "");

        var roles = osv3().identity().roles();
        roles.grantInheritedRoleToUserOnDomain("default", USER, ROLE);
        roles.checkInheritedRoleOfUserOnDomain("default", USER, ROLE);
        roles.revokeInheritedRoleFromUserOnDomain("default", USER, ROLE);
        List<? extends Role> userRoles = roles.listInheritedRolesOfUserOnDomain("default", USER);
        roles.grantInheritedRoleToGroupOnDomain("default", GROUP, ROLE);
        roles.checkInheritedRoleOfGroupOnDomain("default", GROUP, ROLE);
        roles.revokeInheritedRoleFromGroupOnDomain("default", GROUP, ROLE);
        List<? extends Role> groupRoles = roles.listInheritedRolesOfGroupOnDomain("default", GROUP);
        roles.grantInheritedRoleToUserOnProject(PROJECT, USER, ROLE);
        roles.checkInheritedRoleOfUserOnProject(PROJECT, USER, ROLE);
        roles.revokeInheritedRoleFromUserOnProject(PROJECT, USER, ROLE);
        roles.grantInheritedRoleToGroupOnProject(PROJECT, GROUP, ROLE);
        roles.checkInheritedRoleOfGroupOnProject(PROJECT, GROUP, ROLE);
        roles.revokeInheritedRoleFromGroupOnProject(PROJECT, GROUP, ROLE);

        String[][] expected = {
                {"PUT", "/v3/OS-INHERIT/domains/default/users/" + USER + "/roles/" + ROLE + "/inherited_to_projects"},
                {"HEAD", "/v3/OS-INHERIT/domains/default/users/" + USER + "/roles/" + ROLE + "/inherited_to_projects"},
                {"DELETE", "/v3/OS-INHERIT/domains/default/users/" + USER + "/roles/" + ROLE + "/inherited_to_projects"},
                {"GET", "/v3/OS-INHERIT/domains/default/users/" + USER + "/roles/inherited_to_projects"},
                {"PUT", "/v3/OS-INHERIT/domains/default/groups/g1/roles/" + ROLE + "/inherited_to_projects"},
                {"HEAD", "/v3/OS-INHERIT/domains/default/groups/g1/roles/" + ROLE + "/inherited_to_projects"},
                {"DELETE", "/v3/OS-INHERIT/domains/default/groups/g1/roles/" + ROLE + "/inherited_to_projects"},
                {"GET", "/v3/OS-INHERIT/domains/default/groups/g1/roles/inherited_to_projects"},
                {"PUT", "/v3/OS-INHERIT/projects/" + PROJECT + "/users/" + USER + "/roles/" + ROLE + "/inherited_to_projects"},
                {"HEAD", "/v3/OS-INHERIT/projects/" + PROJECT + "/users/" + USER + "/roles/" + ROLE + "/inherited_to_projects"},
                {"DELETE", "/v3/OS-INHERIT/projects/" + PROJECT + "/users/" + USER + "/roles/" + ROLE + "/inherited_to_projects"},
                {"PUT", "/v3/OS-INHERIT/projects/" + PROJECT + "/groups/g1/roles/" + ROLE + "/inherited_to_projects"},
                {"HEAD", "/v3/OS-INHERIT/projects/" + PROJECT + "/groups/g1/roles/" + ROLE + "/inherited_to_projects"},
                {"DELETE", "/v3/OS-INHERIT/projects/" + PROJECT + "/groups/g1/roles/" + ROLE + "/inherited_to_projects"}};
        for (String[] e : expected) {
            RecordedRequest r = takeRequest();
            Assert.assertEquals(r.getMethod(), e[0], e[1]);
            Assert.assertTrue(r.getPath().endsWith(e[1]), r.getPath() + " vs " + e[1]);
        }
        Assert.assertEquals(userRoles.get(0).getName(), "admin");
        Assert.assertEquals(groupRoles.size(), 1);
    }

    public void impliedRolesAndInferences() throws Exception {
        String inference = "{\"role_inference\": {\"prior_role\": {\"id\": \"" + ROLE + "\", \"name\": \"admin\", \"links\": {}},"
                + " \"implies\": {\"id\": \"" + MEMBER + "\", \"name\": \"member\", \"links\": {}}}, \"links\": {}}";
        respondWith(201, inference);
        respondWith(200, inference);
        respondWith(204);
        respondWith(200, "{\"role_inference\": {\"prior_role\": {\"id\": \"" + ROLE + "\", \"name\": \"admin\"}, \"implies\": [{\"id\": \"" + MEMBER + "\", \"name\": \"member\"}]}, \"links\": {}}");
        respondWith(200, "{\"role_inferences\": [{\"prior_role\": {\"id\": \"" + ROLE + "\", \"name\": \"admin\"}, \"implies\": [{\"id\": \"" + MEMBER + "\", \"name\": \"member\"}]}], \"links\": {}}");
        respondWith(204);

        RoleInference created = osv3().identity().roles().createImpliedRole(ROLE, MEMBER);
        RoleInference got = osv3().identity().roles().getImpliedRole(ROLE, MEMBER);
        boolean exists = osv3().identity().roles().checkImpliedRole(ROLE, MEMBER).isSuccess();
        List<? extends RoleInference> implied = osv3().identity().roles().listImpliedRoles(ROLE);
        List<? extends RoleInference> all = osv3().identity().roles().listRoleInferences();
        boolean deleted = osv3().identity().roles().deleteImpliedRole(ROLE, MEMBER).isSuccess();

        RecordedRequest put = takeRequest();
        Assert.assertEquals(put.getMethod(), "PUT");
        Assert.assertTrue(put.getPath().endsWith("/v3/roles/" + ROLE + "/implies/" + MEMBER));
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertEquals(takeRequest().getMethod(), "HEAD");
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/roles/" + ROLE + "/implies"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/role_inferences"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getImplies().get(0).getName(), "member");
        Assert.assertEquals(got.getPriorRole().getId(), ROLE);
        Assert.assertTrue(exists);
        Assert.assertEquals(implied.get(0).getImplies().get(0).getId(), MEMBER);
        Assert.assertEquals(all.get(0).getPriorRole().getName(), "admin");
        Assert.assertTrue(deleted);
    }

    public void userRoleListingsOnUsersService() throws Exception {
        respondWith(200, ROLES);
        respondWith(200, ROLES);
        // these listings already exist on users() (userId first)
        osv3().identity().users().listProjectUserRoles(USER, PROJECT);
        osv3().identity().users().listDomainUserRoles(USER, "default");
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/projects/" + PROJECT + "/users/" + USER + "/roles"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/domains/default/users/" + USER + "/roles"));
    }
}
