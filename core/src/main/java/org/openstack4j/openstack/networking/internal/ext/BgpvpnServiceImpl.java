package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.BgpvpnService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.Bgpvpn;
import org.openstack4j.model.network.options.BgpvpnOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronBgpvpn;
import org.openstack4j.openstack.networking.domain.ext.NeutronBgpvpn.NeutronBgpvpnList;

public class BgpvpnServiceImpl extends BaseNeutronExtService implements BgpvpnService {

    private static final String PATH = "/bgpvpn/bgpvpns";
    private static final String ROOT = "bgpvpn";

    @Override
    public List<? extends Bgpvpn> list() {
        return list(null);
    }

    @Override
    public List<? extends Bgpvpn> list(Map<String, String> filters) {
        return listOf(NeutronBgpvpnList.class, PATH, filters);
    }

    @Override
    public Bgpvpn get(String id) {
        return show(NeutronBgpvpn.class, PATH + "/" + id(id));
    }

    @Override
    public Bgpvpn create(BgpvpnOptions options) {
        return create(NeutronBgpvpn.class, PATH, ROOT, options);
    }

    @Override
    public Bgpvpn update(String id, BgpvpnOptions options) {
        return update(NeutronBgpvpn.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
