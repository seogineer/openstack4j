package org.openstack4j.model.dns.v2.options;

import java.util.Objects;

/** The body of {@code POST /v2/tsigkeys}. */
public final class TsigKeyCreate extends DesignateAttributes<TsigKeyCreate> {

    private TsigKeyCreate() {
    }

    public static TsigKeyCreate create(String name, String algorithm, String secret, String scope, String resourceId) {
        return new TsigKeyCreate().put("name", Objects.requireNonNull(name, "name")).put("algorithm", Objects.requireNonNull(algorithm, "algorithm")).put("secret", Objects.requireNonNull(secret, "secret")).put("scope", Objects.requireNonNull(scope, "scope")).put("resource_id", Objects.requireNonNull(resourceId, "resourceId"));
    }

    @Override
    protected TsigKeyCreate self() {
        return this;
    }
}
