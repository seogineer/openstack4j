package org.openstack4j.api.network;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;
import org.openstack4j.api.Builders;
import org.openstack4j.api.network.ext2.AbstractNetworkingExtTest;
import org.openstack4j.model.network.Port;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Port extra_dhcp_opts are sent on create and update, each option key once, and read back with ip_version. */
@Test(suiteName = "Network/PortExtraDhcpOpts")
public class NeutronPortExtraDhcpOptTests extends AbstractNetworkingExtTest {

    private static final String PORT_OBJ = "{\"id\":\"p1\",\"network_id\":\"n1\",\"name\":\"port\",\"admin_state_up\":true,"
            + "\"extra_dhcp_opts\":[{\"opt_name\":\"bootfile-name\",\"opt_value\":\"pxelinux.0\",\"ip_version\":4},"
            + "{\"opt_name\":\"tftp-server\",\"opt_value\":\"fd00::1\",\"ip_version\":6}]}";
    private static final String PORT_WITH_OPTS = "{\"port\":" + PORT_OBJ + "}";
    private static final String PORTS_WITH_OPTS = "{\"ports\":[" + PORT_OBJ + "]}";
    private static final String PORT_PLAIN = "{\"port\":{\"id\":\"p1\",\"network_id\":\"n1\",\"name\":\"port\"}}";

    private static Set<String> keys(JsonNode node) {
        Set<String> keys = new TreeSet<>();
        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) keys.add(it.next());
        return keys;
    }

    private static Set<String> set(String... names) {
        return new TreeSet<>(Arrays.asList(names));
    }

    private static Port portWithOpt() {
        return Builders.port().networkId("n1")
                .extraDhcpOpt(Builders.extraDhcpOpt().optName("bootfile-name").optValue("pxelinux.0").build()).build();
    }

    public void createSendsExtraDhcpOpts() throws Exception {
        respondWith(201, PORT_WITH_OPTS);
        osv3().networking().port().create(portWithOpt());

        JsonNode opts = body(expect("POST", "/ports")).get("port").get("extra_dhcp_opts");
        Assert.assertNotNull(opts, "extra_dhcp_opts must be sent");
        Assert.assertEquals(opts.size(), 1);
        Assert.assertEquals(keys(opts.get(0)), set("opt_name", "opt_value"), opts.toString());
        Assert.assertEquals(opts.get(0).get("opt_name").asText(), "bootfile-name");
        Assert.assertEquals(opts.get(0).get("opt_value").asText(), "pxelinux.0");
    }

    public void bulkCreateSendsExtraDhcpOpts() throws Exception {
        respondWith(201, PORTS_WITH_OPTS);
        Port plain = Builders.port().networkId("n2").build();
        osv3().networking().port().create(Arrays.asList(portWithOpt(), plain));

        JsonNode ports = body(expect("POST", "/ports")).get("ports");
        Assert.assertEquals(ports.size(), 2);
        JsonNode opts = ports.get(0).get("extra_dhcp_opts");
        Assert.assertNotNull(opts, ports.toString());
        Assert.assertEquals(opts.size(), 1);
        Assert.assertEquals(keys(opts.get(0)), set("opt_name", "opt_value"), opts.toString());
        Assert.assertFalse(ports.get(1).has("extra_dhcp_opts"), ports.get(1).toString());
    }

    public void builderIpVersionIsSent() throws Exception {
        respondWith(201, PORT_WITH_OPTS);
        osv3().networking().port().create(Builders.port().networkId("n1")
                .extraDhcpOpt(Builders.extraDhcpOpt().optName("tftp-server").optValue("fd00::1").ipVersion(6).build()).build());

        JsonNode opt = body(expect("POST", "/ports")).get("port").get("extra_dhcp_opts").get(0);
        Assert.assertEquals(keys(opt), set("opt_name", "opt_value", "ip_version"), opt.toString());
        Assert.assertTrue(opt.get("ip_version").isInt(), opt.toString());
        Assert.assertEquals(opt.get("ip_version").asInt(), 6);
    }

    public void builderUpdateSendsEachKeyOnce() throws Exception {
        respondWith(200, PORT_WITH_OPTS);
        Port port = Builders.port().name("x")
                .extraDhcpOpt(Builders.extraDhcpOpt().optName("bootfile-name").optValue("pxelinux.0").build()).build();
        port.setId("p2");

        osv3().networking().port().update(port);
        JsonNode opts = body(expect("PUT", "/ports/p2")).get("port").get("extra_dhcp_opts");
        Assert.assertEquals(opts.size(), 1);
        Assert.assertEquals(keys(opts.get(0)), set("opt_name", "opt_value"), opts.toString());
    }

    public void getThenUpdateKeepsIpVersion() throws Exception {
        respondWith(200, PORT_WITH_OPTS);
        respondWith(200, PORT_WITH_OPTS);
        Port p = osv3().networking().port().get("p1");
        expect("GET", "/ports/p1");

        osv3().networking().port().update(p);
        JsonNode opts = body(expect("PUT", "/ports/p1")).get("port").get("extra_dhcp_opts");
        Assert.assertEquals(opts.size(), 2);
        for (JsonNode opt : opts) {
            Assert.assertEquals(keys(opt), set("opt_name", "opt_value", "ip_version"), opts.toString());
        }
        Assert.assertEquals(opts.get(0).get("opt_name").asText(), "bootfile-name");
        Assert.assertEquals(opts.get(0).get("opt_value").asText(), "pxelinux.0");
        Assert.assertEquals(opts.get(0).get("ip_version").asInt(), 4);
        Assert.assertEquals(opts.get(1).get("opt_name").asText(), "tftp-server");
        Assert.assertEquals(opts.get(1).get("opt_value").asText(), "fd00::1");
        Assert.assertEquals(opts.get(1).get("ip_version").asInt(), 6);
    }

    public void responseExposesExtraDhcpOpts() throws Exception {
        respondWith(200, PORT_WITH_OPTS);
        respondWith(200, PORT_PLAIN);
        respondWith(200, PORT_PLAIN);
        respondWith(201, PORT_PLAIN);
        Port p = osv3().networking().port().get("p1");
        expect("GET", "/ports/p1");
        Assert.assertEquals(p.getExtraDhcpOpts().size(), 2);
        Assert.assertEquals(p.getExtraDhcpOpts().get(0).getOptName(), "bootfile-name");
        Assert.assertEquals(p.getExtraDhcpOpts().get(0).getOptValue(), "pxelinux.0");
        Assert.assertEquals(p.getExtraDhcpOpts().get(0).getIpVersion(), Integer.valueOf(4));
        Assert.assertEquals(p.getExtraDhcpOpts().get(1).getIpVersion(), Integer.valueOf(6));

        Port plain = osv3().networking().port().get("p1");
        expect("GET", "/ports/p1");
        Assert.assertNull(plain.getExtraDhcpOpts());
        osv3().networking().port().update(plain);
        Assert.assertFalse(body(expect("PUT", "/ports/p1")).get("port").has("extra_dhcp_opts"));
        osv3().networking().port().create(Builders.port().networkId("n1").build());
        Assert.assertFalse(body(expect("POST", "/ports")).get("port").has("extra_dhcp_opts"));
    }
}
