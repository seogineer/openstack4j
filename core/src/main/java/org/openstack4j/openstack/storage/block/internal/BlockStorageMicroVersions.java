package org.openstack4j.openstack.storage.block.internal;

import java.util.Collections;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionSupport;

/** Block storage (Cinder v3) microversion constants and session state lookup. */
public final class BlockStorageMicroVersions {

    public static final MicroVersion MINIMUM = new MicroVersion(3, 0);
    public static final MicroVersion LATEST = new MicroVersion(3, 71);

    /** Shared policy: one {@code OpenStack-API-Version: volume <v>} header, state keyed by the block storage endpoint. */
    public static final MicroVersionSupport SUPPORT = new MicroVersionSupport(ServiceType.BLOCK_STORAGE, "volume", MINIMUM, LATEST,
            "volume", Collections.emptyList(), "os.blockStorage().microVersions().negotiate()");

    private BlockStorageMicroVersions() {
    }

    /** @return block storage microversion {@code 3.minor} */
    public static MicroVersion V(int minor) {
        return new MicroVersion(3, minor);
    }

    /** @return this session's block storage state, or {@code null} when microversions were never turned on */
    public static MicroVersionState currentState() {
        return SUPPORT.currentState();
    }

    /** Strips the {@code /v2} or {@code /v3} segment and anything after it (such as a project id) from a block storage endpoint. */
    public static String rootUrl(String endpoint) {
        String trimmed = endpoint.replaceAll("/+$", "");
        return trimmed.replaceAll("/v[23](\\.\\d+)?(/.*)?$", "");
    }
}
