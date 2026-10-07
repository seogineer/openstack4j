package org.openstack4j.api.identity.v3;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Endpoint;
import org.openstack4j.model.identity.v3.EndpointGroup;
import org.openstack4j.model.identity.v3.Project;

/**
 * OS-EP-FILTER: endpoint groups and project/endpoint associations ({@code /v3/OS-EP-FILTER}).
 */
public interface EndpointFilterService extends RestService {

    /**
     * Lists endpoint groups.
     *
     * @return the result
     */
    List<? extends EndpointGroup> listEndpointGroups();

    /**
     * @param id the id
     * @return the result
     */
    EndpointGroup getEndpointGroup(String id);

    /**
     * Checks that the endpoint group exists (HEAD).
     *
     * @param id the id
     * @return the action response
     */
    ActionResponse checkEndpointGroup(String id);

    /**
     * Creates an endpoint group selecting catalog endpoints by filters such as interface, service_id and region_id.
     *
     * @param name the name
     * @param description the description
     * @param filters the filters
     * @return the result
     */
    EndpointGroup createEndpointGroup(String name, String description, Map<String, Object> filters);

    /**
     * Updates an endpoint group; null arguments are left unchanged.
     *
     * @param id the id
     * @param name the name
     * @param description the description
     * @param filters the filters
     * @return the result
     */
    EndpointGroup updateEndpointGroup(String id, String name, String description, Map<String, Object> filters);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse deleteEndpointGroup(String id);

    /**
     * Lists the endpoints the group selects.
     *
     * @param eg the eg
     * @return the result
     */
    List<? extends Endpoint> endpointGroupEndpoints(String eg);

    /**
     * Lists the projects associated with the group.
     *
     * @param eg the eg
     * @return the result
     */
    List<? extends Project> endpointGroupProjects(String eg);

    /**
     * @param eg the eg
     * @param p the p
     * @return the action response
     */
    ActionResponse addProjectToEndpointGroup(String eg, String p);

    /**
     * @param eg the eg
     * @param p the p
     * @return the action response
     */
    ActionResponse checkProjectInEndpointGroup(String eg, String p);

    /**
     * @param eg the eg
     * @param p the p
     * @return the action response
     */
    ActionResponse removeProjectFromEndpointGroup(String eg, String p);

    /**
     * Returns the project when it is associated with the endpoint group.
     *
     * @param eg the eg
     * @param p the p
     * @return the result
     */
    Project getProjectEndpointGroup(String eg, String p);

    /**
     * Lists the endpoint groups associated with the project.
     *
     * @param p the p
     * @return the result
     */
    List<? extends EndpointGroup> projectEndpointGroups(String p);

    /**
     * Lists the endpoints associated with the project, directly or through endpoint groups.
     *
     * @param p the p
     * @return the result
     */
    List<? extends Endpoint> projectEndpoints(String p);

    /**
     * @param p the p
     * @param e the e
     * @return the action response
     */
    ActionResponse addEndpointToProject(String p, String e);

    /**
     * @param p the p
     * @param e the e
     * @return the action response
     */
    ActionResponse checkEndpointInProject(String p, String e);

    /**
     * @param p the p
     * @param e the e
     * @return the action response
     */
    ActionResponse removeEndpointFromProject(String p, String e);

    /**
     * Lists the projects associated with the endpoint.
     *
     * @param e the e
     * @return the result
     */
    List<? extends Project> endpointProjects(String e);
}
