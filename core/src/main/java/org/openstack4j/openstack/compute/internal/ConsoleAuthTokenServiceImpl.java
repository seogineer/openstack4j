package org.openstack4j.openstack.compute.internal;

import java.util.Objects;

import org.openstack4j.api.compute.ConsoleAuthTokenService;
import org.openstack4j.model.compute.ConsoleConnectionInfo;
import org.openstack4j.openstack.compute.domain.NovaConsoleConnectionInfo;

/** {@code GET /os-console-auth-tokens/{token}} */
public class ConsoleAuthTokenServiceImpl extends BaseComputeServices implements ConsoleAuthTokenService {

    @Override
    public ConsoleConnectionInfo get(String token) {
        Objects.requireNonNull(token);
        return get(NovaConsoleConnectionInfo.class, uri("/os-console-auth-tokens/%s", token)).execute();
    }
}
