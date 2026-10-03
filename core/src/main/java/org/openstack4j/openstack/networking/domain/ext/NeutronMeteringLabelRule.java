package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.MeteringLabelRule;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("metering_label_rule")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronMeteringLabelRule implements MeteringLabelRule {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("metering_label_id") private String meteringLabelId;
    @JsonProperty("direction") private String direction;
    @JsonProperty("remote_ip_prefix") private String remoteIpPrefix;
    @JsonProperty("source_ip_prefix") private String sourceIpPrefix;
    @JsonProperty("destination_ip_prefix") private String destinationIpPrefix;
    @JsonProperty("excluded") private Boolean excluded;

    @Override public String getId() { return id; }
    @Override public String getMeteringLabelId() { return meteringLabelId; }
    @Override public String getDirection() { return direction; }
    @Override public String getRemoteIpPrefix() { return remoteIpPrefix; }
    @Override public String getSourceIpPrefix() { return sourceIpPrefix; }
    @Override public String getDestinationIpPrefix() { return destinationIpPrefix; }
    @Override public Boolean isExcluded() { return excluded; }

    public static class MeteringLabelRules extends ListResult<NeutronMeteringLabelRule> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("metering_label_rules")
        private List<NeutronMeteringLabelRule> list;

        @Override
        protected List<NeutronMeteringLabelRule> value() {
            return list;
        }
    }
}
