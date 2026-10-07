package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Node;
import org.openstack4j.model.baremetal.options.NodeCreate;
import org.openstack4j.model.common.ActionResponse;

/** Bare metal nodes ({@code /v1/nodes}). A node is addressed by its UUID or (microversion 1.5+) its name. */
public interface NodeService extends RestService {

    /** @return the nodes (summary fields; use {@code listDetail()} for all fields) */
    List<? extends Node> list();

    /**
     * @param filters query parameters such as {@code provision_state}, {@code maintenance}, {@code driver},
     *                {@code resource_class}, {@code limit}, {@code marker}, {@code fields}
     * @return the matching nodes (summary fields)
     */
    List<? extends Node> list(Map<String, String> filters);

    /** @return the nodes with all fields */
    List<? extends Node> listDetail();

    /** @return the matching nodes with all fields */
    List<? extends Node> listDetail(Map<String, String> filters);

    /** @return the node, or {@code null} when it does not exist */
    Node get(String nodeIdent);

    /** @return the created node (provision state {@code enroll} from microversion 1.11) */
    Node create(NodeCreate node);

    /**
     * Updates a node with JSON Patch operations, e.g. {@code BaremetalPatch.replace("/description", "rack 3")}.
     *
     * @return the updated node
     */
    Node update(String nodeIdent, List<BaremetalPatch> patches);

    ActionResponse delete(String nodeIdent);
}
