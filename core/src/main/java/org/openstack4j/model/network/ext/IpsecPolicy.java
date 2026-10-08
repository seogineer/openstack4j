package org.openstack4j.model.network.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An IPsec policy (phase 2) of IPsec site connections. Fields without a getter are in getAttributes(). */
public interface IpsecPolicy extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getTransformProtocol();
    String getAuthAlgorithm();
    String getEncapsulationMode();
    String getEncryptionAlgorithm();
    String getPfs();
    Map<String, Object> getLifetime();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
