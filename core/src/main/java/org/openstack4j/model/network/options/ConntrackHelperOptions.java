package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a router conntrack helper create or update. */
public class ConntrackHelperOptions extends NeutronAttributes<ConntrackHelperOptions> {

    public static ConntrackHelperOptions create(String protocol, int port, String helper) {
        return new ConntrackHelperOptions().protocol(Objects.requireNonNull(protocol)).port(port).helper(Objects.requireNonNull(helper));
    }

    public static ConntrackHelperOptions update() {
        return new ConntrackHelperOptions();
    }

    @Override
    protected ConntrackHelperOptions self() {
        return this;
    }

    public ConntrackHelperOptions protocol(String protocol) { return put("protocol", protocol); }
    public ConntrackHelperOptions port(Integer port) { return put("port", port); }
    public ConntrackHelperOptions helper(String helper) { return put("helper", helper); }
}
