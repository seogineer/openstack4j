package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.QosMinimumBandwidthRule;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("minimum_bandwidth_rule")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronQosMinimumBandwidthRule implements QosMinimumBandwidthRule {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("min_kbps") private Long minKbps;
    @JsonProperty("direction") private String direction;
    @JsonProperty("qos_policy_id") private String qosPolicyId;

    @Override public String getId() { return id; }
    @Override public Long getMinKbps() { return minKbps; }
    @Override public String getDirection() { return direction; }
    @Override public String getQosPolicyId() { return qosPolicyId; }

    public static class Rules extends ListResult<NeutronQosMinimumBandwidthRule> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("minimum_bandwidth_rules")
        private List<NeutronQosMinimumBandwidthRule> list;

        @Override
        protected List<NeutronQosMinimumBandwidthRule> value() {
            return list;
        }
    }
}
