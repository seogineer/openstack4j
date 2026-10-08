package org.openstack4j.openstack.rating.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.rating.RatingService;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class RatingServiceImpl extends BaseOpenStackService implements RatingService {

    public RatingServiceImpl() {
        // the catalog usually has the bare root (http://host:8889); a /v1 suffix of older installs is dropped
        super(ServiceType.RATING, url -> url.replaceAll("/+$", "").replaceAll("/v[12](/.*)?$", ""));
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapOf(String path, Map<String, String> filters) {
        Map<String, Object> body = get(Map.class, path).params(filters == null ? Collections.emptyMap() : filters).execute(propagate404());
        return body == null ? new HashMap<>() : body;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Map<String, Object> body, String key) {
        Object list = body.get(key);
        return list instanceof List ? (List<Map<String, Object>>) list : Collections.emptyList();
    }

    @Override
    public Map<String, Object> listDataframes(Map<String, String> filters) {
        return mapOf("/v2/dataframes", filters);
    }

    @Override
    public ActionResponse addDataframes(List<Map<String, Object>> dataframes) {
        return postWithResponse("/v2/dataframes").entity(JsonBody.of(Map.of("dataframes", Objects.requireNonNull(dataframes, "dataframes")))).execute();
    }

    @Override
    public List<Map<String, Object>> listModules() {
        return list(mapOf("/v2/rating/modules", null), "modules");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getModule(String moduleId) {
        return get(Map.class, "/v2/rating/modules/" + id(moduleId)).execute();
    }

    @Override
    public ActionResponse updateModule(String moduleId, Boolean enabled, Integer priority) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (enabled != null)
            body.put("enabled", enabled);
        if (priority != null)
            body.put("priority", priority);
        return putWithResponse("/v2/rating/modules/" + id(moduleId)).entity(JsonBody.of(body)).execute();
    }

    @Override
    public List<Map<String, Object>> listScopes(Map<String, String> filters) {
        return list(mapOf("/v2/scope", filters), "results");
    }

    @Override
    public ActionResponse resetScopes(Map<String, ?> body) {
        return putWithResponse("/v2/scope").entity(JsonBody.of(Objects.requireNonNull(body, "body"))).execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> patchScope(Map<String, ?> body) {
        return patch(Map.class, "/v2/scope").entity(JsonBody.of(Objects.requireNonNull(body, "body"))).execute(propagate404());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> createScope(Map<String, ?> body) {
        return post(Map.class, "/v2/scope").entity(JsonBody.of(Objects.requireNonNull(body, "body"))).execute(propagate404());
    }

    @Override
    public Map<String, Object> summary(Map<String, String> filters) {
        return mapOf("/v2/summary", filters);
    }

    @Override
    public ActionResponse reprocess(List<String> scopeIds, String start, String end, String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("scope_ids", Objects.requireNonNull(scopeIds, "scopeIds"));
        body.put("start_reprocess_time", Objects.requireNonNull(start, "start"));
        body.put("end_reprocess_time", Objects.requireNonNull(end, "end"));
        body.put("reason", Objects.requireNonNull(reason, "reason"));
        return postWithResponse("/v2/task/reprocesses").entity(JsonBody.of(body)).execute();
    }

    @Override
    public List<Map<String, Object>> listReprocesses(Map<String, String> filters) {
        return list(mapOf("/v2/task/reprocesses", filters), "results");
    }

    @Override
    public List<Map<String, Object>> getReprocesses(String scopeId) {
        return list(mapOf("/v2/task/reprocesses/" + id(scopeId), null), "results");
    }
}
