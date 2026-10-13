package org.openstack4j.api.identity.v3.ext;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.RoleAssignment;
import org.openstack4j.model.identity.v3.options.RoleAssignmentListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Role assignment list filters. */
@Test(suiteName = "Identity/RoleAssignmentListOptions")
public class RoleAssignmentListOptionsTests extends AbstractIdentityExtTest {

    public void domainScopeWithNames() throws Exception {
        respondWith(200, "{\"role_assignments\":[{\"role\":{\"id\":\"r1\",\"name\":\"admin\"},"
                + "\"scope\":{\"domain\":{\"id\":\"d1\",\"name\":\"Default\"}},"
                + "\"user\":{\"id\":\"u1\",\"name\":\"alice\"}}]}");

        List<? extends RoleAssignment> list = osv3().identity().roles()
                .listRoleAssignments(RoleAssignmentListOptions.create().scopeDomainId("d1").includeNames());

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "GET");
        String path = decodedPath(request);
        Assert.assertTrue(path.contains("/role_assignments?"), path);
        Assert.assertTrue(path.contains("scope.domain.id=d1"), path);
        Assert.assertTrue(path.contains("include_names=true"), path);
        Assert.assertFalse(path.contains("effective"), path);
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0).getDomainId(), "d1");
        Assert.assertEquals(list.get(0).getDomainName(), "Default");
        assertNoMoreRequests();
    }

    public void allFilters() throws Exception {
        respondWith(200, "{\"role_assignments\":[]}");

        osv3().identity().roles().listRoleAssignments(RoleAssignmentListOptions.create()
                .scopeProjectId("p1").scopeSystem().inheritedToProjects().userId("u1").groupId("g1").roleId("r1")
                .effective().includeNames().includeSubtree());

        String path = decodedPath(takeRequest());
        for (String expected : new String[]{"scope.project.id=p1", "scope.system=all", "scope.OS-INHERIT:inherited_to=projects",
                "user.id=u1", "group.id=g1", "role.id=r1", "effective=true", "include_names=true", "include_subtree=true"}) {
            Assert.assertTrue(path.contains(expected), expected + " missing in " + path);
        }
        assertNoMoreRequests();
    }

    public void noFalseValues() throws Exception {
        respondWith(200, "{\"role_assignments\":[]}");

        osv3().identity().roles().listRoleAssignments(RoleAssignmentListOptions.create());

        String path = decodedPath(takeRequest());
        Assert.assertTrue(path.endsWith("/role_assignments"), path);
        Assert.assertFalse(path.contains("?"), path);
        Assert.assertFalse(path.contains("false"), path);
        assertNoMoreRequests();
    }
}
