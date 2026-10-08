package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.VpnService;
import org.openstack4j.model.network.options.VpnServiceOptions;

/** VPN services ({@code /v2.0/vpn/vpnservices}). */
public interface VpnServiceService extends RestService {

    /** @return the VPN services */
    List<? extends VpnService> list();

    /** @param filters query parameters such as {@code name}, {@code router_id}, {@code status}, {@code project_id} */
    List<? extends VpnService> list(Map<String, String> filters);

    /** @return the VPN service, or {@code null} when it does not exist */
    VpnService get(String id);

    VpnService create(VpnServiceOptions options);

    /** Changes only the fields set in {@code options}. */
    VpnService update(String id, VpnServiceOptions options);

    ActionResponse delete(String id);
}
