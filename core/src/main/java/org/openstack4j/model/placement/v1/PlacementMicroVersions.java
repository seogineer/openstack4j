package org.openstack4j.model.placement.v1;

import org.openstack4j.openstack.internal.MicroVersion;

/**
 * Placement microversions this library knows about.
 */
public final class PlacementMicroVersions {

    /** The oldest microversion the {@code placement().v1} services support (Rocky). */
    public static final MicroVersion MINIMUM = new MicroVersion(1, 28);
    /** The newest microversion this library understands. */
    public static final MicroVersion LATEST = new MicroVersion(1, 39);

    public static final MicroVersion V1_30 = new MicroVersion(1, 30);
    public static final MicroVersion V1_31 = new MicroVersion(1, 31);
    public static final MicroVersion V1_32 = new MicroVersion(1, 32);
    public static final MicroVersion V1_33 = new MicroVersion(1, 33);
    public static final MicroVersion V1_35 = new MicroVersion(1, 35);
    public static final MicroVersion V1_36 = new MicroVersion(1, 36);
    public static final MicroVersion V1_37 = new MicroVersion(1, 37);
    public static final MicroVersion V1_38 = new MicroVersion(1, 38);
    public static final MicroVersion V1_39 = new MicroVersion(1, 39);

    private PlacementMicroVersions() {
    }
}
