package org.openstack4j.openstack.storage.block.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.storage.block.domain.*;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;
import org.openstack4j.api.storage.BlockClusterService;
import org.openstack4j.model.storage.block.StorageCluster;
import org.openstack4j.model.storage.block.options.ClusterListOptions;
import org.openstack4j.openstack.storage.block.domain.CinderCluster.Clusters;

public class BlockClusterServiceImpl extends BaseBlockStorageServices implements BlockClusterService {

    @Override public List<? extends StorageCluster> list() { return list(ClusterListOptions.create()); }
    @Override public List<? extends StorageCluster> listDetail() { return listDetail(ClusterListOptions.create()); }

    @Override
    public List<? extends StorageCluster> list(ClusterListOptions options) {
        requireOptions(options);
        return get(Clusters.class, uri("/clusters")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public List<? extends StorageCluster> listDetail(ClusterListOptions options) {
        requireOptions(options);
        return get(Clusters.class, uri("/clusters/detail")).params(options.toQueryParams()).execute().getList();
    }

    private void requireOptions(ClusterListOptions options) {
        Objects.requireNonNull(options);
        requireMicroVersion("Clusters", V(7));
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Cluster list filters " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
    }

    @Override
    public StorageCluster get(String name, String binary) {
        Objects.requireNonNull(name);
        requireMicroVersion("Clusters", V(7));
        return get(CinderCluster.class, uri("/clusters/%s", name)).param("binary", binary == null ? "cinder-volume" : binary).execute();
    }

    @Override
    public StorageCluster enable(String name, String binary) {
        requireMicroVersion("Clusters", V(7));
        return put(CinderCluster.class, uri("/clusters/enable")).entity(JsonBody.of(body(name, binary, null))).execute();
    }

    @Override
    public StorageCluster disable(String name, String binary, String reason) {
        requireMicroVersion("Clusters", V(7));
        return put(CinderCluster.class, uri("/clusters/disable")).entity(JsonBody.of(body(name, binary, reason))).execute();
    }

    private static Map<String, Object> body(String name, String binary, String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", Objects.requireNonNull(name));
        if (binary != null) body.put("binary", binary);
        if (reason != null) body.put("disabled_reason", reason);
        return body;
    }
}
