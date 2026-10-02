package org.openstack4j.api.storage;

import java.util.List;
import org.openstack4j.model.storage.block.ResourceFilter;

import org.openstack4j.common.RestService;

/** Allowed list filters per resource ({@code GET /resource_filters}, block storage microversion 3.33+). */
public interface BlockResourceFilterService extends RestService {

    List<? extends ResourceFilter> list();

    /** @param resource such as {@code volume} or {@code snapshot} */
    List<? extends ResourceFilter> list(String resource);
}
