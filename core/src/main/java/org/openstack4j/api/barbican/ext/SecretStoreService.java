package org.openstack4j.api.barbican.ext;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.barbican.ext.SecretStore;
import org.openstack4j.model.common.ActionResponse;

/** Secret store back ends ({@code /v1/secret-stores}; multiple back ends must be enabled on the server). */
public interface SecretStoreService extends RestService {

    /** @return the secret stores */
    List<? extends SecretStore> list();

    /** @return the secret store, or {@code null} when it does not exist */
    SecretStore get(String secretStoreId);

    /** @return the global default secret store */
    SecretStore globalDefault();

    /** @return the project's preferred secret store, or {@code null} when none is set */
    SecretStore preferred();

    /** Makes a secret store the project's preferred one (admin). */
    ActionResponse setPreferred(String secretStoreId);

    /** Removes the project's preferred secret store (admin). */
    ActionResponse unsetPreferred(String secretStoreId);
}
