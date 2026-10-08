package org.openstack4j.openstack.registration.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.openstack4j.api.registration.RegistrationService;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.HttpEntityHandler;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class RegistrationServiceImpl extends BaseOpenStackService implements RegistrationService {

    public RegistrationServiceImpl() {
        // catalogs register http://host:5050/v1 or the bare root; paths carry /v1 themselves
        super(ServiceType.REGISTRATION, url -> url.replaceAll("/+$", "").replaceAll("/v1(/.*)?$", ""));
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
    private Map<String, Object> strict(Invocation<Map> invocation) {
        Map<String, Object> body = invocation.execute(propagate404());
        return body == null ? new HashMap<>() : body;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Map<String, Object> body, String key) {
        Object list = body == null ? null : body.get(key);
        return list instanceof List ? (List<Map<String, Object>>) list : Collections.emptyList();
    }

    private static <R> Invocation<R> filtered(Invocation<R> invocation, Map<String, ?> filters) {
        if (filters != null && !filters.isEmpty()) {
            try {
                invocation.param("filters", ObjectMapperSingleton.getContext(Map.class).writer().without(SerializationFeature.INDENT_OUTPUT).writeValueAsString(filters));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("filters cannot be written as JSON", e);
            }
        }
        return invocation;
    }

    private ActionResponse post(String path, Map<String, ?> body) {
        return act(postWithResponse(path).entity(JsonBody.of(body)));
    }

    /**
     * Executes an action. Adjutant answers with {@code {"notes": [...]}}, {@code {"errors": [...]}},
     * {@code {"errors": {"field": [...]}}} or a bare JSON string; a failure carries the errors (or the string) as its fault.
     */
    private static ActionResponse act(Invocation<ActionResponse> invocation) {
        HttpResponse response = invocation.executeWithResponse();
        int status = response.getStatus();
        if (status < 400) {
            HttpEntityHandler.closeQuietly(response);
            return ActionResponse.actionSuccess(status);
        }
        String fault = null;
        try (InputStream in = response.getInputStream()) {
            if (in != null)
                fault = fault(ObjectMapperSingleton.getContext(Map.class).readTree(in));
        } catch (IOException | RuntimeException e) {
            // not JSON: fall back to the status line
        } finally {
            HttpEntityHandler.closeQuietly(response);
        }
        return ActionResponse.actionFailed(fault != null && !fault.isBlank() ? fault : "Status: " + status + ", Reason: " + response.getStatusMessage(), status);
    }

    private static String fault(JsonNode body) {
        if (body == null)
            return null;
        if (body.isTextual())
            return body.asText();
        JsonNode errors = body.get("errors");
        if (errors == null)
            return null;
        List<String> messages = new ArrayList<>();
        if (errors.isObject()) {
            errors.fields().forEachRemaining(e -> messages.add(e.getKey() + ": " + text(e.getValue())));
        } else {
            messages.add(text(errors));
        }
        return String.join("; ", messages);
    }

    private static String text(JsonNode node) {
        if (!node.isArray())
            return node.asText();
        List<String> parts = new ArrayList<>();
        node.forEach(n -> parts.add(n.isTextual() ? n.asText() : n.toString()));
        return String.join("; ", parts);
    }

    @Override
    public Map<String, Object> versions() {
        return strict(get(Map.class, "/"));
    }

    @Override
    public Map<String, Object> version() {
        return strict(get(Map.class, "/v1"));
    }

    @Override
    public Map<String, Object> status() {
        return strict(get(Map.class, "/v1/status"));
    }

    @Override
    public Map<String, Object> listTasks(Map<String, String> params, Map<String, ?> filters) {
        Invocation<Map> invocation = get(Map.class, "/v1/tasks");
        if (params != null)
            invocation.params(params);
        return strict(filtered(invocation, filters));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getTask(String taskId) {
        return get(Map.class, "/v1/tasks/" + id(taskId)).execute();
    }

    @Override
    public ActionResponse updateTask(String taskId, Map<String, ?> data) {
        return act(putWithResponse("/v1/tasks/" + id(taskId)).entity(JsonBody.of(Objects.requireNonNull(data, "data"))));
    }

    @Override
    public ActionResponse approveTask(String taskId) {
        return post("/v1/tasks/" + id(taskId), Map.of("approved", true));
    }

    @Override
    public ActionResponse cancelTask(String taskId) {
        return act(deleteWithResponse("/v1/tasks/" + id(taskId)));
    }

    @Override
    public List<Map<String, Object>> listTokens(Map<String, ?> filters) {
        return list(strict(filtered(get(Map.class, "/v1/tokens"), filters)), "tokens");
    }

    @Override
    public ActionResponse reissueToken(String taskId) {
        return post("/v1/tokens", Map.of("task", id(taskId)));
    }

    @Override
    public ActionResponse deleteExpiredTokens() {
        return act(deleteWithResponse("/v1/tokens"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getToken(String token) {
        return get(Map.class, "/v1/tokens/" + id(token)).execute();
    }

    @Override
    public ActionResponse submitToken(String token, Map<String, ?> fields) {
        return post("/v1/tokens/" + id(token), fields == null ? Map.of() : fields);
    }

    @Override
    public List<Map<String, Object>> listNotifications(Map<String, ?> filters) {
        return list(strict(filtered(get(Map.class, "/v1/notifications"), filters)), "notifications");
    }

    @Override
    public ActionResponse acknowledgeNotifications(List<String> notificationIds) {
        return post("/v1/notifications", Map.of("notifications", Objects.requireNonNull(notificationIds, "notificationIds")));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getNotification(String notificationId) {
        return get(Map.class, "/v1/notifications/" + id(notificationId)).execute();
    }

    @Override
    public ActionResponse acknowledgeNotification(String notificationId) {
        return post("/v1/notifications/" + id(notificationId), Map.of("acknowledged", true));
    }

    @Override
    public List<Map<String, Object>> listUsers() {
        return list(strict(get(Map.class, "/v1/openstack/users")), "users");
    }

    @Override
    public ActionResponse inviteUser(String email, List<String> roles, List<String> inheritedRoles, Map<String, ?> extra) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (extra != null)
            body.putAll(extra);
        body.put("email", Objects.requireNonNull(email, "email"));
        body.put("roles", Objects.requireNonNull(roles, "roles"));
        if (inheritedRoles != null)
            body.put("inherited_roles", inheritedRoles);
        return post("/v1/openstack/users", body);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getUser(String userId) {
        return get(Map.class, "/v1/openstack/users/" + id(userId)).execute();
    }

    @Override
    public ActionResponse cancelInvite(String userId) {
        return act(deleteWithResponse("/v1/openstack/users/" + id(userId)));
    }

    @Override
    public Map<String, Object> getUserRoles(String userId) {
        return strict(get(Map.class, "/v1/openstack/users/" + id(userId) + "/roles"));
    }

    private static Map<String, Object> roles(List<String> roles, List<String> inheritedRoles) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("roles", Objects.requireNonNull(roles, "roles"));
        if (inheritedRoles != null)
            body.put("inherited_roles", inheritedRoles);
        return body;
    }

    @Override
    public ActionResponse addUserRoles(String userId, List<String> roles, List<String> inheritedRoles) {
        return act(putWithResponse("/v1/openstack/users/" + id(userId) + "/roles").entity(JsonBody.of(roles(roles, inheritedRoles))));
    }

    @Override
    public ActionResponse removeUserRoles(String userId, List<String> roles, List<String> inheritedRoles) {
        return act(deleteWithResponse("/v1/openstack/users/" + id(userId) + "/roles").entity(JsonBody.of(roles(roles, inheritedRoles))));
    }

    @Override
    public List<Map<String, Object>> listRoles() {
        return list(strict(get(Map.class, "/v1/openstack/roles")), "roles");
    }

    @Override
    public ActionResponse resetPassword(String email, String username) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", Objects.requireNonNull(email, "email"));
        if (username != null)
            body.put("username", username);
        return post("/v1/openstack/users/password-reset", body);
    }

    @Override
    public ActionResponse updateEmail(String newEmail) {
        return post("/v1/openstack/users/email-update", Map.of("new_email", Objects.requireNonNull(newEmail, "newEmail")));
    }

    @Override
    public ActionResponse signUp(String email, String projectName, Map<String, ?> extra) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (extra != null)
            body.putAll(extra);
        body.put("email", Objects.requireNonNull(email, "email"));
        body.put("project_name", Objects.requireNonNull(projectName, "projectName"));
        return post("/v1/openstack/sign-up", body);
    }

    @Override
    public Map<String, Object> getQuotas(List<String> regions) {
        Invocation<Map> invocation = get(Map.class, "/v1/openstack/quotas");
        if (regions != null && !regions.isEmpty())
            invocation.param("regions", String.join(",", regions));
        return strict(invocation);
    }

    @Override
    public ActionResponse updateQuotas(String size, List<String> regions) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", Objects.requireNonNull(size, "size"));
        if (regions != null)
            body.put("regions", regions);
        return post("/v1/openstack/quotas", body);
    }
}
