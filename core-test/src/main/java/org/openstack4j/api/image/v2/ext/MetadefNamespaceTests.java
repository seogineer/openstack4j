package org.openstack4j.api.image.v2.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.image.v2.ext.MetadefNamespace;
import org.openstack4j.model.image.v2.ext.MetadefResourceTypeAssociation;
import org.openstack4j.model.image.v2.options.MetadefNamespaceOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/MetadefNamespaces")
public class MetadefNamespaceTests extends AbstractImageExtTest {

    private static final String NS = "OS::Compute::Libvirt";
    private static final String NS_JSON = "{\"created_at\": \"2016-06-28T14:57:10Z\", \"description\": \"The libvirt compute driver options.\", \"display_name\": \"libvirt Driver Options\","
            + " \"namespace\": \"" + NS + "\", \"owner\": \"admin\", \"properties\": {\"boot_menu\": {\"description\": \"d\", \"enum\": [\"true\", \"false\"], \"title\": \"Boot Menu\", \"type\": \"string\"}},"
            + " \"protected\": true, \"resource_type_associations\": [{\"created_at\": \"2016-06-28T14:57:10Z\", \"name\": \"OS::Glance::Image\", \"prefix\": \"hw_\"}],"
            + " \"schema\": \"/v2/schemas/metadefs/namespace\", \"self\": \"/v2/metadefs/namespaces/" + NS + "\", \"updated_at\": \"2016-06-28T14:57:10Z\", \"visibility\": \"public\"}";

    public void namespaces() throws Exception {
        respondWith(201, NS_JSON);
        respondWith(200, "{\"first\": \"/v2/metadefs/namespaces\", \"namespaces\": [" + NS_JSON + "], \"schema\": \"/v2/schemas/metadefs/namespaces\"}");
        respondWith(200, "{\"namespaces\": []}");
        respondWith(200, NS_JSON);
        respondWith(200, NS_JSON);
        respondWith(204);

        var metadefs = osv3().imagesV2().metadefs();
        MetadefNamespace created = metadefs.createNamespace(MetadefNamespaceOptions.create(NS).displayName("libvirt Driver Options").visibility("public").protectedNamespace(true));
        List<? extends MetadefNamespace> all = metadefs.listNamespaces();
        metadefs.listNamespaces(Map.of("resource_types", "OS::Glance::Image"));
        metadefs.getNamespace(NS);
        metadefs.updateNamespace(NS, MetadefNamespaceOptions.update(NS).description("changed"));
        metadefs.deleteNamespace(NS);

        RecordedRequest create = expect("POST", "/v2/metadefs/namespaces");
        Assert.assertEquals(body(create).get("namespace").asText(), NS);
        Assert.assertTrue(body(create).get("protected").asBoolean());
        Assert.assertFalse(body(create).has("description"));
        expect("GET", "/v2/metadefs/namespaces");
        Assert.assertTrue(decodedPath(takeRequest()).endsWith("/v2/metadefs/namespaces?resource_types=OS::Glance::Image"));
        expect("GET", "/v2/metadefs/namespaces/" + NS);
        RecordedRequest update = expect("PUT", "/v2/metadefs/namespaces/" + NS);
        Assert.assertEquals(body(update).get("namespace").asText(), NS);
        Assert.assertEquals(body(update).get("description").asText(), "changed");
        expect("DELETE", "/v2/metadefs/namespaces/" + NS);
        Assert.assertTrue(created.isProtected());
        Assert.assertEquals(created.getDisplayName(), "libvirt Driver Options");
        Assert.assertEquals(((Map<?, ?>) created.getProperties().get("boot_menu")).get("title"), "Boot Menu");
        Assert.assertEquals(all.get(0).getResourceTypeAssociations().get(0).get("prefix"), "hw_");
    }

    public void resourceTypes() throws Exception {
        respondWith(200, "{\"resource_types\": [{\"created_at\": \"2014-08-28T18:13:04Z\", \"name\": \"OS::Glance::Image\", \"updated_at\": \"2014-08-28T18:13:04Z\"}]}");
        respondWith(200, "{\"resource_type_associations\": [{\"created_at\": \"2018-03-05T18:20:44Z\", \"name\": \"OS::Nova::Flavor\", \"prefix\": \"hw:\"}]}");
        respondWith(201, "{\"created_at\": \"2014-09-19T16:09:13Z\", \"name\": \"OS::Cinder::Volume\", \"prefix\": \"hw_\", \"properties_target\": \"image\", \"updated_at\": \"2014-09-19T16:09:13Z\"}");
        respondWith(204);

        var metadefs = osv3().imagesV2().metadefs();
        Assert.assertEquals(metadefs.listResourceTypes().get(0).getName(), "OS::Glance::Image");
        Assert.assertEquals(metadefs.listResourceTypeAssociations(NS).get(0).getPrefix(), "hw:");
        MetadefResourceTypeAssociation association = metadefs.associateResourceType(NS, "OS::Cinder::Volume", "hw_", "image");
        Assert.assertTrue(metadefs.dissociateResourceType(NS, "OS::Cinder::Volume").isSuccess());

        expect("GET", "/v2/metadefs/resource_types");
        expect("GET", "/v2/metadefs/namespaces/" + NS + "/resource_types");
        RecordedRequest associate = expect("POST", "/v2/metadefs/namespaces/" + NS + "/resource_types");
        Assert.assertEquals(body(associate).get("name").asText(), "OS::Cinder::Volume");
        Assert.assertEquals(body(associate).get("properties_target").asText(), "image");
        expect("DELETE", "/v2/metadefs/namespaces/" + NS + "/resource_types/OS::Cinder::Volume");
        Assert.assertEquals(association.getPropertiesTarget(), "image");
    }
}
