package org.openstack4j.openstack.workflow.internal.ext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.api.workflow.ext.MistralExtService;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class MistralExtServiceImpl extends BaseOpenStackService implements MistralExtService {

    private static final ObjectMapper PLAIN = new ObjectMapper();

    public MistralExtServiceImpl() {
        super(ServiceType.WORKFLOW);
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

    /** Reads a JSON object from a body Mistral may label text/plain. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> json(HttpResponse response) {
        try (InputStream in = response.getInputStream()) {
            byte[] bytes = in == null ? new byte[0] : in.readAllBytes();
            return bytes.length == 0 ? new LinkedHashMap<>() : PLAIN.readValue(bytes, Map.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ExecutionOptions<Map<String, Object>> jsonOptions() {
        return ExecutionOptions.create(MistralExtServiceImpl::json, PropagateOnStatus.on(404));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> text(Invocation<Map> invocation, String content) {
        byte[] bytes = Objects.requireNonNull(content, "content").getBytes(StandardCharsets.UTF_8);
        return (Map<String, Object>) (Map) invocation.entity(Payloads.create(new ByteArrayInputStream(bytes))).contentType("text/plain")
                .execute((ExecutionOptions) jsonOptions());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapOf(String path, Map<String, String> filters, boolean strict) {
        Invocation<Map> invocation = get(Map.class, path).params(filters == null ? Collections.emptyMap() : filters);
        return strict ? invocation.execute(propagate404()) : invocation.execute();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOf(String path, String key, Map<String, String> filters) {
        Map<String, Object> body = mapOf(path, filters, true);
        Object list = body == null ? null : body.get(key);
        return list instanceof List ? (List<Map<String, Object>>) list : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> send(Invocation<Map> invocation, Map<String, ?> body) {
        Map<String, Object> result = invocation.entity(JsonBody.of(body)).execute(propagate404());
        return result == null ? new HashMap<>() : result;
    }

    @Override
    public Map<String, Object> validateWorkflow(String definition) {
        return text(post(Map.class, "/workflows/validate"), definition);
    }

    @Override
    public Map<String, Object> validateWorkbook(String definition) {
        return text(post(Map.class, "/workbooks/validate"), definition);
    }

    @Override
    public Map<String, Object> validateAction(String definition) {
        return text(post(Map.class, "/actions/validate"), definition);
    }

    @Override
    public List<Map<String, Object>> listCodeSources(Map<String, String> filters) {
        return listOf("/code_sources", "code_sources", filters);
    }

    @Override
    public Map<String, Object> getCodeSource(String identifier) {
        return mapOf("/code_sources/" + id(identifier), null, false);
    }

    @Override
    public Map<String, Object> createCodeSource(String name, String content, String scope) {
        Invocation<Map> invocation = post(Map.class, "/code_sources").param("name", id(name));
        if (scope != null)
            invocation.param("scope", scope);
        return text(invocation, content);
    }

    @Override
    public Map<String, Object> updateCodeSource(String identifier, String content, String scope) {
        Invocation<Map> invocation = put(Map.class, "/code_sources").param("identifier", id(identifier));
        if (scope != null)
            invocation.param("scope", scope);
        return text(invocation, content);
    }

    @Override
    public ActionResponse deleteCodeSource(String identifier) {
        return deleteWithResponse("/code_sources/" + id(identifier)).execute();
    }

    @Override
    public List<Map<String, Object>> listDynamicActions(Map<String, String> filters) {
        return listOf("/dynamic_actions", "dynamic_actions", filters);
    }

    @Override
    public Map<String, Object> getDynamicAction(String identifier) {
        return mapOf("/dynamic_actions/" + id(identifier), null, false);
    }

    @Override
    public Map<String, Object> createDynamicAction(Map<String, ?> action) {
        return send(post(Map.class, "/dynamic_actions"), Objects.requireNonNull(action, "action"));
    }

    @Override
    public Map<String, Object> updateDynamicAction(Map<String, ?> action) {
        return send(put(Map.class, "/dynamic_actions"), Objects.requireNonNull(action, "action"));
    }

    @Override
    public ActionResponse deleteDynamicAction(String identifier) {
        return deleteWithResponse("/dynamic_actions/" + id(identifier)).execute();
    }

    @Override
    public List<Map<String, Object>> listEventTriggers(Map<String, String> filters) {
        return listOf("/event_triggers", "event_triggers", filters);
    }

    @Override
    public Map<String, Object> getEventTrigger(String id) {
        return mapOf("/event_triggers/" + id(id), null, false);
    }

    @Override
    public Map<String, Object> createEventTrigger(Map<String, ?> trigger) {
        return send(post(Map.class, "/event_triggers"), Objects.requireNonNull(trigger, "trigger"));
    }

    @Override
    public Map<String, Object> updateEventTrigger(String id, Map<String, ?> fields) {
        return send(put(Map.class, "/event_triggers/" + id(id)), Objects.requireNonNull(fields, "fields"));
    }

    @Override
    public ActionResponse deleteEventTrigger(String id) {
        return deleteWithResponse("/event_triggers/" + id(id)).execute();
    }

    @Override
    public List<Map<String, Object>> listSubExecutions(String executionId, Map<String, String> filters) {
        return listOf("/executions/" + id(executionId) + "/executions", "executions", filters);
    }

    @Override
    public Map<String, Object> executionReport(String executionId, Map<String, String> filters) {
        Map<String, Object> report = mapOf("/executions/" + id(executionId) + "/report", filters, true);
        return report == null ? new HashMap<>() : report;
    }

    @Override
    public List<Map<String, Object>> listTaskExecutions(String taskExecutionId) {
        return listOf("/tasks/" + id(taskExecutionId) + "/executions", "executions", null);
    }

    @Override
    public List<Map<String, Object>> listMembers(String workflowId) {
        return listOf("/workflows/" + id(workflowId) + "/members", "members", null);
    }

    @Override
    public Map<String, Object> getMember(String workflowId, String memberId) {
        return mapOf("/workflows/" + id(workflowId) + "/members/" + id(memberId), null, false);
    }

    @Override
    public Map<String, Object> addMember(String workflowId, String memberId) {
        return send(post(Map.class, "/workflows/" + id(workflowId) + "/members"), Map.of("member_id", id(memberId)));
    }

    @Override
    public Map<String, Object> updateMember(String workflowId, String memberId, String status) {
        return send(put(Map.class, "/workflows/" + id(workflowId) + "/members/" + id(memberId)), Map.of("status", Objects.requireNonNull(status, "status")));
    }

    @Override
    public ActionResponse removeMember(String workflowId, String memberId) {
        return deleteWithResponse("/workflows/" + id(workflowId) + "/members/" + id(memberId)).execute();
    }
}
