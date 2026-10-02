package org.openstack4j.api.placement.v1;

import java.util.function.Supplier;

import org.openstack4j.api.placement.v1.exceptions.PlacementConcurrentUpdateException;

/**
 * Helpers for working with the Placement API.
 */
public final class Placement {

    private Placement() {
    }

    /**
     * Runs {@code action} and runs it again when it fails with {@link PlacementConcurrentUpdateException}, up to
     * {@code maxAttempts} times in total. The action must re-read the current generation itself.
     */
    public static <T> T retryOnConcurrentUpdate(int maxAttempts, Supplier<T> action) {
        if (maxAttempts < 1)
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (PlacementConcurrentUpdateException e) {
                if (attempt >= maxAttempts)
                    throw e;
            }
        }
    }
}
