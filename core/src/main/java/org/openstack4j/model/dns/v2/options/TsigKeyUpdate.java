package org.openstack4j.model.dns.v2.options;


/** The body of {@code PATCH /v2/tsigkeys/{id}}: only the fields set are changed. */
public final class TsigKeyUpdate extends DesignateAttributes<TsigKeyUpdate> {

    private TsigKeyUpdate() {
    }

    public static TsigKeyUpdate create() {
        return new TsigKeyUpdate();
    }

    @Override
    protected TsigKeyUpdate self() {
        return this;
    }

    public TsigKeyUpdate name(String name) {
        return put("name", name);
    }

    public TsigKeyUpdate algorithm(String algorithm) {
        return put("algorithm", algorithm);
    }

    public TsigKeyUpdate secret(String secret) {
        return put("secret", secret);
    }

    public TsigKeyUpdate scope(String scope) {
        return put("scope", scope);
    }

    public TsigKeyUpdate resourceId(String resourceId) {
        return put("resource_id", resourceId);
    }
}
