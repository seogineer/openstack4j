package org.openstack4j.api.barbican.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/** Secret payloads, user metadata and consumers. */
public interface SecretExtService extends RestService {

    /** Stores the payload of a secret created without one (two-step creation), as {@code text/plain}. */
    ActionResponse storeTextPayload(String secretId, String payload);

    /** Stores a binary payload ({@code application/octet-stream}, sent base64-encoded). */
    ActionResponse storeBinaryPayload(String secretId, byte[] payload);

    /** @return the payload as text ({@code Accept: text/plain}); a missing secret raises */
    String getTextPayload(String secretId);

    /** @return the payload bytes ({@code Accept: application/octet-stream}); a missing secret raises */
    byte[] getBinaryPayload(String secretId);

    /** @return the user metadata of a secret; a missing secret raises */
    Map<String, String> getMetadata(String secretId);

    /** Replaces all user metadata of a secret. */
    ActionResponse replaceMetadata(String secretId, Map<String, String> metadata);

    /** Adds one metadata item (fails when the key exists). */
    ActionResponse addMetadataItem(String secretId, String key, String value);

    /** @return one metadata value, or {@code null} when it is not set */
    String getMetadataItem(String secretId, String key);

    ActionResponse updateMetadataItem(String secretId, String key, String value);

    ActionResponse deleteMetadataItem(String secretId, String key);

    /** @return the consumers of a secret ({@code service}, {@code resource_type}, {@code resource_id} …); needs a Barbican with secret consumers (API 1.1), the client does not check the server version first */
    List<Map<String, Object>> listConsumers(String secretId);

    /** @param filters {@code limit} (server default 10, max 100) and {@code offset} @return one page of consumers */
    List<Map<String, Object>> listConsumers(String secretId, Map<String, String> filters);

    /** Registers a consumer of a secret, e.g. service {@code image}, resource type {@code image} and the image id. Sends {@code OpenStack-API-Version: key-manager 1.1}; a Barbican without secret consumers answers with an error (the client does not check the server version first). */
    ActionResponse registerConsumer(String secretId, String service, String resourceType, String resourceId);

    ActionResponse removeConsumer(String secretId, String service, String resourceType, String resourceId);

    /**
     * Moves a secret to another secret store ({@code PUT /v1/secrets/{id}/secret-store/{store}}, key-manager 1.3, multiple back
     * ends enabled). Sends {@code OpenStack-API-Version: key-manager 1.3}; a Barbican without it answers with an error (the client
     * does not check the server version first).
     *
     * @param secretStoreId the store's UUID, e.g. {@link org.openstack4j.model.barbican.ext.SecretStore#getSecretStoreId()}
     */
    ActionResponse migrateToSecretStore(String secretId, String secretStoreId);
}
