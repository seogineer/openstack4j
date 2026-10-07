package org.openstack4j.model.dns.v2.options;

import java.util.Objects;

/** The body of {@code POST /v2/blacklists}. */
public final class BlacklistCreate extends DesignateAttributes<BlacklistCreate> {

    private BlacklistCreate() {
    }

    public static BlacklistCreate create(String pattern) {
        return new BlacklistCreate().put("pattern", Objects.requireNonNull(pattern, "pattern"));
    }

    @Override
    protected BlacklistCreate self() {
        return this;
    }

    public BlacklistCreate description(String description) {
        return put("description", description);
    }
}
