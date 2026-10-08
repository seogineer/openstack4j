package org.openstack4j.openstack.reservation.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.reservation.ReservableHost;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("host")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlazarReservableHost implements ReservableHost {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("hypervisor_hostname") private String hypervisorHostname;
    @JsonProperty("hypervisor_type") private String hypervisorType;
    @JsonProperty("hypervisor_version") private Long hypervisorVersion;
    @JsonProperty("vcpus") private Integer vcpus;
    @JsonProperty("memory_mb") private Integer memoryMb;
    @JsonProperty("local_gb") private Integer localGb;
    @JsonProperty("cpu_info") private String cpuInfo;
    @JsonProperty("service_name") private String serviceName;
    @JsonProperty("reservable") private Boolean reservable;
    @JsonProperty("trust_id") private String trustId;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getHypervisorHostname() { return hypervisorHostname; }
    @Override public String getHypervisorType() { return hypervisorType; }
    @Override public Long getHypervisorVersion() { return hypervisorVersion; }
    @Override public Integer getVcpus() { return vcpus; }
    @Override public Integer getMemoryMb() { return memoryMb; }
    @Override public Integer getLocalGb() { return localGb; }
    @Override public String getCpuInfo() { return cpuInfo; }
    @Override public String getServiceName() { return serviceName; }
    @Override public Boolean isReservable() { return reservable; }
    @Override public String getTrustId() { return trustId; }
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

    public static class BlazarReservableHostList extends ListResult<BlazarReservableHost> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("hosts")
        private List<BlazarReservableHost> list;

        @Override
        protected List<BlazarReservableHost> value() {
            return list;
        }
    }
}
