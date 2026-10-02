package org.openstack4j.openstack.internal.microversion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.OSClientSession;

/**
 * Microversion policy of one service: library range, header shape, session state lookup, negotiation and floor
 * checks. One instance per service, shared by all its request classes.
 */
public final class MicroVersionSupport {

    public static final String API_VERSION_HEADER = "OpenStack-API-Version";

    private final ServiceType serviceType;
    private final String keyPrefix;
    private final MicroVersion minimum;
    private final MicroVersion latest;
    private final String headerServiceName;
    private final List<String> extraHeaderNames;
    private final String enableHint;

    /**
     * @param keyPrefix         state key prefix, for example {@code "compute"}
     * @param headerServiceName the service name in {@code OpenStack-API-Version: <name> <version>}
     * @param extraHeaderNames  additional headers that carry the bare version, such as {@code X-OpenStack-Nova-API-Version}
     * @param enableHint        the call that turns microversions on, quoted in error messages
     */
    public MicroVersionSupport(ServiceType serviceType, String keyPrefix, MicroVersion minimum, MicroVersion latest,
            String headerServiceName, List<String> extraHeaderNames, String enableHint) {
        this.serviceType = serviceType;
        this.keyPrefix = keyPrefix;
        this.minimum = minimum;
        this.latest = latest;
        this.headerServiceName = headerServiceName;
        this.extraHeaderNames = List.copyOf(extraHeaderNames);
        this.enableHint = enableHint;
    }

    public MicroVersion getMinimum() {
        return minimum;
    }

    public MicroVersion getLatest() {
        return latest;
    }

    private String key(OSClientSession<?, ?> session) {
        return keyPrefix + "|" + session.getEndpoint(serviceType);
    }

    /** @return this session's state for the service, or {@code null} when microversions were never turned on */
    public MicroVersionState currentState() {
        if (!MicroVersionStore.hasAny())
            return null;    // nobody turned microversions on: no catalog lookup, no lock
        OSClientSession<?, ?> session = OSClientSession.getCurrent();
        return session == null ? null : MicroVersionStore.get(session, key(session));
    }

    /** @return the microversion a request would carry under the given ceilings, or {@code null} when off */
    public MicroVersion effective(MicroVersion classCeiling, MicroVersion ceiling) {
        MicroVersionState state = currentState();
        if (state == null || !state.isEnabled())
            return null;
        MicroVersion version = state.getPinned() != null ? state.getPinned() : MicroVersions.min(latest, state.getServerMax());
        if (classCeiling != null)
            version = MicroVersions.min(version, classCeiling);
        if (ceiling != null)
            version = MicroVersions.min(version, ceiling);
        return version;
    }

    /** @return the request headers that select {@code version} */
    public Map<String, String> headers(MicroVersion version) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(API_VERSION_HEADER, headerServiceName + " " + version);
        for (String name : extraHeaderNames)
            headers.put(name, version.toString());
        return headers;
    }

    /** Fails before any request when {@code feature} needs a microversion the session does not send. */
    public void require(String feature, MicroVersion floor, MicroVersion effective) {
        if (effective == null)
            throw new MicroVersionException(feature + " requires " + headerServiceName + " microversion " + floor
                    + "; turn microversions on with " + enableHint);
        if (effective.compareTo(floor) < 0) {
            MicroVersionState state = currentState();
            throw new MicroVersionException(String.format(
                    "%s requires %s microversion %s, but the session sends %s (server max %s)",
                    feature, headerServiceName, floor, effective, state == null ? "?" : state.getServerMax()));
        }
    }

    /** Turns microversions on at min(latest, server max); discovers the server range once per session and endpoint. */
    public MicroVersionState negotiate(Supplier<VersionRange> discovery) {
        MicroVersionState state = ensureState(discovery);
        state.setPinned(null);
        state.setEnabled(true);
        return state;
    }

    /** Turns microversions on at a fixed version inside the usable range. */
    public MicroVersionState use(String version, Supplier<VersionRange> discovery) {
        MicroVersion requested = MicroVersions.parse(version);
        MicroVersionState state = ensureState(discovery);
        MicroVersion lowest = MicroVersions.max(minimum, state.getServerMin());
        MicroVersion highest = MicroVersions.min(latest, state.getServerMax());
        if (requested.compareTo(lowest) < 0 || requested.compareTo(highest) > 0)
            throw new MicroVersionException(String.format(
                    "%s microversion %s is outside the usable range %s - %s (library %s - %s, server %s - %s)",
                    capitalize(headerServiceName), requested, lowest, highest, minimum, latest,
                    state.getServerMin(), state.getServerMax()));
        state.setPinned(requested);
        state.setEnabled(true);
        return state;
    }

    public void clear() {
        MicroVersionState state = currentState();
        if (state != null) {
            state.setEnabled(false);
            state.setPinned(null);
        }
    }

    private MicroVersionState ensureState(Supplier<VersionRange> discovery) {
        MicroVersionState state = currentState();
        if (state != null)
            return state;
        VersionRange range = discovery.get();
        if (range == null || range.getMax() == null)
            throw new MicroVersionException("This " + headerServiceName
                    + " endpoint does not support microversions (no API with a version range)");
        OSClientSession<?, ?> session = OSClientSession.getCurrent();
        return MicroVersionStore.putIfAbsent(session, key(session),
                new MicroVersionState(range.getMin() == null ? minimum : range.getMin(), range.getMax()));
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
