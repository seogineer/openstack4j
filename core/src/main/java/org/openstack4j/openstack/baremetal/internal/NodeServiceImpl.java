package org.openstack4j.openstack.baremetal.internal;

import java.util.List;

import org.openstack4j.api.baremetal.NodeService;
import org.openstack4j.model.baremetal.Node;
import org.openstack4j.openstack.baremetal.domain.IronicNode.Nodes;

public class NodeServiceImpl extends BaseBaremetalServices implements NodeService {

    @Override
    public List<? extends Node> list() {
        return get(Nodes.class, "/nodes").execute().getList();
    }
}
