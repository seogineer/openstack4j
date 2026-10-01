package org.openstack4j.connectors.httpclient;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
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
}
