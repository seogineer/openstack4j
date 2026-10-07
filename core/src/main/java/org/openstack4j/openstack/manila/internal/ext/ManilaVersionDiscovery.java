package org.openstack4j.openstack.manila.internal.ext;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.openstack.manila.domain.ext.ManilaVersions;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.manila.internal.ManilaMicroVersions;

/** Fetches the Manila root document. Extends BaseOpenStackService directly so no microversion header is added. */
final class ManilaVersionDiscovery extends BaseOpenStackService {

    ManilaVersionDiscovery() {
        super(ServiceType.SHARE, ManilaMicroVersions::rootUrl);
    }

    ManilaVersions fetch() {
        return request(HttpMethod.GET, ManilaVersions.class, "/").execute();
    }
}
