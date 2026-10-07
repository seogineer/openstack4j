package org.openstack4j.model.octavia.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An Octavia L7 policy: an action taken when its rules match a request. */
public interface L7Policy extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getListenerId();
    String getAction();
    Integer getPosition();
    String getRedirectPoolId();
    String getRedirectUrl();
    String getRedirectPrefix();
    Integer getRedirectHttpCode();
    Boolean isAdminStateUp();
    String getProvisioningStatus();
    String getOperatingStatus();
    String getProjectId();
    List<Map<String, Object>> getRules();
    List<String> getTags();
}
