package org.openstack4j.api.image.v2.ext;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;

/** Base for the Glance extension tests: Glance on the mock server's image port, v3 token. */
public abstract class AbstractImageExtTest extends AbstractTest {

    @Override
    protected Service service() {
        return Service.IMAGE;
    }

    protected JsonNode body(RecordedRequest request) throws IOException {
        return new ObjectMapper().readTree(request.getBody().clone().readUtf8());
    }

    protected static String decodedPath(RecordedRequest request) {
        return URLDecoder.decode(request.getPath(), StandardCharsets.UTF_8);
    }

    protected RecordedRequest expect(String method, String pathSuffix) throws InterruptedException {
        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), method, decodedPath(request));
        Assert.assertTrue(decodedPath(request).endsWith(pathSuffix), decodedPath(request) + " does not end with " + pathSuffix);
        return request;
    }
}
