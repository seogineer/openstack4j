package org.openstack4j.api.network.ext2;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;

/** Base for the Neutron extension tests: Neutron on the mock server's network port, v3 token. */
public abstract class AbstractNetworkingExtTest extends AbstractTest {

    protected static final String PROJECT = "2580a7b51d564c1d848ee27fda2db713";

    @Override
    protected Service service() {
        return Service.NETWORK;
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

    /** Asserts method and path suffix of the next request and returns it. */
    protected RecordedRequest expect(String method, String pathSuffix) throws InterruptedException {
        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), method, decodedPath(request));
        Assert.assertTrue(decodedPath(request).endsWith(pathSuffix), decodedPath(request) + " does not end with " + pathSuffix);
        return request;
    }
}
