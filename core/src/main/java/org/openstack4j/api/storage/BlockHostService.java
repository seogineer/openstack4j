package org.openstack4j.api.storage;

import java.util.List;
import org.openstack4j.model.storage.block.StorageHost;
import org.openstack4j.model.storage.block.StorageHostResource;

import org.openstack4j.common.RestService;

/** Hosts running block storage services ({@code /os-hosts}; admin). */
public interface BlockHostService extends RestService {

    List<? extends StorageHost> list();

    /** Per-project resource usage of one host. */
    List<? extends StorageHostResource> get(String hostName);
}
