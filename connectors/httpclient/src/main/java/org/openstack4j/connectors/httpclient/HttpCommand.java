package org.openstack4j.connectors.httpclient;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.routing.RoutingSupport;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.openstack4j.api.exceptions.ConnectionException;
import org.openstack4j.core.transport.HttpRequest;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.core.transport.functions.EndpointURIFromRequestFunction;

/**
 * HttpCommand is responsible for executing the actual request driven by the
 * HttpExecutor.
 */
public final class HttpCommand<R> {

    HttpUriRequestBase clientReq;
    private HttpRequest<R> request;
    private CloseableHttpClient client;
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
        HttpCommand<R> command = new HttpCommand<R>(request);
        command.initialize();
        return command;
    }

    private void initialize() {
        URI url = null;
        try {
            url = populateQueryParams(request);
        } catch (URISyntaxException e) {
            throw new ConnectionException(e.getMessage(), e.getIndex(), e);
        }
        client = HttpClientFactory.INSTANCE.getClient(request.getConfig());

        switch (request.getMethod()) {
            case POST:
                clientReq = new HttpPost(url);
                break;
            case PUT:
                clientReq = new HttpPut(url);
                break;
            case DELETE:
                clientReq = new HttpDelete(url);
                break;
            case HEAD:
                clientReq = new HttpHead(url);
                break;
            case PATCH:
                clientReq = new HttpPatch(url);
                break;
            case GET:
                clientReq = new HttpGet(url);
                break;
            default:
                throw new IllegalArgumentException("Unsupported http method: " + request.getMethod());
        }
        clientReq.setHeader("Accept", "application/json");
        populateHeaders(request);
    }

    /**
     * Executes the command and returns the Response
     *
     * @return the response
     */
    public ClassicHttpResponse execute() throws Exception {
        if (request.getEntity() != null) {
            if (InputStream.class.isAssignableFrom(request.getEntity().getClass())) {
                clientReq.setEntity(new InputStreamEntity((InputStream) request.getEntity(), -1,
                        ContentType.parse(request.getContentType())));
            } else {
                String json = ObjectMapperSingleton.getContext(request.getEntity().getClass()).writer()
                        .writeValueAsString(request.getEntity());
                clientReq.setEntity(new StringEntity(json,
                        ContentType.parse(request.getContentType()).withCharset(StandardCharsets.UTF_8)));
            }
        } else if (request.hasJson()) {
            clientReq.setEntity(new StringEntity(request.getJson(), ContentType.APPLICATION_JSON));
        }

        return client.executeOpen(RoutingSupport.determineHost(clientReq), clientReq, null);
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
     * @return incremement's the retry count and returns self
     */
    public HttpCommand<R> incrementRetriesAndReturn() {
        initialize();
        retries++;
        return this;
    }

    public HttpRequest<R> getRequest() {
        return request;
    }

    private URI populateQueryParams(HttpRequest<R> request) throws URISyntaxException {

        URIBuilder uri = new URIBuilder(new EndpointURIFromRequestFunction().apply(request));

        if (!request.hasQueryParams())
            return uri.build();

        for (Map.Entry<String, List<Object>> entry : request.getQueryParams().entrySet()) {
            for (Object o : entry.getValue()) {
                uri.addParameter(entry.getKey(), String.valueOf(o));
            }
        }
        return uri.build();
    }

    private void populateHeaders(HttpRequest<R> request) {

        if (!request.hasHeaders())
            return;

        for (Map.Entry<String, Object> h : request.getHeaders().entrySet()) {
            // HttpClient computes these from the entity and rejects requests that already carry them
            if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(h.getKey())
                    || HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(h.getKey()))
                continue;
            // a caller's Accept replaces the default application/json instead of adding a second one
            if (HttpHeaders.ACCEPT.equalsIgnoreCase(h.getKey()))
                clientReq.setHeader(h.getKey(), String.valueOf(h.getValue()));
            else
                clientReq.addHeader(h.getKey(), String.valueOf(h.getValue()));
        }
    }
}
