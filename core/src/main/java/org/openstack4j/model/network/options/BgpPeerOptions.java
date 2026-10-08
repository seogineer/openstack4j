package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a BGP peer create or update; only the fields set are sent. */
public class BgpPeerOptions extends NeutronAttributes<BgpPeerOptions> {

    public static BgpPeerOptions create(String name, String peerIp, Integer remoteAs) {
        return new BgpPeerOptions().put("name", Objects.requireNonNull(name, "name")).put("peer_ip", Objects.requireNonNull(peerIp, "peerIp")).put("remote_as", Objects.requireNonNull(remoteAs, "remoteAs"));
    }

    /** An update that sends only the fields set afterwards. */
    public static BgpPeerOptions update() {
        return new BgpPeerOptions();
    }

    @Override
    protected BgpPeerOptions self() {
        return this;
    }

    public BgpPeerOptions name(String value) {
        return put("name", value);
    }

    public BgpPeerOptions peerIp(String value) {
        return put("peer_ip", value);
    }

    public BgpPeerOptions remoteAs(Integer value) {
        return put("remote_as", value);
    }

    /** {@code none} or {@code md5}. */
    public BgpPeerOptions authType(String value) {
        return put("auth_type", value);
    }

    /** The MD5 password. */
    public BgpPeerOptions password(String value) {
        return put("password", value);
    }
}
