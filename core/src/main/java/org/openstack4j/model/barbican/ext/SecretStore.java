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
}
