package org.openstack4j.connectors.httpclient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.UntrustedSSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the initial HttpClient and keeps it as a singleton to preserve pooling strategies within the Http Client
 *
 * @author Jeremy Unruh
 */
public class HttpClientFactory {

    public static final HttpClientFactory INSTANCE = new HttpClientFactory();
    private static final String USER_AGENT = "OpenStack4j-Agent";
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientFactory.class);
    private static HttpClientConfigInterceptor INTERCEPTOR;
    private CloseableHttpClient client;

    /**
     * Registers a HttpClientConfigInterceptor that is invoked prior to a new HttpClient being created.
     *
     * @param interceptor the http config interceptor
     */
    public static void registerInterceptor(HttpClientConfigInterceptor interceptor) {
        INTERCEPTOR = interceptor;
    }

    /**
     * Creates or Returns an existing HttpClient
     *
     * @param config the configuration
     * @return CloseableHttpClient
     */
    CloseableHttpClient getClient(Config config) {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = buildClient(config);
                }
            }
        }
        return client;
    }

    private CloseableHttpClient buildClient(Config config) {
        HttpClientBuilder cb = HttpClientBuilder.create().setUserAgent(USER_AGENT);

        if (config.getProxy() != null) {
            try {
                URL url = new URL(config.getProxy().getHost());
                cb.setProxy(new HttpHost(url.getProtocol(), url.getHost(), config.getProxy().getPort()));
            } catch (MalformedURLException e) {
                LOG.error(e.getMessage(), e);
            }
        }

        PoolingHttpClientConnectionManagerBuilder cmb = PoolingHttpClientConnectionManagerBuilder.create();

        SSLContext sslContext = null;
        HostnameVerifier hostnameVerifier = null;
        if (config.isIgnoreSSLVerification()) {
            sslContext = UntrustedSSL.getSSLContext();
            hostnameVerifier = NoopHostnameVerifier.INSTANCE;
        }
        if (config.getSslContext() != null)
            sslContext = config.getSslContext();
        if (config.getHostNameVerifier() != null)
            hostnameVerifier = config.getHostNameVerifier();
        if (sslContext != null || hostnameVerifier != null) {
            cmb.setTlsSocketStrategy(new DefaultClientTlsStrategy(
                    sslContext != null ? sslContext : SSLContexts.createDefault(),
                    hostnameVerifier != null ? hostnameVerifier : HttpsSupport.getDefaultHostnameVerifier()));
        }

        if (config.getMaxConnections() > 0)
            cmb.setMaxConnTotal(config.getMaxConnections());

        if (config.getMaxConnectionsPerRoute() > 0)
            cmb.setMaxConnPerRoute(config.getMaxConnectionsPerRoute());

        // Validate pooled connections on every lease, as HttpClient 4.3's stale check did; otherwise a connection
        // the server (or a load balancer) already closed makes the next non-idempotent request fail
        ConnectionConfig.Builder ccb = ConnectionConfig.custom().setValidateAfterInactivity(TimeValue.ZERO_MILLISECONDS);
        if (config.getConnectTimeout() > 0)
            ccb.setConnectTimeout(Timeout.ofMilliseconds(config.getConnectTimeout()));
        cmb.setDefaultConnectionConfig(ccb.build());

        RequestConfig.Builder rcb = RequestConfig.custom();

        if (config.getReadTimeout() > 0)
            rcb.setResponseTimeout(Timeout.ofMilliseconds(config.getReadTimeout()));

        cb.setConnectionManager(cmb.build());

        if (INTERCEPTOR != null) {
            INTERCEPTOR.onClientCreate(cb, rcb, config);
        }

        return cb.setDefaultRequestConfig(rcb.build()).build();
    }
}
