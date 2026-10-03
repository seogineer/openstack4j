package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.QosPacketRateLimitRule;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("packet_rate_limit_rule")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronQosPacketRateLimitRule implements QosPacketRateLimitRule {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("max_kpps") private Long maxKpps;
    @JsonProperty("max_burst_kpps") private Long maxBurstKpps;
    @JsonProperty("direction") private String direction;
    @JsonProperty("qos_policy_id") private String qosPolicyId;

    @Override public String getId() { return id; }
    @Override public Long getMaxKpps() { return maxKpps; }
    @Override public Long getMaxBurstKpps() { return maxBurstKpps; }
    @Override public String getDirection() { return direction; }
    @Override public String getQosPolicyId() { return qosPolicyId; }

    public static class Rules extends ListResult<NeutronQosPacketRateLimitRule> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("packet_rate_limit_rules")
        private List<NeutronQosPacketRateLimitRule> list;

        @Override
        protected List<NeutronQosPacketRateLimitRule> value() {
            return list;
        }
    }
}
