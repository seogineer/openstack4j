package org.openstack4j.openstack.baremetal.domain;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.baremetal.NodeStates;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IronicNodeStates implements NodeStates {

    private static final long serialVersionUID = 1L;

    @JsonProperty("power_state") private String powerState;
    @JsonProperty("target_power_state") private String targetPowerState;
    @JsonProperty("provision_state") private String provisionState;
    @JsonProperty("target_provision_state") private String targetProvisionState;
    @JsonProperty("last_error") private String lastError;
    @JsonProperty("console_enabled") private Boolean consoleEnabled;
    @JsonProperty("boot_mode") private String bootMode;
    @JsonProperty("secure_boot") private Boolean secureBoot;
    @JsonProperty("raid_config") private Map<String, Object> raidConfig;
    @JsonProperty("target_raid_config") private Map<String, Object> targetRaidConfig;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getPowerState() { return powerState; }
    @Override public String getTargetPowerState() { return targetPowerState; }
    @Override public String getProvisionState() { return provisionState; }
    @Override public String getTargetProvisionState() { return targetProvisionState; }
    @Override public String getLastError() { return lastError; }
    @Override public Boolean getConsoleEnabled() { return consoleEnabled; }
    @Override public String getBootMode() { return bootMode; }
    @Override public Boolean getSecureBoot() { return secureBoot; }
    @Override public Map<String, Object> getRaidConfig() { return raidConfig; }
    @Override public Map<String, Object> getTargetRaidConfig() { return targetRaidConfig; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
