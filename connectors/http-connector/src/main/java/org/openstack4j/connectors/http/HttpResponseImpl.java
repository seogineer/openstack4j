package org.openstack4j.connectors.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.core.transport.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponseImpl implements HttpResponse {

    private static final Logger LOG = LoggerFactory.getLogger(HttpResponseImpl.class);

    private Map<String, List<String>> headers;
    private int responseCode;
    private String responseMessage;
    private byte[] data;

    private HttpResponseImpl(Map<String, List<String>> headers,
            int responseCode, String responseMessage, byte[] data) {
        this.headers = headers;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.data = data;
    }

    /**
     * Wrap the given Response
     *
     * @return the HttpResponse
     */
    public static HttpResponseImpl wrap(Map<String, List<String>> headers,
            int responseCode, String responseMessage, byte[] data) {
        return new HttpResponseImpl(headers, responseCode, responseMessage, data);
    }

    /**
     * Gets the entity and Maps any errors which will result in a
     * ResponseException
     *
     * @param <T> the generic type
     * @param returnType the return type
     * @return the entity
     */
    public <T> T getEntity(Class<T> returnType) {
        return getEntity(returnType, null);
    }

    /**
     * Gets the entity and Maps any errors which will result in a
     * ResponseException
     *
     * @param <T> the generic type
     * @param returnType the return type
     * @param options the execution options
     * @return the entity
     */
    @Override
    public <T> T getEntity(Class<T> returnType, ExecutionOptions<T> options) {
        return HttpEntityHandler.handle(this, returnType, options, Boolean.TRUE);
    }

    /**
     * Gets the status from the previous Request
     *
     * @return the status code
     */
    public int getStatus() {
        return responseCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStatusMessage() {
        return responseMessage;
    }

    /**
     * @return the input stream
     */
    public InputStream getInputStream() {
        if (data == null)
            return null;

        return new ByteArrayInputStream(data);
    }

    /**
     * Returns a Header value from the specified name key
     *
     * @param name the name of the header to query for
     * @return the header as a String or null if not found
     */
    public String header(String name) {
        if (name == null) return null;
        for (String key : headers.keySet()) {
            if (key != null && key.equalsIgnoreCase(name)) {
                return headers.get(key).get(0);
            }
        }
        return null;
    }

    /**
     * @return the a Map of Header Name to Header Value, with case-insensitive keys
     */
    public Map<String, String> headers() {
        Map<String, String> retHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() == null) continue;
            for (String value : entry.getValue()) {
                retHeaders.put(canonicalName(entry.getKey()), value);
            }
        }

        return retHeaders;
    }

    /**
     * The JDK HttpClient lower-cases header names. Callers derive keys from them (Swift metadata, for example), so
     * restore the usual capitalisation: "x-container-meta-year" becomes "X-Container-Meta-Year".
     */
    static String canonicalName(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        boolean upper = true;
        for (char c : name.toCharArray()) {
            sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
            upper = c == '-';
        }
        return sb.toString();
    }

    /**
     * The JDK HttpClient does not expose the reason phrase, so map the status codes OpenStack APIs commonly return.
     */
    static String reasonPhrase(int status) {
        switch (status) {
            case 200: return "OK";
            case 201: return "Created";
            case 202: return "Accepted";
            case 204: return "No Content";
            case 300: return "Multiple Choices";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 409: return "Conflict";
            case 413: return "Request Entity Too Large";
            case 415: return "Unsupported Media Type";
            case 500: return "Internal Server Error";
            case 501: return "Not Implemented";
            case 503: return "Service Unavailable";
            default: return "";
        }
    }

    @Override
    public <T> T readEntity(Class<T> typeToReadAs) {

        if (data == null) {
            return null;
        }

        try {
            return ObjectMapperSingleton.getContext(typeToReadAs).reader(typeToReadAs).readValue(data);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ClientResponseException(e.getMessage(), 0, e);
        }
    }

    @Override
    public void close() throws IOException {
        // Not Implemented - closing handle by HttpCommand
    }

    @Override
    public String getContentType() {
        return header(ClientConstants.HEADER_CONTENT_TYPE);
    }
}
