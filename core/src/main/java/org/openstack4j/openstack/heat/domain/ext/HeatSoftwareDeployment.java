package org.openstack4j.openstack.heat.domain.ext;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.heat.ext.SoftwareDeployment;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("software_deployment")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeatSoftwareDeployment implements SoftwareDeployment {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("server_id") private String serverId;
    @JsonProperty("config_id") private String configId;
    @JsonProperty("action") private String action;
    @JsonProperty("status") private String status;
    @JsonProperty("status_reason") private String statusReason;
    @JsonProperty("input_values") private Map<String, Object> inputValues;
    @JsonProperty("output_values") private Map<String, Object> outputValues;
    @JsonProperty("creation_time") private String creationTime;
    @JsonProperty("updated_time") private String updatedTime;

    @Override public String getId() { return id; }
    @Override public String getServerId() { return serverId; }
    @Override public String getConfigId() { return configId; }
    @Override public String getAction() { return action; }
    @Override public String getStatus() { return status; }
    @Override public String getStatusReason() { return statusReason; }
    @Override public Map<String, Object> getInputValues() { return inputValues; }
    @Override public Map<String, Object> getOutputValues() { return outputValues; }
    @Override public String getCreationTime() { return creationTime; }
    @Override public String getUpdatedTime() { return updatedTime; }

    public static class SoftwareDeployments extends ListResult<HeatSoftwareDeployment> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("software_deployments")
        private List<HeatSoftwareDeployment> list;

        @Override
        protected List<HeatSoftwareDeployment> value() {
            return list;
        }
    }
}
