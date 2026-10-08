package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.IpsecSiteConnection;
import org.openstack4j.model.network.options.IpsecSiteConnectionOptions;

/** IPsec site connections ({@code /v2.0/vpn/ipsec-site-connections}). */
public interface IpsecSiteConnectionService extends RestService {

    /** @return the IPsec site connections */
    List<? extends IpsecSiteConnection> list();

    /** @param filters query parameters such as {@code name}, {@code vpnservice_id}, {@code status}, {@code peer_address} */
    List<? extends IpsecSiteConnection> list(Map<String, String> filters);

    /** @return the IPsec site connection, or {@code null} when it does not exist */
    IpsecSiteConnection get(String id);

    IpsecSiteConnection create(IpsecSiteConnectionOptions options);

    /** Changes only the fields set in {@code options}. */
    IpsecSiteConnection update(String id, IpsecSiteConnectionOptions options);

    ActionResponse delete(String id);
}
