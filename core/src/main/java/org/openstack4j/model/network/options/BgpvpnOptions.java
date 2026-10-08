package org.openstack4j.model.network.options;

import java.util.List;

/** Body of a BGP VPN create or update; only the fields set are sent. */
public class BgpvpnOptions extends NeutronAttributes<BgpvpnOptions> {

    public static BgpvpnOptions create() {
        return new BgpvpnOptions();
    }

    /** An update that sends only the fields set afterwards. */
    public static BgpvpnOptions update() {
        return new BgpvpnOptions();
    }

    @Override
    protected BgpvpnOptions self() {
        return this;
    }

    public BgpvpnOptions name(String value) {
        return put("name", value);
    }

    /** {@code l3} (default) or {@code l2}. */
    public BgpvpnOptions type(String value) {
        return put("type", value);
    }

    public BgpvpnOptions routeTargets(List<String> value) {
        return put("route_targets", value);
    }

    public BgpvpnOptions importTargets(List<String> value) {
        return put("import_targets", value);
    }

    public BgpvpnOptions exportTargets(List<String> value) {
        return put("export_targets", value);
    }

    public BgpvpnOptions routeDistinguishers(List<String> value) {
        return put("route_distinguishers", value);
    }

    public BgpvpnOptions vni(Integer value) {
        return put("vni", value);
    }

    public BgpvpnOptions localPref(Integer value) {
        return put("local_pref", value);
    }

    /** Admin only: the owner project. */
    public BgpvpnOptions projectId(String value) {
        return put("project_id", value);
    }
}
