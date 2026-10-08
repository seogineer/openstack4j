package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.BgpSpeakerService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.BgpSpeaker;
import org.openstack4j.model.network.options.BgpSpeakerOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronBgpSpeaker;
import org.openstack4j.openstack.networking.domain.ext.NeutronBgpSpeaker.NeutronBgpSpeakerList;

public class BgpSpeakerServiceImpl extends BaseNeutronExtService implements BgpSpeakerService {

    private static final String PATH = "/bgp-speakers";
    private static final String ROOT = "bgp_speaker";

    @Override
    public List<? extends BgpSpeaker> list() {
        return list(null);
    }

    @Override
    public List<? extends BgpSpeaker> list(Map<String, String> filters) {
        return listOf(NeutronBgpSpeakerList.class, PATH, filters);
    }

    @Override
    public BgpSpeaker get(String id) {
        return show(NeutronBgpSpeaker.class, PATH + "/" + id(id));
    }

    @Override
    public BgpSpeaker create(BgpSpeakerOptions options) {
        return create(NeutronBgpSpeaker.class, PATH, ROOT, options);
    }

    @Override
    public BgpSpeaker update(String id, BgpSpeakerOptions options) {
        return update(NeutronBgpSpeaker.class, PATH + "/" + id(id), ROOT, options);
    }

    @Override
    public ActionResponse addPeer(String id, String bgpPeerId) {
        return change(id, "add_bgp_peer", Map.of("bgp_peer_id", id(bgpPeerId)));
    }

    @Override
    public ActionResponse removePeer(String id, String bgpPeerId) {
        return change(id, "remove_bgp_peer", Map.of("bgp_peer_id", id(bgpPeerId)));
    }

    @Override
    public ActionResponse addGatewayNetwork(String id, String networkId) {
        return change(id, "add_gateway_network", Map.of("network_id", id(networkId)));
    }

    @Override
    public ActionResponse removeGatewayNetwork(String id, String networkId) {
        return change(id, "remove_gateway_network", Map.of("network_id", id(networkId)));
    }

    private ActionResponse change(String id, String action, Map<String, ?> body) {
        return putWithResponse(PATH + "/" + id(id) + "/" + action).entity(org.openstack4j.openstack.internal.microversion.JsonBody.of(body)).execute();
    }

    @Override
    public List<Map<String, Object>> advertisedRoutes(String id) {
        return maps(PATH + "/" + id(id) + "/get_advertised_routes", "advertised_routes");
    }

    @Override
    public List<Map<String, Object>> dragents(String id) {
        return maps(PATH + "/" + id(id) + "/bgp-dragents", "agents");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> maps(String path, String key) {
        Map<String, Object> body = showStrict(Map.class, path);
        Object list = body == null ? null : body.get(key);
        return list instanceof List ? (List<Map<String, Object>>) list : java.util.Collections.emptyList();
    }

    @Override
    public ActionResponse addToDragent(String agentId, String bgpSpeakerId) {
        return postWithResponse("/agents/" + id(agentId) + "/bgp-drinstances")
                .entity(org.openstack4j.openstack.internal.microversion.JsonBody.of(Map.of("bgp_speaker_id", id(bgpSpeakerId)))).execute();
    }

    @Override
    public ActionResponse removeFromDragent(String agentId, String bgpSpeakerId) {
        return remove("/agents/" + id(agentId) + "/bgp-drinstances/" + id(bgpSpeakerId));
    }

    @Override
    public List<? extends BgpSpeaker> listOnDragent(String agentId) {
        return listOf(NeutronBgpSpeakerList.class, "/agents/" + id(agentId) + "/bgp-drinstances", null);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
