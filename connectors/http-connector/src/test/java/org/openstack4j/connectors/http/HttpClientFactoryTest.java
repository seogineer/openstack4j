package org.openstack4j.connectors.http;

import javax.net.ssl.SSLContext;

import org.openstack4j.core.transport.Config;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HttpClientFactoryTest {

    @Test
    public void sameConfigReusesClient() {
        Config config = Config.newConfig().withReadTimeout(1234);

        Assert.assertSame(HttpClientFactory.get(config), HttpClientFactory.get(config));
    }

    @Test
    public void cacheStaysBoundedWhenEveryCallBringsANewSslContext() throws Exception {
        // e.g. an application that reloads certificates and builds a new SSLContext for every OSFactory call
        for (int i = 0; i < 100; i++) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            HttpClientFactory.get(Config.newConfig().withSSLContext(sslContext));
        }

        Assert.assertTrue(HttpClientFactory.cachedClients() <= HttpClientFactory.MAX_CACHED_CLIENTS,
                "cached clients: " + HttpClientFactory.cachedClients());
    }
}
