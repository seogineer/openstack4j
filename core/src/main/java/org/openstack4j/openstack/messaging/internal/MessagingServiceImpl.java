package org.openstack4j.openstack.messaging.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonValue;
import org.openstack4j.api.messaging.MessagingService;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.HttpEntityHandler;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class MessagingServiceImpl extends BaseOpenStackService implements MessagingService {

    private static volatile String clientId = UUID.randomUUID().toString();

    public MessagingServiceImpl() {
        // catalogs register the bare root (http://host:8888) or .../v2; paths carry /v2 themselves
        super(ServiceType.MESSAGING, url -> url.replaceAll("/+$", "").replaceAll("/v[12](\\.\\d+)?(/.*)?$", ""));
    }

    @Override
    protected <R> Invocation<R> decorate(Invocation<R> invocation) {
        return invocation.header("Client-ID", clientId);
    }

    @Override
    public void useClientId(String id) {
        clientId = Objects.requireNonNull(id, "clientId");
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

    private static String queue(String queueName) {
        return "/v2/queues/" + id(queueName);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> strict(Invocation<Map> invocation) {
        Map<String, Object> body = invocation.execute(propagate404());
        return body == null ? new HashMap<>() : body;
    }

    /** Executes a request answered with 204 and no body when there is nothing to return; {@code null} then. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> orNoContent(Invocation<Map> invocation) {
        HttpResponse response = invocation.executeWithResponse();
        if (response.getStatus() == 204) {
            HttpEntityHandler.closeQuietly(response);
            return null;
        }
        return response.getEntity(Map.class, propagate404());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Map<String, Object> body, String key) {
        Object list = body == null ? null : body.get(key);
        return list instanceof List ? (List<Map<String, Object>>) list : Collections.emptyList();
    }

    private static Map<String, String> params(Map<String, String> filters) {
        return filters == null ? Collections.emptyMap() : filters;
    }

    @Override
    public Map<String, Object> listQueues(Map<String, String> filters) {
        return strict(get(Map.class, "/v2/queues").params(params(filters)));
    }

    @Override
    public ActionResponse createQueue(String queueName, Map<String, ?> metadata) {
        return putWithResponse(queue(queueName)).entity(JsonBody.of(metadata == null ? Map.of() : metadata)).execute();
    }

    @Override
    public Map<String, Object> getQueue(String queueName) {
        return strict(get(Map.class, queue(queueName)));
    }

    @Override
    public Map<String, Object> updateQueue(String queueName, List<Map<String, Object>> patch) {
        return strict(patch(Map.class, queue(queueName)).entity(new ListBody(Objects.requireNonNull(patch, "patch")))
                .contentType("application/openstack-messaging-v2.0-json-patch"));
    }

    @Override
    public ActionResponse deleteQueue(String queueName) {
        return deleteWithResponse(queue(queueName)).execute();
    }

    @Override
    public Map<String, Object> queueStats(String queueName) {
        return strict(get(Map.class, queue(queueName) + "/stats"));
    }

    @Override
    public Map<String, Object> shareQueue(String queueName, List<String> paths, List<String> methods, String expires) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paths", Objects.requireNonNull(paths, "paths"));
        body.put("methods", Objects.requireNonNull(methods, "methods"));
        if (expires != null)
            body.put("expires", expires);
        return strict(post(Map.class, queue(queueName) + "/share").entity(JsonBody.of(body)));
    }

    @Override
    public ActionResponse purgeQueue(String queueName, List<String> resourceTypes) {
        Map<String, Object> body = resourceTypes == null ? Map.of() : Map.of("resource_types", resourceTypes);
        return postWithResponse(queue(queueName) + "/purge").entity(JsonBody.of(body)).execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> postMessages(String queueName, List<Map<String, Object>> messages) {
        Map<String, Object> body = strict(post(Map.class, queue(queueName) + "/messages").entity(JsonBody.of(Map.of("messages", Objects.requireNonNull(messages, "messages")))));
        Object resources = body.get("resources");
        return resources instanceof List ? (List<String>) resources : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> listMessages(String queueName, Map<String, String> filters) {
        // 204 when there are no messages
        return list(orNoContent(get(Map.class, queue(queueName) + "/messages").params(params(filters))), "messages");
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> getMessages(String queueName, List<String> messageIds) {
        return list(orNoContent(get(Map.class, queue(queueName) + "/messages").param("ids", String.join(",", Objects.requireNonNull(messageIds, "messageIds")))), "messages");
    }

    @Override
    public ActionResponse deleteMessages(String queueName, List<String> messageIds) {
        return deleteWithResponse(queue(queueName) + "/messages").param("ids", String.join(",", Objects.requireNonNull(messageIds, "messageIds"))).execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> popMessages(String queueName, int count) {
        return list(orNoContent(delete(Map.class, queue(queueName) + "/messages").param("pop", count)), "messages");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getMessage(String queueName, String messageId) {
        return get(Map.class, queue(queueName) + "/messages/" + id(messageId)).execute();
    }

    @Override
    public ActionResponse deleteMessage(String queueName, String messageId, String claimId) {
        Invocation<ActionResponse> invocation = deleteWithResponse(queue(queueName) + "/messages/" + id(messageId));
        if (claimId != null)
            invocation.param("claim_id", claimId);
        return invocation.execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> claimMessages(String queueName, int ttl, int grace, Integer limit) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ttl", ttl);
        body.put("grace", grace);
        Invocation<Map> invocation = post(Map.class, queue(queueName) + "/claims").entity(JsonBody.of(body));
        if (limit != null)
            invocation.param("limit", limit);
        // 204 when there is nothing to claim
        return list(orNoContent(invocation), "messages");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getClaim(String queueName, String claimId) {
        return get(Map.class, queue(queueName) + "/claims/" + id(claimId)).execute();
    }

    @Override
    public ActionResponse updateClaim(String queueName, String claimId, int ttl, Integer grace) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ttl", ttl);
        if (grace != null)
            body.put("grace", grace);
        return patchWithResponse(queue(queueName) + "/claims/" + id(claimId)).entity(JsonBody.of(body)).execute();
    }

    @Override
    public ActionResponse releaseClaim(String queueName, String claimId) {
        return deleteWithResponse(queue(queueName) + "/claims/" + id(claimId)).execute();
    }

    @Override
    public List<Map<String, Object>> listSubscriptions(String queueName, Map<String, String> filters) {
        return list(strict(get(Map.class, queue(queueName) + "/subscriptions").params(params(filters))), "subscriptions");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getSubscription(String queueName, String subscriptionId) {
        return get(Map.class, queue(queueName) + "/subscriptions/" + id(subscriptionId)).execute();
    }

    @Override
    public String createSubscription(String queueName, String subscriber, int ttl, Map<String, ?> options) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subscriber", Objects.requireNonNull(subscriber, "subscriber"));
        body.put("ttl", ttl);
        body.put("options", options == null ? Map.of() : options);
        return (String) strict(post(Map.class, queue(queueName) + "/subscriptions").entity(JsonBody.of(body))).get("subscription_id");
    }

    @Override
    public ActionResponse updateSubscription(String queueName, String subscriptionId, Map<String, ?> fields) {
        return patchWithResponse(queue(queueName) + "/subscriptions/" + id(subscriptionId)).entity(JsonBody.of(Objects.requireNonNull(fields, "fields"))).execute();
    }

    @Override
    public ActionResponse deleteSubscription(String queueName, String subscriptionId) {
        return deleteWithResponse(queue(queueName) + "/subscriptions/" + id(subscriptionId)).execute();
    }

    @Override
    public ActionResponse confirmSubscription(String queueName, String subscriptionId, boolean confirmed) {
        return postWithResponse(queue(queueName) + "/subscriptions/" + id(subscriptionId) + "/confirm").entity(JsonBody.of(Map.of("confirmed", confirmed))).execute();
    }

    @Override
    public List<Map<String, Object>> listPools(Map<String, String> filters) {
        return list(strict(get(Map.class, "/v2/pools").params(params(filters))), "pools");
    }

    @Override
    public ActionResponse createPool(String poolName, Map<String, ?> pool) {
        return putWithResponse("/v2/pools/" + id(poolName)).entity(JsonBody.of(Objects.requireNonNull(pool, "pool"))).execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getPool(String poolName) {
        return get(Map.class, "/v2/pools/" + id(poolName)).execute();
    }

    @Override
    public ActionResponse updatePool(String poolName, Map<String, ?> fields) {
        return patchWithResponse("/v2/pools/" + id(poolName)).entity(JsonBody.of(Objects.requireNonNull(fields, "fields"))).execute();
    }

    @Override
    public ActionResponse deletePool(String poolName) {
        return deleteWithResponse("/v2/pools/" + id(poolName)).execute();
    }

    @Override
    public List<Map<String, Object>> listFlavors(Map<String, String> filters) {
        return list(strict(get(Map.class, "/v2/flavors").params(params(filters))), "flavors");
    }

    @Override
    public ActionResponse createFlavor(String flavorName, Map<String, ?> flavor) {
        return putWithResponse("/v2/flavors/" + id(flavorName)).entity(JsonBody.of(Objects.requireNonNull(flavor, "flavor"))).execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getFlavor(String flavorName) {
        return get(Map.class, "/v2/flavors/" + id(flavorName)).execute();
    }

    @Override
    public ActionResponse updateFlavor(String flavorName, Map<String, ?> fields) {
        return patchWithResponse("/v2/flavors/" + id(flavorName)).entity(JsonBody.of(Objects.requireNonNull(fields, "fields"))).execute();
    }

    @Override
    public ActionResponse deleteFlavor(String flavorName) {
        return deleteWithResponse("/v2/flavors/" + id(flavorName)).execute();
    }

    @Override
    public boolean ping() {
        return getWithResponse("/v2/ping").execute().isSuccess();
    }

    @Override
    public Map<String, Object> health() {
        return strict(get(Map.class, "/v2/health"));
    }

    /** A request body that is a JSON list. */
    static final class ListBody implements ModelEntity {
        private static final long serialVersionUID = 1L;
        private final List<?> items;

        ListBody(List<?> items) {
            this.items = items;
        }

        @JsonValue
        public List<?> items() {
            return items;
        }
    }
}
