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
     * Lists software deployments.
     *
     * @return the deployments
     */
    List<? extends SoftwareDeployment> list();

    /**
     * Lists software deployments, optionally filtered (server_id).
     *
     * @param filters the filters
     * @return the deployments
     */
    List<? extends SoftwareDeployment> list(Map<String, String> filters);

    /**
     * Returns a deployment; null if missing.
     *
     * @param id the id
     * @return the deployment, or {@code null} when it does not exist
     */
    SoftwareDeployment get(String id);

    /**
     * Creates a deployment of a config to a server.
     *
     * @throws org.openstack4j.api.exceptions.ResponseException on an error status, including 404
     * @param options the options
     * @return the created deployment
     */
    SoftwareDeployment create(SoftwareDeploymentOptions options);

    /**
     * Updates a deployment (for example status and output values signalled back); only the fields set are sent.
     *
     * @throws org.openstack4j.api.exceptions.ResponseException if the deployment does not exist (404)
     * @param id the id
     * @param options the options
     * @return the updated deployment
     */
    SoftwareDeployment update(String id, SoftwareDeploymentOptions options);

    /**
     * @param id the id
     * @return the action response; a failed response with code 404 when the deployment does not exist
     */
    ActionResponse delete(String id);

    /**
     * Returns the deployment metadata a server polls for.
     *
     * @throws org.openstack4j.api.exceptions.ResponseException on an error status, including 404
     * @param serverId the server id
     * @return the deployment metadata of the server
     */
    List<Map<String, Object>> metadata(String serverId);
}
