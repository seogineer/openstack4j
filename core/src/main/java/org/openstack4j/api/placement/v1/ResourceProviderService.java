package org.openstack4j.api.placement.v1;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.ResourceProvider;
import org.openstack4j.model.placement.v1.ResourceProviderCreate;
import org.openstack4j.model.placement.v1.ResourceProviderListOptions;
import org.openstack4j.model.placement.v1.ResourceProviderUpdate;

/** Placement resource providers ({@code /resource_providers}). */
public interface ResourceProviderService extends RestService {

    List<? extends ResourceProvider> list();

    List<? extends ResourceProvider> list(ResourceProviderListOptions options);

    /** @return the provider, or {@code null} when it does not exist */
    ResourceProvider get(String providerUuid);

    ResourceProvider create(ResourceProviderCreate request);

    ResourceProvider update(String providerUuid, ResourceProviderUpdate request);

    /** Fails (409) while the provider still has inventories, allocations or child providers. */
    ActionResponse delete(String providerUuid);
}
