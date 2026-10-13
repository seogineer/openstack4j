package org.openstack4j.openstack.workflow.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.openstack.internal.BaseOpenStackService;


/**
 * Base class for all Mistral services.
 *
 * @author Renat Akhmerov
 */

class BaseMistralService extends BaseOpenStackService {

    /** Mistral exposes its DSL upload controllers as text/plain; pecan answers 406 to an Accept that excludes it. */
    static final String DSL_ACCEPT = "application/json, text/plain";

    BaseMistralService() {
        super(ServiceType.WORKFLOW);
    }

    /**
     * Buffers the definition so every connector sends it with a Content-Length (a chunked body is refused). The
     * stream is read to its end but not closed; it stays the caller's.
     */
    static Payload<InputStream> definition(InputStream text) {
        try {
            return Payloads.create(new ByteArrayInputStream(Objects.requireNonNull(text).readAllBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
