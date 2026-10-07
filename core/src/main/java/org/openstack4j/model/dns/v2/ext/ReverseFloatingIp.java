package org.openstack4j.model.dns.v2.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** The PTR record of a floating IP ({@code id} is {@code <region>:<floating IP id>}). Fields without a getter are in getAttributes(). */
public interface ReverseFloatingIp extends ModelEntity {
    String getId();
    String getPtrdname();
    String getDescription();
    Integer getTtl();
    String getAddress();
    String getStatus();
    String getAction();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
