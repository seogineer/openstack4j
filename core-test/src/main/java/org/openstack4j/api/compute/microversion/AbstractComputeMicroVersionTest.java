package org.openstack4j.api.compute.microversion;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.openstack.internal.microversion.MicroVersionStore;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

public abstract class AbstractComputeMicroVersionTest extends AbstractTest {

    protected static final String SERVER = "96a38bed-26b5-410b-8cef-1913a2e0e0b8";

    @Override
    protected Service service() {
        return Service.COMPUTE;
    }

    @BeforeMethod
    public void forgetMicroVersions() {
        MicroVersionStore.clearAll();
    }

    /** Enqueues the Nova root document advertising {@code max}. */
    protected void respondWithNovaVersions(String max) {
        respondWith(200, "{\"versions\":[{\"id\":\"v2.0\",\"status\":\"DEPRECATED\",\"version\":\"\",\"min_version\":\"\"},"
                + "{\"id\":\"v2.1\",\"status\":\"CURRENT\",\"version\":\"" + max + "\",\"min_version\":\"2.1\"}]}");
    }

    /** Negotiates against a server advertising {@code max} and consumes the root request. */
    protected void negotiate(String max) throws InterruptedException {
        respondWithNovaVersions(max);
        osv3().compute().microVersions().negotiate();
        RecordedRequest root = takeRequest();
        Assert.assertEquals(root.getPath(), "/");
    }

    protected void assertVersionHeaders(RecordedRequest request, String expected) {
        Assert.assertEquals(request.getHeader("OpenStack-API-Version"), "compute " + expected);
        Assert.assertEquals(request.getHeader("X-OpenStack-Nova-API-Version"), expected);
    }

    protected void assertNoVersionHeaders(RecordedRequest request) {
        Assert.assertNull(request.getHeader("OpenStack-API-Version"));
        Assert.assertNull(request.getHeader("X-OpenStack-Nova-API-Version"));
    }

    protected void assertNoMoreRequests() throws InterruptedException {
        Assert.assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS), "unexpected extra request");
    }

    protected JsonNode body(RecordedRequest request) throws IOException {
        return new ObjectMapper().readTree(request.getBody().clone().readUtf8());
    }
}
