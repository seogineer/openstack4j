package org.openstack4j.openstack.baremetal.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.baremetal.ChassisService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Chassis;
import org.openstack4j.model.baremetal.Node;
import org.openstack4j.model.baremetal.options.ChassisCreate;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.baremetal.domain.IronicChassis;
import org.openstack4j.openstack.baremetal.domain.IronicChassis.IronicChassisList;
import org.openstack4j.openstack.baremetal.domain.IronicNode.Nodes;

public class ChassisServiceImpl extends BaseBaremetalServices implements ChassisService {

    @Override
    public List<? extends Chassis> list() {
        return list(null);
    }

    @Override
    public List<? extends Chassis> list(Map<String, String> filters) {
        return listOf(IronicChassisList.class, "/chassis", filters);
    }

    @Override
    public List<? extends Chassis> listDetail() {
        return listOf(IronicChassisList.class, "/chassis/detail", null);
    }

    @Override
    public List<? extends Node> listNodes(String chassisId) {
        return listOf(Nodes.class, "/chassis/" + id(chassisId) + "/nodes/detail", null);
    }

    @Override
    public Chassis get(String chassisId) {
        return show(IronicChassis.class, "/chassis/" + id(chassisId));
    }

    @Override
    public Chassis create(ChassisCreate create) {
        return create(IronicChassis.class, "/chassis", create);
    }

    @Override
    public Chassis update(String chassisId, List<BaremetalPatch> patches) {
        return patchWith(IronicChassis.class, "/chassis/" + id(chassisId), patches);
    }

    @Override
    public ActionResponse delete(String chassisId) {
        return remove("/chassis/" + id(chassisId));
    }
}
