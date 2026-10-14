package org.openstack4j.api.image.v2.ext;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.image.v2.ext.MetadefNamespace;
import org.openstack4j.model.image.v2.options.MetadefNamespaceOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/MetadefNamespaceResourceType")
public class MetadefResourceTypeFilterTests extends AbstractImageExtTest {

    private static final String NS = "OS::Compute::Libvirt";
    private static final String NS_JSON = "{\"namespace\": \"" + NS + "\", \"visibility\": \"public\","
            + " \"properties\": {\"hw_boot_menu\": {\"title\": \"Boot Menu\", \"type\": \"string\"}}}";

    public void getNamespaceWithResourceType() throws Exception {
        respondWith(200, NS_JSON);

        MetadefNamespace namespace = osv3().imagesV2().metadefs().getNamespace(NS, "OS::Glance::Image");

        RecordedRequest request = expect("GET", "/v2/metadefs/namespaces/" + NS + "?resource_type=OS::Glance::Image");
        Assert.assertEquals(namespace.getNamespace(), NS);
        Assert.assertTrue(namespace.getProperties().containsKey("hw_boot_menu"));
        Assert.assertNotNull(request);
    }

    public void getNamespaceWithoutResourceTypeSendsNoQuery() throws Exception {
        respondWith(200, NS_JSON);
        respondWith(200, NS_JSON);

        osv3().imagesV2().metadefs().getNamespace(NS, null);
        osv3().imagesV2().metadefs().getNamespace(NS);

        Assert.assertFalse(takeRequest().getPath().contains("?"));
        Assert.assertFalse(takeRequest().getPath().contains("?"));
    }

    public void getNamespace404() throws Exception {
        respondWith(404, "{\"message\": \"nope\"}");

        Assert.assertNull(osv3().imagesV2().metadefs().getNamespace(NS, "OS::Glance::Image"));

        takeRequest();
    }

    public void createNamespaceFullDefinition() throws Exception {
        respondWith(201, NS_JSON);

        osv3().imagesV2().metadefs().createNamespace(MetadefNamespaceOptions.create("ns")
                .attribute("properties", Map.of("p1", Map.of("type", "string", "title", "P1")))
                .attribute("objects", List.of(Map.of("name", "o1", "description", "obj")))
                .attribute("tags", List.of(Map.of("name", "t")))
                .attribute("resource_type_associations", List.of(Map.of("name", "OS::Glance::Image"))));

        JsonNode body = body(expect("POST", "/v2/metadefs/namespaces"));
        Assert.assertEquals(body.get("namespace").asText(), "ns");
        Assert.assertEquals(body.get("properties").get("p1").get("title").asText(), "P1");
        Assert.assertEquals(body.get("objects").get(0).get("name").asText(), "o1");
        Assert.assertEquals(body.get("tags").get(0).get("name").asText(), "t");
        Assert.assertEquals(body.get("resource_type_associations").get(0).get("name").asText(), "OS::Glance::Image");
        Assert.assertEquals(Set.copyOf(java.util.stream.StreamSupport.stream(
                java.util.Spliterators.spliteratorUnknownSize(body.fieldNames(), 0), false).toList()),
                Set.of("namespace", "properties", "objects", "tags", "resource_type_associations"));
    }
}
