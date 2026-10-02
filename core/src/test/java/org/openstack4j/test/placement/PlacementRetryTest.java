package org.openstack4j.test.placement;

import java.util.concurrent.atomic.AtomicInteger;

import org.openstack4j.api.placement.v1.Placement;
import org.openstack4j.api.placement.v1.exceptions.PlacementConcurrentUpdateException;
import org.openstack4j.api.placement.v1.exceptions.PlacementException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PlacementRetryTest {

    private static PlacementConcurrentUpdateException conflict() {
        return new PlacementConcurrentUpdateException("conflict", 409, "placement.concurrent_update", "detail", "req-1");
    }

    @Test
    public void retriesUntilSuccess() {
        AtomicInteger calls = new AtomicInteger();
        String result = Placement.retryOnConcurrentUpdate(3, () -> {
            if (calls.incrementAndGet() < 3) throw conflict();
            return "ok";
        });
        Assert.assertEquals(result, "ok");
        Assert.assertEquals(calls.get(), 3);
    }

    @Test
    public void rethrowsLastConflictAfterMaxAttempts() {
        AtomicInteger calls = new AtomicInteger();
        try {
            Placement.retryOnConcurrentUpdate(2, () -> {
                calls.incrementAndGet();
                throw conflict();
            });
            Assert.fail("expected PlacementConcurrentUpdateException");
        } catch (PlacementConcurrentUpdateException expected) {
            Assert.assertEquals(calls.get(), 2);
        }
    }

    @Test
    public void otherExceptionsAreNotRetried() {
        AtomicInteger calls = new AtomicInteger();
        try {
            Placement.retryOnConcurrentUpdate(5, () -> {
                calls.incrementAndGet();
                throw new PlacementException("in use", 409, "placement.inventory.inuse", "detail", "req-1");
            });
            Assert.fail("expected PlacementException");
        } catch (PlacementConcurrentUpdateException unexpected) {
            Assert.fail("must not be a concurrent update");
        } catch (PlacementException expected) {
            Assert.assertEquals(calls.get(), 1);
            Assert.assertEquals(expected.getErrorCode(), "placement.inventory.inuse");
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void maxAttemptsMustBePositive() {
        Placement.retryOnConcurrentUpdate(0, () -> "never");
    }
}
