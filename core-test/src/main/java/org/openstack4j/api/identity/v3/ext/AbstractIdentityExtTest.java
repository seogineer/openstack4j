package org.openstack4j.api.identity.v3.ext;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;

/** Base for the identity extension tests: Keystone on the mock server's identity port, v3 token. */
public abstract class AbstractIdentityExtTest extends AbstractTest {

    protected static final String USER = "e365357fdf4d4a37a07fde3209cac4aa";
    protected static final String PROJECT = "2580a7b51d564c1d848ee27fda2db713";

    @Override
    protected Service service() {
        return Service.IDENTITY;
    }

    protected static Map<String, String> tokenHeaders(String tokenId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Subject-Token", tokenId);
        headers.put("Content-Type", "application/json");
        return headers;
    }

    protected JsonNode body(RecordedRequest request) throws IOException {
        return new ObjectMapper().readTree(request.getBody().clone().readUtf8());
    }

    /** The request path with percent-escapes decoded; connectors encode reserved characters differently. */
    protected static String decodedPath(RecordedRequest request) {
        return URLDecoder.decode(request.getPath(), StandardCharsets.UTF_8);
    }

    protected void assertNoMoreRequests() throws InterruptedException {
        Assert.assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS), "unexpected extra request");
    }
}
