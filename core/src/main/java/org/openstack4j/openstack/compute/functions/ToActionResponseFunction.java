package org.openstack4j.openstack.compute.functions;

import java.util.function.Function;
import org.openstack4j.core.transport.HttpEntityHandler;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.functions.ResponseToActionResponse;
import org.openstack4j.model.common.ActionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Function which consumes the input of an HttpResponse and returns the
 * corresponding ActionResponse
 *
 * @author Jeremy Unruh
 */
public class ToActionResponseFunction implements Function<HttpResponse, ActionResponse> {

    public static final ToActionResponseFunction INSTANCE = new ToActionResponseFunction();
    private static final Logger LOG = LoggerFactory.getLogger(ToActionResponseFunction.class);
    private static final String FAILED_MSG = "Cannot '%s' while instance in in state of %s";

    @Override
    public ActionResponse apply(HttpResponse response) {
        return apply(response, null);
    }

    public ActionResponse apply(HttpResponse response, String action) {
        try {
            int status = response.getStatus();
            if (status == 409 || status == 500) {
                ActionResponse ar = parse(response, true);
                if (ar != null) {
                    return ar;
                }

                LOG.error("{}, {}", status, response.getStatusMessage());
                if (action == null) {
                    return ActionResponse.actionFailed("Instance currently is in build state", 409);
                }
                return ActionResponse.actionFailed(String.format(FAILED_MSG, action, action), 409);
            }
            if (status >= 400) {
                ActionResponse ar = parse(response, false);
                return ar != null ? ar
                        : ActionResponse.actionFailed(String.format("Status: %d, Reason: %s", status, response.getStatusMessage()), status);
            }
            return ActionResponse.actionSuccess();
        } finally {
            HttpEntityHandler.closeQuietly(response);
        }
    }

    /**
     * The error mapped from the body, or null when the body is empty or not JSON (connectors throw on such bodies).
     */
    private static ActionResponse parse(HttpResponse response, boolean returnNullIfNotMapped) {
        try {
            return ResponseToActionResponse.INSTANCE.apply(response, returnNullIfNotMapped);
        } catch (RuntimeException e) {
            LOG.debug("Unreadable error body for status {}: {}", response.getStatus(), e.getMessage());
            return null;
        }
    }

}
