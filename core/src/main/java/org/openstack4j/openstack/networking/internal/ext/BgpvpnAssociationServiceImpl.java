package org.openstack4j.openstack.networking.internal.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.networking.ext.BgpvpnAssociationService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.BgpvpnNetworkAssociation;
import org.openstack4j.model.network.ext.BgpvpnPortAssociation;
import org.openstack4j.model.network.ext.BgpvpnRouterAssociation;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.networking.domain.ext.NeutronBgpvpnNetworkAssociation;
import org.openstack4j.openstack.networking.domain.ext.NeutronBgpvpnNetworkAssociation.NeutronBgpvpnNetworkAssociationList;
import org.openstack4j.openstack.networking.domain.ext.NeutronBgpvpnPortAssociation;
import org.openstack4j.openstack.networking.domain.ext.NeutronBgpvpnPortAssociation.NeutronBgpvpnPortAssociationList;
import org.openstack4j.openstack.networking.domain.ext.NeutronBgpvpnRouterAssociation;
import org.openstack4j.openstack.networking.domain.ext.NeutronBgpvpnRouterAssociation.NeutronBgpvpnRouterAssociationList;

public class BgpvpnAssociationServiceImpl extends BaseNeutronExtService implements BgpvpnAssociationService {

    private static String path(String bgpvpnId, String kind) {
        return "/bgpvpn/bgpvpns/" + id(bgpvpnId) + "/" + kind;
    }

    private <E> E postTo(Class<E> type, String path, String root, Map<String, ?> fields) {
        return post(type, path).entity(JsonBody.of(root, fields)).execute(NeutronExecution.propagate404());
    }

    private <E> E putTo(Class<E> type, String path, String root, Map<String, ?> fields) {
        return put(type, path).entity(JsonBody.of(root, fields)).execute(NeutronExecution.propagate404());
    }

    @Override
    public List<? extends BgpvpnNetworkAssociation> listNetworkAssociations(String bgpvpnId) {
        return listOf(NeutronBgpvpnNetworkAssociationList.class, path(bgpvpnId, "network_associations"), null);
    }

    @Override
    public BgpvpnNetworkAssociation getNetworkAssociation(String bgpvpnId, String associationId) {
        return show(NeutronBgpvpnNetworkAssociation.class, path(bgpvpnId, "network_associations") + "/" + id(associationId));
    }

    @Override
    public BgpvpnNetworkAssociation associateNetwork(String bgpvpnId, String networkId) {
        return postTo(NeutronBgpvpnNetworkAssociation.class, path(bgpvpnId, "network_associations"), "network_association", Map.of("network_id", id(networkId)));
    }

    @Override
    public ActionResponse deleteNetworkAssociation(String bgpvpnId, String associationId) {
        return remove(path(bgpvpnId, "network_associations") + "/" + id(associationId));
    }

    @Override
    public List<? extends BgpvpnRouterAssociation> listRouterAssociations(String bgpvpnId) {
        return listOf(NeutronBgpvpnRouterAssociationList.class, path(bgpvpnId, "router_associations"), null);
    }

    @Override
    public BgpvpnRouterAssociation getRouterAssociation(String bgpvpnId, String associationId) {
        return show(NeutronBgpvpnRouterAssociation.class, path(bgpvpnId, "router_associations") + "/" + id(associationId));
    }

    @Override
    public BgpvpnRouterAssociation associateRouter(String bgpvpnId, String routerId, Boolean advertiseExtraRoutes) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("router_id", id(routerId));
        if (advertiseExtraRoutes != null)
            fields.put("advertise_extra_routes", advertiseExtraRoutes);
        return postTo(NeutronBgpvpnRouterAssociation.class, path(bgpvpnId, "router_associations"), "router_association", fields);
    }

    @Override
    public BgpvpnRouterAssociation updateRouterAssociation(String bgpvpnId, String associationId, boolean advertiseExtraRoutes) {
        return putTo(NeutronBgpvpnRouterAssociation.class, path(bgpvpnId, "router_associations") + "/" + id(associationId), "router_association",
                Map.of("advertise_extra_routes", advertiseExtraRoutes));
    }

    @Override
    public ActionResponse deleteRouterAssociation(String bgpvpnId, String associationId) {
        return remove(path(bgpvpnId, "router_associations") + "/" + id(associationId));
    }

    @Override
    public List<? extends BgpvpnPortAssociation> listPortAssociations(String bgpvpnId) {
        return listOf(NeutronBgpvpnPortAssociationList.class, path(bgpvpnId, "port_associations"), null);
    }

    @Override
    public BgpvpnPortAssociation getPortAssociation(String bgpvpnId, String associationId) {
        return show(NeutronBgpvpnPortAssociation.class, path(bgpvpnId, "port_associations") + "/" + id(associationId));
    }

    @Override
    public BgpvpnPortAssociation associatePort(String bgpvpnId, Map<String, ?> fields) {
        return postTo(NeutronBgpvpnPortAssociation.class, path(bgpvpnId, "port_associations"), "port_association", Objects.requireNonNull(fields, "fields"));
    }

    @Override
    public BgpvpnPortAssociation updatePortAssociation(String bgpvpnId, String associationId, Map<String, ?> fields) {
        return putTo(NeutronBgpvpnPortAssociation.class, path(bgpvpnId, "port_associations") + "/" + id(associationId), "port_association",
                Objects.requireNonNull(fields, "fields"));
    }

    @Override
    public ActionResponse deletePortAssociation(String bgpvpnId, String associationId) {
        return remove(path(bgpvpnId, "port_associations") + "/" + id(associationId));
    }
}
