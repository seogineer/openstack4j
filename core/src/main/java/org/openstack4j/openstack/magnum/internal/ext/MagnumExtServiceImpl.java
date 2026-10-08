package org.openstack4j.openstack.magnum.internal.ext;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.magnum.ext.MagnumExtService;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class MagnumExtServiceImpl extends BaseOpenStackService implements MagnumExtService {

    private static final String API_VERSION = "OpenStack-API-Version";

    public MagnumExtServiceImpl() {
        super(ServiceType.MAGNUM);
    }

    private static <T> ExecutionOptions<T> propagate404() {
        return ExecutionOptions.create(PropagateOnStatus.on(404));
    }

    private static String id(String value) {
        Objects.requireNonNull(value, "id");
        if (value.isBlank() || value.indexOf('/') >= 0 || value.indexOf('?') >= 0 || value.indexOf('#') >= 0)
            throw new IllegalArgumentException("Not a valid identifier: '" + value + "'");
        return value;
    }

    @Override
    public String resizeCluster(String clusterId, int nodeCount, List<String> nodesToRemove, String nodegroup) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("node_count", nodeCount);
        if (nodesToRemove != null)
            body.put("nodes_to_remove", nodesToRemove);
        if (nodegroup != null)
            body.put("nodegroup", nodegroup);
        return clusterAction(clusterId, "resize", "container-infra 1.7", body);
    }

    @Override
    public String upgradeCluster(String clusterId, String clusterTemplate, Integer maxBatchSize, String nodegroup) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cluster_template", Objects.requireNonNull(clusterTemplate, "clusterTemplate"));
        if (maxBatchSize != null)
            body.put("max_batch_size", maxBatchSize);
        if (nodegroup != null)
            body.put("nodegroup", nodegroup);
        return clusterAction(clusterId, "upgrade", "container-infra 1.8", body);
    }

    @SuppressWarnings("unchecked")
    private String clusterAction(String clusterId, String action, String version, Map<String, Object> body) {
        Map<String, Object> response = post(Map.class, "/clusters/" + id(clusterId) + "/actions/" + action).header(API_VERSION, version)
                .entity(JsonBody.of(body)).execute(propagate404());
        return response == null ? null : (String) response.get("uuid");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getCaCertificate(String clusterId, String caCertType) {
        return get(Map.class, "/certificates/" + id(clusterId)).param("ca_cert_type", Objects.requireNonNull(caCertType, "caCertType"))
                .execute(propagate404());
    }

    private static Map<String, Object> quota(String projectId, String resource, int hardLimit) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("project_id", Objects.requireNonNull(projectId, "projectId"));
        body.put("resource", Objects.requireNonNull(resource, "resource"));
        body.put("hard_limit", hardLimit);
        return body;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> createQuota(String projectId, String resource, int hardLimit) {
        return post(Map.class, "/quotas").entity(JsonBody.of(quota(projectId, resource, hardLimit))).execute(propagate404());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> listQuotas(Map<String, String> filters) {
        Map<String, Object> body = get(Map.class, "/quotas").params(filters == null ? Collections.emptyMap() : filters).execute(propagate404());
        Object quotas = body == null ? null : body.get("quotas");
        return quotas instanceof List ? (List<Map<String, Object>>) quotas : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getQuota(String projectId, String resource) {
        Map<String, Object> body = get(Map.class, "/quotas/" + id(projectId) + "/" + id(resource)).execute(propagate404());
        return body == null ? new HashMap<>() : body;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> updateQuota(String projectId, String resource, int hardLimit) {
        return patch(Map.class, "/quotas/" + id(projectId) + "/" + id(resource)).entity(JsonBody.of(quota(projectId, resource, hardLimit))).execute(propagate404());
    }

    @Override
    public ActionResponse deleteQuota(String projectId, String resource) {
        return deleteWithResponse("/quotas/" + id(projectId) + "/" + id(resource)).execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> stats(String projectId) {
        Invocation<Map> invocation = get(Map.class, "/stats");
        if (projectId != null)
            invocation.param("project_id", projectId);
        Map<String, Object> body = invocation.execute(propagate404());
        return body == null ? new HashMap<>() : body;
    }
}
