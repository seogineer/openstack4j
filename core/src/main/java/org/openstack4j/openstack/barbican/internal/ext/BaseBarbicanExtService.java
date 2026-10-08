package org.openstack4j.openstack.barbican.internal.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.openstack.barbican.internal.BaseBarbicanServices;

/**
 * Shared helpers of the Barbican services added in 4.6. A 404 from a list, create, update or a call that returns a
 * model is raised; single gets return null and ActionResponse calls report a failed response.
 */
public abstract class BaseBarbicanExtService extends BaseBarbicanServices {

    public static <T> ExecutionOptions<T> propagate404() {
        return ExecutionOptions.create(PropagateOnStatus.on(404));
    }

    /** @return {@code value}, which must be a non-blank path segment (no {@code /}, {@code ?} or {@code #}) */
    protected static String id(String value) {
        Objects.requireNonNull(value, "id");
        if (value.isBlank() || value.indexOf('/') >= 0 || value.indexOf('?') >= 0 || value.indexOf('#') >= 0)
            throw new IllegalArgumentException("Not a valid identifier: '" + value + "'");
        return value;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> mapOf(String path) {
        return get(Map.class, path).execute(propagate404());
    }

    /** @return the list of objects under {@code key}; a missing parent raises */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> mapsOf(String path, String key) {
        Map<String, Object> body = mapOf(path);
        Object list = body == null ? null : body.get(key);
        return list instanceof List ? (List<Map<String, Object>>) list : Collections.emptyList();
    }

    protected static byte[] bytes(HttpResponse response) {
        try (InputStream in = response.getInputStream()) {
            return in == null ? new byte[0] : in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
