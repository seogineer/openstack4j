package org.openstack4j.api.identity.v3;

import java.util.Date;
import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.identity.v3.RevocationEvent;

/**
 * OS-REVOKE token revocation events ({@code GET /v3/OS-REVOKE/events}).
 */
public interface RevocationEventService extends RestService {

    /**
     * Lists token revocation events, optionally only those since a time.
     *
     * @return the result
     */
    List<? extends RevocationEvent> list();

    /**
     * Lists token revocation events, optionally only those since a time.
     *
     * @param since the since
     * @return the result
     */
    List<? extends RevocationEvent> list(Date since);
}
