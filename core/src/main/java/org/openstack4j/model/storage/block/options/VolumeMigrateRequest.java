package org.openstack4j.model.storage.block.options;

/** Body of the {@code os-migrate_volume} action. Either {@code host} or {@code cluster} (3.16+), not both. */
public class VolumeMigrateRequest {

    private String host;
    private String cluster;
    private Boolean forceHostCopy;
    private Boolean lockVolume;

    public static VolumeMigrateRequest create() {
        return new VolumeMigrateRequest();
    }

    public VolumeMigrateRequest host(String host) { this.host = host; return this; }
    /** 3.16+ */
    public VolumeMigrateRequest cluster(String cluster) { this.cluster = cluster; return this; }
    public VolumeMigrateRequest forceHostCopy(boolean forceHostCopy) { this.forceHostCopy = forceHostCopy; return this; }
    public VolumeMigrateRequest lockVolume(boolean lockVolume) { this.lockVolume = lockVolume; return this; }

    public String getHost() { return host; }
    public String getCluster() { return cluster; }
    public Boolean getForceHostCopy() { return forceHostCopy; }
    public Boolean getLockVolume() { return lockVolume; }
}
