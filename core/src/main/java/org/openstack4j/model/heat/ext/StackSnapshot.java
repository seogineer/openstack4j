package org.openstack4j.model.heat.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A stack snapshot. */
public interface StackSnapshot extends ModelEntity {
    String getId();
    String getName();
    String getAction();
    String getStatus();
    String getStatusReason();
    String getCreationTime();
    Map<String, Object> getData();
}
