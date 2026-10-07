package org.openstack4j.model.octavia.ext;

import java.util.List;

import org.openstack4j.model.ModelEntity;

/** An Octavia L7 rule: one condition of an L7 policy. */
public interface L7Rule extends ModelEntity {
    String getId();
    String getType();
    String getCompareType();
    String getKey();
    String getValue();
    Boolean isInvert();
    Boolean isAdminStateUp();
    String getProvisioningStatus();
    String getOperatingStatus();
    String getProjectId();
    List<String> getTags();
}
