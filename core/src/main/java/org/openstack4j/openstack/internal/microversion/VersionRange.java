package org.openstack4j.openstack.internal.microversion;

import org.openstack4j.openstack.internal.MicroVersion;

/** The microversion range a service root document advertises. {@code min} may be {@code null}. */
public final class VersionRange {

    private final MicroVersion min;
    private final MicroVersion max;

    public VersionRange(MicroVersion min, MicroVersion max) {
        this.min = min;
        this.max = max;
    }

    public MicroVersion getMin() {
        return min;
    }

    public MicroVersion getMax() {
        return max;
    }
}
