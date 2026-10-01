package org.openstack4j.connectors.http;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.UntrustedSSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds one JDK {@link HttpClient} per {@link Config} and reuses it, because each client owns a selector thread
 * and a connection pool.
 */
final class HttpClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientFactory.class);
    private static final Map<Config, HttpClient> CLIENTS = new ConcurrentHashMap<>();

    private HttpClientFactory() {
    }

    static HttpClient get(Config config) {
        return CLIENTS.computeIfAbsent(config != null ? config : Config.DEFAULT, HttpClientFactory::build);
    }

    private static HttpClient build(Config config) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL);

        if (config.getConnectTimeout() > 0)
            builder.connectTimeout(Duration.ofMillis(config.getConnectTimeout()));

        if (config.getProxy() != null)
            builder.proxy(ProxySelector.of(new InetSocketAddress(config.getProxy().getRawHost(), config.getProxy().getPort())));

        if (config.isIgnoreSSLVerification())
            builder.sslContext(UntrustedSSL.getSSLContext());

        if (config.getSslContext() != null)
            builder.sslContext(config.getSslContext());

        if (config.getHostNameVerifier() != null)
            LOG.warn("Config.withHostnameVerifier is not supported by the JDK HttpClient connector and is ignored. "
                    + "Use -Djdk.internal.httpclient.disableHostnameVerification=true to disable hostname verification.");

        return builder.build();
    }
}
