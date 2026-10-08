package org.openstack4j.model.instanceha;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A failure notification (host, process or VM) and its recovery. Fields without a getter are in getAttributes(). */
public interface Notification extends ModelEntity {
    String getNotificationUuid();
    String getType();
    String getStatus();
    String getSourceHostUuid();
    String getGeneratedTime();
    Map<String, Object> getPayload();
    List<Map<String, Object>> getRecoveryWorkflowDetails();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
