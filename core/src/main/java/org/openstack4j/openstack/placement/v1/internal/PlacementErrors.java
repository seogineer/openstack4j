package org.openstack4j.openstack.placement.v1.internal;

import org.openstack4j.api.placement.v1.exceptions.PlacementConcurrentUpdateException;
import org.openstack4j.api.placement.v1.exceptions.PlacementException;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.PropagateResponse;
import org.openstack4j.openstack.placement.v1.domain.PlacementErrorResponse;

/** Turns Placement error responses into {@link PlacementException}s. */
final class PlacementErrors {

    static final String CONCURRENT_UPDATE = "placement.concurrent_update";

    /** For writes: every error status throws. */
    static final PropagateResponse THROW_ALL = response -> {
        if (response.getStatus() >= 400)
            throw toException(response);
    };

    /** For reads: 404 is left to openstack4j, which returns {@code null}; other errors throw. */
    static final PropagateResponse THROW_EXCEPT_404 = response -> {
        if (response.getStatus() >= 400 && response.getStatus() != 404)
            throw toException(response);
    };

    private PlacementErrors() {
    }

    static PlacementException toException(HttpResponse response) {
        PlacementErrorResponse.Error error = null;
        try {
            PlacementErrorResponse body = response.readEntity(PlacementErrorResponse.class);
            error = body == null ? null : body.first();
        } catch (RuntimeException notPlacementJson) {
            // proxies and load balancers answer with HTML or an empty body; keep the HTTP status
        }
        int status = response.getStatus();
        String code = error == null ? null : error.code;
        String detail = error == null ? response.getStatusMessage() : (error.detail != null ? error.detail.trim() : error.title);
        String requestId = error == null ? null : error.requestId;
        String message = "Placement request failed with " + status + (code == null ? "" : " " + code)
                + (detail == null || detail.isEmpty() ? "" : ": " + detail);
        if (CONCURRENT_UPDATE.equals(code))
            return new PlacementConcurrentUpdateException(message, status, code, detail, requestId);
        return new PlacementException(message, status, code, detail, requestId);
    }
}
