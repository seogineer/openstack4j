package org.openstack4j.api.workflow;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Base class for all Workflow Service tests.
 *
 * @author Renat Akhmerov
 */
public class WorkflowBaseTest extends AbstractTest {

    static void assertIsUUID(String value) {
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    static void assertNotEmptyString(String value) {
        assertNotNull(value);
        assertTrue(value.length() > 0);
    }

    /** The last request made; earlier ones (also from other tests of the class) are discarded. */
    protected RecordedRequest lastRequest() throws InterruptedException {
        RecordedRequest last = null, request;
        while ((request = server.takeRequest(100, TimeUnit.MILLISECONDS)) != null)
            last = request;
        assertNotNull(last, "no request recorded");
        return last;
    }

    /** The body is a JSON object whose keys are all snake_case (a camelCase twin makes Mistral answer 400). */
    static JsonNode assertSnakeCaseKeys(String body) throws Exception {
        JsonNode tree = new ObjectMapper().readTree(body);
        for (Iterator<String> names = tree.fieldNames(); names.hasNext(); ) {
            String name = names.next();
            assertFalse(name.matches(".*[A-Z].*"), "camelCase key " + name + " in " + body);
        }
        return tree;
    }

    /** A definition upload accepts text/plain, carries a Content-Length and sends the definition unchanged. */
    protected void assertDefinitionUpload(String resource) throws Exception {
        RecordedRequest request = lastRequest();
        String accept = request.getHeader("Accept");
        assertTrue(accept != null && accept.contains("text/plain"), "Accept: " + accept);
        assertNotNull(request.getHeader("Content-Length"));
        assertNull(request.getHeader("Transfer-Encoding"));
        assertEquals(request.getBody().readUtf8(),
                new String(getClass().getResourceAsStream(resource).readAllBytes(), StandardCharsets.UTF_8));
    }

    @Override
    protected Service service() {
        return Service.WORKFLOW;
    }
}
