package org.openstack4j.openstack.ironic.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.ironic.PortgroupService;
import org.openstack4j.model.ironic.BaremetalPatch;
import org.openstack4j.model.ironic.Portgroup;
import org.openstack4j.model.ironic.options.PortgroupCreate;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.ironic.domain.IronicPortgroup;
import org.openstack4j.openstack.ironic.domain.IronicPortgroup.IronicPortgroups;

public class PortgroupServiceImpl extends BaseIronicServices implements PortgroupService {

    @Override
    public List<? extends Portgroup> list() {
        return list(null);
    }

    @Override
    public List<? extends Portgroup> list(Map<String, String> filters) {
        return listOf(IronicPortgroups.class, "/portgroups", filters);
    }

    @Override
    public List<? extends Portgroup> listDetail() {
        return listDetail(null);
    }

    @Override
    public List<? extends Portgroup> listDetail(Map<String, String> filters) {
        return listOf(IronicPortgroups.class, "/portgroups/detail", filters);
    }

    @Override
    public List<? extends Portgroup> listByNode(String nodeIdent) {
        return listOf(IronicPortgroups.class, "/nodes/" + id(nodeIdent) + "/portgroups/detail", null);
    }

    @Override
    public Portgroup get(String ident) {
        return show(IronicPortgroup.class, "/portgroups/" + id(ident));
    }

    @Override
    public Portgroup create(PortgroupCreate create) {
        return create(IronicPortgroup.class, "/portgroups", create);
    }

    @Override
    public Portgroup update(String ident, List<BaremetalPatch> patches) {
        return patchWith(IronicPortgroup.class, "/portgroups/" + id(ident), patches);
    }

    @Override
    public ActionResponse delete(String ident) {
        return remove("/portgroups/" + id(ident));
    }
}
