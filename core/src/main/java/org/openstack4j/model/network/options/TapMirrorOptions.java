package org.openstack4j.model.network.options;

import java.util.Map;
import java.util.Objects;

/** Body of a tap mirror create or update; only the fields set are sent. */
public class TapMirrorOptions extends NeutronAttributes<TapMirrorOptions> {

    public static TapMirrorOptions create(String portId, Map<String, Integer> directions, String remoteIp, String mirrorType) {
        return new TapMirrorOptions().put("port_id", Objects.requireNonNull(portId, "portId")).put("directions", Objects.requireNonNull(directions, "directions")).put("remote_ip", Objects.requireNonNull(remoteIp, "remoteIp")).put("mirror_type", Objects.requireNonNull(mirrorType, "mirrorType"));
    }

    /** An update that sends only the fields set afterwards. */
    public static TapMirrorOptions update() {
        return new TapMirrorOptions();
    }

    @Override
    protected TapMirrorOptions self() {
        return this;
    }

    public TapMirrorOptions portId(String value) {
        return put("port_id", value);
    }

    public TapMirrorOptions directions(Map<String, Integer> value) {
        return put("directions", value);
    }

    public TapMirrorOptions remoteIp(String value) {
        return put("remote_ip", value);
    }

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
