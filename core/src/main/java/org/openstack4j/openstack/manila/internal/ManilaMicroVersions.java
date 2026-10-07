package org.openstack4j.openstack.manila.internal;

import java.util.List;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionSupport;

/** Shared file systems (Manila v2) microversion constants and session state lookup. */
public final class ManilaMicroVersions {

    public static final MicroVersion MINIMUM = new MicroVersion(2, 0);
    public static final MicroVersion LATEST = new MicroVersion(2, 99);

    /** {@code X-OpenStack-Manila-API-Version: <v>} (the header Manila reads) plus {@code OpenStack-API-Version: shared-file-system <v>}. */
    public static final MicroVersionSupport SUPPORT = new MicroVersionSupport(ServiceType.SHARE, "share", MINIMUM, LATEST,
            "shared-file-system", List.of("X-OpenStack-Manila-API-Version"), "os.share().microVersions().negotiate()");

    private ManilaMicroVersions() {
    }

    /** @return shared file systems microversion {@code 2.minor} */
    public static MicroVersion V(int minor) {
        return new MicroVersion(2, minor);
    }

    public static MicroVersionState currentState() {
        return SUPPORT.currentState();
    }

    /** Strips a trailing {@code /v1} or {@code /v2} (and anything after it, such as a project id) from a Manila endpoint. */
    public static String rootUrl(String endpoint) {
        String trimmed = endpoint.replaceAll("/+$", "");
        return trimmed.replaceAll("/v[12](/.*)?$", "");
    }

    /** Points a Manila endpoint at the v2 API, keeping a project id: {@code .../v1/<p>} becomes {@code .../v2/<p>}. */
    public static String v2Url(String endpoint) {
        String trimmed = endpoint.replaceAll("/+$", "");
        if (trimmed.matches(".*/v2(/.*)?$"))
            return trimmed;
        if (trimmed.matches(".*/v1(/.*)?$"))
            return trimmed.replaceFirst("/v1(/[^/]*)?$", "/v2$1");
        return trimmed + "/v2";
    }
}
