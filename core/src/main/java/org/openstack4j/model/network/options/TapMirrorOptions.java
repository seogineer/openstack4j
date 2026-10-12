package org.openstack4j.model.network.options;

import java.util.Map;
import java.util.Objects;

/** Body of a tap mirror create or update; only the fields set are sent. */
public class TapMirrorOptions extends NeutronAttributes<TapMirrorOptions> {

    public static TapMirrorOptions create(String portId, Map<String, Integer> directions, String remoteIp, String mirrorType) {
        return new TapMirrorOptions().put("port_id", Objects.requireNonNull(portId, "portId")).put("directions", Objects.requireNonNull(directions, "directions")).put("remote_ip", Objects.requireNonNull(remoteIp, "remoteIp")).put("mirror_type", Objects.requireNonNull(mirrorType, "mirrorType"));
    }

    /** An update that sends only the fields set afterwards; setters marked "create only" are rejected by Neutron on update (400). */
    public static TapMirrorOptions update() {
        return new TapMirrorOptions();
    }

    @Override
    protected TapMirrorOptions self() {
        return this;
    }

    /** Create only: Neutron rejects it on update (400). */
    public TapMirrorOptions portId(String value) {
        return put("port_id", value);
    }

    /** Create only: Neutron rejects it on update (400). */
    public TapMirrorOptions directions(Map<String, Integer> value) {
        return put("directions", value);
    }

    /** Create only: Neutron rejects it on update (400). */
    public TapMirrorOptions remoteIp(String value) {
        return put("remote_ip", value);
    }

    /** Create only: Neutron rejects it on update (400). */
    public TapMirrorOptions mirrorType(String value) {
        return put("mirror_type", value);
    }

    public TapMirrorOptions name(String value) {
        return put("name", value);
    }

    public TapMirrorOptions description(String value) {
        return put("description", value);
    }
}
