package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A subnet of a share network (one per availability zone). Fields without a getter are in getAttributes(). */
public interface ShareNetworkSubnet extends ModelEntity {
    String getId();
    String getShareNetworkId();
    String getShareNetworkName();
    String getAvailabilityZone();
    String getNeutronNetId();
    String getNeutronSubnetId();
    String getNetworkType();
    Integer getSegmentationId();
    String getCidr();
    Integer getIpVersion();
    String getGateway();
    Integer getMtu();
    Map<String, Object> getMetadata();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
