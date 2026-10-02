package org.openstack4j.openstack.placement.v1.internal;

import java.util.regex.Pattern;

import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.PlacementMicroVersions;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.OSClientSession;
import org.openstack4j.openstack.placement.v1.domain.PlacementRoot;

/**
 * Base of the {@code placement.v1} services: microversion negotiation, the {@code OpenStack-API-Version} header,
 * per-feature version checks and Placement error mapping.
 */
public abstract class BasePlacementV1Service extends BaseOpenStackService {

    static final String API_VERSION_HEADER = "OpenStack-API-Version";
    private static final Pattern NAME = Pattern.compile("^[A-Z0-9_]+$");

    protected BasePlacementV1Service() {
        super(ServiceType.PLACEMENT);
    }

    /** Returns the cached server range for this session and endpoint, fetching {@code GET /} the first time. */
    PlacementSessionState.State state() {
        OSClientSession<?, ?> session = OSClientSession.getCurrent();
        String endpoint = session.getEndpoint(ServiceType.PLACEMENT);
        PlacementSessionState.State state = PlacementSessionState.get(session, endpoint);
        if (state != null)
            return state;
        PlacementRoot root = request(HttpMethod.GET, PlacementRoot.class, "/")
                .execute(ExecutionOptions.create(PlacementErrors.THROW_ALL));
        return PlacementSessionState.putIfAbsent(session, endpoint,
                new PlacementSessionState.State(root.getMinVersion(), root.getMaxVersion()));
    }

    /** The microversion sent with requests: the pinned one, otherwise min(library latest, server max). */
    protected MicroVersion microVersion() {
        PlacementSessionState.State state = state();
        if (state.pinned != null)
            return state.pinned;
        MicroVersion version = min(PlacementMicroVersions.LATEST, state.serverMax);
        if (version.compareTo(PlacementMicroVersions.MINIMUM) < 0)
            throw new PlacementMicroVersionException(String.format(
                    "The placement v1 API requires placement microversion %s or later, but the server supports up to %s",
                    PlacementMicroVersions.MINIMUM, state.serverMax));
        return version;
    }

    protected void requireMicroVersion(String feature, MicroVersion required) {
        MicroVersion version = microVersion();
        if (version.compareTo(required) < 0)
            throw new PlacementMicroVersionException(String.format(
                    "%s requires placement microversion %s, but the negotiated version is %s (server max %s)",
                    feature, required, version, state().serverMax));
    }

    protected <R> Invocation<R> placement(HttpMethod method, Class<R> type, String path) {
        return request(method, type, path).header(API_VERSION_HEADER, "placement " + microVersion());
    }

    protected <R> R executeOrNull(Invocation<R> invocation) {
        return invocation.execute(ExecutionOptions.create(PlacementErrors.THROW_EXCEPT_404));
    }

    protected <R> R executeOrThrow(Invocation<R> invocation) {
        return invocation.execute(ExecutionOptions.create(PlacementErrors.THROW_ALL));
    }

    protected ActionResponse executeAction(Invocation<ActionResponse> invocation) {
        return invocation.execute();
    }

    /** Placement names (resource classes, traits) are upper case letters, digits and underscores. */
    protected static String requireName(String kind, String name) {
        if (name == null || !NAME.matcher(name).matches())
            throw new IllegalArgumentException(kind + " name must match " + NAME.pattern() + ": " + name);
        return name;
    }

    static MicroVersion min(MicroVersion a, MicroVersion b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    static MicroVersion max(MicroVersion a, MicroVersion b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
}
