package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Port;
import org.openstack4j.model.baremetal.Portgroup;
import org.openstack4j.model.baremetal.options.PortCreate;
import org.openstack4j.model.baremetal.options.PortgroupCreate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Baremetal/Ports")
public class BaremetalPortTests extends AbstractBaremetalTest {

    private static final String PORT = "{\"uuid\": \"d2b30520-907d-46c8-bfee-c5586e6fb3a1\", \"address\": \"11:11:11:11:11:11\", \"node_uuid\": \"6d85703a-565d-469a-96ce-30b6de53079d\","
            + " \"portgroup_uuid\": null, \"pxe_enabled\": true, \"physical_network\": \"physnet1\", \"is_smartnic\": false,"
            + " \"local_link_connection\": {\"switch_id\": \"0a:1b:2c:3d:4e:5f\", \"port_id\": \"Ethernet3/1\"}, \"extra\": {}, \"internal_info\": {}, \"links\": []}";
    private static final String PORTGROUP = "{\"uuid\": \"11ee2c18-fb2a-4a0a-a2d1-0dbf56cdd5e2\", \"name\": \"bond0\", \"address\": \"11:11:11:11:11:11\","
            + " \"node_uuid\": \"6d85703a-565d-469a-96ce-30b6de53079d\", \"mode\": \"active-backup\", \"properties\": {\"miimon\": 100}, \"standalone_ports_supported\": true,"
            + " \"extra\": {}, \"internal_info\": {}, \"links\": []}";

    public void portLifecycle() throws Exception {
        respondWith(201, PORT);
        respondWith(200, "{\"ports\": [" + PORT + "]}");
        respondWith(200, "{\"ports\": [" + PORT + "]}");
        respondWith(200, "{\"ports\": []}");
        respondWith(200, "{\"ports\": []}");
        respondWith(200, PORT);
        respondWith(200, PORT);
        respondWith(204);

        var ports = osv3().baremetal().ports();
        Port created = ports.create(PortCreate.create("6d85703a-565d-469a-96ce-30b6de53079d", "11:11:11:11:11:11").pxeEnabled(true)
                .localLinkConnection(Map.of("switch_id", "0a:1b:2c:3d:4e:5f")));
        List<? extends Port> detail = ports.listDetail(Map.of("address", "11:11:11:11:11:11"));
        ports.list();
        ports.listByNode("n1");
        ports.listByPortgroup("bond0");
        ports.get("d2b30520-907d-46c8-bfee-c5586e6fb3a1");
        ports.update("d2b30520-907d-46c8-bfee-c5586e6fb3a1", List.of(BaremetalPatch.replace("/pxe_enabled", false)));
        Assert.assertTrue(ports.delete("d2b30520-907d-46c8-bfee-c5586e6fb3a1").isSuccess());

        var create = expect("POST", "/v1/ports");
        Assert.assertEquals(body(create).get("node_uuid").asText(), "6d85703a-565d-469a-96ce-30b6de53079d");
        Assert.assertTrue(body(create).get("pxe_enabled").asBoolean());
        Assert.assertFalse(body(create).has("extra"));
        Assert.assertTrue(decodedPath(expect("GET", "/v1/ports/detail?address=11:11:11:11:11:11")).contains("/v1/ports/detail"));
        expect("GET", "/v1/ports");
        expect("GET", "/v1/nodes/n1/ports/detail");
        expect("GET", "/v1/portgroups/bond0/ports/detail");
        expect("GET", "/v1/ports/d2b30520-907d-46c8-bfee-c5586e6fb3a1");
        expect("PATCH", "/v1/ports/d2b30520-907d-46c8-bfee-c5586e6fb3a1");
        expect("DELETE", "/v1/ports/d2b30520-907d-46c8-bfee-c5586e6fb3a1");

        Assert.assertEquals(created.getAddress(), "11:11:11:11:11:11");
        Assert.assertEquals(created.isPxeEnabled(), Boolean.TRUE);
        Assert.assertEquals(created.getLocalLinkConnection().get("port_id"), "Ethernet3/1");
        Assert.assertEquals(detail.get(0).getPhysicalNetwork(), "physnet1");
        Assert.assertNotNull(detail.get(0).getAttributes().get("links"));
    }

    public void portgroupLifecycle() throws Exception {
        respondWith(201, PORTGROUP);
        respondWith(200, "{\"portgroups\": [" + PORTGROUP + "]}");
        respondWith(200, "{\"portgroups\": []}");
        respondWith(200, PORTGROUP);
        respondWith(200, PORTGROUP);
        respondWith(204);

        var groups = osv3().baremetal().portgroups();
        Portgroup created = groups.create(PortgroupCreate.create("6d85703a-565d-469a-96ce-30b6de53079d").name("bond0").mode("active-backup")
                .properties(Map.of("miimon", 100)));
        List<? extends Portgroup> all = groups.listDetail();
        groups.listByNode("n1");
        groups.get("bond0");
        groups.update("bond0", List.of(BaremetalPatch.add("/extra/rack", "3")));
        Assert.assertTrue(groups.delete("bond0").isSuccess());

        Assert.assertEquals(body(expect("POST", "/v1/portgroups")).get("mode").asText(), "active-backup");
        expect("GET", "/v1/portgroups/detail");
        expect("GET", "/v1/nodes/n1/portgroups/detail");
        expect("GET", "/v1/portgroups/bond0");
        expect("PATCH", "/v1/portgroups/bond0");
        expect("DELETE", "/v1/portgroups/bond0");

        Assert.assertEquals(created.getName(), "bond0");
        Assert.assertEquals(created.getProperties().get("miimon"), 100);
        Assert.assertEquals(all.get(0).isStandalonePortsSupported(), Boolean.TRUE);
    }

    public void missingPortIsNull() throws Exception {
        respondWith(404, "{\"error_message\": \"not found\"}");
        Assert.assertNull(osv3().baremetal().ports().get("missing"));
        takeRequest();
    }
}
