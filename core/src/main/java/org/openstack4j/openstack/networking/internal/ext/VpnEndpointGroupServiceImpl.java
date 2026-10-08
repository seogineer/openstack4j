package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.VpnEndpointGroupService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.VpnEndpointGroup;
import org.openstack4j.model.network.options.VpnEndpointGroupOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronVpnEndpointGroup;
import org.openstack4j.openstack.networking.domain.ext.NeutronVpnEndpointGroup.NeutronVpnEndpointGroupList;

public class VpnEndpointGroupServiceImpl extends BaseNeutronExtService implements VpnEndpointGroupService {

    private static final String PATH = "/vpn/endpoint-groups";
    private static final String ROOT = "endpoint_group";

    @Override
    public List<? extends VpnEndpointGroup> list() {
        return list(null);
    }

    @Override
    public List<? extends VpnEndpointGroup> list(Map<String, String> filters) {
        return listOf(NeutronVpnEndpointGroupList.class, PATH, filters);
    }

    @Override
    public VpnEndpointGroup get(String id) {
        return show(NeutronVpnEndpointGroup.class, PATH + "/" + id(id));
    }

    @Override
    public VpnEndpointGroup create(VpnEndpointGroupOptions options) {
        return create(NeutronVpnEndpointGroup.class, PATH, ROOT, options);
    }

    @Override
    public VpnEndpointGroup update(String id, VpnEndpointGroupOptions options) {
        return update(NeutronVpnEndpointGroup.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
