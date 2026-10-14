package org.openstack4j.openstack.ironic.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.ironic.AllocationService;
import org.openstack4j.model.ironic.BaremetalPatch;
import org.openstack4j.model.ironic.Allocation;
import org.openstack4j.model.ironic.options.AllocationCreate;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.ironic.domain.IronicAllocation;
import org.openstack4j.openstack.ironic.domain.IronicAllocation.IronicAllocationList;

public class AllocationServiceImpl extends BaseBaremetalServices implements AllocationService {

    @Override
    public List<? extends Allocation> list() {
        return list(null);
    }

    @Override
    public List<? extends Allocation> list(Map<String, String> filters) {
        return listOf(IronicAllocationList.class, "/allocations", filters);
    }

    @Override
    public Allocation get(String ident) {
        return show(IronicAllocation.class, "/allocations/" + id(ident));
    }

    @Override
    public Allocation create(AllocationCreate create) {
        return create(IronicAllocation.class, "/allocations", create);
    }

    @Override
    public Allocation update(String ident, List<BaremetalPatch> patches) {
        return patchWith(IronicAllocation.class, "/allocations/" + id(ident), patches);
    }

    @Override
    public Allocation getForNode(String nodeIdent) {
        return show(IronicAllocation.class, "/nodes/" + id(nodeIdent) + "/allocation");
    }

    @Override
    public ActionResponse deleteForNode(String nodeIdent) {
        return remove("/nodes/" + id(nodeIdent) + "/allocation");
    }

    @Override
    public ActionResponse delete(String ident) {
        return remove("/allocations/" + id(ident));
    }
}
