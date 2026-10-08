package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a BGP speaker create or update; only the fields set are sent. */
public class BgpSpeakerOptions extends NeutronAttributes<BgpSpeakerOptions> {

    public static BgpSpeakerOptions create(String name, Long localAs, Integer ipVersion) {
        return new BgpSpeakerOptions().put("name", Objects.requireNonNull(name, "name")).put("local_as", Objects.requireNonNull(localAs, "localAs")).put("ip_version", Objects.requireNonNull(ipVersion, "ipVersion"));
    }

    /** An update that sends only the fields set afterwards. */
    public static BgpSpeakerOptions update() {
        return new BgpSpeakerOptions();
    }

    @Override
    protected BgpSpeakerOptions self() {
        return this;
    }

    public BgpSpeakerOptions name(String value) {
        return put("name", value);
    }

    public BgpSpeakerOptions localAs(Long value) {
        return put("local_as", value);
    }

    public BgpSpeakerOptions ipVersion(Integer value) {
        return put("ip_version", value);
    }

    public BgpSpeakerOptions advertiseFloatingIpHostRoutes(Boolean value) {
        return put("advertise_floating_ip_host_routes", value);
    }

    public BgpSpeakerOptions advertiseTenantNetworks(Boolean value) {
        return put("advertise_tenant_networks", value);
    }
}
