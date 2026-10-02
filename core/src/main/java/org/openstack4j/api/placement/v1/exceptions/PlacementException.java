package org.openstack4j.api.placement.v1.exceptions;

import org.openstack4j.api.exceptions.ClientResponseException;

/**
 * A Placement API request failed. Carries the Placement error code (for example {@code placement.inventory.inuse}).
 */
public class PlacementException extends ClientResponseException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String detail;
    private final String requestId;

    public PlacementException(String message, int status, String errorCode, String detail, String requestId) {
        super(message, status);
        this.errorCode = errorCode;
        this.detail = detail;
        this.requestId = requestId;
    }

    /** @return the Placement error code, or {@code null} when the response body was not a Placement error */
    public String getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detail;
    }

    public String getRequestId() {
        return requestId;
    }
}
