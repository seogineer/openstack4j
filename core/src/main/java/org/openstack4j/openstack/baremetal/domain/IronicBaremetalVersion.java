package org.openstack4j.openstack.baremetal.domain;

import org.openstack4j.model.baremetal.BaremetalVersion;

public class IronicBaremetalVersion implements BaremetalVersion {

    private static final long serialVersionUID = 1L;

    private final String serverMinVersion;
    private final String serverMaxVersion;
    private final String microVersion;
    private final boolean pinned;
    private final boolean enabled;

    public IronicBaremetalVersion(String serverMinVersion, String serverMaxVersion, String microVersion, boolean pinned, boolean enabled) {
        this.serverMinVersion = serverMinVersion;
        this.serverMaxVersion = serverMaxVersion;
        this.microVersion = microVersion;
        this.pinned = pinned;
        this.enabled = enabled;
    }

    @Override public String getServerMinVersion() { return serverMinVersion; }
    @Override public String getServerMaxVersion() { return serverMaxVersion; }
    @Override public String getMicroVersion() { return microVersion; }
    @Override public boolean isPinned() { return pinned; }
    @Override public boolean isEnabled() { return enabled; }
}
