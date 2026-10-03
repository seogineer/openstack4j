package org.openstack4j.api.network.ext2;

import java.util.Map;

import org.openstack4j.model.network.options.PortForwardingUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/Attributes")
public class NeutronAttributesTests {

    public void unsetFieldsAreOmitted() {
        Map<String, Object> map = PortForwardingUpdate.create().internalPort(8080).description(null).toMap();
        Assert.assertEquals(map, Map.of("internal_port", 8080));
    }

    public void explicitAttributeKeepsNull() {
        Map<String, Object> map = PortForwardingUpdate.create().attribute("description", null).toMap();
        Assert.assertTrue(map.containsKey("description"));
        Assert.assertNull(map.get("description"));
    }

    public void toMapIsACopy() {
        PortForwardingUpdate update = PortForwardingUpdate.create().protocol("tcp");
        update.toMap().put("protocol", "udp");
        Assert.assertEquals(update.toMap().get("protocol"), "tcp");
    }
}
