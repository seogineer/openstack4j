package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.VpnServiceService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.VpnService;
import org.openstack4j.model.network.options.VpnServiceOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronVpnService;
import org.openstack4j.openstack.networking.domain.ext.NeutronVpnService.NeutronVpnServiceList;

public class VpnServiceServiceImpl extends BaseNeutronExtService implements VpnServiceService {

    private static final String PATH = "/vpn/vpnservices";
    private static final String ROOT = "vpnservice";

    @Override
    public List<? extends VpnService> list() {
        return list(null);
    }

    @Override
    public List<? extends VpnService> list(Map<String, String> filters) {
        return listOf(NeutronVpnServiceList.class, PATH, filters);
    }

    @Override
    public VpnService get(String id) {
        return show(NeutronVpnService.class, PATH + "/" + id(id));
    }

    @Override
    public VpnService create(VpnServiceOptions options) {
        return create(NeutronVpnService.class, PATH, ROOT, options);
    }

    @Override
    public VpnService update(String id, VpnServiceOptions options) {
        return update(NeutronVpnService.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
