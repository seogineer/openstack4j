package org.openstack4j.model.network.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A VPN endpoint group (local subnets or peer CIDRs). Fields without a getter are in getAttributes(). */
public interface VpnEndpointGroup extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getType();
    List<String> getEndpoints();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
