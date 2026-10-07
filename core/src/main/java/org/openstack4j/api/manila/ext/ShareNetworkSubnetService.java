package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareNetworkSubnet;
import org.openstack4j.model.manila.ext.options.ShareNetworkSubnetCreate;

/** Subnets of a share network ({@code /v2/share-networks/{id}/subnets}, microversion 2.51). */
public interface ShareNetworkSubnetService extends RestService {

    /** @return the subnets of a share network; a missing share network raises */
    List<? extends ShareNetworkSubnet> list(String shareNetworkId);

    /** @return the subnet, or {@code null} when it does not exist */
    ShareNetworkSubnet get(String shareNetworkId, String subnetId);

    ShareNetworkSubnet create(String shareNetworkId, ShareNetworkSubnetCreate create);

    ActionResponse delete(String shareNetworkId, String subnetId);

    /** @return the subnet's metadata (2.78); a missing subnet raises */
    Map<String, String> getMetadata(String shareNetworkId, String subnetId);

    /** @return one metadata value, or {@code null} (2.78) */
    String getMetadataItem(String shareNetworkId, String subnetId, String key);

    /** Adds or changes metadata items (2.78) and returns all of them. */
    Map<String, String> setMetadata(String shareNetworkId, String subnetId, Map<String, String> metadata);

    /** Replaces all metadata (2.78) and returns it. */
    Map<String, String> replaceMetadata(String shareNetworkId, String subnetId, Map<String, String> metadata);

    ActionResponse deleteMetadataItem(String shareNetworkId, String subnetId, String key);
}
