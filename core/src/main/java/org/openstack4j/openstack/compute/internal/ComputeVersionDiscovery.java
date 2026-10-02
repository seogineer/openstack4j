package org.openstack4j.openstack.compute.internal;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.openstack.compute.domain.NovaVersions;
import org.openstack4j.openstack.internal.BaseOpenStackService;

/** Fetches the Nova root document. Extends BaseOpenStackService directly so no microversion header is added. */
final class ComputeVersionDiscovery extends BaseOpenStackService {

    ComputeVersionDiscovery() {
        super(ServiceType.COMPUTE, ComputeMicroVersions::rootUrl);
    }

    NovaVersions fetch() {
        return request(HttpMethod.GET, NovaVersions.class, "/").execute();
    }
}
