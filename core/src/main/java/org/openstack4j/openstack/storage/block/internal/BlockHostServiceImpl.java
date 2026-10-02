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
import org.openstack4j.api.storage.BlockHostService;
import org.openstack4j.model.storage.block.StorageHost;
import org.openstack4j.model.storage.block.StorageHostResource;
import org.openstack4j.openstack.storage.block.domain.CinderHost.Hosts;

public class BlockHostServiceImpl extends BaseBlockStorageServices implements BlockHostService {

    @Override
    public List<? extends StorageHost> list() {
        return get(Hosts.class, uri("/os-hosts")).execute().getList();
    }

    @Override
    public List<? extends StorageHostResource> get(String hostName) {
        CinderHostDetail detail = get(CinderHostDetail.class, uri("/os-hosts/%s", Objects.requireNonNull(hostName))).execute();
        return detail == null ? Collections.emptyList() : detail.resources();
    }
}
