package org.openstack4j.model.heat.options;

import java.util.Map;
import java.util.Objects;

/** Body of a software deployment create or update (sent without a root key). */
public class SoftwareDeploymentOptions extends HeatAttributes<SoftwareDeploymentOptions> {

    public static SoftwareDeploymentOptions create(String serverId, String configId) {
        return new SoftwareDeploymentOptions().put("server_id", Objects.requireNonNull(serverId)).put("config_id", Objects.requireNonNull(configId));
    }

    /** An update that sends only the fields set afterwards. */
    public static SoftwareDeploymentOptions update() {
        return new SoftwareDeploymentOptions();
    }

    @Override
    protected SoftwareDeploymentOptions self() {
        return this;
    }

    public SoftwareDeploymentOptions configId(String value) { return put("config_id", value); }
    public SoftwareDeploymentOptions action(String value) { return put("action", value); }
    public SoftwareDeploymentOptions status(String value) { return put("status", value); }
    public SoftwareDeploymentOptions statusReason(String value) { return put("status_reason", value); }
    public SoftwareDeploymentOptions inputValues(Map<String, Object> value) { return put("input_values", value); }
    public SoftwareDeploymentOptions outputValues(Map<String, Object> value) { return put("output_values", value); }
    public SoftwareDeploymentOptions stackUserProjectId(String value) { return put("stack_user_project_id", value); }
}
