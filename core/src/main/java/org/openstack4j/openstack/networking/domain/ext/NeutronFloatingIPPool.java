package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.network.ext.FloatingIPPool;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronFloatingIPPool implements FloatingIPPool {

    private static final long serialVersionUID = 1L;

    @JsonProperty("subnet_id") private String subnetId;
    @JsonProperty("subnet_name") private String subnetName;
    @JsonProperty("network_id") private String networkId;
    @JsonProperty("project_id") private String projectId;

    @Override public String getSubnetId() { return subnetId; }
    @Override public String getSubnetName() { return subnetName; }
    @Override public String getNetworkId() { return networkId; }
    @Override public String getProjectId() { return projectId; }

    public static class FloatingIPPools extends ListResult<NeutronFloatingIPPool> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("floatingip_pools")
        private List<NeutronFloatingIPPool> list;

        @Override
        protected List<NeutronFloatingIPPool> value() {
            return list;
        }
    }
}
