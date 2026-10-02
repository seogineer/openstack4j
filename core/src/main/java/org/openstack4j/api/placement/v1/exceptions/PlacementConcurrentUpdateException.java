package org.openstack4j.api.placement.v1.exceptions;

/**
 * The resource provider or consumer generation sent with a write no longer matches the server
 * ({@code placement.concurrent_update}). Re-read the current state and apply the change again, for example with
 * {@link org.openstack4j.api.placement.v1.Placement#retryOnConcurrentUpdate}.
 */
public class PlacementConcurrentUpdateException extends PlacementException {

    private static final long serialVersionUID = 1L;

    public PlacementConcurrentUpdateException(String message, int status, String errorCode, String detail, String requestId) {
        super(message, status, errorCode, detail, requestId);
    }
}
