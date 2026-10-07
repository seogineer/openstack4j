package org.openstack4j.model.dns.v2.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A TSIG key that signs zone transfers with the back-end servers (admin). Fields without a getter are in getAttributes(). */
public interface TsigKey extends ModelEntity {
    String getId();
    String getName();
    String getAlgorithm();
    String getSecret();
    String getScope();
    String getResourceId();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
