package org.openstack4j.openstack.barbican.internal.ext;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.barbican.ext.SecretExtService;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class SecretExtServiceImpl extends BaseBarbicanExtService implements SecretExtService {

    /** Secret consumers need key manager API 1.1. */
    private static final String API_VERSION_1_1 = "key-manager 1.1";

    private static String secret(String secretId) {
        return "/secrets/" + id(secretId);
    }

    @Override
    public ActionResponse storeTextPayload(String secretId, String payload) {
        byte[] bytes = Objects.requireNonNull(payload, "payload").getBytes(StandardCharsets.UTF_8);
        return putWithResponse(secret(secretId)).entity(Payloads.create(new ByteArrayInputStream(bytes))).contentType("text/plain").execute();
    }

    @Override
    public ActionResponse storeBinaryPayload(String secretId, byte[] payload) {
        byte[] encoded = Base64.getEncoder().encode(Objects.requireNonNull(payload, "payload"));
        return putWithResponse(secret(secretId)).entity(Payloads.create(new ByteArrayInputStream(encoded)))
                .contentType("application/octet-stream").header("Content-Encoding", "base64").execute();
    }

    @Override
    public String getTextPayload(String secretId) {
        return new String(payload(secretId, "text/plain"), StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getBinaryPayload(String secretId) {
        return payload(secretId, "application/octet-stream");
    }

    private byte[] payload(String secretId, String accept) {
        return get(byte[].class, secret(secretId) + "/payload").header("Accept", accept)
                .execute(ExecutionOptions.create(BaseBarbicanExtService::bytes, PropagateOnStatus.on(404)));
    }

    @Override
    public Map<String, String> getMetadata(String secretId) {
        Map<String, Object> body = mapOf(secret(secretId) + "/metadata");
        Map<String, String> metadata = new LinkedHashMap<>();
        Object inner = body == null ? null : body.get("metadata");
        if (inner instanceof Map)
            ((Map<?, ?>) inner).forEach((k, v) -> metadata.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
        return metadata;
    }

    @Override
    public ActionResponse replaceMetadata(String secretId, Map<String, String> metadata) {
        return putWithResponse(secret(secretId) + "/metadata").entity(JsonBody.of("metadata", Objects.requireNonNull(metadata, "metadata"))).execute();
    }

    @Override
    public ActionResponse addMetadataItem(String secretId, String key, String value) {
        return postWithResponse(secret(secretId) + "/metadata/").entity(item(key, value)).execute();
    }

    @Override
    public String getMetadataItem(String secretId, String key) {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = get(Map.class, secret(secretId) + "/metadata/" + id(key)).execute();
        Object value = body == null ? null : body.get("value");
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public ActionResponse updateMetadataItem(String secretId, String key, String value) {
        return putWithResponse(secret(secretId) + "/metadata/" + id(key)).entity(item(key, value)).execute();
    }

    @Override
    public ActionResponse deleteMetadataItem(String secretId, String key) {
        return deleteWithResponse(secret(secretId) + "/metadata/" + id(key)).execute();
    }

    private static JsonBody item(String key, String value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", Objects.requireNonNull(key, "key"));
        item.put("value", Objects.requireNonNull(value, "value"));
        return JsonBody.of(item);
    }

    @Override
    public List<Map<String, Object>> listConsumers(String secretId) {
        return mapsOf(secret(secretId) + "/consumers", "consumers");
    }

    @Override
    public ActionResponse registerConsumer(String secretId, String service, String resourceType, String resourceId) {
        return postWithResponse(secret(secretId) + "/consumers").header("OpenStack-API-Version", API_VERSION_1_1)
                .entity(consumer(service, resourceType, resourceId)).execute();
    }

    @Override
    public ActionResponse removeConsumer(String secretId, String service, String resourceType, String resourceId) {
        return deleteWithResponse(secret(secretId) + "/consumers").header("OpenStack-API-Version", API_VERSION_1_1)
                .entity(consumer(service, resourceType, resourceId)).execute();
    }

    private static JsonBody consumer(String service, String resourceType, String resourceId) {
        Map<String, Object> consumer = new LinkedHashMap<>();
        consumer.put("service", Objects.requireNonNull(service, "service"));
        consumer.put("resource_type", Objects.requireNonNull(resourceType, "resourceType"));
        consumer.put("resource_id", Objects.requireNonNull(resourceId, "resourceId"));
        return JsonBody.of(consumer);
    }
}
