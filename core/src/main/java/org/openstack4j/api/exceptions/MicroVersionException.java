package org.openstack4j.api.exceptions;

/**
 * The requested microversion or a feature that needs a newer microversion is not available. Raised before any request
 * is sent.
 */
public class MicroVersionException extends OS4JException {

    private static final long serialVersionUID = 1L;

    public MicroVersionException(String message) {
        super(message);
    }
}
