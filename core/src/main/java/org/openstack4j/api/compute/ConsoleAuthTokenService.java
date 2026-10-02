package org.openstack4j.api.compute;

import org.openstack4j.common.RestService;
import org.openstack4j.model.compute.ConsoleConnectionInfo;

/** Console connection details by token ({@code GET /os-console-auth-tokens/{token}}); admin only. */
public interface ConsoleAuthTokenService extends RestService {

    /** All console types from 2.31; RDP only before. */
    ConsoleConnectionInfo get(String token);
}
