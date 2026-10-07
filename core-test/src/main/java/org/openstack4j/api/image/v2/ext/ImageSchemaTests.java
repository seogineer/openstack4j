package org.openstack4j.api.image.v2.ext;

import java.util.Map;
import java.util.function.Supplier;

import org.openstack4j.api.image.v2.ext.ImageSchemaService;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/Schemas")
public class ImageSchemaTests extends AbstractImageExtTest {

    public void everySchemaPath() throws Exception {
        ImageSchemaService schemas = osv3().imagesV2().schemas();
        Object[][] cases = {
                {(Supplier<Map<String, Object>>) schemas::image, "/v2/schemas/image"},
                {(Supplier<Map<String, Object>>) schemas::images, "/v2/schemas/images"},
                {(Supplier<Map<String, Object>>) schemas::member, "/v2/schemas/member"},
                {(Supplier<Map<String, Object>>) schemas::members, "/v2/schemas/members"},
                {(Supplier<Map<String, Object>>) schemas::task, "/v2/schemas/task"},
                {(Supplier<Map<String, Object>>) schemas::tasks, "/v2/schemas/tasks"},
                {(Supplier<Map<String, Object>>) schemas::metadefNamespace, "/v2/schemas/metadefs/namespace"},
                {(Supplier<Map<String, Object>>) schemas::metadefNamespaces, "/v2/schemas/metadefs/namespaces"},
                {(Supplier<Map<String, Object>>) schemas::metadefObject, "/v2/schemas/metadefs/object"},
                {(Supplier<Map<String, Object>>) schemas::metadefObjects, "/v2/schemas/metadefs/objects"},
                {(Supplier<Map<String, Object>>) schemas::metadefProperty, "/v2/schemas/metadefs/property"},
                {(Supplier<Map<String, Object>>) schemas::metadefProperties, "/v2/schemas/metadefs/properties"},
                {(Supplier<Map<String, Object>>) schemas::metadefResourceType, "/v2/schemas/metadefs/resource_type"},
                {(Supplier<Map<String, Object>>) schemas::metadefResourceTypes, "/v2/schemas/metadefs/resource_types"},
                {(Supplier<Map<String, Object>>) schemas::metadefTag, "/v2/schemas/metadefs/tag"},
                {(Supplier<Map<String, Object>>) schemas::metadefTags, "/v2/schemas/metadefs/tags"}};
        for (Object[] c : cases) {
            respondWith(200, "{\"name\": \"x\", \"properties\": {\"id\": {\"type\": \"string\"}}, \"additionalProperties\": {\"type\": \"string\"}}");
            @SuppressWarnings("unchecked")
            Map<String, Object> schema = ((Supplier<Map<String, Object>>) c[0]).get();
            expect("GET", (String) c[1]);
            Assert.assertEquals(schema.get("name"), "x");
            Assert.assertTrue(schema.get("properties") instanceof Map);
        }
    }
}
