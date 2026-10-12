package org.openstack4j.model.network.options;

import java.util.List;

/** Body of a BGP VPN create or update; only the fields set are sent. */
public class BgpvpnOptions extends NeutronAttributes<BgpvpnOptions> {

    public static BgpvpnOptions create() {
        return new BgpvpnOptions();
    }

    /** An update that sends only the fields set afterwards; setters marked "create only" are rejected by Neutron on update (400). */
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

    /** Create only (Neutron rejects it on update). {@code l3} (default) or {@code l2}. */
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

    /** Create only: Neutron rejects it on update (400). */
    public BgpvpnOptions vni(Integer value) {
        return put("vni", value);
    }

    public BgpvpnOptions localPref(Long value) {
        return put("local_pref", value);
    }

    /** Create only (Neutron rejects it on update). Admin only: the owner project. */
    public BgpvpnOptions projectId(String value) {
        return put("project_id", value);
    }
}
