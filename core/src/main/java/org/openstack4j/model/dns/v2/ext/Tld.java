package org.openstack4j.model.dns.v2.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A top-level domain zones may be created under (admin). Fields without a getter are in getAttributes(). */
public interface Tld extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
