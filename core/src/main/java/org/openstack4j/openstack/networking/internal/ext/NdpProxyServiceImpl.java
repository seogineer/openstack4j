package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.NdpProxyService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.NdpProxy;
import org.openstack4j.model.network.options.NdpProxyOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronNdpProxy;
import org.openstack4j.openstack.networking.domain.ext.NeutronNdpProxy.NdpProxies;

public class NdpProxyServiceImpl extends BaseNeutronExtService implements NdpProxyService {

    private static final String PATH = "/ndp_proxies";
    private static final String ROOT = "ndp_proxy";

    @Override public List<? extends NdpProxy> list() { return listOf(NdpProxies.class, PATH, null); }
    @Override public List<? extends NdpProxy> list(Map<String, String> filters) { return listOf(NdpProxies.class, PATH, filters); }
    @Override public NdpProxy get(String id) { return show(NeutronNdpProxy.class, PATH + "/" + id(id)); }
    @Override public NdpProxy create(NdpProxyOptions options) { return create(NeutronNdpProxy.class, PATH, ROOT, options); }
    @Override public NdpProxy update(String id, NdpProxyOptions options) { return update(NeutronNdpProxy.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }
}
