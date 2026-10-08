package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.VpnEndpointGroup;
import org.openstack4j.model.network.options.VpnEndpointGroupOptions;

/** VPN endpoint groups ({@code /v2.0/vpn/endpoint-groups}). */
public interface VpnEndpointGroupService extends RestService {

    /** @return the VPN endpoint groups */
    List<? extends VpnEndpointGroup> list();

    /** @param filters query parameters such as {@code name}, {@code type} */
    List<? extends VpnEndpointGroup> list(Map<String, String> filters);

    /** @return the VPN endpoint group, or {@code null} when it does not exist */
    VpnEndpointGroup get(String id);

    VpnEndpointGroup create(VpnEndpointGroupOptions options);

    /** Changes only the fields set in {@code options}. */
    VpnEndpointGroup update(String id, VpnEndpointGroupOptions options);

    ActionResponse delete(String id);
}
