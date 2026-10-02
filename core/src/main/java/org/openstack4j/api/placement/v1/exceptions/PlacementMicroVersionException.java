package org.openstack4j.api.placement.v1.exceptions;

import org.openstack4j.api.exceptions.MicroVersionException;

/**
 * The requested Placement feature or microversion is not available on this server. Raised before any request is sent.
 */
public class PlacementMicroVersionException extends MicroVersionException {

    private static final long serialVersionUID = 1L;

    public PlacementMicroVersionException(String message) {
        super(message);
    }
}
