package org.openstack4j.openstack.telemetry.internal.ext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openstack4j.api.telemetry.ext.AodhAlarmExtService;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.telemetry.internal.BaseTelemetryAodhServices;

public class AodhAlarmExtServiceImpl extends BaseTelemetryAodhServices implements AodhAlarmExtService {

    private static final ObjectMapper PLAIN = new ObjectMapper();

    private static <T> ExecutionOptions<T> propagate404() {
        return ExecutionOptions.create(PropagateOnStatus.on(404));
    }

    private static String id(String value) {
        Objects.requireNonNull(value, "id");
        if (value.isBlank() || value.indexOf('/') >= 0 || value.indexOf('?') >= 0 || value.indexOf('#') >= 0)
            throw new IllegalArgumentException("Not a valid identifier: '" + value + "'");
        return value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(List<?> list) {
        return list == null ? Collections.emptyList() : (List<Map<String, Object>>) list;
    }

    @Override
    public List<Map<String, Object>> history(String alarmId, Map<String, String> filters) {
        return maps(get(List.class, "/alarms/" + id(alarmId) + "/history").params(filters == null ? Collections.emptyMap() : filters).execute(propagate404()));
    }

    @Override
    public String getState(String alarmId) {
        return get(String.class, "/alarms/" + id(alarmId) + "/state").execute(propagate404());
    }

    @Override
    public String setState(String alarmId, String state) {
        try {
            return put(String.class, "/alarms/" + id(alarmId) + "/state").json(PLAIN.writeValueAsString(Objects.requireNonNull(state, "state")))
                    .execute(propagate404());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public List<Map<String, Object>> queryAlarms(String filter, String orderby, Integer limit) {
        return maps(post(List.class, "/query/alarms").entity(JsonBody.of(query(filter, orderby, limit))).execute(propagate404()));
    }

    @Override
    public List<Map<String, Object>> queryHistory(String filter, String orderby, Integer limit) {
        return maps(post(List.class, "/query/alarms/history").entity(JsonBody.of(query(filter, orderby, limit))).execute(propagate404()));
    }

    private static Map<String, Object> query(String filter, String orderby, Integer limit) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (filter != null)
            body.put("filter", filter);
        if (orderby != null)
            body.put("orderby", orderby);
        if (limit != null)
            body.put("limit", limit);
        return body;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Integer> getQuotas(String projectId) {
        return quotas(get(Map.class, "/quotas").param("project_id", id(projectId)).execute(propagate404()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Integer> setQuotas(String projectId, Map<String, Integer> quotas) {
        List<Map<String, Object>> entries = new ArrayList<>();
        Objects.requireNonNull(quotas, "quotas").forEach((resource, limit) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("resource", resource);
            entry.put("limit", limit);
            entries.add(entry);
        });
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("project_id", id(projectId));
        body.put("quotas", entries);
        return quotas(post(Map.class, "/quotas").entity(JsonBody.of(body)).execute(propagate404()));
    }

    @Override
    public ActionResponse deleteQuotas(String projectId) {
        return deleteWithResponse("/quotas").param("project_id", id(projectId)).execute();
    }

    private static Map<String, Integer> quotas(Map<?, ?> body) {
        Map<String, Integer> result = new LinkedHashMap<>();
        Object list = body == null ? null : body.get("quotas");
        if (list instanceof List)
            for (Object entry : (List<?>) list)
                if (entry instanceof Map && ((Map<?, ?>) entry).get("limit") instanceof Number)
                    result.put(String.valueOf(((Map<?, ?>) entry).get("resource")), ((Number) ((Map<?, ?>) entry).get("limit")).intValue());
        return result;
    }
}
