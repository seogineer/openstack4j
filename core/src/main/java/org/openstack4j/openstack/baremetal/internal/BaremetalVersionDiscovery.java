package org.openstack4j.openstack.baremetal.internal;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.openstack.baremetal.domain.IronicVersions;
import org.openstack4j.openstack.internal.BaseOpenStackService;

/** Fetches the Ironic root document. Extends BaseOpenStackService directly so no microversion header is added. */
final class BaremetalVersionDiscovery extends BaseOpenStackService {

    BaremetalVersionDiscovery() {
        super(ServiceType.BAREMETAL, BaremetalMicroVersions::rootUrl);
    }

    IronicVersions fetch() {
        return request(HttpMethod.GET, IronicVersions.class, "/").execute();
    }
}
