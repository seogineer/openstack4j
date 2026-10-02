package org.openstack4j.openstack.storage.block.domain;

import org.openstack4j.model.storage.block.BlockStorageVersion;

public class CinderBlockStorageVersion implements BlockStorageVersion {

    private static final long serialVersionUID = 1L;

    private final String serverMinVersion;
    private final String serverMaxVersion;
    private final String microVersion;
    private final boolean pinned;
    private final boolean enabled;

    public CinderBlockStorageVersion(String serverMinVersion, String serverMaxVersion, String microVersion, boolean pinned, boolean enabled) {
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
