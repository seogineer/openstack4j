package org.openstack4j.model.network.options;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Body of a service graph create or update; only the fields set are sent. */
public class SfcServiceGraphOptions extends NeutronAttributes<SfcServiceGraphOptions> {

    public static SfcServiceGraphOptions create(Map<String, List<String>> portChains) {
        return new SfcServiceGraphOptions().put("port_chains", Objects.requireNonNull(portChains, "portChains"));
    }

    /** An update that sends only the fields set afterwards. */
    public static SfcServiceGraphOptions update() {
        return new SfcServiceGraphOptions();
    }

    @Override
    protected SfcServiceGraphOptions self() {
        return this;
    }

    public SfcServiceGraphOptions portChains(Map<String, List<String>> value) {
        return put("port_chains", value);
    }

    public SfcServiceGraphOptions name(String value) {
        return put("name", value);
    }

    public SfcServiceGraphOptions description(String value) {
        return put("description", value);
    }
}
