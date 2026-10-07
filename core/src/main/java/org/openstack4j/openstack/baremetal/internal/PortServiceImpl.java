package org.openstack4j.openstack.baremetal.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.baremetal.PortService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Port;
import org.openstack4j.model.baremetal.options.PortCreate;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.baremetal.domain.IronicPort;
import org.openstack4j.openstack.baremetal.domain.IronicPort.IronicPorts;

public class PortServiceImpl extends BaseBaremetalServices implements PortService {

    @Override
    public List<? extends Port> list() {
        return list(null);
    }

    @Override
    public List<? extends Port> list(Map<String, String> filters) {
        return listOf(IronicPorts.class, "/ports", filters);
    }

    @Override
    public List<? extends Port> listDetail() {
        return listDetail(null);
    }

    @Override
    public List<? extends Port> listDetail(Map<String, String> filters) {
        return listOf(IronicPorts.class, "/ports/detail", filters);
    }

    @Override
    public List<? extends Port> listByNode(String nodeIdent) {
        return listOf(IronicPorts.class, "/nodes/" + id(nodeIdent) + "/ports/detail", null);
    }

    @Override
    public List<? extends Port> listByPortgroup(String portgroupIdent) {
        return listOf(IronicPorts.class, "/portgroups/" + id(portgroupIdent) + "/ports/detail", null);
    }
    @Override
    public Port get(String ident) {
        return show(IronicPort.class, "/ports/" + id(ident));
    }

    @Override
    public Port create(PortCreate create) {
        return create(IronicPort.class, "/ports", create);
    }

    @Override
    public Port update(String ident, List<BaremetalPatch> patches) {
        return patchWith(IronicPort.class, "/ports/" + id(ident), patches);
    }

    @Override
    public ActionResponse delete(String ident) {
        return remove("/ports/" + id(ident));
    }
}
