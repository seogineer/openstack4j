package org.openstack4j.api.image.v2.ext;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.image.v2.ext.MetadefTag;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/MetadefTags")
public class MetadefTagTests extends AbstractImageExtTest {

    private static final String P = "/v2/metadefs/namespaces/ns1";
    private static final String TAG = "{\"created_at\": \"2015-05-06T23:16:12Z\", \"name\": \"sample-tag2\", \"updated_at\": \"2015-05-06T23:16:12Z\"}";

    public void tagLifecycle() throws Exception {
        respondWith(201, TAG);
        respondWith(200, "{\"tags\": [{\"name\": \"sample-tag1\"}, {\"name\": \"sample-tag2\"}]}");
        respondWith(200, TAG);
        respondWith(200, "{\"created_at\": \"2015-05-06T23:16:12Z\", \"name\": \"renamed\", \"updated_at\": \"2015-05-07T00:00:00Z\"}");
        respondWith(204);
        respondWith(204);

        var metadefs = osv3().imagesV2().metadefs();
        MetadefTag created = metadefs.createTag("ns1", "sample-tag2");
        List<? extends MetadefTag> all = metadefs.listTags("ns1");
        metadefs.getTag("ns1", "sample-tag2");
        MetadefTag renamed = metadefs.updateTag("ns1", "sample-tag2", "renamed");
        metadefs.deleteTag("ns1", "renamed");
        metadefs.deleteAllTags("ns1");

        RecordedRequest create = expect("POST", P + "/tags/sample-tag2");
        Assert.assertEquals(create.getBodySize(), 0);
        expect("GET", P + "/tags");
        expect("GET", P + "/tags/sample-tag2");
        Assert.assertEquals(body(expect("PUT", P + "/tags/sample-tag2")).get("name").asText(), "renamed");
        expect("DELETE", P + "/tags/renamed");
        expect("DELETE", P + "/tags");
        Assert.assertEquals(created.getName(), "sample-tag2");
        Assert.assertEquals(all.size(), 2);
        Assert.assertEquals(renamed.getName(), "renamed");
    }

    public void createTagsSendsAppendHeader() throws Exception {
        respondWith(201, "{\"tags\": [{\"name\": \"a\"}, {\"name\": \"b\"}]}");
        respondWith(201, "{\"tags\": [{\"name\": \"c\"}]}");

        List<? extends MetadefTag> replaced = osv3().imagesV2().metadefs().createTags("ns1", List.of("a", "b"), false);
        osv3().imagesV2().metadefs().createTags("ns1", List.of("c"), true);

        RecordedRequest first = expect("POST", P + "/tags");
        Assert.assertEquals(first.getHeader("X-Openstack-Append"), "false");
        Assert.assertEquals(body(first).get("tags").get(1).get("name").asText(), "b");
        Assert.assertEquals(expect("POST", P + "/tags").getHeader("X-Openstack-Append"), "true");
        Assert.assertEquals(replaced.get(0).getName(), "a");
    }
}
