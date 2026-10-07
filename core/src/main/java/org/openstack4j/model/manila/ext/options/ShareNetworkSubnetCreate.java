package org.openstack4j.model.manila.ext.options;

import java.util.Map;

/** The body of {@code POST /v2/share-networks/{id}/subnets} (microversion 2.51). */
public final class ShareNetworkSubnetCreate extends ManilaAttributes<ShareNetworkSubnetCreate> {

    private ShareNetworkSubnetCreate() {
    }

    public static ShareNetworkSubnetCreate create() {
        return new ShareNetworkSubnetCreate();
    }

    @Override
    protected ShareNetworkSubnetCreate self() {
        return this;
    }

    public ShareNetworkSubnetCreate neutronNetId(String neutronNetId) {
        return put("neutron_net_id", neutronNetId);
    }

    public ShareNetworkSubnetCreate neutronSubnetId(String neutronSubnetId) {
        return put("neutron_subnet_id", neutronSubnetId);
    }

    /** {@code null} makes the subnet the default one of the share network. */
    public ShareNetworkSubnetCreate availabilityZone(String availabilityZone) {
        return put("availability_zone", availabilityZone);
    }

    /** Needs microversion 2.78. */
    public ShareNetworkSubnetCreate metadata(Map<String, String> metadata) {
        return put("metadata", metadata);
    }
}
