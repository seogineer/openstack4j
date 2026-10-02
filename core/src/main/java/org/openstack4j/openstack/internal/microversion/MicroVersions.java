package org.openstack4j.openstack.internal.microversion;

import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.openstack.internal.MicroVersion;

public final class MicroVersions {

    private MicroVersions() {
    }

    public static MicroVersion min(MicroVersion a, MicroVersion b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static MicroVersion max(MicroVersion a, MicroVersion b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static MicroVersion parse(String value) {
        try {
            return new MicroVersion(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new MicroVersionException("Invalid microversion '" + value + "': expected 'X.Y'");
        }
    }
}
