package org.openstack4j.openstack.dns.v2.internal.ext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.dns.v2.ext.DesignateQuotaService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class DesignateQuotaServiceImpl extends BaseDesignateExtService implements DesignateQuotaService {

    @Override
    public Map<String, Integer> get(String projectId) {
        return quotas(showStrict(Map.class, "/quotas/" + id(projectId)));
    }

    @Override
    public Map<String, Integer> update(String projectId, Map<String, Integer> quotas) {
        Objects.requireNonNull(quotas, "quotas");
        return quotas(patch(Map.class, "/quotas/" + id(projectId)).entity(JsonBody.of(quotas)).execute(propagate404()));
    }

    @Override
    public ActionResponse reset(String projectId) {
        return remove("/quotas/" + id(projectId));
    }

    private static Map<String, Integer> quotas(Map<?, ?> body) {
        Map<String, Integer> quotas = new LinkedHashMap<>();
        if (body != null)
            body.forEach((k, v) -> {
                if (v instanceof Number)
                    quotas.put(String.valueOf(k), ((Number) v).intValue());
            });
        return quotas;
    }
}
