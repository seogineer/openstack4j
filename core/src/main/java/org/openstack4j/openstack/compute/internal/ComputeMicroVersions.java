package org.openstack4j.openstack.compute.internal;

import java.util.List;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionSupport;

/** Compute microversion constants and session state lookup. */
public final class ComputeMicroVersions {

    public static final MicroVersion MINIMUM = new MicroVersion(2, 1);
    public static final MicroVersion LATEST = new MicroVersion(2, 104);

    /** Shared policy: both Nova headers, state keyed by the compute endpoint. */
    public static final MicroVersionSupport SUPPORT = new MicroVersionSupport(ServiceType.COMPUTE, "compute", MINIMUM, LATEST,
            "compute", List.of("X-OpenStack-Nova-API-Version"), "os.compute().microVersions().negotiate()");

    private ComputeMicroVersions() {
    }

    /** @return compute microversion {@code 2.minor} */
    public static MicroVersion V(int minor) {
        return new MicroVersion(2, minor);
    }

    /** @return this session's compute state, or {@code null} when microversions were never turned on */
    public static MicroVersionState currentState() {
        return SUPPORT.currentState();
    }

    /** Strips the {@code /v2[.1]} segment and anything after it (such as a tenant id) from a compute endpoint. */
    public static String rootUrl(String endpoint) {
        String trimmed = endpoint.replaceAll("/+$", "");
        return trimmed.replaceAll("/v2(\\.\\d+)?(/.*)?$", "");
    }
}
