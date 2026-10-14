package org.openstack4j.api.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.api.Builders;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.compute.QuotaSet;
import org.openstack4j.openstack.compute.domain.NovaQuotaSet;
import org.testng.Assert;
import org.testng.annotations.Test;

/** server_groups and server_group_members of the quota set and quota class set. */
@Test(suiteName = "Compute/QuotaSetServerGroups")
public class QuotaSetServerGroupTests extends AbstractTest {

    private static final String QUOTA_SET = "{\"quota_set\":{\"id\":\"t1\",\"cores\":20,\"instances\":10,\"ram\":51200,"
            + "\"server_groups\":10,\"server_group_members\":11}}";
    private static final String QUOTA_CLASS_SET = "{\"quota_class_set\":{\"id\":\"c1\",\"cores\":20,\"instances\":10,"
            + "\"server_groups\":7,\"server_group_members\":9}}";

    @Override
    protected Service service() {
        return Service.COMPUTE;
    }

    public void quotaSetServerGroups() throws Exception {
        respondWith(200, QUOTA_SET);

        QuotaSet quota = osv3().compute().quotaSets().get("t1");

        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().contains("/os-quota-sets/t1"), request.getPath());
        Assert.assertEquals(quota.getServerGroups(), Integer.valueOf(10));
        Assert.assertEquals(quota.getServerGroupMembers(), Integer.valueOf(11));
        Assert.assertEquals(quota.getCores(), 20);
    }

    public void quotaSetWithoutKeysIsNull() throws Exception {
        respondWith(200, "{\"quota_set\":{\"id\":\"t1\",\"cores\":20}}");

        QuotaSet quota = osv3().compute().quotaSets().get("t1");

        takeRequest();
        Assert.assertNull(quota.getServerGroups());
        Assert.assertNull(quota.getServerGroupMembers());
    }

    public void quotaClassSetServerGroups() throws Exception {
        respondWith(200, QUOTA_CLASS_SET);

        QuotaSet quota = osv3().compute().quotaSets().updateForClass("c1", Builders.quotaSet().cores(20).build());

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertTrue(request.getPath().contains("/os-quota-class-sets/c1"), request.getPath());
        Assert.assertEquals(quota.getServerGroups(), Integer.valueOf(7));
        Assert.assertEquals(quota.getServerGroupMembers(), Integer.valueOf(9));
    }

    public void updateBodyDoesNotCarryServerGroups() throws Exception {
        respondWith(200, QUOTA_SET);

        osv3().compute().quotaSets().updateForTenant("t1", Builders.quotaSet().cores(20).build());

        JsonNode sent = new ObjectMapper().readTree(takeRequest().getBody().readUtf8()).get("quota_set");
        Assert.assertEquals(sent.get("cores").asInt(), 20);
        Assert.assertFalse(sent.has("server_groups"), sent.toString());
        Assert.assertFalse(sent.has("server_group_members"), sent.toString());
        Assert.assertFalse(sent.has("serverGroups"), sent.toString());
    }

    public void notSerialized() throws Exception {
        NovaQuotaSet quota = ObjectMapperSingleton.getContext(NovaQuotaSet.class).readValue(QUOTA_SET, NovaQuotaSet.class);
        Assert.assertEquals(quota.getServerGroups(), Integer.valueOf(10));
        String written = ObjectMapperSingleton.getContext(NovaQuotaSet.class).writeValueAsString(quota).toLowerCase(java.util.Locale.ROOT).replace("_", "");
        Assert.assertFalse(written.contains("\"servergroups\""), written);
        Assert.assertFalse(written.contains("\"servergroupmembers\""), written);
    }
}
