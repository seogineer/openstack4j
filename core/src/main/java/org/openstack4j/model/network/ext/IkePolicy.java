package org.openstack4j.model.network.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An IKE policy (phase 1) of IPsec site connections. Fields without a getter are in getAttributes(). */
public interface IkePolicy extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getAuthAlgorithm();
    String getEncryptionAlgorithm();
    String getPfs();
    String getPhase1NegotiationMode();
    String getIkeVersion();
    Map<String, Object> getLifetime();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
