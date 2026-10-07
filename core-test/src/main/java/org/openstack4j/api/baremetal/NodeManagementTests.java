package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.baremetal.NodeService;
import org.openstack4j.model.baremetal.NodeStates;
import org.openstack4j.model.baremetal.options.NodeProvision;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Baremetal/NodeManagement")
public class NodeManagementTests extends AbstractBaremetalTest {

    private static final String STATES = "{\"console_enabled\": false, \"last_error\": null, \"power_state\": \"power off\", \"provision_state\": \"available\","
            + " \"provision_updated_at\": \"2016-08-18T22:28:49.946416+00:00\", \"raid_config\": {}, \"target_power_state\": null, \"target_provision_state\": null,"
            + " \"target_raid_config\": {}, \"boot_mode\": \"uefi\", \"secure_boot\": true}";

    private NodeService nodes() {
        return osv3().baremetal().nodes();
    }

    public void statesAndStateChanges() throws Exception {
        respondWith(200, STATES);
        for (int i = 0; i < 5; i++)
            respondWith(202);
        respondWith(204);

        NodeStates states = nodes().getStates("n1");
        Assert.assertTrue(nodes().setPowerState("n1", "power on").isSuccess());
        Assert.assertTrue(nodes().setPowerState("n1", "soft power off", 300).isSuccess());
        Assert.assertTrue(nodes().setProvisionState("n1", NodeProvision.target("active").configDrive("http://example.com/cd.iso")).isSuccess());
        Assert.assertTrue(nodes().setBootMode("n1", "bios").isSuccess());
        Assert.assertTrue(nodes().setSecureBoot("n1", false).isSuccess());
        Assert.assertTrue(nodes().setRaidConfig("n1", Map.of("logical_disks", List.of(Map.of("size_gb", 100, "raid_level", "1")))).isSuccess());

        expect("GET", "/v1/nodes/n1/states");
        Assert.assertEquals(body(expect("PUT", "/v1/nodes/n1/states/power")).toString(), "{\"target\":\"power on\"}");
        Assert.assertEquals(body(expect("PUT", "/v1/nodes/n1/states/power")).toString(), "{\"target\":\"soft power off\",\"timeout\":300}");
        Assert.assertEquals(body(expect("PUT", "/v1/nodes/n1/states/provision")).toString(), "{\"target\":\"active\",\"configdrive\":\"http://example.com/cd.iso\"}");
        Assert.assertEquals(body(expect("PUT", "/v1/nodes/n1/states/boot_mode")).toString(), "{\"target\":\"bios\"}");
        Assert.assertEquals(body(expect("PUT", "/v1/nodes/n1/states/secure_boot")).toString(), "{\"target\":false}");
        Assert.assertEquals(body(expect("PUT", "/v1/nodes/n1/states/raid")).get("logical_disks").get(0).get("raid_level").asText(), "1");

        Assert.assertEquals(states.getPowerState(), "power off");
        Assert.assertEquals(states.getProvisionState(), "available");
        Assert.assertEquals(states.getBootMode(), "uefi");
        Assert.assertEquals(states.getSecureBoot(), Boolean.TRUE);
        Assert.assertEquals(states.getConsoleEnabled(), Boolean.FALSE);
        Assert.assertNull(states.getLastError());
        Assert.assertEquals(states.getAttributes().get("provision_updated_at"), "2016-08-18T22:28:49.946416+00:00");
    }

    public void consoleBootDeviceNmiValidateMaintenance() throws Exception {
        respondWith(200, "{\"console_enabled\": true, \"console_info\": {\"type\": \"shellinabox\", \"url\": \"http://127.0.0.1:4321\"}}");
        respondWith(202);
        respondWith(200, "{\"boot_device\": \"pxe\", \"persistent\": false}");
        respondWith(204);
        respondWith(200, "{\"supported_boot_devices\": [\"pxe\", \"disk\"]}");
        respondWith(204);
        respondWith(200, "{\"boot\": {\"result\": true, \"reason\": null}, \"deploy\": {\"result\": false, \"reason\": \"missing ipmi_address\"}}");
        respondWith(202);
        respondWith(202);

        Map<String, Object> console = nodes().getConsole("n1");
        Assert.assertTrue(nodes().setConsoleEnabled("n1", false).isSuccess());
        Map<String, Object> bootDevice = nodes().getBootDevice("n1");
        Assert.assertTrue(nodes().setBootDevice("n1", "disk", true).isSuccess());
        List<String> supported = nodes().getSupportedBootDevices("n1");
        Assert.assertTrue(nodes().injectNmi("n1").isSuccess());
        Map<String, Map<String, Object>> validation = nodes().validate("n1");
        Assert.assertTrue(nodes().setMaintenance("n1", "replacing disk").isSuccess());
        Assert.assertTrue(nodes().unsetMaintenance("n1").isSuccess());

        expect("GET", "/v1/nodes/n1/states/console");
        Assert.assertEquals(body(expect("PUT", "/v1/nodes/n1/states/console")).toString(), "{\"enabled\":false}");
        expect("GET", "/v1/nodes/n1/management/boot_device");
        Assert.assertEquals(body(expect("PUT", "/v1/nodes/n1/management/boot_device")).toString(), "{\"boot_device\":\"disk\",\"persistent\":true}");
        expect("GET", "/v1/nodes/n1/management/boot_device/supported");
        Assert.assertEquals(body(expect("PUT", "/v1/nodes/n1/management/inject_nmi")).toString(), "{}");
        expect("GET", "/v1/nodes/n1/validate");
        Assert.assertEquals(body(expect("PUT", "/v1/nodes/n1/maintenance")).toString(), "{\"reason\":\"replacing disk\"}");
        expect("DELETE", "/v1/nodes/n1/maintenance");

        Assert.assertEquals(console.get("console_enabled"), Boolean.TRUE);
        Assert.assertEquals(bootDevice.get("boot_device"), "pxe");
        Assert.assertEquals(supported, List.of("pxe", "disk"));
        Assert.assertEquals(validation.get("deploy").get("reason"), "missing ipmi_address");
    }

    public void maintenanceWithoutReasonSendsEmptyBody() throws Exception {
        respondWith(202);
        nodes().setMaintenance("n1", null);
        Assert.assertEquals(body(expect("PUT", "/v1/nodes/n1/maintenance")).toString(), "{}");
    }

    public void actionOnMissingNodeFailsAndStatesRaise() throws Exception {
        String missing = "{\"error_message\": \"{\\\"faultstring\\\": \\\"Node n9 could not be found.\\\", \\\"faultcode\\\": \\\"Client\\\", \\\"debuginfo\\\": null}\"}";
        respondWith(404, missing);
        respondWith(404, missing);
        Assert.assertFalse(nodes().setPowerState("n9", "power on").isSuccess());
        takeRequest();
        try {
            nodes().getStates("n9");
            Assert.fail("expected a ResponseException");
        } catch (org.openstack4j.api.exceptions.ResponseException expected) {
            Assert.assertEquals(expected.getStatus(), 404);
        }
        takeRequest();
    }
}
