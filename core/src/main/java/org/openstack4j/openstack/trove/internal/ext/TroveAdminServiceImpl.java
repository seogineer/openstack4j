package org.openstack4j.openstack.trove.internal.ext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.trove.ext.TroveAdminService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class TroveAdminServiceImpl extends BaseTroveExtService implements TroveAdminService {

    private static String instance(String instanceId) {
        return "/mgmt/instances/" + id(instanceId);
    }

    @Override
    public List<Map<String, Object>> listInstances(Map<String, String> filters) {
        return listOf("/mgmt/instances", "instances", filters);
    }

    @Override
    public Map<String, Object> getInstance(String instanceId) {
        return objectOf(instance(instanceId), "instance");
    }

    @Override
    public ActionResponse stop(String instanceId) {
        return action(instance(instanceId), "stop", Map.of());
    }

    @Override
    public ActionResponse reboot(String instanceId) {
        return action(instance(instanceId), "reboot", Map.of());
    }

    @Override
    public ActionResponse migrate(String instanceId, String host) {
        return action(instance(instanceId), "migrate", host == null ? Map.of() : Map.of("host", host));
    }

    @Override
    public ActionResponse resetTaskStatus(String instanceId) {
        return action(instance(instanceId), "reset-task-status", Map.of());
    }

    @Override
    public ActionResponse rebuild(String instanceId, String imageId) {
        return action(instance(instanceId), "rebuild", Map.of("image_id", Objects.requireNonNull(imageId, "imageId")));
    }

    @Override
    public Map<String, Object> rootHistory(String instanceId) {
        return objectOf(instance(instanceId) + "/root", "root_history");
    }

    @Override
    public List<Map<String, Object>> listDatastoreVersions() {
        return listOf("/mgmt/datastore-versions", "versions", null);
    }

    @Override
    public Map<String, Object> getDatastoreVersion(String versionId) {
        return objectOf("/mgmt/datastore-versions/" + id(versionId), "version");
    }

    @Override
    public ActionResponse createDatastoreVersion(Map<String, ?> version) {
        return postWithResponse("/mgmt/datastore-versions").entity(JsonBody.of("version", Objects.requireNonNull(version, "version"))).execute();
    }

    @Override
    public ActionResponse updateDatastoreVersion(String versionId, Map<String, ?> fields) {
        return patchWithResponse("/mgmt/datastore-versions/" + id(versionId)).entity(JsonBody.of(Objects.requireNonNull(fields, "fields"))).execute();
    }

    @Override
    public ActionResponse deleteDatastoreVersion(String versionId) {
        return remove("/mgmt/datastore-versions/" + id(versionId));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> createParameter(String versionId, Map<String, ?> parameter) {
        // Trove answers with the parameter itself (the api-ref sample shows a list)
        Map<String, Object> body = post(Map.class, "/mgmt/datastores/versions/" + id(versionId) + "/parameters")
                .entity(JsonBody.of("configuration-parameter", Objects.requireNonNull(parameter, "parameter"))).execute(propagate404());
        return body == null ? new HashMap<>() : body;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> updateParameter(String versionId, String parameterName, Map<String, ?> parameter) {
        Map<String, Object> body = put(Map.class, "/mgmt/datastores/versions/" + id(versionId) + "/parameters/" + id(parameterName))
                .entity(JsonBody.of("configuration-parameter", Objects.requireNonNull(parameter, "parameter"))).execute(propagate404());
        return body == null ? new HashMap<>() : body;
    }

    @Override
    public ActionResponse deleteParameter(String versionId, String parameterName) {
        return remove("/mgmt/datastores/versions/" + id(versionId) + "/parameters/" + id(parameterName));
    }

    @Override
    public List<Map<String, Object>> getQuotas(String projectId) {
        return listOf("/mgmt/quotas/" + id(projectId), "quotas", null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> updateQuotas(String projectId, Map<String, Integer> quotas) {
        Map<String, Object> body = put(Map.class, "/mgmt/quotas/" + id(projectId))
                .entity(JsonBody.of("quotas", Objects.requireNonNull(quotas, "quotas"))).execute(propagate404());
        Object inner = body == null ? null : body.get("quotas");
        return inner instanceof Map ? (Map<String, Object>) inner : new HashMap<>();
    }
}
