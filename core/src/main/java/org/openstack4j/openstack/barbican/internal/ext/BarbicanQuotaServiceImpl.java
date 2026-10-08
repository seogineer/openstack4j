package org.openstack4j.openstack.barbican.internal.ext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.barbican.ext.BarbicanQuotaService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class BarbicanQuotaServiceImpl extends BaseBarbicanExtService implements BarbicanQuotaService {

    @Override
    public Map<String, Integer> effective() {
        Map<String, Object> body = mapOf("/quotas");
        return ints(body == null ? null : body.get("quotas"));
    }

    @Override
    public Map<String, Map<String, Integer>> listProjectQuotas() {
        return listProjectQuotas(null);
    }

    @Override
    public Map<String, Map<String, Integer>> listProjectQuotas(Map<String, String> filters) {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        for (Map<String, Object> entry : mapsOf("/project-quotas", "project_quotas", filters))
            result.put(String.valueOf(entry.get("project_id")), ints(entry.get("project_quotas")));
        return result;
    }

    @Override
    public Map<String, Integer> getProjectQuotas(String projectId) {
        Map<String, Object> body = mapOf("/project-quotas/" + id(projectId));
        return ints(body == null ? null : body.get("project_quotas"));
    }

    @Override
    public ActionResponse setProjectQuotas(String projectId, Map<String, Integer> quotas) {
        return putWithResponse("/project-quotas/" + id(projectId)).entity(JsonBody.of("project_quotas", Objects.requireNonNull(quotas, "quotas"))).execute();
    }

    @Override
    public ActionResponse deleteProjectQuotas(String projectId) {
        return deleteWithResponse("/project-quotas/" + id(projectId)).execute();
    }

    private static Map<String, Integer> ints(Object value) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (value instanceof Map)
            ((Map<?, ?>) value).forEach((k, v) -> {
                if (v instanceof Number)
                    result.put(String.valueOf(k), ((Number) v).intValue());
            });
        return result;
    }
}
