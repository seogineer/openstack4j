package org.openstack4j.openstack.identity.v3.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.core.transport.HttpResponse;

/** Reads Keystone responses that are not JSON (SAML XML, OAuth1 form bodies). */
final class IdentityResponses {

    /**
     * Value of {@link org.openstack4j.core.transport.ClientConstants#HEADER_OS4J_AUTH}: the connectors do not re-authenticate the
     * session on a 401 for requests carrying it. Delegated-auth endpoints answer 401 for their own credentials (consumer secret,
     * client secret, assertion token), which must reach the caller instead of triggering a session re-authentication.
     */
    static final String NO_REAUTH = "Delegated";

    private IdentityResponses() {
    }

    static String text(HttpResponse response) {
        try (HttpResponse r = response) {
            if (r.getStatus() >= 400)
                throw new ClientResponseException(r.getStatusMessage() + " (" + r.getStatus() + ")", r.getStatus());
            try (InputStream in = r.getInputStream()) {
                return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new ClientResponseException(e.getMessage(), 0, e);
        }
    }
}
