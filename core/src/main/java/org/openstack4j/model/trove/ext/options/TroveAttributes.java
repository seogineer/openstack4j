package org.openstack4j.model.trove.ext.options;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base of the Trove create and update options: a fluent map that leaves unset fields out of the request body.
 *
 * @param <S> the concrete options type
 */
public abstract class TroveAttributes<S extends TroveAttributes<S>> {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    protected abstract S self();

    /** Sets a field; a null value leaves the field out of the body. */
    protected S put(String key, Object value) {
        if (value != null)
            fields.put(key, value);
        return self();
    }

    /**
     * Sets any field by its API name, including fields this class has no setter for.
     * Unlike the typed setters, a null value is sent as JSON null.
     */
    public S attribute(String key, Object value) {
        fields.put(key, value);
        return self();
    }

    /** @return a copy of the fields to send */
    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(fields);
    }
}
