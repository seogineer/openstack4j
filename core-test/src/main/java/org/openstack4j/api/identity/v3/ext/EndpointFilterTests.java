package org.openstack4j.api.identity.v3.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.EndpointGroup;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/EndpointFilter")
public class EndpointFilterTests extends AbstractIdentityExtTest {

    private static final String EG = "5e4b09fcc4bd4cb781e47a0fd65d1da5";
    private static final String EP = "e1";
    private static final String EG_JSON = "{\"id\": \"" + EG + "\", \"name\": \"os4j-fixture\", \"description\": \"fixture\", \"filters\": {\"interface\": \"public\"}, \"links\": {}}";
    private static final String ENDPOINTS = "{\"endpoints\": [{\"id\": \"e1\", \"interface\": \"public\", \"url\": \"http://x\", \"service_id\": \"s1\", \"region_id\": \"RegionOne\", \"enabled\": true, \"links\": {}}], \"links\": {}}";
    private static final String PROJECTS = "{\"projects\": [{\"id\": \"" + PROJECT + "\", \"name\": \"admin\", \"domain_id\": \"default\", \"enabled\": true, \"links\": {}}], \"links\": {}}";

    public void endpointGroups() throws Exception {
        respondWith(201, "{\"endpoint_group\": " + EG_JSON + "}");
        respondWith(200, "{\"endpoint_groups\": [" + EG_JSON + "], \"links\": {}}");
        respondWith(200, "{\"endpoint_group\": " + EG_JSON + "}");
        respondWith(200);
        respondWith(200, "{\"endpoint_group\": " + EG_JSON + "}");
        respondWith(204);

        var filter = osv3().identity().endpointFilter();
        EndpointGroup created = filter.createEndpointGroup("os4j-fixture", "fixture", Map.of("interface", "public"));
        List<? extends EndpointGroup> all = filter.listEndpointGroups();
        filter.getEndpointGroup(EG);
        boolean exists = filter.checkEndpointGroup(EG).isSuccess();
        filter.updateEndpointGroup(EG, null, "changed", null);
        filter.deleteEndpointGroup(EG);

        RecordedRequest create = takeRequest();
        Assert.assertTrue(create.getPath().endsWith("/v3/OS-EP-FILTER/endpoint_groups"));
        Assert.assertEquals(body(create).get("endpoint_group").get("filters").get("interface").asText(), "public");
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        Assert.assertTrue(takeRequest().getPath().endsWith("/endpoint_groups/" + EG));
        Assert.assertEquals(takeRequest().getMethod(), "HEAD");
        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PATCH");
        Assert.assertEquals(body(update).get("endpoint_group").get("description").asText(), "changed");
        Assert.assertFalse(body(update).get("endpoint_group").has("name"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getFilters().get("interface"), "public");
        Assert.assertEquals(all.get(0).getName(), "os4j-fixture");
        Assert.assertTrue(exists);
    }

    public void associations() throws Exception {
        respondWith(200, ENDPOINTS);
        respondWith(200, PROJECTS);
        respondWith(204);
        respondWith(204);
        respondWith(200, "{\"project\": {\"id\": \"" + PROJECT + "\", \"name\": \"admin\", \"links\": {}}}");
        respondWith(204);
        respondWith(200, "{\"endpoint_groups\": [" + EG_JSON + "], \"links\": {}}");
        respondWith(200, ENDPOINTS);
        respondWith(204);
        respondWith(204);
        respondWith(204);
        respondWith(200, PROJECTS);

        var filter = osv3().identity().endpointFilter();
        Assert.assertEquals(filter.endpointGroupEndpoints(EG).get(0).getId(), "e1");
        Assert.assertEquals(filter.endpointGroupProjects(EG).get(0).getId(), PROJECT);
        filter.addProjectToEndpointGroup(EG, PROJECT);
        filter.checkProjectInEndpointGroup(EG, PROJECT);
        Assert.assertEquals(filter.getProjectEndpointGroup(EG, PROJECT).getName(), "admin");
        filter.removeProjectFromEndpointGroup(EG, PROJECT);
        Assert.assertEquals(filter.projectEndpointGroups(PROJECT).size(), 1);
        Assert.assertEquals(filter.projectEndpoints(PROJECT).size(), 1);
        filter.addEndpointToProject(PROJECT, EP);
        filter.checkEndpointInProject(PROJECT, EP);
        filter.removeEndpointFromProject(PROJECT, EP);
        Assert.assertEquals(filter.endpointProjects(EP).size(), 1);

        String g = "/v3/OS-EP-FILTER/endpoint_groups/" + EG;
        String p = "/v3/OS-EP-FILTER/projects/" + PROJECT;
        String[][] expected = {{"GET", g + "/endpoints"}, {"GET", g + "/projects"}, {"PUT", g + "/projects/" + PROJECT},
                {"HEAD", g + "/projects/" + PROJECT}, {"GET", g + "/projects/" + PROJECT}, {"DELETE", g + "/projects/" + PROJECT},
                {"GET", p + "/endpoint_groups"}, {"GET", p + "/endpoints"}, {"PUT", p + "/endpoints/e1"}, {"HEAD", p + "/endpoints/e1"},
                {"DELETE", p + "/endpoints/e1"}, {"GET", "/v3/OS-EP-FILTER/endpoints/e1/projects"}};
        for (String[] e : expected) {
            RecordedRequest r = takeRequest();
            Assert.assertEquals(r.getMethod(), e[0], e[1]);
            Assert.assertTrue(r.getPath().endsWith(e[1]), r.getPath() + " vs " + e[1]);
        }
    }
}
