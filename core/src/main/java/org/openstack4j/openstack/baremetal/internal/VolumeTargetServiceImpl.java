package org.openstack4j.openstack.baremetal.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.baremetal.VolumeTargetService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.VolumeTarget;
import org.openstack4j.model.baremetal.options.VolumeTargetCreate;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.baremetal.domain.IronicVolumeTarget;
import org.openstack4j.openstack.baremetal.domain.IronicVolumeTarget.IronicVolumeTargetList;

public class VolumeTargetServiceImpl extends BaseBaremetalServices implements VolumeTargetService {

    @Override
    public List<? extends VolumeTarget> list() {
        return list(null);
    }

    @Override
    public List<? extends VolumeTarget> list(Map<String, String> filters) {
        return listOf(IronicVolumeTargetList.class, "/volume/targets", withDetail(filters));
    }

    @Override
    public VolumeTarget get(String ident) {
        return show(IronicVolumeTarget.class, "/volume/targets/" + id(ident));
    }

    @Override
    public VolumeTarget create(VolumeTargetCreate create) {
        return create(IronicVolumeTarget.class, "/volume/targets", create);
    }

    @Override
    public VolumeTarget update(String ident, List<BaremetalPatch> patches) {
        return patchWith(IronicVolumeTarget.class, "/volume/targets/" + id(ident), patches);
    }

    @Override
    public List<? extends VolumeTarget> listByNode(String nodeIdent) {
        return listOf(IronicVolumeTargetList.class, "/nodes/" + id(nodeIdent) + "/volume/targets", Map.of("detail", "true"));
    }

    @Override
    public ActionResponse delete(String ident) {
        return remove("/volume/targets/" + id(ident));
    }
}
