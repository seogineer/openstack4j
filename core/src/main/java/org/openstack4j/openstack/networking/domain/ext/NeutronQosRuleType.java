package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.QosRuleType;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("rule_type")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronQosRuleType implements QosRuleType {

    private static final long serialVersionUID = 1L;

    @JsonProperty("type") private String type;
    @JsonProperty("drivers") private List<Map<String, Object>> drivers;

    @Override public String getType() { return type; }
    @Override public List<Map<String, Object>> getDrivers() { return drivers; }

    public static class RuleTypes extends ListResult<NeutronQosRuleType> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("rule_types")
        private List<NeutronQosRuleType> list;

        @Override
        protected List<NeutronQosRuleType> value() {
            return list;
        }
    }
}
