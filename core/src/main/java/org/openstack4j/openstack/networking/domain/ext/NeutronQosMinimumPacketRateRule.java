package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.QosMinimumPacketRateRule;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("minimum_packet_rate_rule")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronQosMinimumPacketRateRule implements QosMinimumPacketRateRule {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("min_kpps") private Long minKpps;
    @JsonProperty("direction") private String direction;
    @JsonProperty("qos_policy_id") private String qosPolicyId;

    @Override public String getId() { return id; }
    @Override public Long getMinKpps() { return minKpps; }
    @Override public String getDirection() { return direction; }
    @Override public String getQosPolicyId() { return qosPolicyId; }

    public static class Rules extends ListResult<NeutronQosMinimumPacketRateRule> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("minimum_packet_rate_rules")
        private List<NeutronQosMinimumPacketRateRule> list;

        @Override
        protected List<NeutronQosMinimumPacketRateRule> value() {
            return list;
        }
    }
}
