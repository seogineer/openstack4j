package org.openstack4j.connectors.httpclient;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.HttpRequest;
import org.openstack4j.core.transport.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class HttpClientConnectorTest {

    private MockWebServer server;

    @BeforeMethod
    public void startServer() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterMethod(alwaysRun = true)
    public void stopServer() throws Exception {
        server.shutdown();
    }

    private HttpRequest.RequestBuilder<Void> request(HttpMethod method) {
        return HttpRequest.builder()
                .endpoint(server.url("/").toString())
                .path("v3/resource")
                .method(method)
                .config(Config.newConfig());
    }

    private HttpResponse execute(HttpRequest<Void> request) {
        HttpResponse response = new HttpExecutorServiceImpl().execute(request);
        Assert.assertNotNull(response, "connector returned null (exception was swallowed and logged)");
        return response;
    }

    @Test
    public void patchIsSentWithBody() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        HttpResponse response = execute(request(HttpMethod.PATCH).json("{\"name\":\"x\"}").build());
        RecordedRequest recorded = server.takeRequest(5, TimeUnit.SECONDS);

        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertEquals(recorded.getMethod(), "PATCH");
        Assert.assertEquals(recorded.getBody().readUtf8(), "{\"name\":\"x\"}");
        response.close();
    }

    @Test
    public void callerSuppliedContentLengthHeaderIsIgnored() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201));

        HttpResponse response = execute(request(HttpMethod.PUT).header("Content-Length", 0).build());
        RecordedRequest recorded = server.takeRequest(5, TimeUnit.SECONDS);

        Assert.assertEquals(response.getStatus(), 201);
        Assert.assertEquals(recorded.getMethod(), "PUT");
        Assert.assertEquals(recorded.getBodySize(), 0);
        response.close();
    }

    @Test
    public void queryParamsAreEncoded() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        HttpResponse response = execute(request(HttpMethod.GET)
                .queryParam("name", "a b").queryParam("tag", "x").queryParam("tag", "y").build());
        RecordedRequest recorded = server.takeRequest(5, TimeUnit.SECONDS);

        Assert.assertEquals(recorded.getRequestUrl().queryParameter("name"), "a b");
        Assert.assertEquals(recorded.getRequestUrl().queryParameterValues("tag"), Arrays.asList("x", "y"));
        response.close();
    }

    @Test
    public void responseBodyCanBeReadAfterExecute() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json").setBody("{\"name\":\"x\"}"));

        HttpResponse response = execute(request(HttpMethod.GET).build());

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body = response.readEntity(java.util.Map.class);
        Assert.assertEquals(body.get("name"), "x");
        response.close();
    }

    @Test
    public void postSucceedsAfterServerClosedPooledConnection() throws Exception {
        // the server answers and then drops the kept-alive connection, which stays in the client pool
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}").setSocketPolicy(SocketPolicy.DISCONNECT_AT_END));
        server.enqueue(new MockResponse().setResponseCode(201).setBody("{}"));
        execute(request(HttpMethod.GET).build()).close();

        HttpResponse response = execute(request(HttpMethod.POST).json("{}").build());

        Assert.assertEquals(response.getStatus(), 201);
        response.close();
    }

    @Test
    public void postIsNotRetriedOn503() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        HttpResponse response = execute(request(HttpMethod.POST).json("{}").build());

        Assert.assertEquals(response.getStatus(), 503);
        Assert.assertEquals(server.getRequestCount(), 1);
        response.close();
    }

    @Test
    public void postIsNotRetriedOn429AndDoesNotWait() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", "5"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        long start = System.nanoTime();
        HttpResponse response = execute(request(HttpMethod.POST).json("{}").build());
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        Assert.assertEquals(response.getStatus(), 429);
        Assert.assertEquals(server.getRequestCount(), 1);
        Assert.assertTrue(elapsedMs < 3000, "waited " + elapsedMs + "ms");
        response.close();
    }

    @Test
    public void getIsRetriedOn503() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        HttpResponse response = execute(request(HttpMethod.GET).build());

        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertEquals(server.getRequestCount(), 2);
        response.close();
    }

    @Test
    public void patchIsNotRetriedOn503() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        HttpResponse response = execute(request(HttpMethod.PATCH).json("{}").build());

        Assert.assertEquals(response.getStatus(), 503);
        Assert.assertEquals(server.getRequestCount(), 1);
        response.close();
    }

    @Test
    public void strategyReadsRequestFromAttributeOrField() {
        HttpClientFactory.IdempotentResponseRetryStrategy strategy = new HttpClientFactory.IdempotentResponseRetryStrategy();
        BasicHttpResponse unavailable = new BasicHttpResponse(503);
        unavailable.setVersion(HttpVersion.HTTP_1_1);

        // exercises the attribute path (httpclient5 5.3.x); the field path (5.5+) is covered by the request-level tests above
        HttpClientContext get = HttpClientContext.create();
        get.setAttribute(HttpCoreContext.HTTP_REQUEST, new BasicHttpRequest("GET", "/x"));
        Assert.assertTrue(strategy.retryRequest(unavailable, 1, get));

        HttpClientContext post = HttpClientContext.create();
        post.setAttribute(HttpCoreContext.HTTP_REQUEST, new BasicHttpRequest("POST", "/x"));
        Assert.assertFalse(strategy.retryRequest(unavailable, 1, post));

        Assert.assertFalse(strategy.retryRequest(unavailable, 1, HttpClientContext.create()));
        Assert.assertFalse(strategy.retryRequest(unavailable, 1, new BasicHttpContext()));
        Assert.assertFalse(strategy.retryRequest(unavailable, 1, null));
        Assert.assertFalse(strategy.retryRequest(unavailable, 2, get));
    }
}
