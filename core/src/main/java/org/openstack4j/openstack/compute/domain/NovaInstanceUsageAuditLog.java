package org.openstack4j.openstack.compute.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.compute.InstanceUsageAuditLog;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaInstanceUsageAuditLog implements InstanceUsageAuditLog {

    private static final long serialVersionUID = 1L;

    @JsonProperty("hosts_not_run") private List<String> hostsNotRun;
    private Map<String, Object> log;
    @JsonProperty("num_hosts") private Integer numHosts;
    @JsonProperty("num_hosts_done") private Integer numHostsDone;
    @JsonProperty("num_hosts_not_run") private Integer numHostsNotRun;
    @JsonProperty("num_hosts_running") private Integer numHostsRunning;
    @JsonProperty("overall_status") private String overallStatus;
    @JsonProperty("period_beginning") private String periodBeginning;
    @JsonProperty("period_ending") private String periodEnding;
    @JsonProperty("total_errors") private Integer totalErrors;
    @JsonProperty("total_instances") private Integer totalInstances;

    @Override public List<String> getHostsNotRun() { return hostsNotRun; }
    @Override public Map<String, Object> getLog() { return log; }
    @Override public Integer getNumHosts() { return numHosts; }
    @Override public Integer getNumHostsDone() { return numHostsDone; }
    @Override public Integer getNumHostsNotRun() { return numHostsNotRun; }
    @Override public Integer getNumHostsRunning() { return numHostsRunning; }
    @Override public String getOverallStatus() { return overallStatus; }
    @Override public String getPeriodBeginning() { return periodBeginning; }
    @Override public String getPeriodEnding() { return periodEnding; }
    @Override public Integer getTotalErrors() { return totalErrors; }
    @Override public Integer getTotalInstances() { return totalInstances; }

    /** {@code GET /os-instance_usage_audit_log} */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Current implements ModelEntity {
        private static final long serialVersionUID = 1L;
        @JsonProperty("instance_usage_audit_logs")
        public NovaInstanceUsageAuditLog log;
    }

    /** {@code GET /os-instance_usage_audit_log/{before}} */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Before implements ModelEntity {
        private static final long serialVersionUID = 1L;
        @JsonProperty("instance_usage_audit_log")
        public NovaInstanceUsageAuditLog log;
    }
}
