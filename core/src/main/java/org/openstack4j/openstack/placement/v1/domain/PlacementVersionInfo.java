package org.openstack4j.openstack.placement.v1.domain;

import org.openstack4j.model.placement.v1.PlacementVersion;

public class PlacementVersionInfo implements PlacementVersion {

    private static final long serialVersionUID = 1L;

    private final String serverMinVersion;
    private final String serverMaxVersion;
    private final String microVersion;
    private final boolean pinned;

    public PlacementVersionInfo(String serverMinVersion, String serverMaxVersion, String microVersion, boolean pinned) {
        this.serverMinVersion = serverMinVersion;
        this.serverMaxVersion = serverMaxVersion;
        this.microVersion = microVersion;
        this.pinned = pinned;
    }

    @Override
    public String getServerMinVersion() {
        return serverMinVersion;
    }

    @Override
    public String getServerMaxVersion() {
        return serverMaxVersion;
    }

    @Override
    public String getMicroVersion() {
        return microVersion;
    }

    @Override
    public boolean isPinned() {
        return pinned;
    }
}
