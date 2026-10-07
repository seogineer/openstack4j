package org.openstack4j.model.dns.v2.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** The status of a Designate service process. Fields without a getter are in getAttributes(). */
public interface ServiceStatus extends ModelEntity {
    String getId();
    String getHostname();
    String getServiceName();
    String getStatus();
    Map<String, Object> getStats();
    Map<String, Object> getCapabilities();
    String getHeartbeatedAt();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
