package org.openstack4j.model.network.options;

/** Body of a QoS rule create or update; one class for every rule kind (Neutron validates the fields per kind). */
public class QosRuleOptions extends NeutronAttributes<QosRuleOptions> {

    public static QosRuleOptions dscpMarking(int dscpMark) { return new QosRuleOptions().dscpMark(dscpMark); }
    public static QosRuleOptions minimumBandwidth(long minKbps) { return new QosRuleOptions().minKbps(minKbps); }
    public static QosRuleOptions minimumPacketRate(long minKpps) { return new QosRuleOptions().minKpps(minKpps); }
    public static QosRuleOptions packetRateLimit(long maxKpps) { return new QosRuleOptions().maxKpps(maxKpps); }
    /** An update that sends only the fields set afterwards. */
    public static QosRuleOptions update() { return new QosRuleOptions(); }

    @Override
    protected QosRuleOptions self() {
        return this;
    }

    public QosRuleOptions dscpMark(Integer dscpMark) { return put("dscp_mark", dscpMark); }
    public QosRuleOptions minKbps(Long minKbps) { return put("min_kbps", minKbps); }
    public QosRuleOptions minKpps(Long minKpps) { return put("min_kpps", minKpps); }
    public QosRuleOptions maxKpps(Long maxKpps) { return put("max_kpps", maxKpps); }
    public QosRuleOptions maxBurstKpps(Long maxBurstKpps) { return put("max_burst_kpps", maxBurstKpps); }
    /** For the alias bandwidth limit rule. */
    public QosRuleOptions maxKbps(Long maxKbps) { return put("max_kbps", maxKbps); }
    public QosRuleOptions maxBurstKbps(Long maxBurstKbps) { return put("max_burst_kbps", maxBurstKbps); }
    /** egress, ingress or any (packet rate rules). */
    public QosRuleOptions direction(String direction) { return put("direction", direction); }
}
