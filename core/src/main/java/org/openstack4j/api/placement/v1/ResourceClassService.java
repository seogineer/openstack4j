package org.openstack4j.api.placement.v1;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/** Resource classes ({@code /resource_classes}). Only {@code CUSTOM_*} classes can be created or deleted. */
public interface ResourceClassService extends RestService {

    List<String> list();

    boolean exists(String name);

    /** Creates a custom resource class; fails (409) when it exists. */
    void create(String name);

    /** Creates the custom resource class if missing (idempotent). */
    void ensure(String name);

    /** Fails (409) while inventories use the class. */
    ActionResponse delete(String name);
}
