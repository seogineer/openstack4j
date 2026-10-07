package org.openstack4j.openstack.dns.v2.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.dns.v2.ext.ServiceStatus;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DesignateServiceStatus implements ServiceStatus {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("hostname") private String hostname;
    @JsonProperty("service_name") private String serviceName;
    @JsonProperty("status") private String status;
    @JsonProperty("stats") private Map<String, Object> stats;
    @JsonProperty("capabilities") private Map<String, Object> capabilities;
    @JsonProperty("heartbeated_at") private String heartbeatedAt;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getHostname() { return hostname; }
    @Override public String getServiceName() { return serviceName; }
    @Override public String getStatus() { return status; }
    @Override public Map<String, Object> getStats() { return stats; }
    @Override public Map<String, Object> getCapabilities() { return capabilities; }
    @Override public String getHeartbeatedAt() { return heartbeatedAt; }
    @Override public String getCreatedAt() { return createdAt; }
    @Override public String getUpdatedAt() { return updatedAt; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class DesignateServiceStatusList extends ListResult<DesignateServiceStatus> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("service_statuses")
        private List<DesignateServiceStatus> list;

        @Override
        protected List<DesignateServiceStatus> value() {
            return list;
        }
    }
}
