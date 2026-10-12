package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a VPN service create or update; only the fields set are sent. */
public class VpnServiceOptions extends NeutronAttributes<VpnServiceOptions> {

    public static VpnServiceOptions create(String routerId) {
        return new VpnServiceOptions().put("router_id", Objects.requireNonNull(routerId, "routerId"));
    }

    /** An update that sends only the fields set afterwards; setters marked "create only" are rejected by Neutron on update (400). */
    public static VpnServiceOptions update() {
        return new VpnServiceOptions();
    }

    @Override
    protected VpnServiceOptions self() {
        return this;
    }

    /** Create only: Neutron rejects it on update (400). */
    public VpnServiceOptions routerId(String value) {
        return put("router_id", value);
    }

    public VpnServiceOptions name(String value) {
        return put("name", value);
    }

    public VpnServiceOptions description(String value) {
        return put("description", value);
    }

    /** Create only (Neutron rejects it on update). Only for VPN services without endpoint groups (deprecated by Neutron). */
    public VpnServiceOptions subnetId(String value) {
        return put("subnet_id", value);
    }

    /** Create only: Neutron rejects it on update (400). */
    public VpnServiceOptions flavorId(String value) {
        return put("flavor_id", value);
    }

    public VpnServiceOptions adminStateUp(Boolean value) {
        return put("admin_state_up", value);
    }
}
