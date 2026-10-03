package org.openstack4j.api.networking;

import org.openstack4j.model.network.options.PortBindingOptions;
import org.openstack4j.model.network.ext.PortBinding;
import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.options.PortListOptions;

/**
 * OpenStack (Neutron) Port based Operations
 *
 * @author Jeremy Unruh
 */
public interface PortService extends RestService {

    /**
     * Lists all Ports authorized by the current Tenant
     *
     * @return the list of ports
     */
    List<? extends Port> list();

    /**
     * Lists all Ports authorized by the current Tenant
     *
     * @param options filtering options
     * @return the list of ports
     */
    List<? extends Port> list(PortListOptions options);

    /**
     * Lists all Ports authorized by the current Tenant
     * <br/>
     * Supports multiple values, eg: fields=id&fields=name
     * @param params filtering options
     * @return the list of ports
     */
    List<? extends Port> list(Map<String, ? extends Iterable<?>> params);

    /**
     * Gets the Port by ID
     *
     * @param portId the port identifier
     * @return the port or null if not found
     */
    Port get(String portId);

    /**
     * Delete a Port by ID
     *
     * @param portId the port identifier to delete
     * @return the action response
     */
    ActionResponse delete(String portId);

    /**
     * Creates a new Port
     *
     * @param port the port to create
     * @return the newly create Port
     */
    Port create(Port port);

    /**
     * Creates new Ports
     *
     * @param ports the ports to create
     * @return the newly created Ports
     */
    List<? extends Port> create(List<? extends Port> ports);

    /**
     * Updates an existing Port.  The Port identifier must be set on the port object to be successful
     *
     * @param port the port to update
     * @return the updated port
     */
    Port update(Port port);

    /**
     * @param portId the port
     * @return the bindings of the port (binding-extended)
     */
    List<? extends PortBinding> listBindings(String portId);

    /**
     * Creates an inactive binding on another host, as during live migration.
     *
     * @param portId  the port
     * @param options the binding (host, vnic_type, profile)
     * @return the created binding
     */
    PortBinding createBinding(String portId, PortBindingOptions options);

    /**
     * @param portId the port
     * @param host   the host whose binding becomes active
     * @return the activated binding
     */
    PortBinding activateBinding(String portId, String host);

    /**
     * @param portId the port
     * @param host   the host of the binding to delete
     * @return the action response
     */
    ActionResponse deleteBinding(String portId, String host);
}
