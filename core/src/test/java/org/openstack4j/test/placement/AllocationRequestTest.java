package org.openstack4j.test.placement;

import org.openstack4j.model.placement.v1.AllocationRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AllocationRequestTest {

    @Test
    public void builtRequestIsIndependentOfLaterBuilderChanges() {
        AllocationRequest.Builder builder = AllocationRequest.builder().projectId("p").userId("u").consumerType("INSTANCE");
        AllocationRequest first = builder.allocation("rp-1", "VCPU", 2).build();
        AllocationRequest second = builder.allocation("rp-2", "VCPU", 4).allocation("rp-1", "MEMORY_MB", 512).build();

        Assert.assertEquals(first.getAllocations().keySet(), java.util.Collections.singleton("rp-1"));
        Assert.assertEquals(first.getAllocations().get("rp-1").keySet(), java.util.Collections.singleton("VCPU"));
        Assert.assertEquals(second.getAllocations().size(), 2);
    }
}
