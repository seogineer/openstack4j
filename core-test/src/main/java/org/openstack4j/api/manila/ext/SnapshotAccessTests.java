package org.openstack4j.api.manila.ext;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.model.manila.ext.SnapshotAccess;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@Test(suiteName = "Manila/SnapshotAccess")
public class SnapshotAccessTests extends AbstractManilaExtTest {

    private static final String P = "/v2/b80f8d4e28b74188858b654cb1fccf7d";
    private static final String ROOT = "{\"versions\": [{\"id\": \"v1.0\", \"status\": \"DEPRECATED\", \"version\": \"\", \"min_version\": \"\"},"
            + " {\"id\": \"v2.0\", \"status\": \"CURRENT\", \"version\": \"2.99\", \"min_version\": \"2.0\"}]}";
    private static final String ACCESS = "{\"id\": \"a1\", \"access_type\": \"ip\", \"access_to\": \"10.0.0.0/24\", \"state\": \"queued_to_apply\"}";

    @AfterMethod
    public void clearMicroVersions() {
        osv3().share().microVersions().clear();
    }

    public void grantAccess() throws Exception {
        respondWith(202, "{\"snapshot_access\": " + ACCESS + "}");

        SnapshotAccess access = osv3().share().snapshotsExt().grantAccess("sn1", "ip", "10.0.0.0/24");

        RecordedRequest request = expect("POST", P + "/snapshots/sn1/action");
        Assert.assertEquals(request.getHeader("X-OpenStack-Manila-API-Version"), "2.32");
        Assert.assertEquals(body(request).toString(), "{\"allow_access\":{\"access_type\":\"ip\",\"access_to\":\"10.0.0.0/24\"}}");
        Assert.assertEquals(access.getId(), "a1");
        Assert.assertEquals(access.getAccessType(), "ip");
        Assert.assertEquals(access.getAccessTo(), "10.0.0.0/24");
        Assert.assertEquals(access.getState(), "queued_to_apply");
    }

    public void revokeAccess() throws Exception {
        respondWith(202);

        Assert.assertTrue(osv3().share().snapshotsExt().revokeAccess("sn1", "a1").isSuccess());

        RecordedRequest request = expect("POST", P + "/snapshots/sn1/action");
        Assert.assertEquals(request.getHeader("X-OpenStack-Manila-API-Version"), "2.32");
        Assert.assertEquals(body(request).toString(), "{\"deny_access\":{\"access_id\":\"a1\"}}");
    }

    public void listAccess() throws Exception {
        respondWith(200, "{\"snapshot_access_list\": [" + ACCESS + ", {\"id\": \"a2\", \"access_type\": \"ip\", \"access_to\": \"10.0.1.0/24\", \"state\": \"active\", \"extra\": 1}]}");

        List<? extends SnapshotAccess> rules = osv3().share().snapshotsExt().listAccess("sn1");

        Assert.assertEquals(expect("GET", P + "/snapshots/sn1/access-list").getHeader("X-OpenStack-Manila-API-Version"), "2.32");
        Assert.assertEquals(rules.size(), 2);
        Assert.assertEquals(rules.get(1).getState(), "active");
        Assert.assertEquals(rules.get(1).getAttributes().get("extra"), 1);
    }

    public void grantAccessBadRequestRaises() throws Exception {
        respondWith(400, "{\"badRequest\": {\"message\": \"Snapshot of a share without mount_snapshot_support\", \"code\": 400}}");

        try {
            osv3().share().snapshotsExt().grantAccess("sn1", "ip", "10.0.0.0/24");
            Assert.fail("expected a ResponseException");
        } catch (ResponseException expected) {
            Assert.assertEquals(expected.getStatus(), 400);
            Assert.assertTrue(expected.getMessage().contains("mount_snapshot_support"), expected.getMessage());
        }
        expect("POST", P + "/snapshots/sn1/action");
    }

    public void sessionBelow232Fails() throws Exception {
        respondWith(300, ROOT);
        osv3().share().microVersions().use("2.31");
        Assert.assertEquals(decodedPath(takeRequest()), "/");
        int before = server.getRequestCount();

        try {
            osv3().share().snapshotsExt().grantAccess("sn1", "ip", "10.0.0.0/24");
            Assert.fail("expected a MicroVersionException");
        } catch (MicroVersionException expected) {
            Assert.assertTrue(expected.getMessage().contains("2.32"), expected.getMessage());
        }
        Assert.assertEquals(server.getRequestCount(), before, "no request may be sent");
    }
}
