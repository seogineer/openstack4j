package org.openstack4j.api.image.v2.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.image.v2.ext.MetadefObject;
import org.openstack4j.model.image.v2.ext.MetadefProperty;
import org.openstack4j.model.image.v2.options.MetadefObjectOptions;
import org.openstack4j.model.image.v2.options.MetadefPropertyOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/MetadefObjects")
public class MetadefObjectPropertyTests extends AbstractImageExtTest {

    private static final String NS = "OS::Compute::Quota";
    private static final String P = "/v2/metadefs/namespaces/" + NS;

    public void objects() throws Exception {
        String object = "{\"created_at\": \"2014-09-19T18:20:56Z\", \"description\": \"CPU limits\", \"name\": \"CPU Limits\", \"required\": [],"
                + " \"properties\": {\"quota:cpu_period\": {\"description\": \"d\", \"maximum\": 1000000, \"minimum\": 1000, \"title\": \"Quota: CPU Period\", \"type\": \"integer\"}},"
                + " \"schema\": \"/v2/schemas/metadefs/object\", \"self\": \"/v2/metadefs/namespaces/" + NS + "/objects/CPU Limits\", \"updated_at\": \"2014-09-19T18:20:56Z\"}";
        respondWith(201, object);
        respondWith(200, "{\"objects\": [" + object + "], \"schema\": \"v2/schemas/metadefs/objects\"}");
        respondWith(200, object);
        respondWith(200, object);
        respondWith(204);

        var metadefs = osv3().imagesV2().metadefs();
        MetadefObject created = metadefs.createObject(NS, MetadefObjectOptions.create("CPU Limits").description("CPU limits")
                .properties(Map.of("quota:cpu_period", Map.of("type", "integer", "title", "Quota: CPU Period"))));
        List<? extends MetadefObject> all = metadefs.listObjects(NS);
        metadefs.getObject(NS, "CPU Limits");
        metadefs.updateObject(NS, "CPU Limits", MetadefObjectOptions.create("CPU Limits").required(List.of("quota:cpu_period")));
        metadefs.deleteObject(NS, "CPU Limits");

        RecordedRequest create = expect("POST", P + "/objects");
        Assert.assertEquals(body(create).get("properties").get("quota:cpu_period").get("type").asText(), "integer");
        expect("GET", P + "/objects");
        expect("GET", P + "/objects/CPU Limits");
        Assert.assertEquals(body(expect("PUT", P + "/objects/CPU Limits")).get("required").get(0).asText(), "quota:cpu_period");
        expect("DELETE", P + "/objects/CPU Limits");
        Assert.assertEquals(created.getName(), "CPU Limits");
        Assert.assertTrue(created.getRequired().isEmpty());
        Assert.assertEquals(((Map<?, ?>) all.get(0).getProperties().get("quota:cpu_period")).get("minimum"), 1000);
    }

    public void properties() throws Exception {
        String property = "{\"name\": \"hypervisor_type\", \"title\": \"Hypervisor Type\", \"description\": \"The hypervisor type.\", \"type\": \"string\","
                + " \"enum\": [\"xen\", \"qemu\", \"kvm\"]}";
        respondWith(201, property);
        respondWith(200, "{\"properties\": {\"hw_disk_bus\": {\"description\": \"d\", \"enum\": [\"scsi\", \"virtio\"], \"title\": \"Disk Bus\", \"type\": \"string\"},"
                + " \"hw_rng_model\": {\"default\": \"virtio\", \"title\": \"Random Number Generator Device\", \"type\": \"string\"}}}");
        respondWith(200, property);
        respondWith(200, property);
        respondWith(204);

        var metadefs = osv3().imagesV2().metadefs();
        MetadefProperty created = metadefs.createProperty(NS, MetadefPropertyOptions.create("hypervisor_type", "Hypervisor Type", "string")
                .description("The hypervisor type.").enumValues(List.of("xen", "qemu", "kvm")));
        Map<String, ? extends MetadefProperty> all = metadefs.listProperties(NS);
        MetadefProperty one = metadefs.getProperty(NS, "hypervisor_type");
        metadefs.updateProperty(NS, "hypervisor_type", MetadefPropertyOptions.create("hypervisor_type", "Hypervisor Type", "string").defaultValue("kvm"));
        metadefs.deleteProperty(NS, "hypervisor_type");

        RecordedRequest create = expect("POST", P + "/properties");
        Assert.assertEquals(body(create).get("enum").get(2).asText(), "kvm");
        Assert.assertEquals(body(create).get("name").asText(), "hypervisor_type");
        expect("GET", P + "/properties");
        expect("GET", P + "/properties/hypervisor_type");
        Assert.assertEquals(body(expect("PUT", P + "/properties/hypervisor_type")).get("default").asText(), "kvm");
        expect("DELETE", P + "/properties/hypervisor_type");
        Assert.assertEquals(created.getTitle(), "Hypervisor Type");
        Assert.assertEquals(created.getSchema().get("enum"), List.of("xen", "qemu", "kvm"));
        Assert.assertEquals(all.size(), 2);
        Assert.assertEquals(all.get("hw_disk_bus").getName(), "hw_disk_bus");
        Assert.assertEquals(all.get("hw_rng_model").getSchema().get("default"), "virtio");
        Assert.assertEquals(one.getType(), "string");
    }
}
