package org.openstack4j.model.barbican.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A secret store back end (multiple back ends must be enabled on the server). */
public interface SecretStore extends ModelEntity {
    String getSecretStoreRef();
    String getName();
    String getStatus();
    Boolean isGlobalDefault();
    String getSecretStorePlugin();
    String getCryptoPlugin();
    String getCreated();
    String getUpdated();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();

    /** @return the store's id: {@code secret_store_id} when the server sent it (key-manager 1.3), else the last segment of {@link #getSecretStoreRef()}; {@code null} when neither is there */
    default String getSecretStoreId() {
        Object id = getAttributes() == null ? null : getAttributes().get("secret_store_id");
        if (id != null)
            return String.valueOf(id);
        String ref = getSecretStoreRef();
        if (ref == null || ref.isEmpty())
            return null;
        String trimmed = ref.endsWith("/") ? ref.substring(0, ref.length() - 1) : ref;
        return trimmed.substring(trimmed.lastIndexOf('/') + 1);
    }
}
