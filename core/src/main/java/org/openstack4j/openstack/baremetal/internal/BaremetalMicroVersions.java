package org.openstack4j.openstack.baremetal.internal;

import java.util.List;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionSupport;

/** Bare metal (Ironic v1) microversion constants and session state lookup. */
public final class BaremetalMicroVersions {

    public static final MicroVersion MINIMUM = new MicroVersion(1, 1);
    public static final MicroVersion LATEST = new MicroVersion(1, 107);

    /** {@code OpenStack-API-Version: baremetal <v>} plus the legacy {@code X-OpenStack-Ironic-API-Version}. */
    public static final MicroVersionSupport SUPPORT = new MicroVersionSupport(ServiceType.BAREMETAL, "baremetal", MINIMUM, LATEST,
            "baremetal", List.of("X-OpenStack-Ironic-API-Version"), "os.baremetal().microVersions().negotiate()");

    private BaremetalMicroVersions() {
    }

    /** @return bare metal microversion {@code 1.minor} */
    public static MicroVersion V(int minor) {
        return new MicroVersion(1, minor);
    }

    public static MicroVersionState currentState() {
        return SUPPORT.currentState();
    }

    /** Strips a trailing {@code /v1} (and anything after it) from a bare metal endpoint. */
    public static String rootUrl(String endpoint) {
        String trimmed = endpoint.replaceAll("/+$", "");
        return trimmed.replaceAll("/v1(/.*)?$", "");
    }
}
