package org.openstack4j.openstack.storage.block.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.storage.block.domain.*;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;
import org.openstack4j.api.storage.BlockResourceFilterService;
import org.openstack4j.model.storage.block.ResourceFilter;
import org.openstack4j.openstack.storage.block.domain.CinderResourceFilter.ResourceFilters;

public class BlockResourceFilterServiceImpl extends BaseBlockStorageServices implements BlockResourceFilterService {

    @Override
    public List<? extends ResourceFilter> list() {
        requireMicroVersion("Resource filters", V(33));
        return get(ResourceFilters.class, uri("/resource_filters")).execute().getList();
    }

    @Override
    public List<? extends ResourceFilter> list(String resource) {
        requireMicroVersion("Resource filters", V(33));
        return get(ResourceFilters.class, uri("/resource_filters")).param("resource", Objects.requireNonNull(resource)).execute().getList();
    }
}
