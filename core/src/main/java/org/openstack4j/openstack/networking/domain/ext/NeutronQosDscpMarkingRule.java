package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.QosDscpMarkingRule;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("dscp_marking_rule")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronQosDscpMarkingRule implements QosDscpMarkingRule {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("dscp_mark") private Integer dscpMark;
    @JsonProperty("qos_policy_id") private String qosPolicyId;

    @Override public String getId() { return id; }
    @Override public Integer getDscpMark() { return dscpMark; }
    @Override public String getQosPolicyId() { return qosPolicyId; }

    public static class Rules extends ListResult<NeutronQosDscpMarkingRule> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("dscp_marking_rules")
        private List<NeutronQosDscpMarkingRule> list;

        @Override
        protected List<NeutronQosDscpMarkingRule> value() {
            return list;
        }
    }
}
