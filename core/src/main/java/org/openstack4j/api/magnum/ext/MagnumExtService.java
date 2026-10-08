package org.openstack4j.api.magnum.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/** Magnum cluster resize/upgrade, CA certificates by type, quotas and stats. */
public interface MagnumExtService extends RestService {

    /**
     * Resizes a cluster (container-infra microversion 1.7; 1.10 when {@code nodeCount} is 0).
     *
     * @param nodesToRemove nodes to remove when shrinking, or {@code null}
     * @param nodegroup     the node group, or {@code null} for the default worker group
     * @return the cluster uuid
     */
    String resizeCluster(String clusterId, int nodeCount, List<String> nodesToRemove, String nodegroup);

    /**
     * Upgrades a cluster to another cluster template (microversion 1.8).
     *
     * @param maxBatchSize nodes upgraded at a time, or {@code null}
     * @param nodegroup    the node group, or {@code null}
     * @return the cluster uuid
     */
    String upgradeCluster(String clusterId, String clusterTemplate, Integer maxBatchSize, String nodegroup);

    /** @param caCertType e.g. {@code kubernetes}, {@code etcd}, {@code front_proxy} @return {@code cluster_uuid} and {@code pem} */
    Map<String, Object> getCaCertificate(String clusterId, String caCertType);

    /** Sets a quota of a project (admin), e.g. resource {@code Cluster}. */
    Map<String, Object> createQuota(String projectId, String resource, int hardLimit);

    /** @param filters e.g. {@code all_tenants=True} (admin), {@code limit}, {@code marker} */
    List<Map<String, Object>> listQuotas(Map<String, String> filters);

    /** @return the quota; when none is stored Magnum answers the default ({@code hard_limit} = max clusters per project) */
    Map<String, Object> getQuota(String projectId, String resource);

    Map<String, Object> updateQuota(String projectId, String resource, int hardLimit);

    ActionResponse deleteQuota(String projectId, String resource);

    /** @param projectId the project (admin), or {@code null} for all @return {@code clusters} and {@code nodes} counts */
    Map<String, Object> stats(String projectId);
}
