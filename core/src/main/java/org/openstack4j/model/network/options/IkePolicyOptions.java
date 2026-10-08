package org.openstack4j.model.network.options;

import java.util.Map;
import java.util.Objects;

/** Body of a IKE policy create or update; only the fields set are sent. */
public class IkePolicyOptions extends NeutronAttributes<IkePolicyOptions> {

    public static IkePolicyOptions create(String name) {
        return new IkePolicyOptions().put("name", Objects.requireNonNull(name, "name"));
    }

    /** An update that sends only the fields set afterwards. */
    public static IkePolicyOptions update() {
        return new IkePolicyOptions();
    }

    @Override
    protected IkePolicyOptions self() {
        return this;
    }

    public IkePolicyOptions name(String value) {
        return put("name", value);
    }

    public IkePolicyOptions description(String value) {
        return put("description", value);
    }

    /** e.g. {@code sha256}. */
    public IkePolicyOptions authAlgorithm(String value) {
        return put("auth_algorithm", value);
    }

    /** e.g. {@code aes-256}. */
    public IkePolicyOptions encryptionAlgorithm(String value) {
        return put("encryption_algorithm", value);
    }

    /** e.g. {@code group14}. */
    public IkePolicyOptions pfs(String value) {
        return put("pfs", value);
    }

    /** {@code main} or {@code aggressive}. */
    public IkePolicyOptions phase1NegotiationMode(String value) {
        return put("phase1_negotiation_mode", value);
    }

    /** {@code v1} or {@code v2}. */
    public IkePolicyOptions ikeVersion(String value) {
        return put("ike_version", value);
    }

    /** e.g. {@code {"units": "seconds", "value": 3600}}. */
    public IkePolicyOptions lifetime(Map<String, Object> value) {
        return put("lifetime", value);
    }
}
