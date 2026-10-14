package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.manila.ext.ShareNetworkInfo;

/** Share networks at the session microversion (floor 2.7); the legacy shareNetworks() stays at 2.6. */
public interface ShareNetworkExtService extends RestService {

    /**
     * {@code GET /share-networks}.
     *
     * @param filters name, name~, description~ (2.36), project_id, all_tenants, security_service_id, neutron_net_id,
     *                neutron_subnet_id, created_since, created_before, limit, offset ..., may be null
     */
    List<? extends ShareNetworkInfo> list(Map<String, String> filters);

    /**
     * {@code GET /share-networks/detail}: all fields. {@code share_network_subnets} need a session microversion of 2.51
     * or higher, {@code status} and {@code security_service_update_support} 2.63, {@code network_allocation_update_support} 2.70.
     *
     * @param filters see {@link #list(Map)}, may be null
     */
    List<? extends ShareNetworkInfo> listDetails(Map<String, String> filters);

    /** @return the share network, or {@code null} when it does not exist */
    ShareNetworkInfo get(String shareNetworkId);
}
