package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.BgpPeer;
import org.openstack4j.model.network.options.BgpPeerOptions;

/** BGP peers ({@code /v2.0/bgp-peers}). */
public interface BgpPeerService extends RestService {

    /** @return the BGP peers */
    List<? extends BgpPeer> list();

    /** @param filters query parameters such as {@code name}, {@code project_id} */
    List<? extends BgpPeer> list(Map<String, String> filters);

    /** @return the BGP peer, or {@code null} when it does not exist */
    BgpPeer get(String id);

    BgpPeer create(BgpPeerOptions options);

    /** Changes only the fields set in {@code options}. */
    BgpPeer update(String id, BgpPeerOptions options);

    ActionResponse delete(String id);
}
