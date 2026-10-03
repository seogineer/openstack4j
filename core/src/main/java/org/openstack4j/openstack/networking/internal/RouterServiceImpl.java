package org.openstack4j.openstack.networking.internal;

import org.openstack4j.openstack.networking.internal.ext.NeutronExecution;
import org.openstack4j.openstack.networking.domain.ext.NeutronConntrackHelper.ConntrackHelpers;
import org.openstack4j.openstack.networking.domain.ext.NeutronConntrackHelper;
import org.openstack4j.openstack.networking.domain.NeutronAgent.Agents;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.model.network.options.ConntrackHelperOptions;
import org.openstack4j.model.network.ext.ConntrackHelper;
import org.openstack4j.model.network.Agent;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openstack4j.api.networking.RouterService;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.AttachInterfaceType;
import org.openstack4j.model.network.HostRoute;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.builder.RouterBuilder;
import org.openstack4j.openstack.networking.domain.AddRouterInterfaceAction;
import org.openstack4j.openstack.networking.domain.NeutronRouter;
import org.openstack4j.openstack.networking.domain.NeutronRouter.Routers;
import org.openstack4j.openstack.networking.domain.NeutronRouterInterface;

/**
 * RouterService implementation that provides Neutron Router based Service Operations.
 *
 * @author Jeremy Unruh
 */
public class RouterServiceImpl extends BaseNetworkingServices implements RouterService {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Router> list() {
        return get(Routers.class, uri("/routers")).execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Router get(String routerId) {
        Objects.requireNonNull(routerId);
        return get(NeutronRouter.class, uri("/routers/%s", routerId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse delete(String routerId) {
        Objects.requireNonNull(routerId);
        return deleteWithResponse(uri("/routers/%s", routerId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Router create(String name, boolean adminStateUp) {
        Objects.requireNonNull(name);
        return post(NeutronRouter.class, uri("/routers")).entity(NeutronRouter.builder().name(name).adminStateUp(adminStateUp).build()).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Router create(Router router) {
        Objects.requireNonNull(router);
        return post(NeutronRouter.class, uri("/routers")).entity(router).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Router update(Router router) {
        Objects.requireNonNull(router);
        Objects.requireNonNull(router.getId());

        RouterBuilder rb = NeutronRouter.builder().name(router.getName()).adminStateUp(router.isAdminStateUp()).externalGateway(router.getExternalGatewayInfo());
        List<? extends HostRoute> routes = router.getRoutes();

        if (routes != null && !routes.isEmpty()) {
            for (HostRoute route : routes) {
                rb.route(route.getDestination(), route.getNexthop());
            }
        } else {
            rb.noRoutes();
        }

        return put(NeutronRouter.class, uri("/routers/%s", router.getId()))
                .entity(rb.build())
                .execute();
    }

    @Override
    public Router toggleAdminStateUp(String routerId, boolean adminStateUp) {
        Objects.requireNonNull(routerId);
        return put(NeutronRouter.class, uri("/routers/%s", routerId)).entity(NeutronRouter.builder().adminStateUp(adminStateUp).build()).execute();
    }

    @Override
    public RouterInterface attachInterface(String routerId, AttachInterfaceType type, String portOrSubnetId) {
        Objects.requireNonNull(routerId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(portOrSubnetId);
        return put(NeutronRouterInterface.class, uri("/routers/%s/add_router_interface", routerId))
                .entity(AddRouterInterfaceAction.create(type, portOrSubnetId))
                .execute();
    }

    @Override
    public RouterInterface detachInterface(String routerId, String subnetId, String portId) {
        Objects.requireNonNull(routerId);
        if (subnetId == null && portId == null) {
            throw new IllegalStateException("Either a Subnet or Port identifier must be set");
        }
        return put(NeutronRouterInterface.class, uri("/routers/%s/remove_router_interface", routerId))
                .entity(new NeutronRouterInterface(subnetId, portId))
                .execute(ExecutionOptions.<NeutronRouterInterface>create(PropagateOnStatus.on(404)));
    }


    private Router routerAction(String routerId, String action, String key, Object value) {
        return put(NeutronRouter.class, uri("/routers/%s/%s", Objects.requireNonNull(routerId), action))
                .entity(JsonBody.of("router", Collections.singletonMap(key, Objects.requireNonNull(value)))).execute(NeutronExecution.propagate404());
    }

    private static List<Map<String, String>> routes(List<? extends HostRoute> routes) {
        return routes.stream().map(r -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("destination", r.getDestination());
            m.put("nexthop", r.getNexthop());
            return m;
        }).collect(Collectors.toList());
    }

    @Override public Router addExtraRoutes(String routerId, List<? extends HostRoute> r) { return routerAction(routerId, "add_extraroutes", "routes", routes(r)); }
    @Override public Router removeExtraRoutes(String routerId, List<? extends HostRoute> r) { return routerAction(routerId, "remove_extraroutes", "routes", routes(r)); }
    @Override public Router addExternalGateways(String routerId, List<Map<String, Object>> g) { return routerAction(routerId, "add_external_gateways", "external_gateways", g); }
    @Override public Router updateExternalGateways(String routerId, List<Map<String, Object>> g) { return routerAction(routerId, "update_external_gateways", "external_gateways", g); }
    @Override public Router removeExternalGateways(String routerId, List<Map<String, Object>> g) { return routerAction(routerId, "remove_external_gateways", "external_gateways", g); }

    private static String helpers(String routerId) {
        return "/routers/" + Objects.requireNonNull(routerId) + "/conntrack_helpers";
    }

    @Override public List<? extends ConntrackHelper> listConntrackHelpers(String routerId) { return get(ConntrackHelpers.class, helpers(routerId)).execute(NeutronExecution.propagate404()).getList(); }
    @Override public ConntrackHelper getConntrackHelper(String routerId, String id) { return get(NeutronConntrackHelper.class, helpers(routerId) + "/" + Objects.requireNonNull(id)).execute(); }

    @Override
    public ConntrackHelper createConntrackHelper(String routerId, ConntrackHelperOptions options) {
        return post(NeutronConntrackHelper.class, helpers(routerId)).entity(JsonBody.of("conntrack_helper", options.toMap())).execute(NeutronExecution.propagate404());
    }

    @Override
    public ConntrackHelper updateConntrackHelper(String routerId, String id, ConntrackHelperOptions options) {
        return put(NeutronConntrackHelper.class, helpers(routerId) + "/" + Objects.requireNonNull(id)).entity(JsonBody.of("conntrack_helper", options.toMap())).execute(NeutronExecution.propagate404());
    }

    @Override public ActionResponse deleteConntrackHelper(String routerId, String id) { return deleteWithResponse(helpers(routerId) + "/" + Objects.requireNonNull(id)).execute(); }
    @Override public List<? extends Agent> listL3Agents(String routerId) { return get(Agents.class, uri("/routers/%s/l3-agents", Objects.requireNonNull(routerId))).execute(NeutronExecution.propagate404()).getList(); }
}
