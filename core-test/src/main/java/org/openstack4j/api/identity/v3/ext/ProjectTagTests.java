package org.openstack4j.api.identity.v3.ext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/ProjectTags")
public class ProjectTagTests extends AbstractIdentityExtTest {

    public void tagLifecycle() throws Exception {
        respondWith(200, "{\"tags\": [\"prod\"], \"links\": {}}");
        respondWith(201);
        respondWith(204);
        respondWith(200, "{\"tags\": [\"a\", \"b\"], \"links\": {}}");
        respondWith(204);
        respondWith(204);

        List<String> tags = osv3().identity().projects().tags(PROJECT);
        boolean added = osv3().identity().projects().addTag(PROJECT, "web").isSuccess();
        boolean present = osv3().identity().projects().hasTag(PROJECT, "web").isSuccess();
        List<String> replaced = osv3().identity().projects().replaceTags(PROJECT, Arrays.asList("a", "b"));
        osv3().identity().projects().removeTag(PROJECT, "a");
        osv3().identity().projects().removeAllTags(PROJECT);

        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/projects/" + PROJECT + "/tags"));
        RecordedRequest add = takeRequest();
        Assert.assertEquals(add.getMethod(), "PUT");
        Assert.assertTrue(add.getPath().endsWith("/tags/web"));
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        RecordedRequest replace = takeRequest();
        Assert.assertEquals(replace.getMethod(), "PUT");
        Assert.assertEquals(body(replace).get("tags").get(1).asText(), "b");
        RecordedRequest remove = takeRequest();
        Assert.assertEquals(remove.getMethod(), "DELETE");
        Assert.assertTrue(remove.getPath().endsWith("/tags/a"));
        RecordedRequest removeAll = takeRequest();
        Assert.assertEquals(removeAll.getMethod(), "DELETE");
        Assert.assertTrue(removeAll.getPath().endsWith("/projects/" + PROJECT + "/tags"));
        Assert.assertEquals(tags, Arrays.asList("prod"));
        Assert.assertTrue(added);
        Assert.assertTrue(present);
        Assert.assertEquals(replaced, Arrays.asList("a", "b"));
    }

    public void missingTagIsNotSuccess() throws Exception {
        respondWith(404, "{\"error\": {\"code\": 404, \"message\": \"not found\"}}");
        Assert.assertFalse(osv3().identity().projects().hasTag(PROJECT, "nope").isSuccess());
        takeRequest();
    }

    public void tagWithSpacesIsEncodedInPath() throws Exception {
        respondWith(201);
        osv3().identity().projects().addTag(PROJECT, "my tag");
        RecordedRequest request = takeRequest();
        Assert.assertFalse(request.getPath().contains(" "), request.getPath());
        Assert.assertTrue(decodedPath(request).endsWith("/tags/my tag"));
    }

    public void systemScopes() throws Exception {
        respondWith(200, "{\"system\": [{\"all\": true}], \"links\": {}}");
        List<Map<String, Object>> scopes = osv3().identity().tokens().getSystemScopes("tok");
        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/v3/auth/system"));
        Assert.assertEquals(request.getHeader("X-Subject-Token"), "tok");
        Assert.assertEquals(scopes.get(0).get("all"), Boolean.TRUE);
    }
}
