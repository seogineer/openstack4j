package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.Bgpvpn;
import org.openstack4j.model.network.options.BgpvpnOptions;

/** BGP VPNs ({@code /v2.0/bgpvpn/bgpvpns}). */
public interface BgpvpnService extends RestService {

    /** @return the BGP VPNs */
    List<? extends Bgpvpn> list();

    /** @param filters query parameters such as {@code name}, {@code type}, {@code networks}, {@code routers}, {@code ports} */
    List<? extends Bgpvpn> list(Map<String, String> filters);

    /** @return the BGP VPN, or {@code null} when it does not exist */
    Bgpvpn get(String id);

    Bgpvpn create(BgpvpnOptions options);

    /** Changes only the fields set in {@code options}. */
    Bgpvpn update(String id, BgpvpnOptions options);

    ActionResponse delete(String id);
}
