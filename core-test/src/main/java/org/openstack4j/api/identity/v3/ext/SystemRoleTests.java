package org.openstack4j.api.identity.v3.ext;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.Role;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/SystemRoles")
public class SystemRoleTests extends AbstractIdentityExtTest {

    private static final String ROLE = "1222de53b11f40e68497383c7636fbe0";
    private static final String ROLE_JSON = "{\"id\": \"" + ROLE + "\", \"name\": \"admin\", \"domain_id\": null, \"links\": {}}";

    public void userAndGroupSystemRoles() throws Exception {
        for (String actor : new String[] {"users", "groups"}) {
            respondWith(204);
            respondWith(204);
            respondWith(200, "{\"role\": " + ROLE_JSON + "}");
            respondWith(200, "{\"roles\": [" + ROLE_JSON + "], \"links\": {}}");
            respondWith(204);
        }
        var system = osv3().identity().systemRoles();
        system.grantUserRole(USER, ROLE);
        system.checkUserRole(USER, ROLE);
        Role userRole = system.getUserRole(USER, ROLE);
        List<? extends Role> userRoles = system.listUserRoles(USER);
        system.revokeUserRole(USER, ROLE);
        system.grantGroupRole("g1", ROLE);
        system.checkGroupRole("g1", ROLE);
        system.getGroupRole("g1", ROLE);
        List<? extends Role> groupRoles = system.listGroupRoles("g1");
        system.revokeGroupRole("g1", ROLE);

        for (String prefix : new String[] {"/v3/system/users/" + USER, "/v3/system/groups/g1"}) {
            String[][] expected = {{"PUT", prefix + "/roles/" + ROLE}, {"HEAD", prefix + "/roles/" + ROLE}, {"GET", prefix + "/roles/" + ROLE},
                    {"GET", prefix + "/roles"}, {"DELETE", prefix + "/roles/" + ROLE}};
            for (String[] e : expected) {
                RecordedRequest r = takeRequest();
                Assert.assertEquals(r.getMethod(), e[0]);
                Assert.assertTrue(r.getPath().endsWith(e[1]), r.getPath());
            }
        }
        Assert.assertEquals(userRole.getName(), "admin");
        Assert.assertEquals(userRoles.size(), 1);
        Assert.assertEquals(groupRoles.get(0).getId(), ROLE);
    }
}
