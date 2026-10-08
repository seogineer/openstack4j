package org.openstack4j.model.reservation;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A floating IP in the reservation pool. Fields without a getter are in getAttributes(). */
public interface ReservableFloatingIp extends ModelEntity {
    String getId();
    String getFloatingNetworkId();
    String getFloatingIpAddress();
    String getSubnetId();
    Boolean isReservable();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
