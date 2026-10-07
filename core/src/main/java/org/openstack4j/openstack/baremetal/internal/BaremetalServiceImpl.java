package org.openstack4j.openstack.baremetal.internal;

import org.openstack4j.api.Apis;
import org.openstack4j.api.baremetal.BaremetalMicroVersionService;
import org.openstack4j.api.baremetal.BaremetalService;
import org.openstack4j.api.baremetal.NodeService;

public class BaremetalServiceImpl implements BaremetalService {

    @Override public BaremetalMicroVersionService microVersions() { return Apis.get(BaremetalMicroVersionService.class); }
    @Override public NodeService nodes() { return Apis.get(NodeService.class); }
}
