package org.openstack4j.openstack.storage.block.internal;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.storage.block.domain.CinderVersions;

/** Fetches the Cinder root document. Extends BaseOpenStackService directly so no microversion header is added. */
final class BlockStorageVersionDiscovery extends BaseOpenStackService {

    BlockStorageVersionDiscovery() {
        super(ServiceType.BLOCK_STORAGE, BlockStorageMicroVersions::rootUrl);
    }

    /** Cinder answers {@code GET /} with 300 Multiple Choices and the versions document as body. */
    CinderVersions fetch() {
        return request(HttpMethod.GET, CinderVersions.class, "/").execute();
    }
}
