package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.NdpProxy;
import org.openstack4j.model.network.options.NdpProxyOptions;

/**
 * Router NDP proxies ({@code /v2.0/ndp_proxies}, l3-ndp-proxy).
 */
public interface NdpProxyService extends RestService {

    /**
     * Lists NDP proxies.
     *
     * @return the result
     */
    List<? extends NdpProxy> list();

    /**
     * Lists NDP proxies, optionally filtered.
     *
     * @param filters the filters
     * @return the result
     */
    List<? extends NdpProxy> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    NdpProxy get(String id);

    /**
     * Publishes an IPv6 address of an internal port through the router.
     *
     * @param options the options
     * @return the result
     */
    NdpProxy create(NdpProxyOptions options);

    /**
     * Updates the name or description of an NDP proxy.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    NdpProxy update(String id, NdpProxyOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
