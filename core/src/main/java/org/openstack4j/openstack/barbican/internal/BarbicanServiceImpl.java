package org.openstack4j.openstack.barbican.internal;

import org.openstack4j.api.Apis;
import org.openstack4j.api.barbican.BarbicanService;
import org.openstack4j.api.barbican.ContainerService;
import org.openstack4j.api.barbican.SecretService;

/**
 * This class contains getters for all implementation of the available Barbican services
 */
public class BarbicanServiceImpl extends BaseBarbicanServices implements BarbicanService {

    @Override
    public ContainerService containers() {
        return Apis.get(ContainerService.class);
    }

    @Override
    public SecretService secrets() {
        return Apis.get(SecretService.class);
    }

    @Override
    public org.openstack4j.api.barbican.ext.BarbicanAclService acls() {
        return Apis.get(org.openstack4j.api.barbican.ext.BarbicanAclService.class);
    }

    @Override
    public org.openstack4j.api.barbican.ext.SecretExtService secretsExt() {
        return Apis.get(org.openstack4j.api.barbican.ext.SecretExtService.class);
    }

    @Override
    public org.openstack4j.api.barbican.ext.ContainerExtService containersExt() {
        return Apis.get(org.openstack4j.api.barbican.ext.ContainerExtService.class);
    }

    @Override
    public org.openstack4j.api.barbican.ext.OrderService orders() {
        return Apis.get(org.openstack4j.api.barbican.ext.OrderService.class);
    }

    @Override
    public org.openstack4j.api.barbican.ext.BarbicanQuotaService quotas() {
        return Apis.get(org.openstack4j.api.barbican.ext.BarbicanQuotaService.class);
    }

    @Override
    public org.openstack4j.api.barbican.ext.SecretStoreService secretStores() {
        return Apis.get(org.openstack4j.api.barbican.ext.SecretStoreService.class);
    }
}
