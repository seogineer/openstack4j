package org.openstack4j.api.compute.microversion;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.ServerUpdateOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/ServerUpdateDescription")
public class ServerUpdateDescriptionTests extends AbstractComputeMicroVersionTest {

    private static final String UPDATED = "{\"server\": {\"id\": \"s1\"}}";

    public void updateDescriptionSends219() throws Exception {
        negotiate("2.100");
        respondWith(200, UPDATED);

        osv3().compute().servers().update("s1", ServerUpdateOptions.create().name("n").description("d"));

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertTrue(request.getPath().endsWith("/servers/s1"), request.getPath());
        assertVersionHeaders(request, "2.100");
        Assert.assertEquals(body(request).toString(), "{\"server\":{\"name\":\"n\",\"description\":\"d\"}}");
    }

    public void updateEmptyDescription() throws Exception {
        negotiate("2.100");
        respondWith(200, UPDATED);

        osv3().compute().servers().update("s1", ServerUpdateOptions.create().description(""));

        JsonNode server = body(takeRequest()).get("server");
        Assert.assertTrue(server.has("description"), server.toString());
        Assert.assertEquals(server.get("description").asText(), "");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.19.*")
    public void updateDescriptionNeeds219WhenDisabled() throws Exception {
        try {
            osv3().compute().servers().update("s1", ServerUpdateOptions.create().description("d"));
        } finally {
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.19.*")
    public void updateDescriptionNeeds219() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.18");
        try {
            osv3().compute().servers().update("s1", ServerUpdateOptions.create().description("d"));
        } finally {
            assertNoMoreRequests();
        }
    }

    public void updateWithoutDescriptionUnchanged() throws Exception {
        respondWith(200, UPDATED);

        osv3().compute().servers().update("s1", ServerUpdateOptions.create().name("n"));

        RecordedRequest request = takeRequest();
        assertNoVersionHeaders(request);
        JsonNode server = body(request).get("server");
        Assert.assertEquals(server.get("name").asText(), "n");
        Assert.assertFalse(server.has("description"), server.toString());
    }
}
