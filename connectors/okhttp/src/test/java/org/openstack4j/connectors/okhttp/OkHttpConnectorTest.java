package org.openstack4j.connectors.okhttp;

import javax.net.ssl.SSLContext;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.HttpRequest;
import org.openstack4j.core.transport.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OkHttpConnectorTest {

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

    @Test
    public void customSslContextDoesNotThrow() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        Config config = Config.newConfig().withSSLContext(SSLContext.getDefault());

        HttpResponse response = new HttpExecutorServiceImpl().execute(HttpRequest.builder()
                .endpoint(server.url("/").toString())
                .path("v3/resource")
                .method(HttpMethod.GET)
                .config(config)
                .build());

        Assert.assertNotNull(response, "connector returned null (exception was swallowed and logged)");
        Assert.assertEquals(response.getStatus(), 200);
        response.close();
    }
}
