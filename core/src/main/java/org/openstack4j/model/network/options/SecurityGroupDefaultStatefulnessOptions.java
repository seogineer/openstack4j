package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a security group default statefulness create or update. */
public class SecurityGroupDefaultStatefulnessOptions extends NeutronAttributes<SecurityGroupDefaultStatefulnessOptions> {

    public static SecurityGroupDefaultStatefulnessOptions create(boolean stateful) {
        return new SecurityGroupDefaultStatefulnessOptions().put("stateful", stateful);
    }

    /** The statefulness is the only field that changes. */
    public static SecurityGroupDefaultStatefulnessOptions update(boolean stateful) {
        return new SecurityGroupDefaultStatefulnessOptions().put("stateful", stateful);
    }

    @Override
    protected SecurityGroupDefaultStatefulnessOptions self() {
        return this;
    }

    public SecurityGroupDefaultStatefulnessOptions projectId(String value) { return put("project_id", value); }
}
