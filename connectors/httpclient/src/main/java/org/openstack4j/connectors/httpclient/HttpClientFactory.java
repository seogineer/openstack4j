package org.openstack4j.connectors.httpclient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
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
            SSLContext ctx = sslContext != null ? sslContext : SSLContexts.createDefault();
            HostnameVerifier verifier = hostnameVerifier != null ? hostnameVerifier : HttpsSupport.getDefaultHostnameVerifier();
            try {
                TlsStrategy54.apply(cmb, ctx, verifier);
            } catch (NoClassDefFoundError | NoSuchMethodError e) {
                // httpclient5 before 5.4 (e.g. pinned by the application) has no TlsSocketStrategy
                LOG.debug("httpclient5 < 5.4 on the classpath, using SSLConnectionSocketFactory: {}", e.toString());
                setSslSocketFactory(cmb, ctx, verifier);
            }
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

        // HttpClient 5 retries 429/503 once for any method; only idempotent requests may be repeated, and not after a Retry-After longer than 30 seconds.
        // Set before the interceptor so applications can still replace it (setRetryStrategy / disableAutomaticRetries).
        cb.setRetryStrategy(new IdempotentResponseRetryStrategy());

        if (INTERCEPTOR != null) {
            INTERCEPTOR.onClientCreate(cb, rcb, config);
        }

        return cb.setDefaultRequestConfig(rcb.build()).build();
    }

    /**
     * The default strategy retries a 429/503 response once whatever the method, which sends a POST or PATCH twice,
     * and waits for whatever Retry-After says. Only idempotent requests are repeated, and only when the wait is at most
     * {@code maxRetryAfter} (default 30 seconds); otherwise the 429/503 is returned at once. I/O error retries are
     * unchanged (already idempotent-only). Replace it with {@code HttpClientConfigInterceptor}:
     * {@code cb.setRetryStrategy(new HttpClientFactory.IdempotentResponseRetryStrategy(Duration.ofMinutes(2)))}.
     */
    public static final class IdempotentResponseRetryStrategy extends DefaultHttpRequestRetryStrategy {
        /** Default upper bound on a Retry-After wait. */
        public static final Duration DEFAULT_MAX_RETRY_AFTER = Duration.ofSeconds(30);

        private final long maxRetryAfterMillis;

        public IdempotentResponseRetryStrategy() {
            this(DEFAULT_MAX_RETRY_AFTER);
        }

        /** @param maxRetryAfter longest Retry-After to wait for; a longer one is not retried. Not null, not negative. */
        public IdempotentResponseRetryStrategy(Duration maxRetryAfter) {
            Objects.requireNonNull(maxRetryAfter, "maxRetryAfter");
            if (maxRetryAfter.isNegative())
                throw new IllegalArgumentException("maxRetryAfter must not be negative: " + maxRetryAfter);
            long millis;
            try {
                millis = maxRetryAfter.toMillis();
            } catch (ArithmeticException e) {
                millis = Long.MAX_VALUE;
            }
            this.maxRetryAfterMillis = millis;
        }

        @Override
        public boolean retryRequest(HttpResponse response, int execCount, HttpContext context) {
            HttpRequest request = currentRequest(context);
            return request != null && Method.isIdempotent(request.getMethod())
                    && super.retryRequest(response, execCount, context)
                    // httpclient5 5.5+ sleeps for any Retry-After (5.3 only capped it by the response timeout)
                    && getRetryInterval(response, execCount, context).toMilliseconds() <= maxRetryAfterMillis;
        }

        /** httpclient5 5.3.x keeps the request in the "http.request" attribute, 5.5+ in a field of the context. */
        static HttpRequest currentRequest(HttpContext context) {
            if (context == null) return null;
            if (context instanceof HttpCoreContext) {
                HttpRequest request = ((HttpCoreContext) context).getRequest();
                if (request != null) return request;
            }
            Object attribute = context.getAttribute(HttpCoreContext.HTTP_REQUEST);
            return attribute instanceof HttpRequest ? (HttpRequest) attribute : null;
        }
    }

    /**
     * httpclient5 5.4+. A separate class so that the verifier only resolves {@code TlsSocketStrategy} when this is
     * used; inside {@link HttpClientFactory} it would fail the whole factory on an older httpclient5.
     */
    private static final class TlsStrategy54 {
        static void apply(PoolingHttpClientConnectionManagerBuilder cmb, SSLContext ctx, HostnameVerifier verifier) {
            // CLIENT: only the given verifier checks the host name. The (SSLContext, HostnameVerifier) constructor
            // means BOTH, so the JSSE built-in check still rejected a mismatched certificate with SSL verification off
            cmb.setTlsSocketStrategy(new DefaultClientTlsStrategy(ctx, HostnameVerificationPolicy.CLIENT, verifier));
        }
    }

    /** httpclient5 before 5.4 (5.2–5.3 in practice); deprecated since 5.4. */
    @SuppressWarnings("deprecation")
    private static void setSslSocketFactory(PoolingHttpClientConnectionManagerBuilder cmb, SSLContext ctx, HostnameVerifier verifier) {
        cmb.setSSLSocketFactory(new SSLConnectionSocketFactory(ctx, verifier));
    }
}
