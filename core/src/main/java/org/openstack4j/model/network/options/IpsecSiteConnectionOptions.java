package org.openstack4j.model.network.options;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Body of a IPsec site connection create or update; only the fields set are sent. */
public class IpsecSiteConnectionOptions extends NeutronAttributes<IpsecSiteConnectionOptions> {

    public static IpsecSiteConnectionOptions create(String vpnserviceId, String ikepolicyId, String ipsecpolicyId, String peerAddress, String peerId, String psk) {
        return new IpsecSiteConnectionOptions().put("vpnservice_id", Objects.requireNonNull(vpnserviceId, "vpnserviceId")).put("ikepolicy_id", Objects.requireNonNull(ikepolicyId, "ikepolicyId")).put("ipsecpolicy_id", Objects.requireNonNull(ipsecpolicyId, "ipsecpolicyId")).put("peer_address", Objects.requireNonNull(peerAddress, "peerAddress")).put("peer_id", Objects.requireNonNull(peerId, "peerId")).put("psk", Objects.requireNonNull(psk, "psk"));
    }

    /** An update that sends only the fields set afterwards. */
    public static IpsecSiteConnectionOptions update() {
        return new IpsecSiteConnectionOptions();
    }

    @Override
    protected IpsecSiteConnectionOptions self() {
        return this;
    }

    public IpsecSiteConnectionOptions vpnserviceId(String value) {
        return put("vpnservice_id", value);
    }

    public IpsecSiteConnectionOptions ikepolicyId(String value) {
        return put("ikepolicy_id", value);
    }

    public IpsecSiteConnectionOptions ipsecpolicyId(String value) {
        return put("ipsecpolicy_id", value);
    }

    public IpsecSiteConnectionOptions peerAddress(String value) {
        return put("peer_address", value);
    }

    public IpsecSiteConnectionOptions peerId(String value) {
        return put("peer_id", value);
    }

    public IpsecSiteConnectionOptions psk(String value) {
        return put("psk", value);
    }

    public IpsecSiteConnectionOptions name(String value) {
        return put("name", value);
    }

    public IpsecSiteConnectionOptions description(String value) {
        return put("description", value);
    }

    /** Endpoint group of local subnets. */
    public IpsecSiteConnectionOptions localEpGroupId(String value) {
        return put("local_ep_group_id", value);
    }

    /** Endpoint group of peer CIDRs. */
    public IpsecSiteConnectionOptions peerEpGroupId(String value) {
        return put("peer_ep_group_id", value);
    }

    /** Deprecated by Neutron; use endpoint groups. */
    public IpsecSiteConnectionOptions peerCidrs(List<String> value) {
        return put("peer_cidrs", value);
    }

    public IpsecSiteConnectionOptions localId(String value) {
        return put("local_id", value);
    }

    /** {@code bi-directional} or {@code response-only}. */
    public IpsecSiteConnectionOptions initiator(String value) {
        return put("initiator", value);
    }

    public IpsecSiteConnectionOptions mtu(Integer value) {
        return put("mtu", value);
    }

    /** e.g. {@code {"action": "hold", "interval": 30, "timeout": 120}}. */
    public IpsecSiteConnectionOptions dpd(Map<String, Object> value) {
        return put("dpd", value);
    }

    public IpsecSiteConnectionOptions adminStateUp(Boolean value) {
        return put("admin_state_up", value);
    }
}
