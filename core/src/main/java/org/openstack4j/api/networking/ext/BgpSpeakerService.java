package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.BgpSpeaker;
import org.openstack4j.model.network.options.BgpSpeakerOptions;

/** BGP speakers ({@code /v2.0/bgp-speakers}). */
public interface BgpSpeakerService extends RestService {

    /** @return the BGP speakers */
    List<? extends BgpSpeaker> list();

    /** @param filters query parameters such as {@code name}, {@code project_id} */
    List<? extends BgpSpeaker> list(Map<String, String> filters);

    /** @return the BGP speaker, or {@code null} when it does not exist */
    BgpSpeaker get(String id);

    BgpSpeaker create(BgpSpeakerOptions options);

    /** Changes only the fields set in {@code options}. */
    BgpSpeaker update(String id, BgpSpeakerOptions options);

    ActionResponse addPeer(String id, String bgpPeerId);

    ActionResponse removePeer(String id, String bgpPeerId);

    ActionResponse addGatewayNetwork(String id, String networkId);

    ActionResponse removeGatewayNetwork(String id, String networkId);

    /** @return the routes the speaker advertises ({@code cidr}, {@code nexthop}); a missing speaker raises */
    List<Map<String, Object>> advertisedRoutes(String id);

    /** @return the BGP dynamic routing agents hosting the speaker; a missing speaker raises */
    List<Map<String, Object>> dragents(String id);

    /** Schedules a speaker onto a BGP dynamic routing agent (admin). */
    ActionResponse addToDragent(String agentId, String bgpSpeakerId);

    ActionResponse removeFromDragent(String agentId, String bgpSpeakerId);

    /** @return the speakers a BGP dynamic routing agent hosts; a missing agent raises */
    List<? extends BgpSpeaker> listOnDragent(String agentId);

    ActionResponse delete(String id);
}
