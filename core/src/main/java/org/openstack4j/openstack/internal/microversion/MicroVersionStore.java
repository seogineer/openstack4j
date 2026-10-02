package org.openstack4j.openstack.internal.microversion;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Microversion state keyed by client session identity and a service key (for example {@code "compute|<endpoint>"}).
 * Weakly keyed so a discarded session releases its state.
 */
public final class MicroVersionStore {

    private static final Map<Object, Map<String, MicroVersionState>> STATES = new WeakHashMap<>();
    /** Lets callers skip the lock when no session ever turned microversions on. */
    private static volatile boolean any;

    private MicroVersionStore() {
    }

    public static synchronized MicroVersionState get(Object session, String key) {
        Map<String, MicroVersionState> byKey = STATES.get(session);
        return byKey == null ? null : byKey.get(key);
    }

    public static synchronized MicroVersionState putIfAbsent(Object session, String key, MicroVersionState state) {
        any = true;
        return STATES.computeIfAbsent(session, s -> new HashMap<>()).merge(key, state, (existing, ignored) -> existing);
    }

    /** Forgets all state. Intended for tests. */
    public static synchronized void clearAll() {
        STATES.clear();
        any = false;
    }

    /** @return whether any state was ever stored (since the last {@link #clearAll()}); cheap and lock-free */
    public static boolean hasAny() {
        return any;
    }
}
