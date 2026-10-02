package org.openstack4j.model.storage.block;

import java.util.List;

import org.openstack4j.model.ModelEntity;

/** The filters a deployment allows for one resource ({@code GET /resource_filters}, 3.33+). */
public interface ResourceFilter extends ModelEntity {
    String getResource();
    List<String> getFilters();
}
