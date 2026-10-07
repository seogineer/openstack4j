package org.openstack4j.openstack.manila.internal.ext;

import static org.openstack4j.openstack.manila.internal.ManilaMicroVersions.V;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.manila.ext.ShareNetworkSubnetService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareNetworkSubnet;
import org.openstack4j.model.manila.ext.options.ShareNetworkSubnetCreate;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareNetworkSubnet;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareNetworkSubnet.ManilaShareNetworkSubnetList;

public class ShareNetworkSubnetServiceImpl extends BaseManilaExtService implements ShareNetworkSubnetService {

    private static final MicroVersion FLOOR = V(51);
    private static final MicroVersion METADATA = V(78);

    private static String subnets(String shareNetworkId) {
        return "/share-networks/" + id(shareNetworkId) + "/subnets";
    }

    private static String subnet(String shareNetworkId, String subnetId) {
        return subnets(shareNetworkId) + "/" + id(subnetId);
    }

    @Override
    public List<? extends ShareNetworkSubnet> list(String shareNetworkId) {
        return listOf(FLOOR, ManilaShareNetworkSubnetList.class, subnets(shareNetworkId), null);
    }

    @Override
    public ShareNetworkSubnet get(String shareNetworkId, String subnetId) {
        return show(FLOOR, ManilaShareNetworkSubnet.class, subnet(shareNetworkId, subnetId));
    }

    @Override
    public ShareNetworkSubnet create(String shareNetworkId, ShareNetworkSubnetCreate create) {
        Map<String, Object> fields = Objects.requireNonNull(create, "create").toMap();
        String path = subnets(shareNetworkId);
        // Manila reads this body under a hyphenated root
        return at(fields.containsKey("metadata") ? METADATA : FLOOR, post(ManilaShareNetworkSubnet.class, path), path)
                .entity(JsonBody.of("share-network-subnet", fields)).execute(propagate404());
    }

    @Override
    public ActionResponse delete(String shareNetworkId, String subnetId) {
        return remove(FLOOR, subnet(shareNetworkId, subnetId));
    }

    @Override
    public Map<String, String> getMetadata(String shareNetworkId, String subnetId) {
        return metadataOf(METADATA, subnet(shareNetworkId, subnetId) + "/metadata");
    }

    @Override
    public String getMetadataItem(String shareNetworkId, String subnetId, String key) {
        return metadataItem(METADATA, subnet(shareNetworkId, subnetId) + "/metadata", key);
    }

    @Override
    public Map<String, String> setMetadata(String shareNetworkId, String subnetId, Map<String, String> metadata) {
        return writeMetadata(METADATA, subnet(shareNetworkId, subnetId) + "/metadata", metadata, false);
    }

    @Override
    public Map<String, String> replaceMetadata(String shareNetworkId, String subnetId, Map<String, String> metadata) {
        return writeMetadata(METADATA, subnet(shareNetworkId, subnetId) + "/metadata", metadata, true);
    }

    @Override
    public ActionResponse deleteMetadataItem(String shareNetworkId, String subnetId, String key) {
        return remove(METADATA, subnet(shareNetworkId, subnetId) + "/metadata/" + id(key));
    }
}
