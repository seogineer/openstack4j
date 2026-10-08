package org.openstack4j.model.trove.ext.options;

import java.util.Map;
import java.util.Objects;

/** Body of a configuration group create or update; only the fields set are sent. */
public class ConfigurationOptions extends TroveAttributes<ConfigurationOptions> {

    public static ConfigurationOptions create(String name, Map<String, Object> values) {
        return new ConfigurationOptions().put("name", Objects.requireNonNull(name, "name")).put("values", Objects.requireNonNull(values, "values"));
    }

    /** An update that sends only the fields set afterwards. */
    public static ConfigurationOptions update() {
        return new ConfigurationOptions();
    }

    @Override
    protected ConfigurationOptions self() {
        return this;
    }

    public ConfigurationOptions name(String value) {
        return put("name", value);
    }

    public ConfigurationOptions values(Map<String, Object> value) {
        return put("values", value);
    }

    public ConfigurationOptions description(String value) {
        return put("description", value);
    }

    /** e.g. {@code {"type": "mysql", "version": "mysql-5.7"}}. */
    public ConfigurationOptions datastore(Map<String, Object> value) {
        return put("datastore", value);
    }
}
