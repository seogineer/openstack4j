package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.BgpPeerService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.BgpPeer;
import org.openstack4j.model.network.options.BgpPeerOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronBgpPeer;
import org.openstack4j.openstack.networking.domain.ext.NeutronBgpPeer.NeutronBgpPeerList;

public class BgpPeerServiceImpl extends BaseNeutronExtService implements BgpPeerService {

    private static final String PATH = "/bgp-peers";
    private static final String ROOT = "bgp_peer";

    @Override
    public List<? extends BgpPeer> list() {
        return list(null);
    }

    @Override
    public List<? extends BgpPeer> list(Map<String, String> filters) {
        return listOf(NeutronBgpPeerList.class, PATH, filters);
    }

    @Override
    public BgpPeer get(String id) {
        return show(NeutronBgpPeer.class, PATH + "/" + id(id));
    }

    @Override
    public BgpPeer create(BgpPeerOptions options) {
        return create(NeutronBgpPeer.class, PATH, ROOT, options);
    }

    @Override
    public BgpPeer update(String id, BgpPeerOptions options) {
        return update(NeutronBgpPeer.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
