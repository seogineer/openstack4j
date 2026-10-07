package org.openstack4j.openstack.baremetal.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.baremetal.NodeService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Node;
import org.openstack4j.model.baremetal.options.NodeCreate;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.baremetal.domain.IronicNode;
import org.openstack4j.openstack.baremetal.domain.IronicNode.Nodes;

public class NodeServiceImpl extends BaseBaremetalServices implements NodeService {

    @Override
    public List<? extends Node> list() {
        return list(null);
    }

    @Override
    public List<? extends Node> list(Map<String, String> filters) {
        return listOf(Nodes.class, "/nodes", filters);
    }

    @Override
    public List<? extends Node> listDetail() {
        return listDetail(null);
    }

    @Override
    public List<? extends Node> listDetail(Map<String, String> filters) {
        return listOf(Nodes.class, "/nodes/detail", filters);
    }

    @Override
    public Node get(String nodeIdent) {
        return show(IronicNode.class, "/nodes/" + id(nodeIdent));
    }

    @Override
    public Node create(NodeCreate node) {
        return create(IronicNode.class, "/nodes", node);
    }

    @Override
    public Node update(String nodeIdent, List<BaremetalPatch> patches) {
        return patchWith(IronicNode.class, "/nodes/" + id(nodeIdent), patches);
    }

    @Override
    public ActionResponse delete(String nodeIdent) {
        return remove("/nodes/" + id(nodeIdent));
    }
}
