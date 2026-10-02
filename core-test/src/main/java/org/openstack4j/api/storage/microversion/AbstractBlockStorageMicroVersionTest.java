package org.openstack4j.api.storage.microversion;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.openstack.internal.microversion.MicroVersionStore;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

/** Block storage tests that talk to a v3 endpoint ({@code /v3/<project>}), as a Cinder v3 cloud advertises it. */
public abstract class AbstractBlockStorageMicroVersionTest extends AbstractTest {

    protected static final String VOLUME = "4b699b6d-1fe8-41f9-9dbf-65a2cc4927c2";
    protected static final String SNAPSHOT = "8e53384c-dd0e-410d-94a6-8cd5bf21a5bf";

    @Override
    protected Service service() {
        return Service.BLOCK_STORAGE;
    }

    /**
     * Turns the token's volumev2 entry into a volumev3 one so requests go to {@code /v3/<project>/...}. The project
     * id is changed too: endpoint URLs are cached per project across test classes, and the other block storage
     * tests already cached the v2 URL for the shared project.
     */
    @Override
    protected String adjustTokenJson(String json) {
        return json.replace(":8776/v2/", ":8776/v3/").replace("\"volumev2\"", "\"volumev3\"").replace("\"cinderv2\"", "\"cinderv3\"")
                .replace("123ac695d4db400a9001b91bb3b8aa46", "123ac695d4db400a9001b91bb3b8aa47");
    }

    @BeforeMethod
    public void forgetMicroVersions() {
        MicroVersionStore.clearAll();
    }

    /** Enqueues the Cinder root document advertising {@code max}. */
    protected void respondWithCinderVersions(String max) {
        respondWith(300, "{\"versions\": [{\"id\": \"v3.0\", \"status\": \"CURRENT\", \"version\": \"" + max + "\", \"min_version\": \"3.0\","
                + " \"updated\": \"2018-07-17T00:00:00Z\", \"links\": [], \"media-types\": []}]}");
    }

    /** Negotiates against a server advertising {@code max} and consumes the root request. */
    protected void negotiate(String max) throws InterruptedException {
        respondWithCinderVersions(max);
        osv3().blockStorage().microVersions().negotiate();
        RecordedRequest root = takeRequest();
        Assert.assertEquals(root.getPath(), "/");
    }

    protected void assertVersionHeader(RecordedRequest request, String expected) {
        Assert.assertEquals(request.getHeader("OpenStack-API-Version"), "volume " + expected);
        Assert.assertNull(request.getHeader("X-OpenStack-Nova-API-Version"));
    }

    protected void assertNoVersionHeader(RecordedRequest request) {
        Assert.assertNull(request.getHeader("OpenStack-API-Version"));
    }

    protected void assertNoMoreRequests() throws InterruptedException {
        Assert.assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS), "unexpected extra request");
    }

    protected JsonNode body(RecordedRequest request) throws IOException {
        return new ObjectMapper().readTree(request.getBody().clone().readUtf8());
    }
}
