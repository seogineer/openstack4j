package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.baremetal.NodeService;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Baremetal/NodeDetails")
public class NodeDetailTests extends AbstractBaremetalTest {

    private NodeService nodes() {
        return osv3().baremetal().nodes();
    }

    public void biosFirmwareHistoryInventoryChildren() throws Exception {
        respondWith(200, "{\"bios\": [{\"name\": \"virtualization\", \"value\": \"Enabled\", \"links\": []}]}");
        respondWith(200, "{\"virtualization\": {\"name\": \"virtualization\", \"value\": \"Enabled\"}}");
        respondWith(200, "{\"firmware\": [{\"component\": \"bios\", \"current_version\": \"v1.0.0\", \"initial_version\": \"v1.0.0\"}]}");
        respondWith(200, "{\"history\": [{\"uuid\": \"e1\", \"event\": \"power on\", \"severity\": \"INFO\"}], \"next\": null}");
        respondWith(200, "{\"uuid\": \"e1\", \"event\": \"power on\", \"severity\": \"INFO\"}");
        respondWith(200, "{\"inventory\": {\"cpu\": {\"count\": 8}}, \"plugin_data\": {\"macs\": []}}");
        respondWith(200, "{\"children\": [\"c1\", \"c2\"]}");

        List<Map<String, Object>> bios = nodes().listBiosSettings("n1");
        Map<String, Object> virtualization = nodes().getBiosSetting("n1", "virtualization");
        List<Map<String, Object>> firmware = nodes().listFirmwareComponents("n1");
        List<Map<String, Object>> history = nodes().listHistory("n1");
        Map<String, Object> event = nodes().getHistoryEvent("n1", "e1");
        Map<String, Object> inventory = nodes().getInventory("n1");
        List<String> children = nodes().listChildren("n1");

        expect("GET", "/v1/nodes/n1/bios");
        expect("GET", "/v1/nodes/n1/bios/virtualization");
        expect("GET", "/v1/nodes/n1/firmware");
        expect("GET", "/v1/nodes/n1/history");
        expect("GET", "/v1/nodes/n1/history/e1");
        expect("GET", "/v1/nodes/n1/inventory");
        expect("GET", "/v1/nodes/n1/children");
        Assert.assertEquals(bios.get(0).get("value"), "Enabled");
        Assert.assertEquals(virtualization.get("value"), "Enabled");
        Assert.assertEquals(firmware.get(0).get("component"), "bios");
        Assert.assertEquals(history.get(0).get("event"), "power on");
        Assert.assertEquals(event.get("severity"), "INFO");
        Assert.assertTrue(inventory.containsKey("plugin_data"));
        Assert.assertEquals(children, List.of("c1", "c2"));
    }

    public void virtualMediaIndicatorsPassthru() throws Exception {
        respondWith(204);
        respondWith(204);
        respondWith(204);
        respondWith(200, "{\"components\": [{\"name\": \"system\", \"links\": []}]}");
        respondWith(200, "{\"indicators\": [{\"name\": \"power\", \"component\": \"system\", \"readonly\": true, \"states\": [\"ON\", \"OFF\"]}]}");
        respondWith(200, "{\"state\": \"ON\"}");
        respondWith(204);
        respondWith(200, "{\"lookup\": {\"http_methods\": [\"POST\"], \"async\": false, \"description\": \"\", \"attach\": false}}");
        respondWith(202);

        Assert.assertTrue(nodes().attachVirtualMedia("n1", "CDROM", "http://example.com/a.iso", Map.of("image_download_source", "http")).isSuccess());
        Assert.assertTrue(nodes().detachVirtualMedia("n1").isSuccess());
        Assert.assertTrue(nodes().detachVirtualMedia("n1", "CDROM").isSuccess());
        List<Map<String, Object>> components = nodes().listIndicatorComponents("n1");
        List<Map<String, Object>> indicators = nodes().listIndicators("n1", "system");
        String state = nodes().getIndicatorState("n1", "power@system");
        Assert.assertTrue(nodes().setIndicatorState("n1", "power@system", "BLINKING").isSuccess());
        Map<String, Object> methods = nodes().listVendorPassthruMethods("n1");
        Assert.assertTrue(nodes().vendorPassthru("n1", "lookup", Map.of("a", 1)).isSuccess());

        Assert.assertEquals(body(expect("POST", "/v1/nodes/n1/vmedia")).toString(),
                "{\"device_type\":\"CDROM\",\"image_url\":\"http://example.com/a.iso\",\"image_download_source\":\"http\"}");
        expect("DELETE", "/v1/nodes/n1/vmedia");
        expect("DELETE", "/v1/nodes/n1/vmedia?device_types=CDROM");
        expect("GET", "/v1/nodes/n1/management/indicators");
        expect("GET", "/v1/nodes/n1/management/indicators/system");
        expect("GET", "/v1/nodes/n1/management/indicators/power@system");
        Assert.assertEquals(body(expect("PUT", "/v1/nodes/n1/management/indicators/power@system")).toString(), "{\"state\":\"BLINKING\"}");
        expect("GET", "/v1/nodes/n1/vendor_passthru/methods");
        Assert.assertEquals(body(expect("POST", "/v1/nodes/n1/vendor_passthru?method=lookup")).toString(), "{\"a\":1}");
        Assert.assertEquals(components.get(0).get("name"), "system");
        Assert.assertEquals(indicators.get(0).get("readonly"), Boolean.TRUE);
        Assert.assertEquals(state, "ON");
        Assert.assertTrue(methods.containsKey("lookup"));
    }

    public void missingBiosSettingIsNull() throws Exception {
        respondWith(404, "{\"error_message\": \"not found\"}");
        Assert.assertNull(nodes().getBiosSetting("n1", "nope"));
        takeRequest();
    }
}
