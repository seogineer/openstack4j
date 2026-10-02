package org.openstack4j.model.compute.actions;

/** Live migration request for microversion-aware sessions ({@code os-migrateLive}). */
public class LiveMigrateRequest {

    private String host;
    private Boolean blockMigration;
    private boolean blockMigrationAuto;
    private Boolean diskOverCommit;
    private Boolean force;

    public static LiveMigrateRequest create() {
        return new LiveMigrateRequest();
    }

    /** Destination host; {@code null} lets the scheduler choose. */
    public LiveMigrateRequest host(String host) { this.host = host; return this; }

    public LiveMigrateRequest blockMigration(boolean blockMigration) { this.blockMigration = blockMigration; this.blockMigrationAuto = false; return this; }

    /** {@code "block_migration": "auto"} (2.25+). */
    public LiveMigrateRequest blockMigrationAuto() { this.blockMigration = null; this.blockMigrationAuto = true; return this; }

    /** Removed in 2.25; the request is sent at 2.24 or lower. */
    public LiveMigrateRequest diskOverCommit(boolean diskOverCommit) { this.diskOverCommit = diskOverCommit; return this; }

    /** Skip the scheduler check of {@code host} (2.30 - 2.67). */
    public LiveMigrateRequest force(boolean force) { this.force = force; return this; }

    public String getHost() { return host; }
    public Boolean getBlockMigration() { return blockMigration; }
    public boolean isBlockMigrationAuto() { return blockMigrationAuto; }
    public Boolean getDiskOverCommit() { return diskOverCommit; }
    public Boolean getForce() { return force; }
}
