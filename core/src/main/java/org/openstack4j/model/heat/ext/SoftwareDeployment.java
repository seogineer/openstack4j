package org.openstack4j.model.heat.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A software deployment: a software config applied to a server. */
public interface SoftwareDeployment extends ModelEntity {
    String getId();
    String getServerId();
    String getConfigId();
    String getAction();
    String getStatus();
    String getStatusReason();
    Map<String, Object> getInputValues();
    Map<String, Object> getOutputValues();
    String getCreationTime();
    String getUpdatedTime();
}
