package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Chassis;
import org.openstack4j.model.baremetal.Driver;
import org.openstack4j.model.baremetal.options.ChassisCreate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Baremetal/ChassisDrivers")
public class ChassisDriverTests extends AbstractBaremetalTest {

    private static final String CHASSIS = "{\"uuid\": \"dff29d23-1ded-43b4-8ae1-5eebb3e30de1\", \"description\": \"Sample chassis\", \"extra\": {}, \"links\": [], \"nodes\": []}";

    public void chassisLifecycle() throws Exception {
        respondWith(201, CHASSIS);
        respondWith(200, "{\"chassis\": [" + CHASSIS + "]}");
        respondWith(200, "{\"chassis\": []}");
        respondWith(200, "{\"nodes\": [{\"uuid\": \"n1\"}]}");
        respondWith(200, CHASSIS);
        respondWith(200, CHASSIS);
        respondWith(204);

        var chassis = osv3().baremetal().chassis();
        Chassis created = chassis.create(ChassisCreate.create().description("Sample chassis"));
        List<? extends Chassis> detail = chassis.listDetail();
        chassis.list(Map.of("limit", "10"));
        Assert.assertEquals(chassis.listNodes("dff29d23").get(0).getUuid(), "n1");
        chassis.get("dff29d23");
        chassis.update("dff29d23", List.of(BaremetalPatch.replace("/description", "rack 3")));
        Assert.assertTrue(chassis.delete("dff29d23").isSuccess());

        Assert.assertEquals(body(expect("POST", "/v1/chassis")).toString(), "{\"description\":\"Sample chassis\"}");
        expect("GET", "/v1/chassis/detail");
        expect("GET", "/v1/chassis?limit=10");
        expect("GET", "/v1/chassis/dff29d23/nodes/detail");
        expect("GET", "/v1/chassis/dff29d23");
        expect("PATCH", "/v1/chassis/dff29d23");
        expect("DELETE", "/v1/chassis/dff29d23");
        Assert.assertEquals(created.getDescription(), "Sample chassis");
        Assert.assertEquals(detail.get(0).getUuid(), "dff29d23-1ded-43b4-8ae1-5eebb3e30de1");
        Assert.assertNotNull(detail.get(0).getAttributes().get("nodes"));
    }

    public void drivers() throws Exception {
        respondWith(200, "{\"drivers\": [{\"name\": \"ipmi\", \"hosts\": [\"897ab1dad809\"], \"type\": \"dynamic\", \"links\": []}]}");
        respondWith(200, "{\"name\": \"ipmi\", \"hosts\": [\"897ab1dad809\"], \"type\": \"dynamic\", \"default_boot_interface\": \"pxe\"}");
        respondWith(200, "{\"ipmi_address\": \"IP address or hostname of the node. Required.\"}");
        respondWith(200, "{\"raid_level\": \"RAID level for the logical disk. Required.\"}");

        var drivers = osv3().baremetal().drivers();
        List<? extends Driver> all = drivers.list(Map.of("type", "dynamic"));
        Driver ipmi = drivers.get("ipmi");
        Map<String, String> properties = drivers.getProperties("ipmi");
        Map<String, Object> raid = drivers.getRaidLogicalDiskProperties("ipmi");

        expect("GET", "/v1/drivers?type=dynamic");
        expect("GET", "/v1/drivers/ipmi");
        expect("GET", "/v1/drivers/ipmi/properties");
        expect("GET", "/v1/drivers/ipmi/raid/logical_disk_properties");
        Assert.assertEquals(all.get(0).getHosts(), List.of("897ab1dad809"));
        Assert.assertEquals(ipmi.getType(), "dynamic");
        Assert.assertEquals(ipmi.getAttributes().get("default_boot_interface"), "pxe");
        Assert.assertTrue(properties.get("ipmi_address").startsWith("IP address"));
        Assert.assertTrue(raid.containsKey("raid_level"));
    }

    public void missingDriverIsNull() throws Exception {
        respondWith(404, "{\"error_message\": \"not found\"}");
        Assert.assertNull(osv3().baremetal().drivers().get("nope"));
        takeRequest();
    }
}
