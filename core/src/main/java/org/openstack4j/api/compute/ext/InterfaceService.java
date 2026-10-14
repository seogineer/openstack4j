package org.openstack4j.api.compute.ext;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.InterfaceAttachment;

/**
 * API to Create, list, get details for, and delete port interfaces on a Server Instance
 *
 * @author Jeremy Unruh
 */
public interface InterfaceService extends RestService {

    /**
     * Creates and uses a port interface to attach the port to a server instance.
     *
     * @param serverId the server id
     * @param portId the port id to attach
     * @return the attached interface
     */
    InterfaceAttachment create(String serverId, String portId);

    /**
     * Creates a port on a network and attaches it ({@code POST /servers/{id}/os-interface} with {@code net_id}).
     * Nova rejects a requested IP without a network and allows one fixed IP only.
     *
     * @param serverId the server id
     * @param netId the network id
     * @param ipAddress the fixed IP to request on that network, or null to let Neutron choose ({@code fixed_ips[0].ip_address})
     * @return the attached interface
     */
    InterfaceAttachment attachToNetwork(String serverId, String netId, String ipAddress);

    /**
     * List the port interfaces for the specified {@code serverId}
     *
     * @param serverId the server id
     * @return List of interface attachments
     */
    List<? extends InterfaceAttachment> list(String serverId);

    /**
     * Shows information about a specified port interface
     *
     * @param serverId the server id
     * @param attachmentId the attachment identifier
     * @return the interface attachment
     */
    InterfaceAttachment get(String serverId, String attachmentId);

    /**
     * Detaches a specified port interface
     *
     * @param serverId the server id
     * @param attachmentId the attachment identifier
     * @return the action response indicating success or failure
     */
    ActionResponse detach(String serverId, String attachmentId);

}
