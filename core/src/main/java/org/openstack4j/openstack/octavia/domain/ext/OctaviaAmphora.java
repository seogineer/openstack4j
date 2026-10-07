package org.openstack4j.openstack.octavia.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.octavia.ext.Amphora;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("amphora")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaviaAmphora implements Amphora {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("load_balancer_id") private String loadBalancerId;
    @JsonProperty("compute_id") private String computeId;
    @JsonProperty("lb_network_ip") private String lbNetworkIp;
    @JsonProperty("vrrp_ip") private String vrrpIp;
    @JsonProperty("ha_ip") private String haIp;
    @JsonProperty("vrrp_port_id") private String vrrpPortId;
    @JsonProperty("ha_port_id") private String haPortId;
    @JsonProperty("cert_expiration") private String certExpiration;
    @JsonProperty("role") private String role;
    @JsonProperty("status") private String status;
    @JsonProperty("vrrp_interface") private String vrrpInterface;
    @JsonProperty("vrrp_id") private Integer vrrpId;
    @JsonProperty("vrrp_priority") private Integer vrrpPriority;
    @JsonProperty("cached_zone") private String cachedZone;
    @JsonProperty("image_id") private String imageId;
    @JsonProperty("compute_flavor") private String computeFlavor;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @Override public String getId() { return id; }
    @Override public String getLoadBalancerId() { return loadBalancerId; }
    @Override public String getComputeId() { return computeId; }
    @Override public String getLbNetworkIp() { return lbNetworkIp; }
    @Override public String getVrrpIp() { return vrrpIp; }
    @Override public String getHaIp() { return haIp; }
    @Override public String getVrrpPortId() { return vrrpPortId; }
    @Override public String getHaPortId() { return haPortId; }
    @Override public String getCertExpiration() { return certExpiration; }
    @Override public String getRole() { return role; }
    @Override public String getStatus() { return status; }
    @Override public String getVrrpInterface() { return vrrpInterface; }
    @Override public Integer getVrrpId() { return vrrpId; }
    @Override public Integer getVrrpPriority() { return vrrpPriority; }
    @Override public String getCachedZone() { return cachedZone; }
    @Override public String getImageId() { return imageId; }
    @Override public String getComputeFlavor() { return computeFlavor; }
    @Override public String getCreatedAt() { return createdAt; }
    @Override public String getUpdatedAt() { return updatedAt; }

    public static class Amphorae extends ListResult<OctaviaAmphora> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("amphorae")
        private List<OctaviaAmphora> list;

        @Override
        protected List<OctaviaAmphora> value() {
            return list;
        }
    }
}
