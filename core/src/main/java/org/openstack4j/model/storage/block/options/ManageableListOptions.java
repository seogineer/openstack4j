package org.openstack4j.model.storage.block.options;

/** Location and paging for {@code GET /manageable_volumes} and {@code /manageable_snapshots} (3.8+); give host or cluster (3.17+). */
public class ManageableListOptions extends BlockStorageListOptions<ManageableListOptions> {

    public static ManageableListOptions create() {
        return new ManageableListOptions();
    }

    public ManageableListOptions host(String host) { return put("host", host, 0); }
    /** 3.17+ */
    public ManageableListOptions cluster(String cluster) { return put("cluster", cluster, 17); }
}
