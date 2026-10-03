package org.openstack4j.openstack.identity.v3.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.identity.v3.EndpointFilterService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Endpoint;
import org.openstack4j.model.identity.v3.EndpointGroup;
import org.openstack4j.model.identity.v3.Project;
import org.openstack4j.openstack.identity.v3.domain.KeystoneEndpoint.Endpoints;
import org.openstack4j.openstack.identity.v3.domain.KeystoneEndpointGroup;
import org.openstack4j.openstack.identity.v3.domain.KeystoneEndpointGroup.EndpointGroups;
import org.openstack4j.openstack.identity.v3.domain.KeystoneProject;
import org.openstack4j.openstack.identity.v3.domain.KeystoneProject.Projects;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class EndpointFilterServiceImpl extends BaseIdentityServices implements EndpointFilterService {

    private static final String GROUPS = "/OS-EP-FILTER/endpoint_groups/";
    private static final String PROJECTS = "/OS-EP-FILTER/projects/";

    private static String id(String value) {
        return Objects.requireNonNull(value);
    }

    private static Map<String, Object> group(String name, String description, Map<String, Object> filters) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (filters != null) body.put("filters", filters);
        return body;
    }

    @Override public List<? extends EndpointGroup> listEndpointGroups() { return get(EndpointGroups.class, "/OS-EP-FILTER/endpoint_groups").execute().getList(); }
    @Override public EndpointGroup getEndpointGroup(String id) { return get(KeystoneEndpointGroup.class, GROUPS, id(id)).execute(); }
    @Override public ActionResponse checkEndpointGroup(String id) { return head(ActionResponse.class, GROUPS, id(id)).execute(); }

    @Override
    public EndpointGroup createEndpointGroup(String name, String description, Map<String, Object> filters) {
        return post(KeystoneEndpointGroup.class, "/OS-EP-FILTER/endpoint_groups")
                .entity(JsonBody.of("endpoint_group", group(id(name), description, Objects.requireNonNull(filters)))).execute();
    }

    @Override
    public EndpointGroup updateEndpointGroup(String id, String name, String description, Map<String, Object> filters) {
        return patch(KeystoneEndpointGroup.class, GROUPS, id(id)).entity(JsonBody.of("endpoint_group", group(name, description, filters))).execute();
    }

    @Override public ActionResponse deleteEndpointGroup(String id) { return deleteWithResponse(GROUPS, id(id)).execute(); }
    @Override public List<? extends Endpoint> endpointGroupEndpoints(String eg) { return get(Endpoints.class, GROUPS, id(eg), "/endpoints").execute().getList(); }
    @Override public List<? extends Project> endpointGroupProjects(String eg) { return get(Projects.class, GROUPS, id(eg), "/projects").execute().getList(); }
    @Override public ActionResponse addProjectToEndpointGroup(String eg, String p) { return put(ActionResponse.class, GROUPS, id(eg), "/projects/", id(p)).execute(); }
    @Override public ActionResponse checkProjectInEndpointGroup(String eg, String p) { return head(ActionResponse.class, GROUPS, id(eg), "/projects/", id(p)).execute(); }
    @Override public ActionResponse removeProjectFromEndpointGroup(String eg, String p) { return deleteWithResponse(GROUPS, id(eg), "/projects/", id(p)).execute(); }
    @Override public Project getProjectEndpointGroup(String eg, String p) { return get(KeystoneProject.class, GROUPS, id(eg), "/projects/", id(p)).execute(); }
    @Override public List<? extends EndpointGroup> projectEndpointGroups(String p) { return get(EndpointGroups.class, PROJECTS, id(p), "/endpoint_groups").execute().getList(); }
    @Override public List<? extends Endpoint> projectEndpoints(String p) { return get(Endpoints.class, PROJECTS, id(p), "/endpoints").execute().getList(); }
    @Override public ActionResponse addEndpointToProject(String p, String e) { return put(ActionResponse.class, PROJECTS, id(p), "/endpoints/", id(e)).execute(); }
    @Override public ActionResponse checkEndpointInProject(String p, String e) { return head(ActionResponse.class, PROJECTS, id(p), "/endpoints/", id(e)).execute(); }
    @Override public ActionResponse removeEndpointFromProject(String p, String e) { return deleteWithResponse(PROJECTS, id(p), "/endpoints/", id(e)).execute(); }
    @Override public List<? extends Project> endpointProjects(String e) { return get(Projects.class, "/OS-EP-FILTER/endpoints/", id(e), "/projects").execute().getList(); }
}
