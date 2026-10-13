package org.openstack4j.api.storage;

import java.util.List;

import org.openstack4j.api.AbstractTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openstack4j.openstack.storage.block.domain.CinderBackendStoragePool.CinderCapabilities;
import org.openstack4j.openstack.storage.block.domain.VolumeBackendPool;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Test cases for scheduler stats.
 *
 * @author Chen guofeng gf.chen40@gmail.com
 */
@Test(suiteName = "SchedulerStatsGetPool")
public class SchedulerStatsGetPoolTests extends AbstractTest {
    private static final String JSON_SCHEDULER_STATS = "/storage/v2/cinder_scheduler-stats.json";
    private static final String JSON_SCHEDULER_STATS_DETAIL = "/storage/v2/cinder_scheduler-stats_detail.json";
    private static final String JSON_SCHEDULER_STATS_DETAIL_LENIENT = "/storage/v2/cinder_scheduler-stats_detail_lenient.json";

    @Override
    protected Service service() {
        return Service.BLOCK_STORAGE;
    }

    @Test
    public void pools() throws Exception {
        respondWith(JSON_SCHEDULER_STATS);

        List<? extends VolumeBackendPool> pools = osv3().blockStorage().schedulerStatsPools().pools();
        assertEquals(pools.size(), 3);

        VolumeBackendPool pool1 = pools.get(0);
        VolumeBackendPool pool2 = pools.get(1);
        VolumeBackendPool pool3 = pools.get(2);

        assertEquals(pool1.getName(), "cinder1@generic1#GENERIC1");

        assertEquals(pool2.getName(), "cinder2@unmanage1#UNMANAGE1");

        assertEquals(pool3.getName(), "cinder3@ams_backend#AMS_BACKEND");
    }

    @Test
    public void poolsDetail() throws Exception {
        respondWith(JSON_SCHEDULER_STATS_DETAIL);

        List<? extends VolumeBackendPool> pools = osv3().blockStorage().schedulerStatsPools().poolsDetail();
        assertEquals(pools.size(), 2);

        VolumeBackendPool pool4 = pools.get(0);
        VolumeBackendPool pool5 = pools.get(1);


        assertEquals(pool4.getName(), "pool1");
        assertFalse(pool4.getCapabilities().getQosSupport());
        assertEquals(pool4.getCapabilities().getDriverVersion(), "1.0.0");
        assertEquals((long) pool4.getCapabilities().getTotalCapacityGb(), 1024);
        assertEquals((int) pool4.getCapabilities().getReservedPercentage(), 0);
        assertEquals((long) pool4.getCapabilities().getFreeCapacityGb(), 100);
        assertEquals(pool4.getCapabilities().getStorageProtocol(), "iSCSI");


        assertEquals(pool5.getName(), "pool2");
        assertTrue(pool5.getCapabilities().getQosSupport());
        assertEquals(pool5.getCapabilities().getDriverVersion(), "1.0.1");
        assertEquals((long) pool5.getCapabilities().getTotalCapacityGb(), 512);
        assertEquals((int) pool5.getCapabilities().getReservedPercentage(), 0);
        assertEquals((long) pool5.getCapabilities().getFreeCapacityGb(), 200);
        assertEquals(pool5.getCapabilities().getStorageProtocol(), "iSER");
    }

    @Test
    public void lenientCapacities() throws Exception {
        respondWith(JSON_SCHEDULER_STATS_DETAIL_LENIENT);

        List<? extends VolumeBackendPool> pools = osv3().blockStorage().schedulerStatsPools().poolsDetail();
        assertEquals(pools.size(), 3);

        // poolA: keywords give null, a decimal string is rounded half-up, a number stays as it was
        VolumeBackendPool.Capabilities a = pools.get(0).getCapabilities();
        assertNull(a.getTotalCapacityGb());
        assertEquals(a.getFreeCapacityGb(), Long.valueOf(1231));
        assertEquals(a.getAllocatedcapacitygb(), Integer.valueOf(100));
        assertNull(a.getProvisionedcapacitygb());
        assertEquals(a.getReservedPercentage(), Integer.valueOf(0));
        assertEquals(a.getTotalvolumes(), Integer.valueOf(3));

        VolumeBackendPool.Capabilities b = pools.get(1).getCapabilities();
        assertNull(b.getTotalCapacityGb());
        assertNull(b.getFreeCapacityGb());
        assertNull(b.getAllocatedcapacitygb());

        // poolC: a JSON number with a fraction is still truncated, as before
        VolumeBackendPool.Capabilities c = pools.get(2).getCapabilities();
        assertEquals(c.getTotalCapacityGb(), Long.valueOf(1024));
        assertEquals(c.getFreeCapacityGb(), Long.valueOf(100));
        assertEquals(c.getAllocatedcapacitygb(), Integer.valueOf(101));
    }

    @Test
    public void lenientParsingBounds() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        assertNull(mapper.readValue("{\"total_capacity_gb\":\"99999999999999999999\"}", CinderCapabilities.class).getTotalCapacityGb());
        assertNull(mapper.readValue("{\"total_capacity_gb\":\"1e999999999\"}", CinderCapabilities.class).getTotalCapacityGb());
        assertEquals(mapper.readValue("{\"total_capacity_gb\":\"9223372036854775807\"}", CinderCapabilities.class).getTotalCapacityGb(), Long.valueOf(Long.MAX_VALUE));
        assertEquals(mapper.readValue("{\"total_capacity_gb\":\" 1e3 \"}", CinderCapabilities.class).getTotalCapacityGb(), Long.valueOf(1000));
        assertNull(mapper.readValue("{\"total_capacity_gb\":\"NaN\"}", CinderCapabilities.class).getTotalCapacityGb());
        assertNull(mapper.readValue("{\"total_capacity_gb\":\"Infinity\"}", CinderCapabilities.class).getTotalCapacityGb());
        assertNull(mapper.readValue("{\"total_volumes\":\"2147483648\"}", CinderCapabilities.class).getTotalvolumes());
        assertEquals(mapper.readValue("{\"total_volumes\":\"2147483647\"}", CinderCapabilities.class).getTotalvolumes(), Integer.valueOf(Integer.MAX_VALUE));
        assertNull(mapper.readValue("{\"total_volumes\":null}", CinderCapabilities.class).getTotalvolumes());
        assertEquals(mapper.readValue("{\"total_volumes\":7}", CinderCapabilities.class).getTotalvolumes(), Integer.valueOf(7));
    }
}
