package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.BgpvpnNetworkAssociation;
import org.openstack4j.model.network.ext.BgpvpnPortAssociation;
import org.openstack4j.model.network.ext.BgpvpnRouterAssociation;

/** Network, router and port associations of a BGP VPN ({@code /v2.0/bgpvpn/bgpvpns/{id}/...}). */
public interface BgpvpnAssociationService extends RestService {

    /** @return the network associations of a BGP VPN; a missing BGP VPN raises */
    List<? extends BgpvpnNetworkAssociation> listNetworkAssociations(String bgpvpnId);

    /** @return the association, or {@code null} when it does not exist */
    BgpvpnNetworkAssociation getNetworkAssociation(String bgpvpnId, String associationId);

    BgpvpnNetworkAssociation associateNetwork(String bgpvpnId, String networkId);

    ActionResponse deleteNetworkAssociation(String bgpvpnId, String associationId);

    /** @return the router associations of a BGP VPN; a missing BGP VPN raises */
    List<? extends BgpvpnRouterAssociation> listRouterAssociations(String bgpvpnId);

    /** @return the association, or {@code null} when it does not exist */
    BgpvpnRouterAssociation getRouterAssociation(String bgpvpnId, String associationId);

    /** @param advertiseExtraRoutes whether the router's extra routes are advertised; {@code null} keeps the default */
    BgpvpnRouterAssociation associateRouter(String bgpvpnId, String routerId, Boolean advertiseExtraRoutes);

    BgpvpnRouterAssociation updateRouterAssociation(String bgpvpnId, String associationId, boolean advertiseExtraRoutes);

    ActionResponse deleteRouterAssociation(String bgpvpnId, String associationId);

    /** @return the port associations of a BGP VPN; a missing BGP VPN raises */
    List<? extends BgpvpnPortAssociation> listPortAssociations(String bgpvpnId);

    /** @return the association, or {@code null} when it does not exist */
    BgpvpnPortAssociation getPortAssociation(String bgpvpnId, String associationId);

    /**
     * @param fields {@code port_id} and optional {@code routes} (e.g. {@code [{"type": "prefix", "prefix": "20.1.0.0/16"}]}),
     *               {@code advertise_fixed_ips}
     */
    BgpvpnPortAssociation associatePort(String bgpvpnId, Map<String, ?> fields);

    /** @param fields {@code routes} and/or {@code advertise_fixed_ips} */
    BgpvpnPortAssociation updatePortAssociation(String bgpvpnId, String associationId, Map<String, ?> fields);

    ActionResponse deletePortAssociation(String bgpvpnId, String associationId);
}
