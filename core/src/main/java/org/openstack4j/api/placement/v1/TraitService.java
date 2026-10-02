package org.openstack4j.api.placement.v1;

import java.util.Collection;
import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.ResourceProviderTraits;
import org.openstack4j.model.placement.v1.TraitListOptions;

/** Traits ({@code /traits}) and the traits of a resource provider ({@code /resource_providers/{uuid}/traits}). */
public interface TraitService extends RestService {

    List<String> list();

    List<String> list(TraitListOptions options);

    boolean exists(String trait);

    /** Creates a custom trait ({@code CUSTOM_*}); idempotent. */
    void create(String trait);

    /** Fails (409) while a provider has the trait. */
    ActionResponse delete(String trait);

    ResourceProviderTraits listForProvider(String providerUuid);

    /** Replaces the provider's traits; traits must already exist. */
    ResourceProviderTraits replaceForProvider(String providerUuid, long generation, Collection<String> traits);

    ActionResponse deleteForProvider(String providerUuid);
}
