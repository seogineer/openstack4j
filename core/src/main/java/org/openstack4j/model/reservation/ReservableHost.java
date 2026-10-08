package org.openstack4j.model.reservation;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A compute host in the reservation pool; extra capabilities are in getAttributes(). Fields without a getter are in getAttributes(). */
public interface ReservableHost extends ModelEntity {
    String getId();
    String getHypervisorHostname();
    String getHypervisorType();
    Long getHypervisorVersion();
    Integer getVcpus();
    Integer getMemoryMb();
    Integer getLocalGb();
    String getCpuInfo();
    String getServiceName();
    Boolean isReservable();
    String getTrustId();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
