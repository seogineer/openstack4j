package org.openstack4j.openstack.placement.v1.internal;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openstack4j.api.placement.v1.ResourceProviderService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.PlacementMicroVersions;
import org.openstack4j.model.placement.v1.ResourceProvider;
import org.openstack4j.model.placement.v1.ResourceProviderCreate;
import org.openstack4j.model.placement.v1.ResourceProviderListOptions;
import org.openstack4j.model.placement.v1.ResourceProviderUpdate;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceProvider;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceProvider.ResourceProviders;

public class ResourceProviderServiceImpl extends BasePlacementV1Service implements ResourceProviderService {

    @Override
    public List<? extends ResourceProvider> list() {
        return list(ResourceProviderListOptions.create());
    }

    @Override
    public List<? extends ResourceProvider> list(ResourceProviderListOptions options) {
        Objects.requireNonNull(options, "options");
        if (options.usesForbiddenAggregate())
            requireMicroVersion("forbidden aggregates (member_of=!...)", PlacementMicroVersions.V1_32);
        if (options.usesTraitInSyntax())
            requireMicroVersion("the trait 'in:' syntax", PlacementMicroVersions.V1_39);
        Invocation<ResourceProviders> invocation = placement(HttpMethod.GET, ResourceProviders.class, "/resource_providers")
                .param("name", options.getName())
                .param("uuid", options.getUuid())
                .param("in_tree", options.getInTree())
                .param("resources", options.resourcesParameter())
                .param("required", options.requiredParameter());
        for (String aggregate : options.getMemberOf())
            invocation.param("member_of", aggregate);
        return executeOrThrow(invocation).getList();
    }

    @Override
    public ResourceProvider get(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeOrNull(placement(HttpMethod.GET, PlacementResourceProvider.class, uri("/resource_providers/%s", providerUuid)));
    }

    @Override
    public ResourceProvider create(ResourceProviderCreate request) {
        Objects.requireNonNull(request, "request");
        ObjectNode body = ObjectMapperSingleton.getContext(Object.class).createObjectNode();
        body.put("name", request.getName());
        if (request.getUuid() != null) body.put("uuid", request.getUuid());
        if (request.getParentProviderUuid() != null) body.put("parent_provider_uuid", request.getParentProviderUuid());
        return executeOrThrow(placement(HttpMethod.POST, PlacementResourceProvider.class, "/resource_providers").json(body.toString()));
    }

    @Override
    public ResourceProvider update(String providerUuid, ResourceProviderUpdate request) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        Objects.requireNonNull(request, "request");
        if (request.isUnparent())
            requireMicroVersion("removing a provider's parent", PlacementMicroVersions.V1_37);
        ObjectNode body = ObjectMapperSingleton.getContext(Object.class).createObjectNode();
        body.put("name", request.getName());
        if (request.isUnparent()) body.putNull("parent_provider_uuid");
        else if (request.getParentProviderUuid() != null) body.put("parent_provider_uuid", request.getParentProviderUuid());
        return executeOrThrow(placement(HttpMethod.PUT, PlacementResourceProvider.class, uri("/resource_providers/%s", providerUuid))
                .json(body.toString()));
    }

    @Override
    public ActionResponse delete(String providerUuid) {
        Objects.requireNonNull(providerUuid, "providerUuid");
        return executeAction(placement(HttpMethod.DELETE, ActionResponse.class, uri("/resource_providers/%s", providerUuid)));
    }
}
