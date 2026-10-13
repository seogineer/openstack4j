package org.openstack4j.api.identity.v3.ext;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.RoleAssignment;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Role assignments with {@code include_names}. */
@Test(suiteName = "Identity/RoleAssignmentNames")
public class RoleAssignmentNamesTests extends AbstractIdentityExtTest {

    private static final String NAMES_JSON = "/identity/v3/role_assignments_include_names.json";

    public void roleAssignmentsIncludeNames() throws Exception {
        respondWith(NAMES_JSON);

        List<? extends RoleAssignment> list = osv3().identity().roles().listRoleAssignments("p1", true);

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "GET");
        String path = decodedPath(request);
        Assert.assertTrue(path.contains("/role_assignments?"), path);
        Assert.assertTrue(path.contains("include_names=true"), path);
        Assert.assertTrue(path.contains("effective=true"), path);
        Assert.assertTrue(path.contains("scope.project.id=p1"), path);
        Assert.assertEquals(list.size(), 2);
        RoleAssignment user = list.get(0);
        Assert.assertEquals(user.getUserId(), "u1");
        Assert.assertEquals(user.getUserName(), "alice");
        Assert.assertEquals(user.getRoleId(), "r1");
        Assert.assertEquals(user.getRoleName(), "member");
        Assert.assertEquals(user.getProjectId(), "p1");
        Assert.assertEquals(user.getProjectName(), "demo");
        Assert.assertNull(user.getGroupName());
        Assert.assertNull(user.getDomainId());
        Assert.assertNull(user.getDomainName());
        RoleAssignment group = list.get(1);
        Assert.assertEquals(group.getGroupId(), "g1");
        Assert.assertEquals(group.getGroupName(), "ops");
        Assert.assertNull(group.getUserName());
        assertNoMoreRequests();
    }

    public void roleAssignmentsWithoutNames() throws Exception {
        respondWith("/identity/v3/roles_assignment_list.json");

        List<? extends RoleAssignment> list = osv3().identity().roles().listRoleAssignments("p1", false);

        String path = decodedPath(takeRequest());
        Assert.assertTrue(path.contains("effective=true"), path);
        Assert.assertTrue(path.contains("scope.project.id=p1"), path);
        Assert.assertFalse(path.contains("include_names"), path);
        Assert.assertFalse(list.isEmpty());
        for (RoleAssignment each : list) {
            Assert.assertNull(each.getRoleName());
            Assert.assertNull(each.getUserName());
            Assert.assertNull(each.getGroupName());
            Assert.assertNull(each.getProjectName());
            Assert.assertNull(each.getDomainName());
        }
        assertNoMoreRequests();
    }

    public void domainScopedNames() throws Exception {
        respondWith(200, "{\"role_assignments\":[{\"role\":{\"id\":\"r1\",\"name\":\"admin\"},"
                + "\"scope\":{\"domain\":{\"id\":\"d1\",\"name\":\"Default\"}},"
                + "\"user\":{\"id\":\"u1\",\"name\":\"alice\",\"domain\":{\"id\":\"d2\",\"name\":\"Other\"}}}]}");

        List<? extends RoleAssignment> list = osv3().identity().roles().listRoleAssignments("p1", true);

        takeRequest();
        RoleAssignment assignment = list.get(0);
        Assert.assertEquals(assignment.getDomainId(), "d1");
        Assert.assertEquals(assignment.getDomainName(), "Default");
        Assert.assertNull(assignment.getProjectId());
        Assert.assertNull(assignment.getProjectName());
        Assert.assertEquals(assignment.getUserName(), "alice");
        assertNoMoreRequests();
    }
}
