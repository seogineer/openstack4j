package org.openstack4j.api.compute;

import java.util.List;

import org.openstack4j.api.AbstractTest;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.SimpleTenantUsage;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Test cases for the simple tenant usage state mapping
 */
@Test(suiteName = "Compute/SimpleTenantUsage")
public class SimpleTenantUsageTests extends AbstractTest {

    private static final String JSON_USAGE = "/compute/simple_tenant_usage.json";

    @Test
    public void stateAndVmState() throws Exception {
        respondWith(JSON_USAGE);

        SimpleTenantUsage usage = osv3().compute().quotaSets().getTenantUsage("p1");
        List<? extends SimpleTenantUsage.ServerUsage> usages = usage.getServerUsages();
        assertEquals(usages.size(), 4);

        assertEquals(usages.get(0).getState(), Server.Status.ACTIVE);
        assertEquals(usages.get(0).getVmState(), "active");

        assertEquals(usages.get(1).getState(), Server.Status.UNRECOGNIZED);
        assertEquals(usages.get(1).getVmState(), "soft-delete");

        assertEquals(usages.get(2).getState(), Server.Status.UNRECOGNIZED);
        assertEquals(usages.get(2).getVmState(), "building");

        assertNull(usages.get(3).getState());
        assertNull(usages.get(3).getVmState());
    }

    @Test
    public void serializationUnchanged() throws Exception {
        respondWith(JSON_USAGE);

        SimpleTenantUsage usage = osv3().compute().quotaSets().getTenantUsage("p1");
        String json = ObjectMapperSingleton.getContext(Object.class).writeValueAsString(usage.getServerUsages().get(0));
        assertTrue(json.matches("(?s).*\"state\"\\s*:\\s*\"active\".*"), json);
        assertFalse(json.contains("vmState"), json);
        assertFalse(json.contains("vm_state"), json);
    }

    @Override
    protected Service service() {
        return Service.COMPUTE;
    }
}
