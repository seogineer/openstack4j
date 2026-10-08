package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.manila.ShareApiVersion;
import org.openstack4j.model.manila.ext.ShareMessage;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@Test(suiteName = "Manila/MicroVersions")
public class ShareMicroVersionTests extends AbstractManilaExtTest {

    private static final String ROOT = "{\"versions\": [{\"id\": \"v1.0\", \"status\": \"DEPRECATED\", \"version\": \"\", \"min_version\": \"\"},"
            + " {\"id\": \"v2.0\", \"status\": \"CURRENT\", \"version\": \"2.99\", \"min_version\": \"2.0\"}]}";
    private static final String MESSAGE = "{\"id\": \"4b319d29-d5b7-4b6e-8e7c-8d6e53f3c1d4\", \"resource_type\": \"SHARE\", \"resource_id\": \"351cc796\","
            + " \"action_id\": \"001\", \"message_level\": \"ERROR\", \"user_message\": \"allocate host: No storage could be allocated.\","
            + " \"detail_id\": \"008\", \"request_id\": \"req-1\", \"project_id\": \"p1\", \"created_at\": \"2017-08-08T09:00:00\", \"expires_at\": \"2017-09-08T09:00:00\", \"links\": []}";

    @AfterMethod
    public void clearMicroVersions() {
        osv3().share().microVersions().clear();
    }

    public void newMethodSendsItsFloorWhileLegacyStaysAt26() throws Exception {
        respondWith(200, "{\"messages\": [" + MESSAGE + "]}");
        respondWith(200, "{\"shares\": []}");

        List<? extends ShareMessage> messages = osv3().share().messages().list(Map.of("message_level", "ERROR"));
        osv3().share().shares().list();

        RecordedRequest newer = takeRequest();
        Assert.assertEquals(decodedPath(newer), "/v2/b80f8d4e28b74188858b654cb1fccf7d/messages?message_level=ERROR");
        Assert.assertEquals(newer.getHeader("X-OpenStack-Manila-API-Version"), "2.37");
        RecordedRequest legacy = takeRequest();
        Assert.assertTrue(decodedPath(legacy).startsWith("/v1/"), decodedPath(legacy));
        Assert.assertEquals(legacy.getHeader("X-Openstack-Manila-Api-Version"), "2.6");
        Assert.assertEquals(messages.get(0).getUserMessage(), "allocate host: No storage could be allocated.");
    }

    public void negotiateRaisesNewMethodsButNotLegacyOnes() throws Exception {
        respondWith(200, ROOT);
        respondWith(200, "{\"message\": " + MESSAGE + "}");
        respondWith(200, "{\"shares\": []}");

        ShareApiVersion version = osv3().share().microVersions().negotiate();
        ShareMessage message = osv3().share().messages().get("4b319d29-d5b7-4b6e-8e7c-8d6e53f3c1d4");
        osv3().share().shares().list();

        Assert.assertEquals(decodedPath(takeRequest()), "/");
        RecordedRequest show = takeRequest();
        Assert.assertEquals(show.getHeader("X-OpenStack-Manila-API-Version"), "2.99");
        Assert.assertEquals(show.getHeader("OpenStack-API-Version"), "shared-file-system 2.99");
        Assert.assertEquals(takeRequest().getHeader("X-Openstack-Manila-Api-Version"), "2.6");
        Assert.assertEquals(version.getServerMaxVersion(), "2.99");
        Assert.assertEquals(version.getMicroVersion(), "2.99");
        Assert.assertEquals(message.getDetailId(), "008");

        osv3().share().microVersions().use("2.30");
        try {
            osv3().share().messages().list();
            Assert.fail("expected a MicroVersionException");
        } catch (MicroVersionException expected) {
            Assert.assertTrue(expected.getMessage().contains("2.37"), expected.getMessage());
        }

        osv3().share().microVersions().use("2.98");
        respondWith(200, "{\"total_progress\": 100, \"task_state\": \"migration_driver_phase1_done\"}");
        Map<String, Object> progress = osv3().share().sharesExt().migrationProgress("s1");
        RecordedRequest get = takeRequest();
        Assert.assertEquals(get.getMethod(), "GET");
        Assert.assertTrue(decodedPath(get).endsWith("/shares/s1/migration-progress"), decodedPath(get));
        Assert.assertNull(get.getHeader("X-OpenStack-Manila-API-Experimental"));
        Assert.assertEquals(progress.get("total_progress"), 100);
    }

    public void messageDelete() throws Exception {
        respondWith(204);
        Assert.assertTrue(osv3().share().messages().delete("m1").isSuccess());
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
    }
}
