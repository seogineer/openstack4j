package org.openstack4j.api.ironic;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.ironic.BaremetalPatch;
import org.openstack4j.model.ironic.Chassis;
import org.openstack4j.model.ironic.Driver;
import org.openstack4j.model.ironic.options.ChassisCreate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Ironic/ChassisDrivers")
public class ChassisDriverTests extends AbstractIronicTest {

    private static final String CHASSIS = "{\"uuid\": \"dff29d23-1ded-43b4-8ae1-5eebb3e30de1\", \"description\": \"Sample chassis\", \"extra\": {}, \"links\": [], \"nodes\": []}";

    public void chassisLifecycle() throws Exception {
        respondWith(201, CHASSIS);
        respondWith(200, "{\"chassis\": [" + CHASSIS + "]}");
        respondWith(200, "{\"chassis\": []}");
        respondWith(200, "{\"nodes\": [{\"uuid\": \"n1\"}]}");
        respondWith(200, CHASSIS);
        respondWith(200, CHASSIS);
        respondWith(204);

        var chassis = osv3().ironic().chassis();
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

        var drivers = osv3().ironic().drivers();
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
        Assert.assertNull(osv3().ironic().drivers().get("nope"));
        takeRequest();
    }

    public void driverVendorPassthru() throws Exception {
        respondWith(200, "{\"lookup\": {\"http_methods\": [\"POST\"], \"async\": false, \"description\": \"\", \"attach\": false}}");
        respondWith(202);
        respondWith(200, "{\"power\": \"on\"}");
        respondWith(202);
        respondWith(204);
        respondWith(200, "\"done\"");
        respondWith(200, "[1, 2]");

        var drivers = osv3().ironic().drivers();
        Map<String, Object> methods = drivers.listVendorPassthruMethods("ipmi");
        var posted = drivers.vendorPassthru("ipmi", "lookup", Map.of("a", 1));
        Map<String, Object> status = drivers.vendorPassthru("ipmi", HttpMethod.GET, "status", null);
        Map<String, Object> reset = drivers.vendorPassthru("ipmi", HttpMethod.POST, "reset", null);
        Map<String, Object> cache = drivers.vendorPassthru("ipmi", HttpMethod.DELETE, "cache", Map.of());
        Map<String, Object> name = drivers.vendorPassthru("ipmi", HttpMethod.PUT, "name", Map.of("v", "x"));
        Map<String, Object> list = drivers.vendorPassthru("ipmi", HttpMethod.PATCH, "list", Map.of("k", 1));

        expect("GET", "/v1/drivers/ipmi/vendor_passthru/methods");
        Assert.assertEquals(body(expect("POST", "/v1/drivers/ipmi/vendor_passthru?method=lookup")).toString(), "{\"a\":1}");
        RecordedRequest get = expect("GET", "/v1/drivers/ipmi/vendor_passthru?method=status");
        Assert.assertEquals(get.getBody().size(), 0);
        Assert.assertEquals(body(expect("POST", "/v1/drivers/ipmi/vendor_passthru?method=reset")).toString(), "{}");
        RecordedRequest delete = expect("DELETE", "/v1/drivers/ipmi/vendor_passthru?method=cache");
        Assert.assertEquals(delete.getBody().size(), 0);
        Assert.assertEquals(body(expect("PUT", "/v1/drivers/ipmi/vendor_passthru?method=name")).toString(), "{\"v\":\"x\"}");
        Assert.assertEquals(body(expect("PATCH", "/v1/drivers/ipmi/vendor_passthru?method=list")).toString(), "{\"k\":1}");
        Assert.assertTrue(methods.containsKey("lookup"));
        Assert.assertTrue(posted.isSuccess());
        Assert.assertEquals(status.get("power"), "on");
        Assert.assertNull(reset);
        Assert.assertNull(cache);
        Assert.assertEquals(name, Map.of("return", "done"));
        Assert.assertEquals(list.get("return"), List.of(1, 2));
    }

    public void driverPassthruRejectsBadInput() throws Exception {
        var drivers = osv3().ironic().drivers();
        Assert.assertThrows(IllegalArgumentException.class, () -> drivers.vendorPassthru("ipmi", HttpMethod.HEAD, "m", null));
        Assert.assertThrows(IllegalArgumentException.class, () -> drivers.vendorPassthru("ipmi", HttpMethod.GET, "m", Map.of("a", 1)));
        Assert.assertThrows(IllegalArgumentException.class, () -> drivers.vendorPassthru("ipmi", HttpMethod.POST, "a/b", null));
        Assert.assertThrows(NullPointerException.class, () -> drivers.vendorPassthru("ipmi", HttpMethod.POST, null, null));
        Assert.assertThrows(NullPointerException.class, () -> drivers.vendorPassthru("ipmi", null, "m", null));
        Assert.assertThrows(IllegalArgumentException.class, () -> drivers.vendorPassthru("..", HttpMethod.GET, "m", null));
        Assert.assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS), "unexpected extra request");
    }

    public void missingDriverPassthruRaises() throws Exception {
        respondWith(404, "{\"error_message\": \"not found\"}");
        respondWith(404, "{\"error_message\": \"not found\"}");
        respondWith(404, "{\"error_message\": \"not found\"}");

        var drivers = osv3().ironic().drivers();
        Assert.assertThrows(ResponseException.class, () -> drivers.listVendorPassthruMethods("nope"));
        ResponseException e = Assert.expectThrows(ResponseException.class, () -> drivers.vendorPassthru("nope", HttpMethod.GET, "m", null));
        Assert.assertEquals(e.getStatus(), 404);
        var failed = drivers.vendorPassthru("nope", "m", null);
        Assert.assertFalse(failed.isSuccess());
        Assert.assertEquals(failed.getCode(), 404);
        expect("GET", "/v1/drivers/nope/vendor_passthru/methods");
        expect("GET", "/v1/drivers/nope/vendor_passthru?method=m");
        Assert.assertEquals(body(expect("POST", "/v1/drivers/nope/vendor_passthru?method=m")).toString(), "{}");
    }

    public void driverNameIsPathEncoded() throws Exception {
        respondWith(200, "{}");
        osv3().ironic().drivers().listVendorPassthruMethods("my driver");
        Assert.assertTrue(takeRequest().getPath().endsWith("/v1/drivers/my%20driver/vendor_passthru/methods"));
    }
}
