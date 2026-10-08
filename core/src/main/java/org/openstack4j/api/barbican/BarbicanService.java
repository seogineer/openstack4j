package org.openstack4j.api.barbican;

import org.openstack4j.common.RestService;

/**
 * Barbican (Key Management) Operations API
 */
public interface BarbicanService extends RestService {

    /**
     * @return the Container Service API
     */
    ContainerService containers();

    /**
     * @return the Secrets Service API
     */
    SecretService secrets();

    /** @return read ACLs of secrets and containers */
    org.openstack4j.api.barbican.ext.BarbicanAclService acls();

    /** @return secret payloads, metadata and consumers */
    org.openstack4j.api.barbican.ext.SecretExtService secretsExt();

    /** @return container secrets and consumers */
    org.openstack4j.api.barbican.ext.ContainerExtService containersExt();

    /** @return orders */
    org.openstack4j.api.barbican.ext.OrderService orders();

    /** @return quotas */
    org.openstack4j.api.barbican.ext.BarbicanQuotaService quotas();

    /** @return secret store back ends */
    org.openstack4j.api.barbican.ext.SecretStoreService secretStores();
}
