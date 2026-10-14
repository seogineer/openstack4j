package org.openstack4j.api.manila.ext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.manila.ext.ShareInstanceInfo;
import org.openstack4j.model.manila.ext.ShareSnapshotInfo;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@Test(suiteName = "Manila/ExtLists")
public class ManilaExtListTests extends AbstractManilaExtTest {

    private static final String P = "/v2/b80f8d4e28b74188858b654cb1fccf7d";
    private static final String VERSION = "X-OpenStack-Manila-API-Version";
    private static final String SNAPSHOT = "{\"id\": \"sn1\", \"name\": \"snap\", \"status\": \"available\", \"share_id\": \"s1\","
            + " \"project_id\": \"p1\", \"user_id\": \"u1\", \"size\": 1, \"metadata\": {\"k\": \"v\"}}";

    private static final String ROOT = "{\"versions\": [{\"id\": \"v1.0\", \"status\": \"DEPRECATED\", \"version\": \"\", \"min_version\": \"\"},"
            + " {\"id\": \"v2.0\", \"status\": \"CURRENT\", \"version\": \"2.99\", \"min_version\": \"2.0\"}]}";

    /**
     * Pins the session microversion. The first use asks the server's root document (consumed here); when another test
     * already discovered it no request is sent and the queued answer is drained, so nothing leaks into later tests.
     */
    private void useVersion(String version) throws Exception {
        int before = server.getRequestCount();
        respondWith(300, ROOT);
        osv3().share().microVersions().use(version);
        if (server.getRequestCount() > before) {
            Assert.assertEquals(decodedPath(takeRequest()), "/");
        } else {
            new okhttp3.OkHttpClient().newCall(new okhttp3.Request.Builder().url(server.url("/drain")).build()).execute().close();
            takeRequest();
        }
    }

    @AfterMethod
    public void clearMicroVersions() {
        osv3().share().microVersions().clear();
    }


