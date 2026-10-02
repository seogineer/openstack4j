package org.openstack4j.model.compute.actions;

/** Evacuate request for microversion-aware sessions. */
public class EvacuateRequest {

    private String host;
    private String adminPass;
    private Boolean onSharedStorage;
    private Boolean force;

    public static EvacuateRequest create() {
        return new EvacuateRequest();
    }

    public EvacuateRequest host(String host) { this.host = host; return this; }
    public EvacuateRequest adminPass(String adminPass) { this.adminPass = adminPass; return this; }
    /** Removed in 2.14; the request is sent at 2.13 or lower. */
    public EvacuateRequest onSharedStorage(boolean onSharedStorage) { this.onSharedStorage = onSharedStorage; return this; }
    /** 2.29 - 2.67 */
    public EvacuateRequest force(boolean force) { this.force = force; return this; }

    public String getHost() { return host; }
    public String getAdminPass() { return adminPass; }
    public Boolean getOnSharedStorage() { return onSharedStorage; }
    public Boolean getForce() { return force; }
}
