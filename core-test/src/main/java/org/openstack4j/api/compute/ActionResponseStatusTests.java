package org.openstack4j.api.compute;

import java.util.HashMap;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Action;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Every 4xx/5xx answer of an ActionResponse call is a failure carrying the response status.
 */
@Test(suiteName = "Compute/ActionResponseStatus")
public class ActionResponseStatusTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.COMPUTE;
    }

    private int requestsBefore;

    /**
     * Some connectors (Apache HttpClient 5) retry a 429/503 once, so those statuses are queued twice. Consumes every
     * request that was sent and drains a response that was left over, so nothing leaks into other tests.
     */
    private void respondRetryable(int status, String body) {
        requestsBefore = server.getRequestCount();
        respondWith(status, body);
        respondWith(status, body);
    }

    private void finish(String method, String pathSuffix, int responses) throws Exception {
        int made = server.getRequestCount() - requestsBefore;
        for (int i = 0; i < made; i++) {
            RecordedRequest request = takeRequest();
            assertEquals(request.getMethod(), method);
            assertTrue(request.getPath().endsWith(pathSuffix), request.getPath());
        }
        for (int i = made; i < responses; i++) {
            okhttp3.Response leftover = new okhttp3.OkHttpClient().newCall(
                    new okhttp3.Request.Builder().url(server.url("/drain")).build()).execute();
            leftover.close();
            takeRequest();
        }
    }

    private ActionResponse pause() throws Exception {
        return pause(1);
    }

    private ActionResponse pause(int responses) throws Exception {
        if (responses == 1) {
            requestsBefore = server.getRequestCount();
        }
        ActionResponse ar = osv3().compute().servers().action("s1", Action.PAUSE);
        finish("POST", "/servers/s1/action", responses);
        return ar;
    }

    @Test
    public void serviceUnavailableJson() throws Exception {
        respondRetryable(503, "{\"serviceUnavailable\":{\"message\":\"m\",\"code\":503}}");
        ActionResponse ar = pause(2);
        assertFalse(ar.isSuccess());
        assertEquals(ar.getCode(), 503);
        assertNotNull(ar.getFault());
        assertTrue(ar.getFault().contains("m"));
    }

    @Test
    public void serviceUnavailableEmptyBody() throws Exception {
        respondRetryable(503, "");
        ActionResponse ar = pause(2);
        assertFalse(ar.isSuccess());
        assertEquals(ar.getCode(), 503);
    }

    @Test
    public void badGatewayHtml() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/html");
        respondWith(headers, 502, "<html>Bad Gateway</html>");
        ActionResponse ar = pause();
        assertFalse(ar.isSuccess());
        assertEquals(ar.getCode(), 502);
    }

    @Test
    public void notImplemented() throws Exception {
        respondWith(501, "{\"notImplemented\":{\"message\":\"x\",\"code\":501}}");
        ActionResponse ar = osv3().compute().servers().detachVolume("s1", "v1");
        RecordedRequest request = takeRequest();
        assertEquals(request.getMethod(), "DELETE");
        assertTrue(request.getPath().endsWith("/servers/s1/os-volume_attachments/v1"), request.getPath());
        assertFalse(ar.isSuccess());
        assertEquals(ar.getCode(), 501);
    }

    @Test
    public void tooManyRequests() throws Exception {
        respondRetryable(429, "{\"overLimit\":{\"message\":\"slow down\",\"code\":429}}");
        ActionResponse ar = pause(2);
        assertFalse(ar.isSuccess());
        assertEquals(ar.getCode(), 429);
    }

    @Test
    public void gone() throws Exception {
        respondWith(410, "{\"gone\":{\"message\":\"gone\",\"code\":410}}");
        ActionResponse ar = pause();
        assertFalse(ar.isSuccess());
        assertEquals(ar.getCode(), 410);
    }

    @Test
    public void acceptedEmptyBodyIsSuccess() throws Exception {
        respondWith(202);
        ActionResponse ar = pause();
        assertTrue(ar.isSuccess());
    }

    @Test
    public void notFoundStillFails() throws Exception {
        respondWith(404, "{\"itemNotFound\":{\"message\":\"nope\",\"code\":404}}");
        ActionResponse ar = pause();
        assertFalse(ar.isSuccess());
        assertEquals(ar.getCode(), 404);
        assertTrue(ar.getFault().contains("nope"));
    }

    @Test
    public void conflictKeepsMessageAndCode() throws Exception {
        respondWith(409, "{\"conflictingRequest\":{\"message\":\"busy\",\"code\":409}}");
        ActionResponse ar = pause();
        assertFalse(ar.isSuccess());
        assertEquals(ar.getCode(), 409);
        assertTrue(ar.getFault().contains("busy"));
    }

    @Test
    public void internalServerErrorJson() throws Exception {
        respondWith(500, "{\"computeFault\":{\"message\":\"boom\",\"code\":500}}");
        ActionResponse ar = pause();
        assertFalse(ar.isSuccess());
        assertEquals(ar.getCode(), 500);
    }

    @Test
    public void badRequestEmptyBody() throws Exception {
        respondWith(400);
        ActionResponse ar = pause();
        assertFalse(ar.isSuccess());
        assertEquals(ar.getCode(), 400);
    }
}
