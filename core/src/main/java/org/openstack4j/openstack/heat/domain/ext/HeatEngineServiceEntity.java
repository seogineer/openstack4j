package org.openstack4j.openstack.heat.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.heat.ext.HeatEngineService;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HeatEngineServiceEntity implements HeatEngineService {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("binary") private String binary;
    @JsonProperty("host") private String host;
    @JsonProperty("hostname") private String hostname;
    @JsonProperty("engine_id") private String engineId;
    @JsonProperty("topic") private String topic;
    @JsonProperty("status") private String status;
    @JsonProperty("report_interval") private Integer reportInterval;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;
    @JsonProperty("deleted_at") private String deletedAt;

    @Override public String getId() { return id; }
    @Override public String getBinary() { return binary; }
    @Override public String getHost() { return host; }
    @Override public String getHostname() { return hostname; }
    @Override public String getEngineId() { return engineId; }
    @Override public String getTopic() { return topic; }
    @Override public String getStatus() { return status; }
    @Override public Integer getReportInterval() { return reportInterval; }
    @Override public String getCreatedAt() { return createdAt; }
    @Override public String getUpdatedAt() { return updatedAt; }
    @Override public String getDeletedAt() { return deletedAt; }

    public static class Services extends ListResult<HeatEngineServiceEntity> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("services")
        private List<HeatEngineServiceEntity> list;

        @Override
        protected List<HeatEngineServiceEntity> value() {
            return list;
        }
    }
}
