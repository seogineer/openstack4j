package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.IpsecSiteConnectionService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.IpsecSiteConnection;
import org.openstack4j.model.network.options.IpsecSiteConnectionOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronIpsecSiteConnection;
import org.openstack4j.openstack.networking.domain.ext.NeutronIpsecSiteConnection.NeutronIpsecSiteConnectionList;

public class IpsecSiteConnectionServiceImpl extends BaseNeutronExtService implements IpsecSiteConnectionService {

    private static final String PATH = "/vpn/ipsec-site-connections";
    private static final String ROOT = "ipsec_site_connection";

    @Override
    public List<? extends IpsecSiteConnection> list() {
        return list(null);
    }

    @Override
    public List<? extends IpsecSiteConnection> list(Map<String, String> filters) {
        return listOf(NeutronIpsecSiteConnectionList.class, PATH, filters);
    }

    @Override
    public IpsecSiteConnection get(String id) {
        return show(NeutronIpsecSiteConnection.class, PATH + "/" + id(id));
    }

    @Override
    public IpsecSiteConnection create(IpsecSiteConnectionOptions options) {
        return create(NeutronIpsecSiteConnection.class, PATH, ROOT, options);
    }

    @Override
    public IpsecSiteConnection update(String id, IpsecSiteConnectionOptions options) {
        return update(NeutronIpsecSiteConnection.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
