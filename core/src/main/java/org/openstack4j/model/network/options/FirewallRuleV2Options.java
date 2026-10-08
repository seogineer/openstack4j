package org.openstack4j.model.network.options;


/** Body of a firewall rule create or update; only the fields set are sent. */
public class FirewallRuleV2Options extends NeutronAttributes<FirewallRuleV2Options> {

    public static FirewallRuleV2Options create() {
        return new FirewallRuleV2Options();
    }

    /** An update that sends only the fields set afterwards. */
    public static FirewallRuleV2Options update() {
        return new FirewallRuleV2Options();
    }

    @Override
    protected FirewallRuleV2Options self() {
        return this;
    }

    public FirewallRuleV2Options name(String value) {
        return put("name", value);
    }

    public FirewallRuleV2Options description(String value) {
        return put("description", value);
    }

    /** {@code allow}, {@code deny} or {@code reject}. */
    public FirewallRuleV2Options action(String value) {
        return put("action", value);
    }

    /** {@code tcp}, {@code udp}, {@code icmp}, a number, or {@code null} for any. */
    public FirewallRuleV2Options protocol(String value) {
        return put("protocol", value);
    }

    public FirewallRuleV2Options ipVersion(Integer value) {
        return put("ip_version", value);
    }

    public FirewallRuleV2Options sourceIpAddress(String value) {
        return put("source_ip_address", value);
    }

    public FirewallRuleV2Options destinationIpAddress(String value) {
        return put("destination_ip_address", value);
    }

    /** A port or a range {@code 80:90}. */
    public FirewallRuleV2Options sourcePort(String value) {
        return put("source_port", value);
    }

    public FirewallRuleV2Options destinationPort(String value) {
        return put("destination_port", value);
    }

    public FirewallRuleV2Options sourceFirewallGroupId(String value) {
        return put("source_firewall_group_id", value);
    }

    public FirewallRuleV2Options destinationFirewallGroupId(String value) {
        return put("destination_firewall_group_id", value);
    }

    public FirewallRuleV2Options enabled(Boolean value) {
        return put("enabled", value);
    }

    public FirewallRuleV2Options shared(Boolean value) {
        return put("shared", value);
    }
}
