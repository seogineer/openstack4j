package org.openstack4j.model.storage.block.options;

/** Filters for {@code GET /clusters[/detail]} (3.7+; replication filters 3.26+). */
public class ClusterListOptions extends BlockStorageListOptions<ClusterListOptions> {

    public static ClusterListOptions create() {
        return new ClusterListOptions();
    }

    public ClusterListOptions name(String name) { return put("name", name, 0); }
    public ClusterListOptions binary(String binary) { return put("binary", binary, 0); }
    public ClusterListOptions isUp(boolean isUp) { return put("is_up", isUp, 0); }
    public ClusterListOptions disabled(boolean disabled) { return put("disabled", disabled, 0); }
    public ClusterListOptions numHosts(int numHosts) { return put("num_hosts", numHosts, 0); }
    public ClusterListOptions numDownHosts(int numDownHosts) { return put("num_down_hosts", numDownHosts, 0); }
    /** 3.26+ */
    public ClusterListOptions replicationStatus(String status) { return put("replication_status", status, 26); }
    /** 3.26+ */
    public ClusterListOptions frozen(boolean frozen) { return put("frozen", frozen, 26); }
    /** 3.26+ */
    public ClusterListOptions activeBackendId(String backendId) { return put("active_backend_id", backendId, 26); }
}
