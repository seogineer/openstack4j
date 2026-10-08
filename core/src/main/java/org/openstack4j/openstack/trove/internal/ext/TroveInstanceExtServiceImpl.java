package org.openstack4j.openstack.trove.internal.ext;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.trove.ext.TroveInstanceExtService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.trove.Instance;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.trove.domain.TroveInstance.DBInstances;

public class TroveInstanceExtServiceImpl extends BaseTroveExtService implements TroveInstanceExtService {

    private static String instance(String instanceId) {
        return "/instances/" + id(instanceId);
    }

    @Override
    public List<? extends Instance> listDetail() {
        return listDetail(null);
    }

    @Override
    public List<? extends Instance> listDetail(Map<String, String> filters) {
        DBInstances instances = get(DBInstances.class, "/instances/detail").params(filters == null ? Collections.emptyMap() : filters).execute(propagate404());
        return instances == null ? Collections.emptyList() : instances.getList();
    }

    private ActionResponse update(String instanceId, String key, Object value) {
        Map<String, Object> fields = new HashMap<>();
        fields.put(key, value);
        return putWithResponse(instance(instanceId)).entity(JsonBody.of("instance", fields)).execute();
    }

    @Override
    public ActionResponse rename(String instanceId, String name) {
        return update(instanceId, "name", Objects.requireNonNull(name, "name"));
    }

    @Override
    public ActionResponse attachConfiguration(String instanceId, String configurationId) {
        return update(instanceId, "configuration", id(configurationId));
    }

    @Override
    public ActionResponse detachConfiguration(String instanceId) {
        return update(instanceId, "configuration", null);
    }

    @Override
    public ActionResponse upgradeDatastoreVersion(String instanceId, String datastoreVersion) {
        return update(instanceId, "datastore_version", Objects.requireNonNull(datastoreVersion, "datastoreVersion"));
    }

    @Override
    public ActionResponse detachReplica(String instanceId) {
        return update(instanceId, "replica_of", null);
    }

    @Override
    public ActionResponse updateAccess(String instanceId, boolean isPublic, List<String> allowedCidrs) {
        Map<String, Object> access = new LinkedHashMap<>();
        access.put("is_public", isPublic);
        if (allowedCidrs != null)
            access.put("allowed_cidrs", allowedCidrs);
        return update(instanceId, "access", access);
    }

    @Override
    public ActionResponse restart(String instanceId) {
        return action(instance(instanceId), "restart", Map.of());
    }

    @Override
    public ActionResponse resizeFlavor(String instanceId, String flavorId) {
        return action(instance(instanceId), "resize", Map.of("flavorRef", Objects.requireNonNull(flavorId, "flavorId")));
    }

    @Override
    public ActionResponse resizeVolume(String instanceId, int sizeGb) {
        return action(instance(instanceId), "resize", Map.of("volume", Map.of("size", sizeGb)));
    }

    @Override
    public ActionResponse promoteToReplicaSource(String instanceId) {
        return action(instance(instanceId), "promote_to_replica_source", Map.of());
    }

    @Override
    public ActionResponse ejectReplicaSource(String instanceId) {
        return action(instance(instanceId), "eject_replica_source", Map.of());
    }

    @Override
    public ActionResponse resetStatus(String instanceId) {
        return action(instance(instanceId), "reset_status", Map.of());
    }

    @Override
    public List<Map<String, Object>> listBackups(String instanceId) {
        return listOf(instance(instanceId) + "/backups", "backups", null);
    }

    @Override
    public Map<String, Object> configurationDefaults(String instanceId) {
        Map<String, Object> inner = objectOf(instance(instanceId) + "/configuration", "instance");
        Object configuration = inner.get("configuration");
        return configuration instanceof Map ? castMap(configuration) : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @Override
    public List<Map<String, Object>> listLogs(String instanceId) {
        return listOf(instance(instanceId) + "/log", "logs", null);
    }

    @Override
    public Map<String, Object> showLog(String instanceId, String logName) {
        return log(instanceId, logName, null);
    }

    @Override
    public Map<String, Object> enableLog(String instanceId, String logName) {
        return log(instanceId, logName, "enable");
    }

    @Override
    public Map<String, Object> disableLog(String instanceId, String logName) {
        return log(instanceId, logName, "disable");
    }

    @Override
    public Map<String, Object> publishLog(String instanceId, String logName) {
        return log(instanceId, logName, "publish");
    }

    @Override
    public Map<String, Object> discardLog(String instanceId, String logName) {
        return log(instanceId, logName, "discard");
    }

    private Map<String, Object> log(String instanceId, String logName, String operation) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", Objects.requireNonNull(logName, "logName"));
        if (operation != null)
            body.put(operation, 1);
        return postFor(instance(instanceId) + "/log", body, "log");
    }

    @Override
    public Map<String, Object> sslStatus(String instanceId) {
        return objectOf(instance(instanceId) + "/ssl", "ssl");
    }

    @Override
    public Map<String, Object> enableSsl(String instanceId, Map<String, ?> options) {
        Map<String, Object> ssl = new LinkedHashMap<>();
        ssl.put("enable", true);
        if (options != null)
            ssl.putAll(options);
        return postFor(instance(instanceId) + "/ssl", Map.of("ssl", ssl), "ssl");
    }

    @Override
    public Map<String, Object> disableSsl(String instanceId) {
        return postFor(instance(instanceId) + "/ssl", Map.of("ssl", Map.of("disable", true)), "ssl");
    }

    @Override
    public Map<String, Object> rollbackSsl(String instanceId) {
        return postFor(instance(instanceId) + "/ssl", Map.of("ssl", Map.of("rollback", true)), "ssl");
    }

    @Override
    public boolean isRootEnabled(String instanceId) {
        Map<String, Object> body = mapOf(instance(instanceId) + "/root", null);
        return body != null && Boolean.TRUE.equals(body.get("rootEnabled"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> enableRoot(String instanceId, String password) {
        Invocation<Map> invocation = post(Map.class, instance(instanceId) + "/root");
        if (password != null)
            invocation.entity(JsonBody.of(Map.of("password", password)));
        Map<String, Object> body = invocation.execute(propagate404());
        Object user = body == null ? null : body.get("user");
        return user instanceof Map ? (Map<String, Object>) user : new HashMap<>();
    }

    @Override
    public ActionResponse disableRoot(String instanceId) {
        return deleteWithResponse(instance(instanceId) + "/root").execute();
    }
}
