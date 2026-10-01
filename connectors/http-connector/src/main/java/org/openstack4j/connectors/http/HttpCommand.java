package org.openstack4j.connectors.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.HttpRequest;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.util.IOUtil;

/**
 * HttpCommand is responsible for executing the actual request driven by the
 * HttpExecutor.
 */
public final class HttpCommand<R> {

    /** Headers the JDK HttpClient sets itself and refuses to accept from callers. */
    private static final Set<String> RESTRICTED_HEADERS = Set.of("connection", "content-length", "expect", "host", "upgrade");

    private final HttpRequest<R> request;
    private int retries;

    private HttpCommand(HttpRequest<R> request) {
        this.request = request;
    }

    /**
     * Creates a new HttpCommand from the given request
     *
     * @param request the request
     * @return the command
     */
    public static <R> HttpCommand<R> create(HttpRequest<R> request) {
        return new HttpCommand<>(request);
    }

    /**
     * Executes the command and returns the Response
     *
     * @return the response
     */
    public HttpResponse execute() throws IOException, InterruptedException {
        java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder(URI.create(request.getUrl()))
                .method(request.getMethod().name(), bodyPublisher());

        Config config = request.getConfig();
        if (config != null && config.getReadTimeout() > 0)
            builder.timeout(Duration.ofMillis(config.getReadTimeout()));

        if (request.getContentType() != null)
            builder.setHeader("Content-Type", request.getContentType());
        builder.setHeader("Accept", "application/json; charset=utf-8");

        if (request.hasHeaders()) {
            for (Map.Entry<String, Object> h : request.getHeaders().entrySet()) {
                if (!RESTRICTED_HEADERS.contains(h.getKey().toLowerCase(Locale.ROOT)))
                    builder.setHeader(h.getKey(), String.valueOf(h.getValue()));
            }
        }

        java.net.http.HttpResponse<byte[]> response = send(HttpClientFactory.get(config), builder.build());
        return HttpResponseImpl.wrap(response.headers().map(), response.statusCode(),
                HttpResponseImpl.reasonPhrase(response.statusCode()), response.body());
    }

    /**
     * Sends once more when the first attempt fails with an I/O error, which is what a pooled keep-alive connection
     * already closed by the server (or a load balancer) produces. The JDK client only retries idempotent methods
     * itself; HttpURLConnection, which this connector used before, also retried POST once. Timeouts are not retried.
     */
    private static java.net.http.HttpResponse<byte[]> send(HttpClient client, java.net.http.HttpRequest jdkRequest)
            throws IOException, InterruptedException {
        try {
            return client.send(jdkRequest, BodyHandlers.ofByteArray());
        } catch (HttpTimeoutException | ConnectException e) {
            throw e;
        } catch (IOException e) {
            return client.send(jdkRequest, BodyHandlers.ofByteArray());
        }
    }

    private BodyPublisher bodyPublisher() throws IOException {
        Object entity = request.getEntity();
        if (entity != null) {
            if (entity instanceof InputStream)
                return BodyPublishers.ofByteArray(IOUtil.readBytes((InputStream) entity));
            String content = ObjectMapperSingleton.getContext(entity.getClass()).writer().writeValueAsString(entity);
            return BodyPublishers.ofString(content, StandardCharsets.UTF_8);
        }
        if (request.hasJson())
            return BodyPublishers.ofString(request.getJson(), StandardCharsets.UTF_8);
        return BodyPublishers.noBody();
    }

    /**
     * @return true if a request entity has been set
     */
    public boolean hasEntity() {
        return request.getEntity() != null;
    }

    /**
     * @return current retry execution count for this command
     */
    public int getRetries() {
        return retries;
    }

    /**
     * The JDK request is rebuilt from {@link #getRequest()} on every {@link #execute()}, so updated headers
     * (for example a refreshed auth token) are picked up without re-initialisation.
     *
     * @return incremement's the retry count and returns self
     */
    public HttpCommand<R> incrementRetriesAndReturn() {
        retries++;
        return this;
    }

    public HttpRequest<R> getRequest() {
        return request;
    }
}
