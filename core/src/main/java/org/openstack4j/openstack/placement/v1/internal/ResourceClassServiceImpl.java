package org.openstack4j.openstack.placement.v1.internal;

import java.util.List;

import org.openstack4j.api.placement.v1.ResourceClassService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.ResourceClasses;
import org.openstack4j.openstack.placement.v1.domain.PlacementResourceClass;

public class ResourceClassServiceImpl extends BasePlacementV1Service implements ResourceClassService {

    @Override
    public List<String> list() {
        return executeOrThrow(placement(HttpMethod.GET, PlacementResourceClass.ResourceClasses.class, "/resource_classes")).names();
    }

    @Override
    public boolean exists(String name) {
        requireName("resource class", name);
        return executeOrNull(placement(HttpMethod.GET, PlacementResourceClass.class, uri("/resource_classes/%s", name))) != null;
    }

    @Override
    public void create(String name) {
        requireCustom(name);
        executeOrThrow(placement(HttpMethod.POST, Void.class, "/resource_classes").json("{\"name\":\"" + name + "\"}"));
    }

    @Override
    public void ensure(String name) {
        requireCustom(name);
        executeOrThrow(placement(HttpMethod.PUT, Void.class, uri("/resource_classes/%s", name)));
    }

    @Override
    public ActionResponse delete(String name) {
        requireCustom(name);
        return executeAction(placement(HttpMethod.DELETE, ActionResponse.class, uri("/resource_classes/%s", name)));
    }

    private static void requireCustom(String name) {
        requireName("resource class", name);
        if (!ResourceClasses.isCustom(name))
            throw new IllegalArgumentException("Only custom resource classes (CUSTOM_*) can be created or deleted: " + name);
    }
}
