package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a local IP create or update. */
public class LocalIpOptions extends NeutronAttributes<LocalIpOptions> {

    public static LocalIpOptions create() {
        return new LocalIpOptions();
    }

    /** An update that sends only the fields set afterwards. */
    public static LocalIpOptions update() {
        return new LocalIpOptions();
    }

    @Override
    protected LocalIpOptions self() {
        return this;
    }

    public LocalIpOptions name(String value) { return put("name", value); }
    public LocalIpOptions description(String value) { return put("description", value); }
    public LocalIpOptions networkId(String value) { return put("network_id", value); }
    public LocalIpOptions localPortId(String value) { return put("local_port_id", value); }
    public LocalIpOptions localIpAddress(String value) { return put("local_ip_address", value); }
    public LocalIpOptions ipMode(String value) { return put("ip_mode", value); }
}
