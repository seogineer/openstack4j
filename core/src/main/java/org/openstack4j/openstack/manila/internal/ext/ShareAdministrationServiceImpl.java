package org.openstack4j.openstack.manila.internal.ext;

import static org.openstack4j.openstack.manila.internal.ManilaMicroVersions.V;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.manila.ext.ShareAdministrationService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.AvailabilityZone;
import org.openstack4j.model.manila.Service;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.manila.domain.ManilaAvailabilityZone;
import org.openstack4j.openstack.manila.domain.ManilaService;

public class ShareAdministrationServiceImpl extends BaseManilaExtService implements ShareAdministrationService {

    @Override
    public List<? extends AvailabilityZone> availabilityZones() {
        return listOf(V(7), ManilaAvailabilityZone.AvailabilityZones.class, "/availability-zones", null);
    }

    @Override
    public List<? extends Service> services(Map<String, String> filters) {
        return listOf(V(7), ManilaService.Services.class, "/services", filters);
    }

    @Override
    public Map<String, Object> enableService(String host, String binary) {
        return serviceState("enable", host, binary, null);
    }

    @Override
    public Map<String, Object> disableService(String host, String binary, String reason) {
        return serviceState("disable", host, binary, reason);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> serviceState(String change, String host, String binary, String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("host", Objects.requireNonNull(host, "host"));
        body.put("binary", Objects.requireNonNull(binary, "binary"));
        if (reason != null)
            body.put("disabled_reason", reason);
        String path = "/services/" + change;
        return at(reason == null ? V(7) : V(83), put(Map.class, path), path).entity(JsonBody.of(body)).execute(propagate404());
    }

    @Override
    public ActionResponse ensureShares(String host) {
        return send(V(86), postWithResponse("/services/ensure-shares"), "/services/ensure-shares", Map.of("ensure_shares", Map.of("host", Objects.requireNonNull(host, "host"))));
    }

    @Override
    public Map<String, Object> quotaSet(String projectId) {
        return unwrap(showStrict(V(7), Map.class, "/quota-sets/" + id(projectId)), "quota_set");
    }

    @Override
    public Map<String, Object> quotaSetDetail(String projectId) {
        return unwrap(showStrict(V(25), Map.class, "/quota-sets/" + id(projectId) + "/detail"), "quota_set");
    }

    @Override
    public Map<String, Object> quotaSetDefaults(String projectId) {
        return unwrap(showStrict(V(7), Map.class, "/quota-sets/" + id(projectId) + "/defaults"), "quota_set");
    }

    @Override
    public Map<String, Object> updateQuotaSet(String projectId, Map<String, ?> quotas) {
        String path = "/quota-sets/" + id(projectId);
        return unwrap(at(V(7), put(Map.class, path), path).entity(JsonBody.of("quota_set", Objects.requireNonNull(quotas, "quotas"))).execute(propagate404()), "quota_set");
    }

    @Override
    public ActionResponse deleteQuotaSet(String projectId) {
        return remove(V(7), "/quota-sets/" + id(projectId));
    }

    @Override
    public Map<String, Object> quotaClassSet(String quotaClassName) {
        return unwrap(showStrict(V(7), Map.class, "/quota-class-sets/" + id(quotaClassName)), "quota_class_set");
    }

    @Override
    public Map<String, Object> updateQuotaClassSet(String quotaClassName, Map<String, ?> quotas) {
        String path = "/quota-class-sets/" + id(quotaClassName);
        return unwrap(at(V(7), put(Map.class, path), path).entity(JsonBody.of("quota_class_set", Objects.requireNonNull(quotas, "quotas"))).execute(propagate404()), "quota_class_set");
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> shareTypeAccess(String shareTypeId) {
        Map<String, Object> body = showStrict(V(7), Map.class, "/types/" + id(shareTypeId) + "/share_type_access");
        Object access = body == null ? null : body.get("share_type_access");
        return access instanceof List ? (List<Map<String, Object>>) access : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(Map<?, ?> body, String root) {
        Object inner = body == null ? null : body.get(root);
        return inner instanceof Map ? new HashMap<>((Map<String, Object>) inner) : new HashMap<>();
    }
}
