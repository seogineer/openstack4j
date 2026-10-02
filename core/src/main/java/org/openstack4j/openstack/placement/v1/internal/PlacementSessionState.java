package org.openstack4j.openstack.placement.v1.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.openstack4j.openstack.internal.MicroVersion;

/**
 * Negotiated Placement microversion state, kept per client session and per Placement endpoint. Weakly keyed so a
 * discarded session releases its state.
 */
public final class PlacementSessionState {

    private static final Map<Object, Map<String, State>> STATES = new WeakHashMap<>();

    private PlacementSessionState() {
    }

    static final class State {
        final MicroVersion serverMin;
        final MicroVersion serverMax;
        volatile MicroVersion pinned;

        State(MicroVersion serverMin, MicroVersion serverMax) {
            this.serverMin = serverMin;
            this.serverMax = serverMax;
        }
    }

    static synchronized State get(Object session, String endpoint) {
        Map<String, State> byEndpoint = STATES.get(session);
        return byEndpoint == null ? null : byEndpoint.get(endpoint);
    }

    static synchronized State putIfAbsent(Object session, String endpoint, State state) {
        return STATES.computeIfAbsent(session, s -> new HashMap<>()).merge(endpoint, state, (existing, ignored) -> existing);
    }

    /** Forgets every negotiated and pinned version. Intended for tests. */
    public static synchronized void clearAll() {
        STATES.clear();
    }
}
