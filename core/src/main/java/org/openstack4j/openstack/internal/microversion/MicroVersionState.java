package org.openstack4j.openstack.internal.microversion;

import org.openstack4j.openstack.internal.MicroVersion;

/** Server microversion range of one service endpoint in one session, plus the session's choice. */
public final class MicroVersionState {

    private final MicroVersion serverMin;
    private final MicroVersion serverMax;
    private volatile MicroVersion pinned;
    private volatile boolean enabled;

    public MicroVersionState(MicroVersion serverMin, MicroVersion serverMax) {
        this.serverMin = serverMin;
        this.serverMax = serverMax;
    }

    public MicroVersion getServerMin() {
        return serverMin;
    }

    public MicroVersion getServerMax() {
        return serverMax;
    }

    public MicroVersion getPinned() {
        return pinned;
    }

    public void setPinned(MicroVersion pinned) {
        this.pinned = pinned;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
