package org.openstack4j.openstack.placement.v1.internal;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.openstack4j.api.placement.v1.TraitService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.ResourceProviderTraits;
import org.openstack4j.model.placement.v1.TraitListOptions;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceProviderTraits;
import org.openstack4j.openstack.placement.v1.domain.PlacementTraits;

public class TraitServiceImpl extends BasePlacementV1Service implements TraitService {

    private static final String CUSTOM_PREFIX = "CUSTOM_";

    @Override
    public List<String> list() {
        return list(TraitListOptions.create());
    }

    @Override
    public List<String> list(TraitListOptions options) {
        Objects.requireNonNull(options, "options");
        return executeOrThrow(placement(HttpMethod.GET, PlacementTraits.class, "/traits")
                .param("name", options.getName())
                .param("associated", options.getAssociated())).getTraits();
    }

    @Override
    public boolean exists(String trait) {
        requireName("trait", trait);
        return existsByStatus(placement(HttpMethod.GET, ActionResponse.class, uri("/traits/%s", trait)));
    }

    @Override
    public void create(String trait) {
        requireCustom(trait);
        executeOrThrow(placement(HttpMethod.PUT, Void.class, uri("/traits/%s", trait)));
    }

    @Override
    public ActionResponse delete(String trait) {
        requireCustom(trait);
        return executeAction(placement(HttpMethod.DELETE, ActionResponse.class, uri("/traits/%s", trait)));
    }

    @Override
    public ResourceProviderTraits listForProvider(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeOrThrow(placement(HttpMethod.GET, PlacementResourceProviderTraits.class, uri("/resource_providers/%s/traits", providerUuid)));
    }

    @Override
    public ResourceProviderTraits replaceForProvider(String providerUuid, long generation, Collection<String> traits) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        Objects.requireNonNull(traits, "traits");
        for (String trait : traits) requireName("trait", trait);
        return executeOrThrow(placement(HttpMethod.PUT, PlacementResourceProviderTraits.class, uri("/resource_providers/%s/traits", providerUuid))
                .entity(new PlacementResourceProviderTraits(generation, traits)));
    }

    @Override
    public ActionResponse deleteForProvider(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeAction(placement(HttpMethod.DELETE, ActionResponse.class, uri("/resource_providers/%s/traits", providerUuid)));
    }

    private static void requireCustom(String trait) {
        requireName("trait", trait);
        if (!trait.startsWith(CUSTOM_PREFIX))
            throw new IllegalArgumentException("Only custom traits (CUSTOM_*) can be created or deleted: " + trait);
    }
}
