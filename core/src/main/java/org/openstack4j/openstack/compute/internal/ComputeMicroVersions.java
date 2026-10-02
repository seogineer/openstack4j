package org.openstack4j.openstack.compute.internal;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.OSClientSession;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionStore;

/** Compute microversion constants and session state lookup. */
public final class ComputeMicroVersions {

    public static final MicroVersion MINIMUM = new MicroVersion(2, 1);
    public static final MicroVersion LATEST = new MicroVersion(2, 104);

    private ComputeMicroVersions() {
    }

    /** @return compute microversion {@code 2.minor} */
    public static MicroVersion V(int minor) {
        return new MicroVersion(2, minor);
    }

    static String key(OSClientSession<?, ?> session) {
        return "compute|" + session.getEndpoint(ServiceType.COMPUTE);
    }

    /** @return this session's compute state, or {@code null} when microversions were never turned on */
    public static MicroVersionState currentState() {
        if (!MicroVersionStore.hasAny())
            return null;    // nobody turned microversions on: no catalog lookup, no lock
        OSClientSession<?, ?> session = OSClientSession.getCurrent();
        return session == null ? null : MicroVersionStore.get(session, key(session));
    }

    /** Strips the {@code /v2[.1]} segment and anything after it (such as a tenant id) from a compute endpoint. */
    public static String rootUrl(String endpoint) {
        String trimmed = endpoint.replaceAll("/+$", "");
        return trimmed.replaceAll("/v2(\\.\\d+)?(/.*)?$", "");
    }
}