    public void snapshotsDetailFiltersAtSessionVersion() throws Exception {
        useVersion("2.80");
        respondWith(200, "{\"snapshots\": [" + SNAPSHOT + "]}");

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("share_id", "x");
        filters.put("status", "available");
        List<? extends ShareSnapshotInfo> snapshots = osv3().share().snapshotsExt().listDetails(filters);

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "GET");
        String[] pathAndQuery = decodedPath(request).split("\\?", 2);
        Assert.assertTrue(pathAndQuery[0].endsWith("/snapshots/detail"), pathAndQuery[0]);
        Assert.assertEquals(new HashSet<>(Arrays.asList(pathAndQuery[1].split("&"))), Set.of("share_id=x", "status=available"));
        Assert.assertEquals(request.getHeader(VERSION), "2.80");
        Assert.assertEquals(snapshots.size(), 1);
        ShareSnapshotInfo snapshot = snapshots.get(0);
        Assert.assertEquals(snapshot.getProjectId(), "p1");
        Assert.assertEquals(snapshot.getUserId(), "u1");
        Assert.assertFalse(snapshot.getAttributes().containsKey("user_id"));
        Assert.assertTrue(snapshot.getAttributes().containsKey("metadata"));
    }

    public void snapshotsListWithoutSessionUsesFloor() throws Exception {
        respondWith(200, "{\"snapshots\": [{\"id\": \"sn1\", \"name\": \"snap\", \"links\": []}]}");
        respondWith(200, "{\"snapshots\": []}");

        List<? extends ShareSnapshotInfo> snapshots = osv3().share().snapshotsExt().list(Map.of("name", "snap"));
        osv3().share().snapshotsExt().list(null);

        RecordedRequest request = expect("GET", P + "/snapshots?name=snap");
        Assert.assertEquals(request.getHeader(VERSION), "2.7");
        Assert.assertEquals(snapshots.get(0).getName(), "snap");
        Assert.assertNull(snapshots.get(0).getUserId());
        RecordedRequest nullFilters = expect("GET", P + "/snapshots");
        Assert.assertFalse(nullFilters.getPath().contains("?"), nullFilters.getPath());
    }

    public void snapshotsDetailNullFilters() throws Exception {
        respondWith(200, "{\"snapshots\": []}");

        Assert.assertTrue(osv3().share().snapshotsExt().listDetails(null).isEmpty());

        RecordedRequest request = expect("GET", P + "/snapshots/detail");
        Assert.assertFalse(request.getPath().contains("?"), request.getPath());
        Assert.assertEquals(request.getHeader(VERSION), "2.7");
    }

    public void snapshotGet() throws Exception {
        respondWith(200, "{\"snapshot\": " + SNAPSHOT + "}");

        ShareSnapshotInfo snapshot = osv3().share().snapshotsExt().get("sn1");

        RecordedRequest request = expect("GET", P + "/snapshots/sn1");
        Assert.assertEquals(request.getHeader(VERSION), "2.7");
        Assert.assertEquals(snapshot.getUserId(), "u1");
    }

    public void snapshotGet404() throws Exception {
        respondWith(404, "{\"itemNotFound\": {\"message\": \"nope\", \"code\": 404}}");

        Assert.assertNull(osv3().share().snapshotsExt().get("sn1"));

        expect("GET", P + "/snapshots/sn1");
    }

    public void listShareInstancesFilters() throws Exception {
        respondWith(200, "{\"share_instances\": [{\"id\": \"i1\", \"share_id\": \"s1\", \"status\": \"available\", \"export_location\": \"/p\"}]}");

        List<? extends ShareInstanceInfo> instances = osv3().share().sharesExt().listShareInstances(Map.of("export_location_path", "/p"));

        RecordedRequest request = expect("GET", P + "/share_instances?export_location_path=/p");
        Assert.assertEquals(request.getHeader(VERSION), "2.7");
        Assert.assertEquals(instances.size(), 1);
        Assert.assertEquals(instances.get(0).getId(), "i1");
    }

    public void listShareInstancesNullFilters() throws Exception {
        respondWith(200, "{\"share_instances\": []}");

        Assert.assertTrue(osv3().share().sharesExt().listShareInstances(null).isEmpty());

        RecordedRequest request = expect("GET", P + "/share_instances");
        Assert.assertFalse(request.getPath().contains("?"), request.getPath());
    }

    public void getShareInstance() throws Exception {
        respondWith(200, "{\"share_instance\": {\"id\": \"i1\", \"share_id\": \"s1\", \"status\": \"available\"}}");

        ShareInstanceInfo instance = osv3().share().sharesExt().getShareInstance("i1");

        expect("GET", P + "/share_instances/i1");
        Assert.assertEquals(instance.getId(), "i1");
        Assert.assertEquals(instance.getShareId(), "s1");
    }

    public void getShareInstance404() throws Exception {
        respondWith(404, "{\"itemNotFound\": {\"message\": \"nope\", \"code\": 404}}");

        Assert.assertNull(osv3().share().sharesExt().getShareInstance("i1"));

        expect("GET", P + "/share_instances/i1");
    }

    public void sessionBelowFloorFailsBeforeTheRequest() throws Exception {
        useVersion("2.6");
        try {
            osv3().share().snapshotsExt().list(null);
            Assert.fail("expected a MicroVersionException");
        } catch (MicroVersionException expected) {
            Assert.assertTrue(expected.getMessage().contains("2.7"), expected.getMessage());
        }
        try {
            osv3().share().sharesExt().listShareInstances(null);
            Assert.fail("expected a MicroVersionException");
        } catch (MicroVersionException expected) {
            Assert.assertTrue(expected.getMessage().contains("2.7"), expected.getMessage());
        }
        Assert.assertNull(server.takeRequest(200, java.util.concurrent.TimeUnit.MILLISECONDS), "unexpected request");
    }
}
