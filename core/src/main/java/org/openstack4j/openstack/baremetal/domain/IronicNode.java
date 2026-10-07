package org.openstack4j.openstack.baremetal.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.baremetal.Node;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IronicNode implements Node {

    private static final long serialVersionUID = 1L;

    @JsonProperty("uuid") private String uuid;
    @JsonProperty("name") private String name;
    @JsonProperty("driver") private String driver;
    @JsonProperty("provision_state") private String provisionState;
    @JsonProperty("target_provision_state") private String targetProvisionState;
    @JsonProperty("power_state") private String powerState;
    @JsonProperty("target_power_state") private String targetPowerState;
    @JsonProperty("maintenance") private Boolean maintenance;
    @JsonProperty("maintenance_reason") private String maintenanceReason;
    @JsonProperty("instance_uuid") private String instanceUuid;
    @JsonProperty("resource_class") private String resourceClass;
    @JsonProperty("conductor") private String conductor;
    @JsonProperty("conductor_group") private String conductorGroup;
    @JsonProperty("owner") private String owner;
    @JsonProperty("lessee") private String lessee;
    @JsonProperty("description") private String description;
    @JsonProperty("last_error") private String lastError;
    @JsonProperty("console_enabled") private Boolean consoleEnabled;
    @JsonProperty("automated_clean") private Boolean automatedClean;
    @JsonProperty("protected") private Boolean protectedValue;
    @JsonProperty("retired") private Boolean retired;
    @JsonProperty("chassis_uuid") private String chassisUuid;
    @JsonProperty("properties") private Map<String, Object> properties;
    @JsonProperty("driver_info") private Map<String, Object> driverInfo;
    @JsonProperty("instance_info") private Map<String, Object> instanceInfo;
    @JsonProperty("extra") private Map<String, Object> extra;
    @JsonProperty("traits") private List<String> traits;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @Override public String getUuid() { return uuid; }
    @Override public String getName() { return name; }
    @Override public String getDriver() { return driver; }
    @Override public String getProvisionState() { return provisionState; }
    @Override public String getTargetProvisionState() { return targetProvisionState; }
    @Override public String getPowerState() { return powerState; }
    @Override public String getTargetPowerState() { return targetPowerState; }
    @Override public Boolean isMaintenance() { return maintenance; }
    @Override public String getMaintenanceReason() { return maintenanceReason; }
    @Override public String getInstanceUuid() { return instanceUuid; }
    @Override public String getResourceClass() { return resourceClass; }
    @Override public String getConductor() { return conductor; }
    @Override public String getConductorGroup() { return conductorGroup; }
    @Override public String getOwner() { return owner; }
    @Override public String getLessee() { return lessee; }
    @Override public String getDescription() { return description; }
    @Override public String getLastError() { return lastError; }
    @Override public Boolean isConsoleEnabled() { return consoleEnabled; }
    @Override public Boolean getAutomatedClean() { return automatedClean; }
    @Override public Boolean isProtected() { return protectedValue; }
    @Override public Boolean isRetired() { return retired; }
    @Override public String getChassisUuid() { return chassisUuid; }
    @Override public Map<String, Object> getProperties() { return properties; }
    @Override public Map<String, Object> getDriverInfo() { return driverInfo; }
    @Override public Map<String, Object> getInstanceInfo() { return instanceInfo; }
    @Override public Map<String, Object> getExtra() { return extra; }
    @Override public List<String> getTraits() { return traits; }
    @Override public String getCreatedAt() { return createdAt; }
    @Override public String getUpdatedAt() { return updatedAt; }

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override public Map<String, Object> getAttributes() { return attributes; }

    public static class Nodes extends ListResult<IronicNode> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("nodes")
        private List<IronicNode> list;

        @Override
        protected List<IronicNode> value() {
            return list;
        }
    }
}
