package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a metering label rule create. */
public class MeteringLabelRuleOptions extends NeutronAttributes<MeteringLabelRuleOptions> {

    public static MeteringLabelRuleOptions create(String meteringLabelId, String direction) {
        return new MeteringLabelRuleOptions().put("metering_label_id", Objects.requireNonNull(meteringLabelId)).put("direction", Objects.requireNonNull(direction));
    }

    @Override
    protected MeteringLabelRuleOptions self() {
        return this;
    }

    public MeteringLabelRuleOptions remoteIpPrefix(String value) { return put("remote_ip_prefix", value); }
    public MeteringLabelRuleOptions sourceIpPrefix(String value) { return put("source_ip_prefix", value); }
    public MeteringLabelRuleOptions destinationIpPrefix(String value) { return put("destination_ip_prefix", value); }
    public MeteringLabelRuleOptions excluded(Boolean value) { return put("excluded", value); }
}
