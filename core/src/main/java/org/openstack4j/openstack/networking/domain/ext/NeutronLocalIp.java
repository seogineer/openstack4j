package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.LocalIp;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("local_ip")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronLocalIp implements LocalIp {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("local_port_id") private String localPortId;
    @JsonProperty("network_id") private String networkId;
    @JsonProperty("local_ip_address") private String localIpAddress;
    @JsonProperty("ip_mode") private String ipMode;
    @JsonProperty("revision_number") private Integer revisionNumber;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getProjectId() { return projectId; }
    @Override public String getLocalPortId() { return localPortId; }
    @Override public String getNetworkId() { return networkId; }
    @Override public String getLocalIpAddress() { return localIpAddress; }
    @Override public String getIpMode() { return ipMode; }
    @Override public Integer getRevisionNumber() { return revisionNumber; }

    public static class LocalIps extends ListResult<NeutronLocalIp> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("local_ips")
        private List<NeutronLocalIp> list;

        @Override
        protected List<NeutronLocalIp> value() {
            return list;
        }
    }
}
