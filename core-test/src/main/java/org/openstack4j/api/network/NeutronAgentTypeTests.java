package org.openstack4j.api.network;

import org.openstack4j.model.network.Agent;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Neutron agent_type values (neutron-lib, neutron OVN and neutron-dynamic-routing constants). */
@Test(suiteName = "Network/AgentType")
public class NeutronAgentTypeTests {

    public void newAgentTypesAreRecognized() {
        Assert.assertEquals(Agent.Type.forValue("Linux bridge agent"), Agent.Type.LINUX_BRIDGE);
        Assert.assertEquals(Agent.Type.forValue("Metering agent"), Agent.Type.METERING);
        Assert.assertEquals(Agent.Type.forValue("NIC Switch agent"), Agent.Type.NIC_SWITCH);
        Assert.assertEquals(Agent.Type.forValue("Macvtap agent"), Agent.Type.MACVTAP);
        Assert.assertEquals(Agent.Type.forValue("OFA driver agent"), Agent.Type.OFA);
        Assert.assertEquals(Agent.Type.forValue("OVN Controller agent"), Agent.Type.OVN_CONTROLLER);
        Assert.assertEquals(Agent.Type.forValue("OVN Controller Gateway agent"), Agent.Type.OVN_CONTROLLER_GATEWAY);
        Assert.assertEquals(Agent.Type.forValue("OVN Metadata agent"), Agent.Type.OVN_METADATA);
        Assert.assertEquals(Agent.Type.forValue("OVN Neutron agent"), Agent.Type.OVN_NEUTRON);
        Assert.assertEquals(Agent.Type.forValue("BGP dynamic routing agent"), Agent.Type.BGP_DYNAMIC_ROUTING);
        Assert.assertEquals(Agent.Type.forValue("ovn controller agent"), Agent.Type.OVN_CONTROLLER);
    }

    public void existingTypesAndFallbackUnchanged() {
        Assert.assertEquals(Agent.Type.forValue("DHCP agent"), Agent.Type.DHCP);
        Assert.assertEquals(Agent.Type.forValue("L3 agent"), Agent.Type.L3);
        Assert.assertEquals(Agent.Type.forValue("Open vSwitch agent"), Agent.Type.OPEN_VSWITCH);
        Assert.assertEquals(Agent.Type.forValue("Metadata agent"), Agent.Type.METADATA);
        Assert.assertEquals(Agent.Type.forValue("Something new agent"), Agent.Type.UNRECOGNIZED);
        Assert.assertEquals(Agent.Type.forValue(null), Agent.Type.UNRECOGNIZED);
        for (Agent.Type type : Agent.Type.values()) {
            if (type != Agent.Type.UNRECOGNIZED) {
                Assert.assertEquals(Agent.Type.forValue(type.value()), type);
            }
        }
    }
}
