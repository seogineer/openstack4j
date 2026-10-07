package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ShareNetworkSubnet;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("share_network_subnet")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaShareNetworkSubnet implements ShareNetworkSubnet {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("share_network_id") private String shareNetworkId;
    @JsonProperty("share_network_name") private String shareNetworkName;
    @JsonProperty("availability_zone") private String availabilityZone;
    @JsonProperty("neutron_net_id") private String neutronNetId;
    @JsonProperty("neutron_subnet_id") private String neutronSubnetId;
    @JsonProperty("network_type") private String networkType;
    @JsonProperty("segmentation_id") private Integer segmentationId;
    @JsonProperty("cidr") private String cidr;
    @JsonProperty("ip_version") private Integer ipVersion;
    @JsonProperty("gateway") private String gateway;
    @JsonProperty("mtu") private Integer mtu;
    @JsonProperty("metadata") private Map<String, Object> metadata;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getShareNetworkId() { return shareNetworkId; }
    @Override public String getShareNetworkName() { return shareNetworkName; }
    @Override public String getAvailabilityZone() { return availabilityZone; }
    @Override public String getNeutronNetId() { return neutronNetId; }
    @Override public String getNeutronSubnetId() { return neutronSubnetId; }
    @Override public String getNetworkType() { return networkType; }
    @Override public Integer getSegmentationId() { return segmentationId; }
    @Override public String getCidr() { return cidr; }
    @Override public Integer getIpVersion() { return ipVersion; }
    @Override public String getGateway() { return gateway; }
    @Override public Integer getMtu() { return mtu; }
    @Override public Map<String, Object> getMetadata() { return metadata; }
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

    public static class ManilaShareNetworkSubnetList extends ListResult<ManilaShareNetworkSubnet> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("share_network_subnets")
        private List<ManilaShareNetworkSubnet> list;

        @Override
        protected List<ManilaShareNetworkSubnet> value() {
            return list;
        }
    }
}
