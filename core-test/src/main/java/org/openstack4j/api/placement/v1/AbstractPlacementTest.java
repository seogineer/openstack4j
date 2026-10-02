package org.openstack4j.api.placement.v1;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.openstack.placement.v1.internal.PlacementSessionState;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

public abstract class AbstractPlacementTest extends AbstractTest {

    /** The resource provider used throughout the fixtures. */
    protected static final String RP = "3626308f-38dd-4da8-8f0f-6697b05d8f6c";

    @Override
    protected Service service() {
        return Service.PLACEMENT;
    }

    @BeforeMethod
    public void forgetNegotiatedVersions() {
        PlacementSessionState.clearAll();
    }

    /** Enqueues the response of {@code GET /} advertising {@code max} as the server's newest microversion. */
    protected void respondWithVersions(String max) {
        respondWith(200, "{\"versions\":[{\"id\":\"v1.0\",\"max_version\":\"" + max
                + "\",\"min_version\":\"1.0\",\"status\":\"CURRENT\",\"links\":[{\"rel\":\"self\",\"href\":\"\"}]}]}");
    }

    /** Takes the version discovery request and returns the request that followed it. */
    protected RecordedRequest takeVersionAndRequest() throws InterruptedException {
        RecordedRequest root = takeRequest();
        Assert.assertEquals(root.getPath(), "/");
        return takeRequest();
    }

    protected void respondWithError(int status, String code) {
        respondWith(status, "{\"errors\":[{\"status\":" + status + ",\"title\":\"Error\",\"detail\":\"detail for "
                + code + "\",\"code\":\"" + code + "\",\"request_id\":\"req-test\"}]}");
    }

    protected JsonNode body(RecordedRequest request) throws IOException {
        // clone: reading the buffer is destructive and tests call body(request) more than once
        return new ObjectMapper().readTree(request.getBody().clone().readUtf8());
    }

    protected void assertNoMoreRequests() throws InterruptedException {
        Assert.assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS), "unexpected extra request");
    }
}
