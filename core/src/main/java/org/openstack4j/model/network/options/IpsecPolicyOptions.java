package org.openstack4j.model.network.options;

import java.util.Map;
import java.util.Objects;

/** Body of a IPsec policy create or update; only the fields set are sent. */
public class IpsecPolicyOptions extends NeutronAttributes<IpsecPolicyOptions> {

    public static IpsecPolicyOptions create(String name) {
        return new IpsecPolicyOptions().put("name", Objects.requireNonNull(name, "name"));
    }

    /** An update that sends only the fields set afterwards. */
    public static IpsecPolicyOptions update() {
        return new IpsecPolicyOptions();
    }

    @Override
    protected IpsecPolicyOptions self() {
        return this;
    }

    public IpsecPolicyOptions name(String value) {
        return put("name", value);
    }

    public IpsecPolicyOptions description(String value) {
        return put("description", value);
    }

    /** {@code esp}, {@code ah} or {@code ah-esp}. */
    public IpsecPolicyOptions transformProtocol(String value) {
        return put("transform_protocol", value);
    }

    public IpsecPolicyOptions authAlgorithm(String value) {
        return put("auth_algorithm", value);
    }

    /** {@code tunnel} or {@code transport}. */
    public IpsecPolicyOptions encapsulationMode(String value) {
        return put("encapsulation_mode", value);
    }

    public IpsecPolicyOptions encryptionAlgorithm(String value) {
        return put("encryption_algorithm", value);
    }

    public IpsecPolicyOptions pfs(String value) {
        return put("pfs", value);
    }

    public IpsecPolicyOptions lifetime(Map<String, Object> value) {
        return put("lifetime", value);
    }
}
