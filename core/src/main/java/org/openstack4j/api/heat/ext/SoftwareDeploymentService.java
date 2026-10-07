package org.openstack4j.api.heat.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.heat.ext.SoftwareDeployment;
import org.openstack4j.model.heat.options.SoftwareDeploymentOptions;

/**
 * Heat software deployments ({@code /software_deployments}).
 */
public interface SoftwareDeploymentService extends RestService {

    /**
     * Lists software deployments, optionally filtered (server_id).
     *
     * @return the result
     */
    List<? extends SoftwareDeployment> list();

    /**
     * Lists software deployments, optionally filtered (server_id).
     *
     * @param Map<String the map< string
     * @param filters the filters
     * @return the result
     */
    List<? extends SoftwareDeployment> list(Map<String, String> filters);

    /**
     * Returns a deployment; null if missing.
     *
     * @param id the id
     * @return the result
     */
    SoftwareDeployment get(String id);

    /**
     * Creates a deployment of a config to a server.
     *
     * @param options the options
     * @return the result
     */
    SoftwareDeployment create(SoftwareDeploymentOptions options);

    /**
     * Updates a deployment (for example status and output values signalled back); only the fields set are sent.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    SoftwareDeployment update(String id, SoftwareDeploymentOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);

    /**
     * Returns the deployment metadata a server polls for.
     *
     * @param serverId the server id
     * @return the result
     */
    List<Map<String, Object>> metadata(String serverId);
}
