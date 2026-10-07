package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Node;
import org.openstack4j.model.baremetal.options.NodeCreate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Baremetal/Nodes")
public class NodeTests extends AbstractBaremetalTest {

    private static final String NODE = "{\"uuid\": \"6d85703a-565d-469a-96ce-30b6de53079d\", \"name\": \"test_node\", \"driver\": \"ipmi\", \"provision_state\": \"enroll\","
            + " \"power_state\": null, \"maintenance\": false, \"resource_class\": \"bm-large\", \"driver_info\": {\"ipmi_address\": \"192.0.2.1\"}, \"properties\": {\"cpus\": 8},"
            + " \"traits\": [], \"boot_interface\": \"pxe\", \"clean_step\": {}, \"links\": []}";

    public void nodeLifecycle() throws Exception {
        respondWith(201, NODE);
        respondWith(200, "{\"nodes\": [" + NODE + "]}");
        respondWith(200, "{\"nodes\": []}");
        respondWith(200, NODE);
        respondWith(200, NODE);
        respondWith(204);

        var nodes = osv3().baremetal().nodes();
        Node created = nodes.create(NodeCreate.create("ipmi").name("test_node").resourceClass("bm-large").driverInfo(Map.of("ipmi_address", "192.0.2.1")));
        List<? extends Node> detail = nodes.listDetail();
        nodes.list(Map.of("provision_state", "available", "maintenance", "false"));
        nodes.get("test_node");
        Node updated = nodes.update("test_node", List.of(BaremetalPatch.replace("/description", "rack 3"), BaremetalPatch.add("/properties/memory_mb", 4096),
                BaremetalPatch.remove("/extra/old")));
        Assert.assertTrue(nodes.delete("test_node").isSuccess());

        RecordedRequest create = expect("POST", "/v1/nodes");
        Assert.assertEquals(body(create).get("driver").asText(), "ipmi");
        Assert.assertEquals(body(create).get("driver_info").get("ipmi_address").asText(), "192.0.2.1");
        Assert.assertFalse(body(create).has("description"));
        expect("GET", "/v1/nodes/detail");
        String query = decodedPath(takeRequest());
        Assert.assertTrue(query.contains("provision_state=available") && query.contains("maintenance=false"), query);
        expect("GET", "/v1/nodes/test_node");
        RecordedRequest patch = expect("PATCH", "/v1/nodes/test_node");
        Assert.assertEquals(body(patch).toString(), "[{\"op\":\"replace\",\"path\":\"/description\",\"value\":\"rack 3\"},"
                + "{\"op\":\"add\",\"path\":\"/properties/memory_mb\",\"value\":4096},{\"op\":\"remove\",\"path\":\"/extra/old\"}]");
        expect("DELETE", "/v1/nodes/test_node");
        Assert.assertEquals(created.getUuid(), "6d85703a-565d-469a-96ce-30b6de53079d");
        Assert.assertEquals(created.getProvisionState(), "enroll");
        Assert.assertEquals(detail.get(0).getProperties().get("cpus"), 8);
        Assert.assertEquals(detail.get(0).getAttributes().get("boot_interface"), "pxe");
        Assert.assertEquals(updated.getResourceClass(), "bm-large");
    }

    public void getMissingIsNullListMissingRaises() throws Exception {
        respondWith(404, "{\"error_message\": \"{\\\"faultstring\\\": \\\"Node missing could not be found.\\\", \\\"faultcode\\\": \\\"Client\\\", \\\"debuginfo\\\": null}\"}");
        Assert.assertNull(osv3().baremetal().nodes().get("missing"));
        takeRequest();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void updateNeedsAPatch() {
        osv3().baremetal().nodes().update("test_node", List.of());
    }

    public void replaceWithNullSendsValue() throws Exception {
        respondWith(200, NODE);
        osv3().baremetal().nodes().update("test_node", java.util.Arrays.asList(BaremetalPatch.replace("/description", null)));
        Assert.assertEquals(body(expect("PATCH", "/v1/nodes/test_node")).toString(), "[{\"op\":\"replace\",\"path\":\"/description\",\"value\":null}]");
    }

    @Test(expectedExceptions = org.openstack4j.api.exceptions.ResponseException.class)
    public void listDetailRaisesOn404() throws Exception {
        respondWith(404, "{\"error_message\": \"not found\"}");
        try {
            osv3().baremetal().nodes().listDetail();
        } finally {
            takeRequest();
        }
    }
}
