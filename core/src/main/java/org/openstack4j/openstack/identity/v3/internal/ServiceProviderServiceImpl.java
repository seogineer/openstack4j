package org.openstack4j.openstack.identity.v3.internal;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.identity.v3.ServiceProviderService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.ServiceProvider;
import org.openstack4j.openstack.identity.v3.domain.KeystoneServiceProvider;
import org.openstack4j.openstack.identity.v3.domain.KeystoneServiceProvider.ServiceProviders;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class ServiceProviderServiceImpl extends BaseIdentityServices implements ServiceProviderService {

    private static final String PROVIDERS = "/OS-FEDERATION/service_providers";

    private static String provider(String id) {
        return PROVIDERS + "/" + Objects.requireNonNull(id);
    }

    @Override public List<? extends ServiceProvider> list() { return get(ServiceProviders.class, PROVIDERS).execute().getList(); }
    @Override public ServiceProvider get(String id) { return get(KeystoneServiceProvider.class, provider(id)).execute(); }

    @Override
    public ServiceProvider create(String id, Map<String, Object> attributes) {
        return put(KeystoneServiceProvider.class, provider(id)).entity(JsonBody.of("service_provider", Objects.requireNonNull(attributes))).execute();
    }

    @Override
    public ServiceProvider update(String id, Map<String, Object> attributes) {
        return patch(KeystoneServiceProvider.class, provider(id)).entity(JsonBody.of("service_provider", Objects.requireNonNull(attributes))).execute();
    }

    @Override public ActionResponse delete(String id) { return deleteWithResponse(provider(id)).execute(); }
}
