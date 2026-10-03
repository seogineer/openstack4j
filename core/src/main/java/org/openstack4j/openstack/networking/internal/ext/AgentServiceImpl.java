package org.openstack4j.openstack.networking.internal.ext;

import org.openstack4j.openstack.networking.internal.ext.NeutronExecution;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.networking.domain.NeutronRouter.Routers;
import org.openstack4j.openstack.networking.domain.NeutronNetwork.Networks;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Network;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openstack4j.api.networking.ext.AgentService;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.Agent;
import org.openstack4j.openstack.networking.domain.NeutronAgent;
import org.openstack4j.openstack.networking.domain.NeutronAgent.Agents;
import org.openstack4j.openstack.networking.internal.BaseNetworkingServices;

/**
 * Networking (Neutron) Agent Extension API
 *
 * @author Yin Zhang
 */
public class AgentServiceImpl extends BaseNetworkingServices implements AgentService {

    @Override
    public List<? extends Agent> list() {
        return get(Agents.class, uri("/agents")).execute().getList();
    }

    @Override
    public Agent getAgent(String agentId) {
        Objects.requireNonNull(agentId);
        return get(NeutronAgent.class, uri("/agents/%s", agentId)).execute();
    }

    @Override
    public Agent setAdminStateUp(String agentId, boolean state) {
        Objects.requireNonNull(agentId);
        String json = String.format("{\"%s\": { \"%s\": \"%b\"}}", "agent", "admin_state_up", state);
        return put(NeutronAgent.class, uri("/agents/%s", agentId)).json(json).execute(
                ExecutionOptions.<NeutronAgent>create(PropagateOnStatus.on(404)));
    }

    @Override
    public ActionResponse attachNetworkToDhcpAgent(String agentId, String networkId) {
        Objects.requireNonNull(agentId);
        String json = String.format("{\"%s\": \"%s\"}", "network_id", networkId);
        return postWithResponse(uri("/agents/%s/dhcp-networks", agentId)).json(json).execute(ExecutionOptions.<ActionResponse>create(PropagateOnStatus.on(404)));
    }

    @Override
    public ActionResponse detachNetworkToDhcpAgent(String agentId, String networkId) {
        Objects.requireNonNull(agentId);
        return deleteWithResponse(uri("/agents/%s/dhcp-networks/%s", agentId, networkId)).execute(ExecutionOptions.<ActionResponse>create(PropagateOnStatus.on(404)));
    }

    @Override public ActionResponse delete(String agentId) { return deleteWithResponse(uri("/agents/%s", Objects.requireNonNull(agentId))).execute(); }
    @Override public List<? extends Network> listDhcpNetworks(String agentId) { return get(Networks.class, uri("/agents/%s/dhcp-networks", Objects.requireNonNull(agentId))).execute(NeutronExecution.propagate404()).getList(); }
    @Override public List<? extends Router> listL3Routers(String agentId) { return get(Routers.class, uri("/agents/%s/l3-routers", Objects.requireNonNull(agentId))).execute(NeutronExecution.propagate404()).getList(); }

    @Override
    public ActionResponse addRouterToL3Agent(String agentId, String routerId) {
        return postWithResponse(uri("/agents/%s/l3-routers", Objects.requireNonNull(agentId)))
                .entity(JsonBody.of(Collections.singletonMap("router_id", Objects.requireNonNull(routerId)))).execute();
    }

    @Override
    public ActionResponse removeRouterFromL3Agent(String agentId, String routerId) {
        return deleteWithResponse(uri("/agents/%s/l3-routers/%s", Objects.requireNonNull(agentId), Objects.requireNonNull(routerId))).execute();
    }

    @Override public List<? extends Agent> listDhcpAgentsHostingNetwork(String networkId) { return get(Agents.class, uri("/networks/%s/dhcp-agents", Objects.requireNonNull(networkId))).execute(NeutronExecution.propagate404()).getList(); }
}
