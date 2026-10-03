package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.LocalIp;
import org.openstack4j.model.network.ext.LocalIpPortAssociation;
import org.openstack4j.model.network.options.LocalIpOptions;

/**
 * Local IPs ({@code /v2.0/local_ips}, local_ip).
 */
public interface LocalIpService extends RestService {

    /**
     * Lists local IPs, optionally filtered.
     *
     * @return the result
     */
    List<? extends LocalIp> list();

    /**
     * Lists local IPs, optionally filtered.
     *
     * @param Map<String the map< string
     * @param filters the filters
     * @return the result
     */
    List<? extends LocalIp> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    LocalIp get(String id);

    /**
     * Creates a local IP.
     *
     * @param options the options
     * @return the result
     */
    LocalIp create(LocalIpOptions options);

    /**
     * Updates the name or description of a local IP.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    LocalIp update(String id, LocalIpOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);

    /**
     * Lists the ports associated with the local IP.
     *
     * @param localIpId the local ip id
     * @return the result
     */
    List<? extends LocalIpPortAssociation> portAssociations(String localIpId);

    /**
     * Associates a port; a null fixed IP lets Neutron pick the port IP.
     *
     * @param localIpId the local ip id
     * @param fixedPortId the fixed port id
     * @param fixedIp the fixed ip
     * @return the result
     */
    LocalIpPortAssociation associatePort(String localIpId, String fixedPortId, String fixedIp);

    /**
     * Removes the association of a port.
     *
     * @param localIpId the local ip id
     * @param fixedPortId the fixed port id
     * @return the action response
     */
    ActionResponse disassociatePort(String localIpId, String fixedPortId);
}
