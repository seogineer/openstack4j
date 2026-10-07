package org.openstack4j.model.dns.v2.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A pattern of zone names nobody but an admin may create. Fields without a getter are in getAttributes(). */
public interface Blacklist extends ModelEntity {
    String getId();
    String getPattern();
    String getDescription();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
