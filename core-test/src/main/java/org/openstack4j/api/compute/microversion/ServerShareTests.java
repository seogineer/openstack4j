package org.openstack4j.api.compute.microversion;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.ServerShare;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/ServerShares")
public class ServerShareTests extends AbstractComputeMicroVersionTest {

    private static final String SHARE = "{\"share_id\": \"e8debdc0-447a-4376-a10a-4cd9122d7986\", \"status\": \"active\", \"tag\": \"e8debdc0-447a-4376-a10a-4cd9122d7986\","
            + " \"export_location\": \"10.0.0.50:/mnt/foo\", \"uuid\": \"68ba1762-fd6d-4221-8311-f3193dd93404\"}";

    public void listGetAttachDetach() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"shares\": [" + SHARE + "]}");
        respondWith(200, "{\"share\": " + SHARE + "}");
        respondWith(201, "{\"share\": {\"share_id\": \"s2\", \"status\": \"attaching\", \"tag\": \"data\"}}");
        respondWith(202);

        List<? extends ServerShare> shares = osv3().compute().servers().shares(SERVER);
        ServerShare one = osv3().compute().servers().share(SERVER, "e8debdc0-447a-4376-a10a-4cd9122d7986");
        ServerShare attached = osv3().compute().servers().attachShare(SERVER, "s2", "data");
        boolean detached = osv3().compute().servers().detachShare(SERVER, "s2").isSuccess();

        Assert.assertTrue(takeRequest().getPath().endsWith("/servers/" + SERVER + "/shares"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/servers/" + SERVER + "/shares/e8debdc0-447a-4376-a10a-4cd9122d7986"));
        RecordedRequest attach = takeRequest();
        Assert.assertEquals(attach.getMethod(), "POST");
        Assert.assertEquals(body(attach).get("share").get("share_id").asText(), "s2");
        Assert.assertEquals(body(attach).get("share").get("tag").asText(), "data");
        RecordedRequest detach = takeRequest();
        Assert.assertEquals(detach.getMethod(), "DELETE");
        Assert.assertTrue(detach.getPath().endsWith("/servers/" + SERVER + "/shares/s2"));

        Assert.assertEquals(shares.get(0).getExportLocation(), "10.0.0.50:/mnt/foo");
        Assert.assertEquals(one.getUuid(), "68ba1762-fd6d-4221-8311-f3193dd93404");
        Assert.assertEquals(attached.getStatus(), "attaching");
        Assert.assertTrue(detached);
    }

    public void attachWithoutTagOmitsIt() throws Exception {
        negotiate("2.100");
        respondWith(201, "{\"share\": {\"share_id\": \"s2\", \"status\": \"attaching\", \"tag\": \"s2\"}}");
        osv3().compute().servers().attachShare(SERVER, "s2", null);
        Assert.assertFalse(body(takeRequest()).get("share").has("tag"));
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.97.*")
    public void sharesNeed297() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.96");
        try {
            osv3().compute().servers().shares(SERVER);
        } finally {
            assertNoMoreRequests();
        }
    }
}
