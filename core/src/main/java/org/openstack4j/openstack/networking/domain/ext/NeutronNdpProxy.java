package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.NdpProxy;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("ndp_proxy")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronNdpProxy implements NdpProxy {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("router_id") private String routerId;
    @JsonProperty("port_id") private String portId;
    @JsonProperty("ip_address") private String ipAddress;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("revision_number") private Integer revisionNumber;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getRouterId() { return routerId; }
    @Override public String getPortId() { return portId; }
    @Override public String getIpAddress() { return ipAddress; }
    @Override public String getProjectId() { return projectId; }
    @Override public Integer getRevisionNumber() { return revisionNumber; }

    public static class NdpProxies extends ListResult<NeutronNdpProxy> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("ndp_proxies")
        private List<NeutronNdpProxy> list;

        @Override
        protected List<NeutronNdpProxy> value() {
            return list;
        }
    }
}
