package org.openstack4j.model.reservation;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A lease: reservations of hosts, instances or floating IPs for a time window. Fields without a getter are in getAttributes(). */
public interface Lease extends ModelEntity {
    String getId();
    String getName();
    String getStartDate();
    String getEndDate();
    String getStatus();
    Boolean isDegraded();
    String getUserId();
    String getProjectId();
    String getTrustId();
    List<Map<String, Object>> getReservations();
    List<Map<String, Object>> getEvents();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
