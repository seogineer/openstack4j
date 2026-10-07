package org.openstack4j.model.dns.v2.options;


/** The body of {@code PATCH /v2/blacklists/{id}}: only the fields set are changed. */
public final class BlacklistUpdate extends DesignateAttributes<BlacklistUpdate> {

    private BlacklistUpdate() {
    }

    public static BlacklistUpdate create() {
        return new BlacklistUpdate();
    }

    @Override
    protected BlacklistUpdate self() {
        return this;
    }

    public BlacklistUpdate pattern(String pattern) {
        return put("pattern", pattern);
    }

    public BlacklistUpdate description(String description) {
        return put("description", description);
    }
}
