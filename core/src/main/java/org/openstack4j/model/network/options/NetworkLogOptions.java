package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a network log create or update. */
public class NetworkLogOptions extends NeutronAttributes<NetworkLogOptions> {

    public static NetworkLogOptions create(String resourceType) {
        return new NetworkLogOptions().put("resource_type", Objects.requireNonNull(resourceType));
    }

    /** An update that sends only the fields set afterwards. */
    public static NetworkLogOptions update() {
        return new NetworkLogOptions();
    }

    @Override
    protected NetworkLogOptions self() {
        return this;
    }

    public NetworkLogOptions name(String value) { return put("name", value); }
    public NetworkLogOptions description(String value) { return put("description", value); }
    public NetworkLogOptions enabled(Boolean value) { return put("enabled", value); }
    public NetworkLogOptions resourceId(String value) { return put("resource_id", value); }
    public NetworkLogOptions targetId(String value) { return put("target_id", value); }
    public NetworkLogOptions event(String value) { return put("event", value); }
}
