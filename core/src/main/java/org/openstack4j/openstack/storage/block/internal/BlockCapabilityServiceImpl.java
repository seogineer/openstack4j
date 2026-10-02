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
import org.openstack4j.api.storage.BlockCapabilityService;
import org.openstack4j.model.storage.block.BackendCapabilities;

public class BlockCapabilityServiceImpl extends BaseBlockStorageServices implements BlockCapabilityService {

    @Override
    public BackendCapabilities get(String hostName) {
        return get(CinderBackendCapabilities.class, uri("/capabilities/%s", Objects.requireNonNull(hostName))).execute();
    }
}
